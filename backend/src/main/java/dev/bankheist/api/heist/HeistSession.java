package dev.bankheist.api.heist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "heist_sessions")
public class HeistSession {
    @Id
    @Column(name = "session_id")
    private UUID sessionId;
    @Column(name = "player_id", nullable = false)
    private UUID playerId;
    @Column(nullable = false)
    private int level;
    @Column(name = "cash_collected", nullable = false)
    private long cashCollected;
    @Column(name = "diamonds_collected", nullable = false)
    private int diamondsCollected;
    @Column(name = "penalties_incurred", nullable = false)
    private long penaltiesIncurred;
    @Column(name = "final_score", nullable = false)
    private long finalScore;
    @Column(name = "completion_time_ms", nullable = false)
    private long completionTimeMs;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Outcome outcome;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected HeistSession() {}

    public HeistSession(SubmitHeist request, Instant receivedAt) {
        sessionId = request.sessionId();
        playerId = request.playerId();
        level = request.level();
        cashCollected = request.cashCollected();
        diamondsCollected = request.diamondsCollected();
        penaltiesIncurred = request.penaltiesIncurred();
        finalScore = request.finalScore();
        completionTimeMs = request.completionTimeMs();
        outcome = request.outcome();
        // PostgreSQL timestamps have microsecond precision; canonicalize once for reliable retries.
        completedAt = request.completedAt().truncatedTo(ChronoUnit.MICROS);
        this.receivedAt = receivedAt.truncatedTo(ChronoUnit.MICROS);
    }

    public boolean matches(SubmitHeist request) {
        return sessionId.equals(request.sessionId()) && playerId.equals(request.playerId()) && level == request.level()
                && cashCollected == request.cashCollected() && diamondsCollected == request.diamondsCollected()
                && penaltiesIncurred == request.penaltiesIncurred() && finalScore == request.finalScore()
                && completionTimeMs == request.completionTimeMs() && outcome == request.outcome()
                && completedAt.equals(request.completedAt().truncatedTo(ChronoUnit.MICROS));
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getPlayerId() { return playerId; }
    public int getLevel() { return level; }
    public long getCashCollected() { return cashCollected; }
    public int getDiamondsCollected() { return diamondsCollected; }
    public long getPenaltiesIncurred() { return penaltiesIncurred; }
    public long getFinalScore() { return finalScore; }
    public long getCompletionTimeMs() { return completionTimeMs; }
    public Outcome getOutcome() { return outcome; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getReceivedAt() { return receivedAt; }
}
