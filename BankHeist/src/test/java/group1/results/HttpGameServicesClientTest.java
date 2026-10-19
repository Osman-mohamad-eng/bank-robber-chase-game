package group1.results;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HttpGameServicesClientTest {
    @Test
    void sendsRegistrationAndFullSnapshotAndReadsServerData() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> submitted = new AtomicReference<>();
        server.createContext("/api/v1/players", exchange -> {
            byte[] body = "{\"gamesPlayed\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/v1/heists", exchange -> {
            submitted.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
        try {
            HttpGameServicesClient client = client(server);
            PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Test_Player");
            client.register(profile);
            HeistResult result = new HeistResult(UUID.randomUUID(), profile.playerId(), 3, 100,
                    0, 150, -50, 1234, "LOST", Instant.now().toString());
            client.submit(result);
            var payload = JsonParser.parseString(submitted.get()).getAsJsonObject();
            assertEquals(result.sessionId().toString(), payload.get("sessionId").getAsString());
            assertEquals(-50, payload.get("finalScore").getAsLong());
            assertEquals(100, payload.get("cashCollected").getAsLong());
            assertEquals(1, client.get("/players/" + profile.playerId() + "/stats").get().get("gamesPlayed").getAsInt());
        } finally { server.stop(0); }
    }

    @Test
    void distinguishesPermanentConflictsFromTransientFailures() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/players", exchange -> {
            exchange.sendResponseHeaders(409, -1);
            exchange.close();
        });
        server.start();
        try {
            var failure = assertThrows(HttpGameServicesClient.ApiException.class,
                    () -> client(server).register(new PlayerProfile(UUID.randomUUID(), "Existing_Name")));
            assertEquals(409, failure.status());
            assertFalse(failure.retryable());
            assertTrue(new HttpGameServicesClient.ApiException(503, "unavailable").retryable());
        } finally { server.stop(0); }
    }

    private static HttpGameServicesClient client(HttpServer server) {
        return new HttpGameServicesClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
    }
}
