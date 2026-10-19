package dev.bankheist.api;

import static org.junit.jupiter.api.Assertions.*;

import dev.bankheist.api.common.ApiException;
import dev.bankheist.api.heist.HeistService;
import dev.bankheist.api.heist.Outcome;
import dev.bankheist.api.heist.SubmitHeist;
import dev.bankheist.api.player.PlayerService;
import dev.bankheist.api.player.PlayerStatisticsService;
import dev.bankheist.api.leaderboard.LeaderboardMetric;
import dev.bankheist.api.leaderboard.LeaderboardService;
import dev.bankheist.api.quest.QuestService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Invoked explicitly with mvn -Pintegration verify; it uses the real PostgreSQL SQL dialect and migrations. */
@SpringBootTest
@Testcontainers
class PostgresApiIT {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired PlayerService players;
    @Autowired HeistService heists;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlayerStatisticsService statistics;
    @Autowired LeaderboardService leaderboards;
    @Autowired QuestService quests;

    @BeforeEach
    void cleanPlayers() {
        jdbc.execute("TRUNCATE TABLE players CASCADE");
    }

    @Test
    void registersPersistentIdentityAndResolvesCaseInsensitiveUsername() {
        UUID clientId = UUID.randomUUID();
        assertTrue(players.register(clientId, "Alice_1").created());
        assertEquals(clientId, players.register(null, "ALICE_1").player().id());
        assertFalse(players.register(clientId, "alice_1").created());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM players", Long.class));
    }

    @Test
    void storesSignedScoresAndRetryRemainsOneRow() {
        UUID playerId = players.register(null, "Alice_1").player().id();
        SubmitHeist result = result(playerId, UUID.randomUUID(), -50, Outcome.LOST, 1500);
        assertTrue(heists.submit(result).created());
        assertFalse(heists.submit(result).created());
        assertEquals(-50, heists.get(result.sessionId()).finalScore());
        assertEquals(-50, statistics.stats(playerId).highestScore());
        assertNull(statistics.stats(playerId).averageCompletionTimeMs());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM heist_sessions", Long.class));
    }

