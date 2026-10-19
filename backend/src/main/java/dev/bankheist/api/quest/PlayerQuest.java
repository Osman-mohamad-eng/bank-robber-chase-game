package dev.bankheist.api.quest;

import dev.bankheist.api.heist.HeistSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_quests")
@IdClass(PlayerQuestId.class)
public class PlayerQuest {
    @Id
    @Column(name = "player_id")
    private UUID playerId;
    @Id
    @Column(name = "quest_id")
    private Long questId;
    @Column(nullable = false)
    private long progress;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected PlayerQuest() {}

    public PlayerQuest(UUID playerId, Long questId) {
        this.playerId = playerId;
        this.questId = questId;
    }

    /** Returns true only for the transition that earns a reward. Caller holds the player's database lock. */
    public boolean apply(QuestDefinition definition, HeistSession result) {
        if (completedAt != null) return false;
        progress = definition.getMetric().progress(progress, definition.getTarget(),
                definition.getQualifyingTimeMs(), result);
        if (progress >= definition.getTarget()) {
            completedAt = result.getReceivedAt();
            return true;
        }
        return false;
    }

    public Long getQuestId() { return questId; }
    public long getProgress() { return progress; }
    public Instant getCompletedAt() { return completedAt; }
}
