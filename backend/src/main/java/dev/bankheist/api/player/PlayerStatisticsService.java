package dev.bankheist.api.player;

import dev.bankheist.api.common.ResultPage;
import dev.bankheist.api.heist.HeistView;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayerStatisticsService {
    private final PlayerService players;
    private final PlayerStatisticsRepository statistics;

    public PlayerStatisticsService(PlayerService players, PlayerStatisticsRepository statistics) {
        this.players = players;
        this.statistics = statistics;
    }

    public PlayerStatistics stats(UUID playerId) {
        Player player = players.requirePlayer(playerId);
        var totals = statistics.aggregate(playerId);
        double winRate = totals.games() == 0 ? 0.0 : 100.0 * totals.wins() / totals.games();
        return new PlayerStatistics(playerId, player.getUsername(), totals.games(), totals.wins(), totals.losses(),
                winRate, totals.cash(), totals.diamonds(), totals.highestScore(), totals.highestCash(),
                totals.fastest(), totals.average(), player.getRewardPoints());
    }

    public ResultPage<HeistView> history(UUID playerId, Integer level, int limit, int offset) {
        players.requirePlayer(playerId);
        return statistics.history(playerId, level, limit, offset);
    }

    public record PlayerStatistics(UUID playerId, String username, long gamesPlayed, long wins, long losses,
            double winRate, long totalCashCollected, long totalDiamondsCollected, long highestScore,
            long highestCash, Long fastestCompletionTimeMs, Double averageCompletionTimeMs, long rewardPoints) {}
}
