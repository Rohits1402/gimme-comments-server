-- Every comment carries the id of the top-level comment of its thread, so a
-- whole thread can be read in one query. A root comment points at itself.
ALTER TABLE comments
    ADD COLUMN root_comment_id UUID REFERENCES comments (id) ON DELETE CASCADE;

-- Backfill: walk down from every root once, so existing rows get the value that
-- new rows will get at insert time. This is the only recursive walk we ever run.
WITH RECURSIVE thread AS (SELECT id, id AS root_id
                          FROM comments
                          WHERE parent_comment_id IS NULL
                          UNION ALL
                          SELECT c.id, t.root_id
                          FROM comments c
                                   JOIN thread t ON c.parent_comment_id = t.id)
UPDATE comments
SET root_comment_id = thread.root_id FROM thread
WHERE comments.id = thread.id;

ALTER TABLE comments
    ALTER COLUMN root_comment_id SET NOT NULL;

CREATE INDEX idx_comments_root ON comments (root_comment_id);

-- Matches the roots query exactly: same filter, same sort, same direction. It
-- holds only root comments, so it stays small as replies pile up.
CREATE INDEX idx_comments_website_roots
    ON comments (website_id, created_at DESC, id DESC) WHERE parent_comment_id IS NULL;