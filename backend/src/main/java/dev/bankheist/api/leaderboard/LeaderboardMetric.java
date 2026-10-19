package dev.bankheist.api.leaderboard;

/** SQL fragments are fixed server constants, never supplied by a caller. */
public enum LeaderboardMetric {
    SCORE("final_score DESC, completion_time_ms ASC, completed_at ASC, session_id ASC", false),
    MONEY("cash_collected DESC, completion_time_ms ASC, completed_at ASC, session_id ASC", false),
    TIME("completion_time_ms ASC, final_score DESC, completed_at ASC, session_id ASC", true);

    private final String order;
    private final boolean successfulOnly;

    LeaderboardMetric(String order, boolean successfulOnly) {
        this.order = order;
        this.successfulOnly = successfulOnly;
    }

    String order() { return order; }
    boolean successfulOnly() { return successfulOnly; }
}
