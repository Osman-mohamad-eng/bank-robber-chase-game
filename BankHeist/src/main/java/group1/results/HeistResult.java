package group1.results;

import java.util.UUID;

/** Immutable telemetry captured before the board can reset. Times use ISO-8601 UTC. */
public record HeistResult(UUID sessionId, UUID playerId, int level, long cashCollected,
        int diamondsCollected, long penaltiesIncurred, long finalScore,
        long completionTimeMs, String outcome, String completedAt) { }
