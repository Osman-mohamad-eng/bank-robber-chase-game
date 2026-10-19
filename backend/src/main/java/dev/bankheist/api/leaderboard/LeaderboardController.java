package dev.bankheist.api.leaderboard;

import dev.bankheist.api.common.ResultPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import dev.bankheist.api.common.ApiException;

@RestController
@RequestMapping("/api/v1/leaderboards")
public class LeaderboardController {
    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping("/{metric:score|money|time}")
    public ResultPage<LeaderboardEntry> leaderboard(@PathVariable String metric,
            @RequestParam(required = false) @Min(1) Integer level,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        LeaderboardMetric selected = switch (metric) {
            case "score" -> LeaderboardMetric.SCORE;
            case "money" -> LeaderboardMetric.MONEY;
            case "time" -> LeaderboardMetric.TIME;
            default -> throw ApiException.notFound("Leaderboard not found");
        };
        return service.leaderboard(selected, level, limit, offset);
    }
}
