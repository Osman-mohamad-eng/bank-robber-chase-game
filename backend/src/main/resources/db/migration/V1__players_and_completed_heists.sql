CREATE TABLE players (
    id UUID PRIMARY KEY,
    username VARCHAR(32) NOT NULL,
    normalized_username VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reward_points BIGINT NOT NULL DEFAULT 0 CHECK (reward_points >= 0),
    CONSTRAINT valid_username CHECK (username ~ '^[A-Za-z0-9_-]{3,32}$'),
    CONSTRAINT canonical_username CHECK (normalized_username = lower(username))
);

-- Only terminal results live here. Starting and abandoning a run are client concerns.
CREATE TABLE heist_sessions (
    session_id UUID PRIMARY KEY,
    player_id UUID NOT NULL REFERENCES players(id),
    level INTEGER NOT NULL CHECK (level > 0),
    cash_collected BIGINT NOT NULL CHECK (cash_collected >= 0),
    diamonds_collected INTEGER NOT NULL CHECK (diamonds_collected >= 0),
    penalties_incurred BIGINT NOT NULL CHECK (penalties_incurred >= 0),
    final_score BIGINT NOT NULL,
    completion_time_ms BIGINT NOT NULL CHECK (completion_time_ms >= 0),
    outcome VARCHAR(4) NOT NULL CHECK (outcome IN ('WON', 'LOST')),
    completed_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX heists_player_history ON heist_sessions(player_id, completed_at DESC, session_id);
CREATE INDEX heists_score ON heist_sessions(final_score DESC, completion_time_ms, completed_at, session_id);
CREATE INDEX heists_cash ON heist_sessions(cash_collected DESC, completion_time_ms, completed_at, session_id);
CREATE INDEX heists_time ON heist_sessions(completion_time_ms, final_score DESC, completed_at, session_id)
    WHERE outcome = 'WON';
