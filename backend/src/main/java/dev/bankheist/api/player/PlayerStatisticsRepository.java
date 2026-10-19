package dev.bankheist.api.player;

import dev.bankheist.api.common.ResultPage;
import dev.bankheist.api.heist.HeistView;
import dev.bankheist.api.heist.Outcome;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerStatisticsRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PlayerStatisticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Aggregate aggregate(UUID playerId) {
        String sql = """
                SELECT count(*) AS games, count(*) FILTER (WHERE outcome = 'WON') AS wins,
                    count(*) FILTER (WHERE outcome = 'LOST') AS losses,
                    coalesce(sum(cash_collected), 0) AS cash,
                    coalesce(sum(diamonds_collected), 0) AS diamonds,
                    coalesce(max(final_score), 0) AS highest_score,
                    coalesce(max(cash_collected), 0) AS highest_cash,
                    min(completion_time_ms) FILTER (WHERE outcome = 'WON') AS fastest,
                    avg(completion_time_ms) FILTER (WHERE outcome = 'WON') AS average
                FROM heist_sessions WHERE player_id = :playerId
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource("playerId", playerId), (row, number) ->
                new Aggregate(row.getLong("games"), row.getLong("wins"), row.getLong("losses"),
                        row.getLong("cash"), row.getLong("diamonds"), row.getLong("highest_score"),
                        row.getLong("highest_cash"), row.getObject("fastest", Long.class),
                        row.getObject("average") == null ? null : row.getDouble("average")));
    }

    public ResultPage<HeistView> history(UUID playerId, Integer level, int limit, int offset) {
        String filter = " WHERE h.player_id = :playerId" + (level == null ? "" : " AND h.level = :level");
        MapSqlParameterSource parameters = new MapSqlParameterSource("playerId", playerId)
                .addValue("level", level).addValue("limit", limit).addValue("offset", offset);
        Long total = jdbc.queryForObject("SELECT count(*) FROM heist_sessions h" + filter, parameters, Long.class);
        var rows = jdbc.query("""
                SELECT h.*, p.username FROM heist_sessions h JOIN players p ON p.id = h.player_id
                """ + filter + " ORDER BY h.completed_at DESC, h.session_id ASC LIMIT :limit OFFSET :offset",
                parameters, (row, number) -> mapHeist(row));
        return new ResultPage<>(rows, limit, offset, total == null ? 0 : total);
    }

    private static HeistView mapHeist(ResultSet row) throws SQLException {
        return new HeistView(row.getObject("session_id", UUID.class), row.getObject("player_id", UUID.class),
                row.getString("username"), row.getInt("level"), row.getLong("cash_collected"),
                row.getInt("diamonds_collected"), row.getLong("penalties_incurred"), row.getLong("final_score"),
                row.getLong("completion_time_ms"), Outcome.valueOf(row.getString("outcome")),
                row.getTimestamp("completed_at").toInstant(), row.getTimestamp("received_at").toInstant());
    }

    public record Aggregate(long games, long wins, long losses, long cash, long diamonds,
            long highestScore, long highestCash, Long fastest, Double average) {}
}
