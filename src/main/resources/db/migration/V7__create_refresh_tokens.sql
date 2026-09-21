-- One row per signed-in session, not one per user: signing out of a laptop must
-- not sign out a phone. This table is the whole reason the scheme is revocable -
-- an access token is verified by arithmetic and can never be cancelled, but a
-- refresh token is only good if a row here says so.
CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Every token descended from one login shares a family. Tomorrow, presenting
    -- an already-used token revokes the whole family at once, which is how a
    -- stolen token gets shut down without walking a chain of successors.
    family_id  UUID        NOT NULL,
    -- SHA-256 hex, never the token itself. Anyone holding a refresh token can
    -- mint access tokens for weeks, so it is a credential and is stored like one.
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    -- When this token was first exchanged for a successor. Kept apart from revoked_at
    -- on purpose: a token presented seconds after it was rotated is two tabs
    -- refreshing at the same moment, while one presented long afterwards means
    -- somebody kept a copy.
    rotated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- NULL means live. A timestamp rather than a boolean, because when a session
    -- was cut off is the first thing you want to know while investigating one.
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);