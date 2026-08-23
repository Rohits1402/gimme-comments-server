CREATE TABLE otp_tokens
(
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    code       VARCHAR(10)  NOT NULL,
    purpose    VARCHAR(30)  NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT otp_tokens_email_purpose_unique UNIQUE (email, purpose)
);

CREATE INDEX idx_otp_tokens_expires_at ON otp_tokens (expires_at);