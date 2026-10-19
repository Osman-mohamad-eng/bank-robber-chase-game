package dev.bankheist.api.player;

import dev.bankheist.api.common.ResultPage;
import dev.bankheist.api.heist.HeistView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players/{playerId}")
public class PlayerStatisticsController {
    private final PlayerStatisticsService service;

    public PlayerStatisticsController(PlayerStatisticsService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public PlayerStatisticsService.PlayerStatistics stats(@PathVariable UUID playerId) {
        return service.stats(playerId);
    }

    @GetMapping("/history")
    public ResultPage<HeistView> history(@PathVariable UUID playerId,
            @RequestParam(required = false) @Min(1) Integer level,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return service.history(playerId, level, limit, offset);
    }
}
