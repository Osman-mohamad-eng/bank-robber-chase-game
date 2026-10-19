package group1.results;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PendingResultStoreTest {
    @TempDir Path directory;

    @Test
    void survivesRestartAndRemovesOnlyAcknowledgedAttempts() throws Exception {
        var result = result();
        var store = new PendingResultStore(directory);
        store.save(result);
        store.save(result);
        var restarted = new PendingResultStore(directory);
        assertEquals(java.util.List.of(result), restarted.pending());
        assertThrows(java.io.IOException.class, () -> restarted.save(new HeistResult(result.sessionId(),
                result.playerId(), 1, 200, 0, 0, 200, 5000, "WON", result.completedAt())));
        restarted.acknowledge(result);
        assertTrue(restarted.pending().isEmpty());
    }

    @Test
    void preservesPermanentFailuresForInspectionWithoutRetryingThem() throws Exception {
        var store = new PendingResultStore(directory);
        var result = result();
        store.save(result);
        store.reject(result, "HTTP 409: conflicting attempt");
        assertTrue(store.pending().isEmpty());
        assertTrue(Files.exists(directory.resolve("failed").resolve(result.sessionId() + ".json")));
        Files.writeString(directory.resolve("broken.json"), "invalid JSON");
        assertTrue(store.pending().isEmpty());
        assertTrue(Files.exists(directory.resolve("failed/broken.json")));
    }

    @Test
    void loadsOnePersistentPlayerIdentity() throws Exception {
        var first = PlayerProfile.loadOrCreate(directory, "Alice_1");
        assertEquals(first, PlayerProfile.loadOrCreate(directory, "Ignored_Name"));
    }

    private static HeistResult result() {
        return new HeistResult(UUID.randomUUID(), UUID.randomUUID(), 1, 100, 0, 0, 100,
                5000, "WON", Instant.now().toString());
    }
}
