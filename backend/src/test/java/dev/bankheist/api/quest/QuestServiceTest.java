package dev.bankheist.api.quest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.bankheist.api.heist.Outcome;
import dev.bankheist.api.player.Player;
import dev.bankheist.api.player.PlayerService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestServiceTest {
    private final QuestDefinitionRepository definitions = mock(QuestDefinitionRepository.class);
    private final PlayerQuestRepository progress = mock(PlayerQuestRepository.class);
    private final PlayerService players = mock(PlayerService.class);
    private final QuestService service = new QuestService(definitions, progress, players);

    @Test
    void awardsProfilePointsOnlyOnCompletionTransition() {
        Player player = new Player(UUID.randomUUID(), "Alice", "alice", Instant.now());
        QuestDefinition definition = new QuestDefinition(1, "FIRST_ESCAPE", "First Escape", "Win once",
                QuestMetric.WINS, 1, 50, null);
        PlayerQuest current = new PlayerQuest(player.getId(), definition.getId());
        when(definitions.findAllByOrderByIdAsc()).thenReturn(List.of(definition));
        when(progress.findByPlayerId(player.getId())).thenReturn(List.of(current));

        service.apply(player, QuestProgressTest.result(100, 0, 100, Outcome.WON, 45000));
        service.apply(player, QuestProgressTest.result(100, 0, 100, Outcome.WON, 45000));

        assertEquals(50, player.getRewardPoints());
        assertEquals(1, current.getProgress());
    }

    @Test
    void unplayedPlayerSeesAllDefinitionsWithZeroProgress() {
        Player player = new Player(UUID.randomUUID(), "Alice", "alice", Instant.now());
        when(players.requirePlayer(player.getId())).thenReturn(player);
        when(progress.findByPlayerId(player.getId())).thenReturn(List.of());
        when(definitions.findAllByOrderByIdAsc()).thenReturn(List.of(new QuestDefinition(1,
                "FIRST_ESCAPE", "First Escape", "Win once", QuestMetric.WINS, 1, 50, null)));

        var quests = service.get(player.getId());

        assertEquals(1, quests.items().size());
        assertEquals(0, quests.items().get(0).progress());
        assertFalse(quests.items().get(0).completed());
        assertEquals(0, quests.rewardPoints());
        verify(progress, never()).saveAll(any());
    }
}
