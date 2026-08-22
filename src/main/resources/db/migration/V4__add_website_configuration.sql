ALTER TABLE websites
    ADD COLUMN website_configuration JSONB NOT NULL DEFAULT '{}'::jsonb;