package dev.bankheist.api.player;

import dev.bankheist.api.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
    private final PlayerRepository players;
    private final Clock clock;

    public PlayerService(PlayerRepository players, Clock clock) {
        this.players = players;
        this.clock = clock;
    }

    @Transactional
    public Registration register(UUID requestedId, String username) {
        String normalized = username.toLowerCase(Locale.ROOT);
        UUID candidateId = requestedId == null ? UUID.randomUUID() : requestedId;
        int inserted = players.register(candidateId, username, normalized, clock.instant());
        Player player;
        if (requestedId == null) {
            player = players.findByNormalizedUsername(normalized).orElseThrow();
        } else {
            player = players.findById(requestedId)
                    .orElseThrow(() -> ApiException.conflict("Username already belongs to another player"));
            if (!player.getUsername().toLowerCase(Locale.ROOT).equals(normalized)) {
                throw ApiException.conflict("Player ID already belongs to another username");
            }
        }
        return new Registration(view(player), inserted == 1);
    }

    @Transactional(readOnly = true)
    public Player requirePlayer(UUID id) {
        return players.findById(id).orElseThrow(() -> ApiException.notFound("Player not found"));
    }

    public static PlayerView view(Player player) {
        return new PlayerView(player.getId(), player.getUsername(), player.getCreatedAt(), player.getRewardPoints());
    }

    public record PlayerView(UUID id, String username, Instant createdAt, long rewardPoints) {}
    public record Registration(PlayerView player, boolean created) {}
}
