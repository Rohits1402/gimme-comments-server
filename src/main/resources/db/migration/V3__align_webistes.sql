ALTER TABLE websites
    ADD COLUMN description VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE websites
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE websites
    ADD CONSTRAINT websites_url_unique UNIQUE (url);