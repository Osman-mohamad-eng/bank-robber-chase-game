package group1.results;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** One atomically written file per attempt. Acknowledged results alone are deleted. */
public final class PendingResultStore {
    private final Path directory;
    private final Gson gson = new Gson();

    public PendingResultStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    public synchronized void save(HeistResult result) throws IOException {
        Path destination = file(result);
        if (Files.exists(destination)) {
            if (!result.equals(gson.fromJson(Files.readString(destination), HeistResult.class))) {
                throw new IOException("Attempt already has a different pending result: " + result.sessionId());
            }
            return;
        }
        Path temporary = Files.createTempFile(directory, "pending-", ".tmp");
        try {
            Files.writeString(temporary, gson.toJson(result));
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, destination); }
        } finally { Files.deleteIfExists(temporary); }
    }

    public synchronized List<HeistResult> pending() throws IOException {
        List<HeistResult> results = new ArrayList<>();
        List<Path> files;
        try (var paths = Files.list(directory)) {
            files = paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        for (Path file : files) {
            try {
                HeistResult result = gson.fromJson(Files.readString(file), HeistResult.class);
                if (result == null || result.sessionId() == null || result.playerId() == null
                        || !file.getFileName().toString().equals(result.sessionId() + ".json")) {
                    throw new IllegalArgumentException("Invalid pending result identity");
                }
                results.add(result);
            } catch (RuntimeException malformed) {
                quarantine(file, "Unreadable pending result: " + malformed.getMessage());
            }
        }
        return List.copyOf(results);
    }

    public synchronized void acknowledge(HeistResult result) throws IOException {
        Files.deleteIfExists(file(result));
    }

    public synchronized void reject(HeistResult result, String reason) throws IOException {
        quarantine(file(result), reason);
    }

    private void quarantine(Path file, String reason) throws IOException {
        Path failed = directory.resolve("failed");
        Files.createDirectories(failed);
        Files.move(file, failed.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(failed.resolve(file.getFileName() + ".error.txt"), reason);
    }

    private Path file(HeistResult result) { return directory.resolve(result.sessionId() + ".json"); }
}
