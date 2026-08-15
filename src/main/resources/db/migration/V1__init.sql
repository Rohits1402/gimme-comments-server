CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(60)  NOT NULL,
    profile_image VARCHAR(500),
    verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE websites
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    url        VARCHAR(500) NOT NULL,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE comments
(
    id                  UUID PRIMARY KEY,
    website_id          UUID        NOT NULL REFERENCES websites (id) ON DELETE CASCADE,
    user_id             UUID        REFERENCES users (id) ON DELETE SET NULL,
    parent_comment_id   UUID REFERENCES comments (id) ON DELETE CASCADE,
    comment_description TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE likes
(
    id         UUID PRIMARY KEY,
    comment_id UUID        NOT NULL REFERENCES comments (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (comment_id, user_id)
);

CREATE INDEX idx_comments_website ON comments (website_id);
CREATE INDEX idx_comments_parent ON comments (parent_comment_id);
CREATE INDEX idx_websites_user ON websites (user_id);