package dev.bankheist.api.quest;

import dev.bankheist.api.heist.HeistSession;
import dev.bankheist.api.player.Player;
import dev.bankheist.api.player.PlayerService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestService {
    private final QuestDefinitionRepository definitions;
    private final PlayerQuestRepository progress;
    private final PlayerService players;

    public QuestService(QuestDefinitionRepository definitions, PlayerQuestRepository progress, PlayerService players) {
        this.definitions = definitions;
        this.progress = progress;
        this.players = players;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(Player player, HeistSession session) {
        Map<Long, PlayerQuest> existing = progressByQuest(player.getId());
        List<PlayerQuest> updated = definitions.findAllByOrderByIdAsc().stream().map(definition -> {
            PlayerQuest current = existing.getOrDefault(definition.getId(), new PlayerQuest(player.getId(), definition.getId()));
            if (current.apply(definition, session)) player.addRewardPoints(definition.getRewardPoints());
            return current;
        }).toList();
        progress.saveAll(updated);
    }

    @Transactional(readOnly = true)
    public PlayerQuests get(UUID playerId) {
        Player player = players.requirePlayer(playerId);
        Map<Long, PlayerQuest> existing = progressByQuest(playerId);
        List<QuestView> quests = definitions.findAllByOrderByIdAsc().stream().map(definition -> {
            PlayerQuest current = existing.get(definition.getId());
            return new QuestView(definition.getId(), definition.getCode(), definition.getTitle(),
                    definition.getDescription(), definition.getMetric(), definition.getTarget(),
                    definition.getRewardPoints(), current == null ? 0 : current.getProgress(),
                    current != null && current.getCompletedAt() != null,
                    current == null ? null : current.getCompletedAt(), definition.getQualifyingTimeMs());
        }).toList();
        return new PlayerQuests(playerId, player.getRewardPoints(), quests);
    }

    private Map<Long, PlayerQuest> progressByQuest(UUID playerId) {
        return progress.findByPlayerId(playerId).stream()
                .collect(Collectors.toMap(PlayerQuest::getQuestId, Function.identity()));
    }

    public record PlayerQuests(UUID playerId, long rewardPoints, List<QuestView> items) {}
    public record QuestView(long id, String code, String title, String description, QuestMetric metric,
            long target, long rewardPoints, long progress, boolean completed, Instant completedAt, Long qualifyingTimeMs) {}
}
