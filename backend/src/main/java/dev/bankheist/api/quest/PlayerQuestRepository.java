package dev.bankheist.api.quest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerQuestRepository extends JpaRepository<PlayerQuest, PlayerQuestId> {
    List<PlayerQuest> findByPlayerId(UUID playerId);
}
