package dev.bankheist.api.leaderboard;

import dev.bankheist.api.heist.Outcome;
import java.time.Instant;
import java.util.UUID;

public record LeaderboardEntry(long rank, UUID playerId, String username, UUID sessionId, int level,
        long cashCollected, int diamondsCollected, long finalScore, long completionTimeMs,
        Outcome outcome, Instant completedAt) {}
