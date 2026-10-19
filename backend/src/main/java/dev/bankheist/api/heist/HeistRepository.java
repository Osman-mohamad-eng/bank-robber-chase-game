package dev.bankheist.api.heist;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeistRepository extends JpaRepository<HeistSession, UUID> {
    @Modifying
    @Query(value = """
            INSERT INTO heist_sessions(session_id, player_id, level, cash_collected, diamonds_collected,
                penalties_incurred, final_score, completion_time_ms, outcome, completed_at, received_at)
            VALUES (:sessionId, :playerId, :level, :cash, :diamonds, :penalties, :score,
                :duration, :outcome, :completedAt, :receivedAt)
            ON CONFLICT (session_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("sessionId") UUID sessionId, @Param("playerId") UUID playerId,
            @Param("level") int level, @Param("cash") long cash, @Param("diamonds") int diamonds,
            @Param("penalties") long penalties, @Param("score") long score, @Param("duration") long duration,
            @Param("outcome") String outcome, @Param("completedAt") Instant completedAt,
            @Param("receivedAt") Instant receivedAt);
}
