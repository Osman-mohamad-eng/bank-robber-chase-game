package dev.bankheist.api.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.bankheist.api.common.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlayerStatisticsServiceTest {
    private final PlayerService players = mock(PlayerService.class);
    private final PlayerStatisticsRepository repository = mock(PlayerStatisticsRepository.class);
    private final PlayerStatisticsService service = new PlayerStatisticsService(players, repository);
    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void playerExists() {
        Player player = new Player(playerId, "Alice_1", "alice_1", Instant.now());
        player.addRewardPoints(50);
        when(players.requirePlayer(playerId)).thenReturn(player);
    }

    @Test
    void unplayedProfileHasZeroWinRateAndNoInventedCompletionTimes() {
        when(repository.aggregate(playerId)).thenReturn(new PlayerStatisticsRepository.Aggregate(
                0, 0, 0, 0, 0, 0, 0, null, null));

        var stats = service.stats(playerId);

        assertEquals(0.0, stats.winRate());
        assertEquals(0, stats.gamesPlayed());
        assertNull(stats.fastestCompletionTimeMs());
        assertNull(stats.averageCompletionTimeMs());
        assertEquals("Alice_1", stats.username());
        assertEquals(50, stats.rewardPoints());
    }

    @Test
    void calculatesPercentageWithoutIntegerDivisionAndPreservesNegativeBestScore() {
        when(repository.aggregate(playerId)).thenReturn(new PlayerStatisticsRepository.Aggregate(
                3, 2, 1, 300, 3, -50, 100, 40_000L, 50_000.0));

        var stats = service.stats(playerId);

        assertEquals(100.0 * 2 / 3, stats.winRate(), 0.00001);
        assertEquals(-50, stats.highestScore());
        assertEquals(300, stats.totalCashCollected());
        assertEquals(3, stats.totalDiamondsCollected());
        assertEquals(40_000L, stats.fastestCompletionTimeMs());
        assertEquals(50_000.0, stats.averageCompletionTimeMs());
    }

    @Test
    void unknownProfileDoesNotLookLikeAnEmptyHistory() {
        when(players.requirePlayer(playerId)).thenThrow(ApiException.notFound("Player not found"));

        ApiException failure = assertThrows(ApiException.class, () -> service.history(playerId, null, 20, 0));

        assertEquals(HttpStatus.NOT_FOUND, failure.status());
        verifyNoInteractions(repository);
    }
}
