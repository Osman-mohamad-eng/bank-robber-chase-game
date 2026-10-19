package dev.bankheist.api.quest;

import dev.bankheist.api.heist.HeistSession;
import dev.bankheist.api.heist.Outcome;

/** Applies achievement metrics to reported results; it never decides a game's score or outcome. */
public enum QuestMetric {
    SINGLE_CASH, LIFETIME_DIAMONDS, WINS, FAST_WIN, COMPLETED, LIFETIME_CASH, SINGLE_SCORE;

    public long progress(long previous, long target, Long qualifyingTimeMs, HeistSession result) {
        return switch (this) {
            case SINGLE_CASH -> Math.min(target, Math.max(previous, result.getCashCollected()));
            case LIFETIME_DIAMONDS -> addCapped(previous, result.getDiamondsCollected(), target);
            case WINS -> addCapped(previous, result.getOutcome() == Outcome.WON ? 1 : 0, target);
            case FAST_WIN -> addCapped(previous,
                    result.getOutcome() == Outcome.WON && result.getCompletionTimeMs() < qualifyingTimeMs ? 1 : 0,
                    target);
            case COMPLETED -> addCapped(previous, 1, target);
            case LIFETIME_CASH -> addCapped(previous, result.getCashCollected(), target);
            case SINGLE_SCORE -> Math.min(target, Math.max(previous, result.getFinalScore()));
        };
    }

    private static long addCapped(long current, long increment, long target) {
        return increment >= target - current ? target : current + increment;
    }
}
