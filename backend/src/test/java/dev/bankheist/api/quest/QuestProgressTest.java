package dev.bankheist.api.quest;

import static org.junit.jupiter.api.Assertions.*;

import dev.bankheist.api.heist.HeistSession;
import dev.bankheist.api.heist.Outcome;
import dev.bankheist.api.heist.SubmitHeist;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestProgressTest {
    private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

    @Test
    void singleHeistCashUsesMaximumInsteadOfSummingRuns() {
        QuestDefinition definition = definition(QuestMetric.SINGLE_CASH, 2000, null);
        PlayerQuest progress = new PlayerQuest(UUID.randomUUID(), definition.getId());
        assertFalse(progress.apply(definition, result(1200, 0, 0, Outcome.LOST, 1000)));
        assertFalse(progress.apply(definition, result(1200, 0, 0, Outcome.LOST, 1000)));
        assertEquals(1200, progress.getProgress());
        assertTrue(progress.apply(definition, result(2000, 0, 0, Outcome.LOST, 1000)));
        assertEquals(2000, progress.getProgress());
    }

    @Test
    void lifetimeDiamondProgressCapsAtTargetAndCompletesOnlyOnce() {
        QuestDefinition definition = definition(QuestMetric.LIFETIME_DIAMONDS, 3, null);
        PlayerQuest progress = new PlayerQuest(UUID.randomUUID(), definition.getId());
        assertFalse(progress.apply(definition, result(0, 2, 1000, Outcome.LOST, 1000)));
        assertTrue(progress.apply(definition, result(0, 2, 1000, Outcome.LOST, 1000)));
        assertEquals(3, progress.getProgress());
        assertEquals(NOW, progress.getCompletedAt());
        assertFalse(progress.apply(definition, result(0, 100, 50000, Outcome.WON, 1000)));
        assertEquals(3, progress.getProgress());
    }

    @Test
    void speedRunRequiresWinStrictlyUnderSixtySeconds() {
        QuestDefinition definition = definition(QuestMetric.FAST_WIN, 1, 60_000L);
        PlayerQuest progress = new PlayerQuest(UUID.randomUUID(), definition.getId());
        assertFalse(progress.apply(definition, result(0, 0, 0, Outcome.LOST, 1000)));
        assertFalse(progress.apply(definition, result(0, 0, 0, Outcome.WON, 60_000)));
        assertTrue(progress.apply(definition, result(0, 0, 0, Outcome.WON, 59_999)));
    }

    @Test
    void firstEscapeIgnoresLossesAndCompletedQuestCountsThem() {
        HeistSession lost = result(100, 0, -50, Outcome.LOST, 1000);
        assertEquals(0, QuestMetric.WINS.progress(0, 1, null, lost));
        assertEquals(1, QuestMetric.COMPLETED.progress(0, 5, null, lost));
        assertEquals(0, QuestMetric.SINGLE_SCORE.progress(0, 3000, null, lost));
    }

    @Test
    void lifetimeCashCapsWithoutLongOverflow() {
        HeistSession huge = result(Long.MAX_VALUE, 0, 0, Outcome.LOST, 1000);
        assertEquals(10_000, QuestMetric.LIFETIME_CASH.progress(9_900, 10_000, null, huge));
    }

    private static QuestDefinition definition(QuestMetric metric, long target, Long timeLimit) {
        return new QuestDefinition(1, "TEST", "Test", "Test definition", metric, target, 100, timeLimit);
    }

    static HeistSession result(long cash, int diamonds, long score, Outcome outcome, long duration) {
        return new HeistSession(new SubmitHeist(UUID.randomUUID(), UUID.randomUUID(), 1, cash,
                diamonds, 0L, score, duration, outcome, NOW), NOW);
    }
}
