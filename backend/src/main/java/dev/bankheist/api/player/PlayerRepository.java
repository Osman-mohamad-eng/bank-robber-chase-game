package dev.bankheist.api.player;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Optional<Player> findByNormalizedUsername(String username);

    @Modifying
    @Query(value = """
            INSERT INTO players(id, username, normalized_username, created_at)
            VALUES (:id, :username, :normalized, :createdAt)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int register(@Param("id") UUID id, @Param("username") String username,
                 @Param("normalized") String normalized, @Param("createdAt") Instant createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Player p where p.id = :id")
    Optional<Player> findForUpdate(@Param("id") UUID id);
}
