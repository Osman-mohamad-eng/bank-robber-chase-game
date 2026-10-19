package dev.bankheist.api.quest;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PlayerQuestId implements Serializable {
    private UUID playerId;
    private Long questId;

    public PlayerQuestId() {}

    public PlayerQuestId(UUID playerId, Long questId) {
        this.playerId = playerId;
        this.questId = questId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PlayerQuestId key && Objects.equals(playerId, key.playerId)
                && Objects.equals(questId, key.questId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, questId);
    }
}
