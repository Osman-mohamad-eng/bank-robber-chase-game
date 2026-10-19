package group1.results;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Local identity is independent of gameplay entities and works before registration. */
public record PlayerProfile(UUID playerId, String username) {
    public PlayerProfile {
        if (playerId == null || username == null || !username.matches("[A-Za-z0-9_-]{3,32}")) {
            throw new IllegalArgumentException("Username must contain 3–32 letters, digits, underscores or hyphens.");
        }
    }

    public static PlayerProfile loadOrCreate(Path directory, String preferredUsername) throws IOException {
        Gson gson = new Gson();
        Path file = directory.resolve("profile.json");
        if (Files.exists(file)) {
            return gson.fromJson(Files.readString(file), PlayerProfile.class);
        }
        UUID id = UUID.randomUUID();
        String name = preferredUsername == null || preferredUsername.isBlank()
                ? "Player_" + id.toString().substring(0, 8) : preferredUsername.trim();
        PlayerProfile profile = new PlayerProfile(id, name);
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "profile-", ".tmp");
        try {
            Files.writeString(temporary, gson.toJson(profile));
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file); }
        } finally { Files.deleteIfExists(temporary); }
        return profile;
    }
}
