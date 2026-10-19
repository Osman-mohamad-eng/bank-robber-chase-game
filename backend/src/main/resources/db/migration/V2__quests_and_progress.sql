CREATE TABLE quest_definitions (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL,
    metric VARCHAR(32) NOT NULL CHECK (metric IN
        ('SINGLE_CASH', 'LIFETIME_DIAMONDS', 'WINS', 'FAST_WIN', 'COMPLETED', 'LIFETIME_CASH', 'SINGLE_SCORE')),
    target BIGINT NOT NULL CHECK (target > 0),
    reward_points BIGINT NOT NULL CHECK (reward_points > 0),
    qualifying_time_ms BIGINT CHECK (qualifying_time_ms > 0),
    CONSTRAINT fast_win_has_time_limit CHECK ((metric = 'FAST_WIN') = (qualifying_time_ms IS NOT NULL))
);

CREATE TABLE player_quests (
    player_id UUID NOT NULL REFERENCES players(id),
    quest_id BIGINT NOT NULL REFERENCES quest_definitions(id),
    progress BIGINT NOT NULL DEFAULT 0 CHECK (progress >= 0),
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (player_id, quest_id)
);

-- Rewards are profile points, separate from the client score and leaderboard money.
INSERT INTO quest_definitions(id, code, title, description, metric, target, reward_points, qualifying_time_ms) VALUES
    (1, 'CASH_HAUL', 'Cash Haul', 'Collect 2,000 cash in a single heist.', 'SINGLE_CASH', 2000, 100, NULL),
    (2, 'DIAMOND_COLLECTOR', 'Diamond Collector', 'Collect 3 diamonds across completed heists.', 'LIFETIME_DIAMONDS', 3, 75, NULL),
    (3, 'FIRST_ESCAPE', 'First Escape', 'Win your first heist.', 'WINS', 1, 50, NULL),
    (4, 'SPEED_RUN', 'Speed Run', 'Win a heist in less than 60 seconds.', 'FAST_WIN', 1, 100, 60000),
    (5, 'FIVE_HEISTS', 'Five Heists', 'Finish 5 heists, with any outcome.', 'COMPLETED', 5, 75, NULL),
    (6, 'CASH_CAREER', 'Cash Career', 'Collect 10,000 cash across completed heists.', 'LIFETIME_CASH', 10000, 150, NULL),
    (7, 'HIGH_SCORE', 'High Score', 'Reach a final score of 3,000 in one heist.', 'SINGLE_SCORE', 3000, 100, NULL);
