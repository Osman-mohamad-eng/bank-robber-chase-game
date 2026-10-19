package dev.bankheist.api.quest;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestDefinitionRepository extends JpaRepository<QuestDefinition, Long> {
    List<QuestDefinition> findAllByOrderByIdAsc();
}
