package dev.bankheist.api.quest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "quest_definitions")
public class QuestDefinition {
    @Id
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(nullable = false, length = 100)
    private String title;
    @Column(nullable = false)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestMetric metric;
    @Column(nullable = false)
    private long target;
    @Column(name = "reward_points", nullable = false)
    private long rewardPoints;
    @Column(name = "qualifying_time_ms")
    private Long qualifyingTimeMs;

    protected QuestDefinition() {}

    public QuestDefinition(long id, String code, String title, String description, QuestMetric metric,
            long target, long rewardPoints, Long qualifyingTimeMs) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.metric = metric;
        this.target = target;
        this.rewardPoints = rewardPoints;
        this.qualifyingTimeMs = qualifyingTimeMs;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public QuestMetric getMetric() { return metric; }
    public long getTarget() { return target; }
    public long getRewardPoints() { return rewardPoints; }
    public Long getQualifyingTimeMs() { return qualifyingTimeMs; }
}
