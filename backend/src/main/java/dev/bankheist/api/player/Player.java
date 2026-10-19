package dev.bankheist.api.player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {
    @Id
    private UUID id;
    @Column(nullable = false, length = 32)
    private String username;
    @Column(name = "normalized_username", nullable = false, unique = true, length = 32)
    private String normalizedUsername;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;

    protected Player() {}

    public Player(UUID id, String username, String normalizedUsername, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.normalizedUsername = normalizedUsername;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public Instant getCreatedAt() { return createdAt; }
    public long getRewardPoints() { return rewardPoints; }

    public void addRewardPoints(long points) {
        rewardPoints = Math.addExact(rewardPoints, points);
    }
}