    @Test
    void simultaneousRetriesAreIdempotent() throws Exception {
        UUID playerId = players.register(null, "Alice_1").player().id();
        SubmitHeist result = result(playerId, UUID.randomUUID(), 600, Outcome.WON, 45000);
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<HeistService.Submission>> calls = List.of(() -> heists.submit(result), () -> heists.submit(result));
            var futures = executor.invokeAll(calls);
            long created = 0;
            for (var future : futures) if (future.get().created()) created++;
            assertEquals(1, created);
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM heist_sessions", Long.class));
            assertEquals(150, quests.get(playerId).rewardPoints());
            assertEquals(1, quest(playerId, "FIVE_HEISTS").progress());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cannotChangeAcceptedResultOrPlayer() {
        UUID first = players.register(null, "Alice_1").player().id();
        UUID second = players.register(null, "Bob_1").player().id();
        UUID sessionId = UUID.randomUUID();
        heists.submit(result(first, sessionId, 600, Outcome.WON, 45000));

        ApiException failure = assertThrows(ApiException.class,
                () -> heists.submit(result(second, sessionId, 600, Outcome.WON, 45000)));
        assertEquals(HttpStatus.CONFLICT, failure.status());
        assertEquals(first, heists.get(sessionId).playerId());
    }

    @Test
    void emptyStatisticsKeepSuccessfulTimesNullAndHistoryEmpty() {
        UUID player = players.register(null, "New_Player").player().id();
        var stats = statistics.stats(player);
        assertEquals(0, stats.gamesPlayed());
        assertEquals(0, stats.winRate());
        assertNull(stats.fastestCompletionTimeMs());
        assertNull(stats.averageCompletionTimeMs());
        assertTrue(statistics.history(player, null, 20, 0).items().isEmpty());
    }

    @Test
    void statisticsSeparateGrossCashFromScoreAndAverageWinsOnly() {
        UUID player = players.register(null, "Alice_1").player().id();
        heists.submit(result(player, UUID.randomUUID(), 600, Outcome.WON, 40_000));
        heists.submit(result(player, UUID.randomUUID(), 600, Outcome.WON, 60_000));
        heists.submit(result(player, UUID.randomUUID(), -50, Outcome.LOST, 1_000));
        var stats = statistics.stats(player);
        assertEquals(3, stats.gamesPlayed());
        assertEquals(2, stats.wins());
        assertEquals(1, stats.losses());
        assertEquals(100.0 * 2 / 3, stats.winRate(), 0.00001);
        assertEquals(300, stats.totalCashCollected());
        assertEquals(3, stats.totalDiamondsCollected());
        assertEquals(600, stats.highestScore());
        assertEquals(100, stats.highestCash());
        assertEquals(40_000L, stats.fastestCompletionTimeMs());
        assertEquals(50_000.0, stats.averageCompletionTimeMs());
        assertEquals(3, statistics.history(player, null, 1, 1).total());
        assertEquals(1, statistics.history(player, null, 1, 1).items().size());
    }

    @Test
    void leaderboardsPickOneBestRunPerPlayerAndExcludeLossesFromTime() {
        UUID alice = players.register(null, "Alice_1").player().id();
        UUID bob = players.register(null, "Bob_1").player().id();
        heists.submit(result(alice, UUID.randomUUID(), 600, Outcome.WON, 30_000));
        heists.submit(result(alice, UUID.randomUUID(), 900, Outcome.LOST, 1_000));
        heists.submit(result(bob, UUID.randomUUID(), 700, Outcome.WON, 20_000));

        var score = leaderboards.leaderboard(LeaderboardMetric.SCORE, null, 20, 0);
        assertEquals(2, score.total());
        assertEquals(alice, score.items().get(0).playerId());
        assertEquals(900, score.items().get(0).finalScore());
        var time = leaderboards.leaderboard(LeaderboardMetric.TIME, null, 1, 1);
        assertEquals(2, time.total());
        assertEquals(2, time.items().get(0).rank());
        assertEquals(alice, time.items().get(0).playerId());
        assertEquals(30_000, time.items().get(0).completionTimeMs());
        assertTrue(leaderboards.leaderboard(LeaderboardMetric.SCORE, 2, 20, 0).items().isEmpty());
    }

    @Test
    void moneyRankingUsesGrossCashAndLevelFilterPrecedesBestRunSelection() {
        UUID alice = players.register(null, "Alice_1").player().id();
        UUID bob = players.register(null, "Bob_1").player().id();
        heists.submit(custom(alice, UUID.randomUUID(), 1, 300, 0, 300, Outcome.WON, 30_000));
        heists.submit(custom(alice, UUID.randomUUID(), 2, 900, 0, 900, Outcome.LOST, 1_000));
        heists.submit(custom(bob, UUID.randomUUID(), 1, 500, 10, 5500, Outcome.WON, 20_000));
        var allLevels = leaderboards.leaderboard(LeaderboardMetric.MONEY, null, 20, 0);
        assertEquals(alice, allLevels.items().get(0).playerId());
        var levelOne = leaderboards.leaderboard(LeaderboardMetric.MONEY, 1, 20, 0);
        assertEquals(2, levelOne.total());
        assertEquals(bob, levelOne.items().get(0).playerId());
        assertEquals(500, levelOne.items().get(0).cashCollected());
        assertEquals(300, levelOne.items().get(1).cashCollected());
    }

    @Test
    void deterministicTieUsesSessionUuidAsLastCriterion() {
        UUID alice = players.register(null, "Alice_1").player().id();
        UUID bob = players.register(null, "Bob_1").player().id();
        UUID earlierId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID laterId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        heists.submit(custom(alice, laterId, 1, 500, 0, 500, Outcome.WON, 20_000));
        heists.submit(custom(bob, earlierId, 1, 500, 0, 500, Outcome.WON, 20_000));
        for (LeaderboardMetric metric : LeaderboardMetric.values()) {
            var ranking = leaderboards.leaderboard(metric, null, 20, 0);
            assertEquals(earlierId, ranking.items().get(0).sessionId());
            assertEquals(laterId, ranking.items().get(1).sessionId());
        }
    }

    @Test
    void questProgressAndRewardsAreTransactionalAndNotRepeatedOnRetry() {
        UUID player = players.register(null, "Alice_1").player().id();
        SubmitHeist first = custom(player, UUID.randomUUID(), 1, 2500, 1, 3000, Outcome.WON, 59_999);
        heists.submit(first);
        assertEquals(350, quests.get(player).rewardPoints());
        heists.submit(first);
        assertEquals(350, quests.get(player).rewardPoints());
        for (int count = 0; count < 4; count++) {
            heists.submit(custom(player, UUID.randomUUID(), 1, 2500, 1, 3000, Outcome.LOST, 60_000));
        }
        assertEquals(650, quests.get(player).rewardPoints());
        assertTrue(quests.get(player).items().stream().allMatch(QuestService.QuestView::completed));
        assertEquals(5, statistics.stats(player).gamesPlayed());
    }

    @Test
    void concurrentDistinctResultsDoNotLoseLifetimeProgress() throws Exception {
        UUID player = players.register(null, "Alice_1").player().id();
        var executor = Executors.newFixedThreadPool(3);
        try {
            List<Callable<HeistService.Submission>> calls = List.of(
                    () -> heists.submit(result(player, UUID.randomUUID(), 600, Outcome.WON, 45_000)),
                    () -> heists.submit(result(player, UUID.randomUUID(), 600, Outcome.WON, 45_000)),
                    () -> heists.submit(result(player, UUID.randomUUID(), 600, Outcome.WON, 45_000)));
            for (var future : executor.invokeAll(calls)) assertTrue(future.get().created());
            assertEquals(3, quest(player, "FIVE_HEISTS").progress());
            assertTrue(quest(player, "DIAMOND_COLLECTOR").completed());
            assertEquals(225, quests.get(player).rewardPoints());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void questPersistenceFailureRollsBackResultAndRewards() {
        UUID player = players.register(null, "Alice_1").player().id();
        jdbc.execute("ALTER TABLE player_quests ADD CONSTRAINT test_reject_progress CHECK (progress = 0)");
        try {
            assertThrows(RuntimeException.class,
                    () -> heists.submit(result(player, UUID.randomUUID(), 600, Outcome.WON, 45_000)));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM heist_sessions", Long.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM player_quests", Long.class));
            assertEquals(0, players.requirePlayer(player).getRewardPoints());
        } finally {
            jdbc.execute("ALTER TABLE player_quests DROP CONSTRAINT test_reject_progress");
        }
    }

    private QuestService.QuestView quest(UUID player, String code) {
        return quests.get(player).items().stream().filter(quest -> quest.code().equals(code)).findFirst().orElseThrow();
    }

    private static SubmitHeist custom(UUID player, UUID session, int level, long cash, int diamonds,
            long score, Outcome outcome, long duration) {
        return new SubmitHeist(session, player, level, cash, diamonds, 0L, score, duration, outcome,
                Instant.parse("2026-10-18T12:00:00.123456789Z"));
    }

    static SubmitHeist result(UUID player, UUID session, long score, Outcome outcome, long duration) {
        return new SubmitHeist(session, player, 1, 100L, 1, 0L, score, duration, outcome,
                Instant.parse("2026-10-18T12:00:00.123456789Z"));
    }
}
