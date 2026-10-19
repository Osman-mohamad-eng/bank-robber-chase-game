package dev.bankheist.api.heist;

import java.time.Instant;
import java.util.UUID;

public record HeistView(UUID sessionId, UUID playerId, String username, int level, long cashCollected,
        int diamondsCollected, long penaltiesIncurred, long finalScore, long completionTimeMs,
        Outcome outcome, Instant completedAt, Instant receivedAt) {
    public static HeistView from(HeistSession session, String username) {
        return new HeistView(session.getSessionId(), session.getPlayerId(), username, session.getLevel(),
                session.getCashCollected(), session.getDiamondsCollected(), session.getPenaltiesIncurred(),
                session.getFinalScore(), session.getCompletionTimeMs(), session.getOutcome(),
                session.getCompletedAt(), session.getReceivedAt());
    }
}
