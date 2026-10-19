package dev.bankheist.api.leaderboard;

import dev.bankheist.api.common.ResultPage;
import dev.bankheist.api.heist.Outcome;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LeaderboardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public LeaderboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ResultPage<LeaderboardEntry> leaderboard(LeaderboardMetric metric, Integer level, int limit, int offset) {
        String filter = " WHERE true" + (metric.successfulOnly() ? " AND outcome = 'WON'" : "")
                + (level == null ? "" : " AND level = :level");
        MapSqlParameterSource parameters = new MapSqlParameterSource("level", level)
                .addValue("limit", limit).addValue("offset", offset);
        Long total = jdbc.queryForObject("SELECT count(DISTINCT player_id) FROM heist_sessions" + filter,
                parameters, Long.class);
        String sql = """
                WITH player_runs AS (
                    SELECT *, row_number() OVER (PARTITION BY player_id ORDER BY %s) AS player_run
                    FROM heist_sessions %s
                ), ranked AS (
                    SELECT *, row_number() OVER (ORDER BY %s) AS global_rank
                    FROM player_runs WHERE player_run = 1
                )
                SELECT ranked.*, p.username FROM ranked JOIN players p ON ranked.player_id = p.id
                ORDER BY global_rank LIMIT :limit OFFSET :offset
                """.formatted(metric.order(), filter, metric.order());
        var entries = jdbc.query(sql, parameters, (row, number) -> new LeaderboardEntry(
                row.getLong("global_rank"), row.getObject("player_id", UUID.class), row.getString("username"),
                row.getObject("session_id", UUID.class), row.getInt("level"), row.getLong("cash_collected"),
                row.getInt("diamonds_collected"), row.getLong("final_score"), row.getLong("completion_time_ms"),
                Outcome.valueOf(row.getString("outcome")), row.getTimestamp("completed_at").toInstant()));
        return new ResultPage<>(entries, limit, offset, total == null ? 0 : total);
    }
}
