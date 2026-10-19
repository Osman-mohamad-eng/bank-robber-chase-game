package group1.results;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AsyncResultReporterTest {
    @TempDir Path directory;

    @Test
    void retriesTransientFailureUsingIdenticalAttemptOffSwingThread() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> firstPayload = new AtomicReference<>();
        AtomicReference<String> secondPayload = new AtomicReference<>();
        CountDownLatch received = new CountDownLatch(2);
        server.createContext("/api/v1/players", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/api/v1/heists", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            int call = calls.incrementAndGet();
            if (call == 1) firstPayload.set(body); else secondPayload.set(body);
            exchange.sendResponseHeaders(call == 1 ? 503 : 200, -1);
            exchange.close();
            received.countDown();
        });
        server.start();
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Retry_Player");
        PendingResultStore store = new PendingResultStore(directory);
        HttpGameServicesClient api = new HttpGameServicesClient(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
        HeistResult result = new HeistResult(UUID.randomUUID(), profile.playerId(), 1, 100, 0, 0, 100,
                5000, "WON", Instant.now().toString());
        try (AsyncResultReporter reporter = new AsyncResultReporter(api, profile, store, Duration.ofMillis(50))) {
            SwingUtilities.invokeAndWait(() -> reporter.submit(result));
            assertTrue(received.await(5, TimeUnit.SECONDS), "A failed result should be retried");
            // Wait for the worker's acknowledgement after the HTTP response has been read.
            reporter.drain();
            assertTrue(store.pending().isEmpty());
            assertEquals(firstPayload.get(), secondPayload.get());
            assertEquals(result.sessionId().toString(), JsonParser.parseString(secondPayload.get())
                    .getAsJsonObject().get("sessionId").getAsString());
            assertEquals(2, calls.get());
        } finally { server.stop(0); }
    }

    @Test
    void shutdownPreservesQueuedResultWhenServicesAreUnavailable() throws Exception {
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Offline_Player");
        PendingResultStore store = new PendingResultStore(directory);
        HttpGameServicesClient api = new HttpGameServicesClient(URI.create("http://127.0.0.1:1"));
        HeistResult result = new HeistResult(UUID.randomUUID(), profile.playerId(), 1, 0, 0, 150, -150,
                5000, "LOST", Instant.now().toString());
        AsyncResultReporter reporter = new AsyncResultReporter(api, profile, store);
        reporter.submit(result);
        reporter.close();
        assertEquals(java.util.List.of(result), new PendingResultStore(directory).pending());
    }

    @Test
    void permanentRegistrationConflictPreservesResultWithoutEndlessRetries() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger registrations = new AtomicInteger();
        server.createContext("/api/v1/players", exchange -> {
            registrations.incrementAndGet();
            exchange.sendResponseHeaders(409, -1);
            exchange.close();
        });
        server.start();
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Taken_Name");
        PendingResultStore store = new PendingResultStore(directory);
        HeistResult result = new HeistResult(UUID.randomUUID(), profile.playerId(), 1, 0, 0, 150, -150,
                5000, "LOST", Instant.now().toString());
        HttpGameServicesClient api = new HttpGameServicesClient(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
        try (AsyncResultReporter reporter = new AsyncResultReporter(api, profile, store, Duration.ofMillis(50))) {
            reporter.submit(result);
            reporter.drain();
            reporter.drain();
            assertEquals(1, registrations.get());
            assertTrue(store.pending().isEmpty());
            assertTrue(java.nio.file.Files.exists(directory.resolve("failed/" + result.sessionId() + ".json")));
        } finally { server.stop(0); }
    }
}
