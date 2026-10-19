package dev.bankheist.api.leaderboard;

import dev.bankheist.api.common.ResultPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {
    private final LeaderboardRepository repository;

    public LeaderboardService(LeaderboardRepository repository) {
        this.repository = repository;
    }

    public ResultPage<LeaderboardEntry> leaderboard(LeaderboardMetric metric, Integer level, int limit, int offset) {
        return repository.leaderboard(metric, level, limit, offset);
    }
}
