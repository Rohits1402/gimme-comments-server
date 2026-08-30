import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api.js';
import { useStore } from '../store.jsx';
import {
  Avatar,
  Button,
  Card,
  ConfirmButton,
  CopyButton,
  EmptyState,
  Field,
  IconBack,
  IconTrash,
  TimeAgo,
} from '../ui.jsx';

const DEFAULT_ACCENT = '#4f46e5';

function embedSnippet(websiteId) {
  // Built from wherever the dashboard is being served, so it is right on localhost
  // and right in production. The old panel hardcoded the 2023 server's address —
  // complete with a double slash before the filename.
  const origin = window.location.origin;
  return (
    `<div id="gimme-comments-root" data-gimme_comments_website_id="${websiteId}"></div>\n` +
    `<script src="${origin}/initialize-gimme-comments.js"></script>`
  );
}

function Settings({ website, onSaved }) {
  const { notify } = useStore();
  const config = website.website_configuration ?? {};

  const [name, setName] = useState(website.website_name ?? '');
  const [description, setDescription] = useState(website.website_description ?? '');
  const [theme, setTheme] = useState(config.theme ?? 'auto');
  const [accent, setAccent] = useState(config.accent ?? DEFAULT_ACCENT);
  const [busy, setBusy] = useState(false);

  const save = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const data = await api.patch(`/websites/${website.id}`, {
        website_name: name,
        website_description: description,
        website_configuration: { ...config, theme, accent },
      });
      onSaved(data.website);
      notify('success', 'Saved.');
    } catch (err) {
      notify('error', err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card title="Settings">
      <form onSubmit={save}>
        <Field id="s-name" label="Name" required value={name} onChange={(e) => setName(e.target.value)} />
        <Field
          id="s-desc"
          label="Description"
          textarea
          rows={2}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />

        <div className="gc-field-row">
          <label className="gc-field" htmlFor="s-theme">
            <span className="gc-field-label">Widget theme</span>
            <select
              id="s-theme"
              className="gc-input"
              value={theme}
              onChange={(e) => setTheme(e.target.value)}
            >
              <option value="auto">Match the page</option>
              <option value="light">Always light</option>
              <option value="dark">Always dark</option>
            </select>
            <span className="gc-field-hint">
              &ldquo;Match the page&rdquo; reads your site&rsquo;s own background colour.
            </span>
          </label>

          <label className="gc-field gc-field-narrow" htmlFor="s-accent">
            <span className="gc-field-label">Accent</span>
            <input
              id="s-accent"
              type="color"
              className="gc-input gc-input-color"
              value={accent}
              onChange={(e) => setAccent(e.target.value)}
            />
            <span className="gc-field-hint">Buttons and links.</span>
          </label>
        </div>

        <Button variant="primary" type="submit" busy={busy}>
          Save settings
        </Button>
      </form>
    </Card>
  );
}

const byOldest = (a, b) => new Date(a.created_at) - new Date(b.created_at);

/**
 * The API sends one page of threads: the root comments first, newest first, then
 * every reply belonging to them. Shown in that raw order a moderator would see
 * twenty openers followed by a pile of orphaned replies, so each reply is moved
 * to sit directly under the comment it answers.
 * <p>
 * The thread order itself is left exactly as the server sent it. Re-sorting the
 * whole list by date would let a newly loaded page insert rows above what the
 * moderator is already reading.
 */
function inThreadOrder(list) {
  const byId = new Map(list.map((c) => [c.id, c]));
  const childrenOf = new Map();
  const roots = [];

  for (const c of list) {
    const parent = c.comment_parent ? byId.get(c.comment_parent) : null;
    if (!parent) {
      roots.push(c);
      continue;
    }
    if (!childrenOf.has(parent.id)) childrenOf.set(parent.id, []);
    childrenOf.get(parent.id).push(c);
  }

  const out = [];
  const walk = (node) => {
    // buried is filled in after the walk: it is however many rows the recursion
    // added below this one, which is exactly what deleting this comment removes.
    const row = { comment: node, buried: 0 };
    out.push(row);
    const before = out.length;
    for (const child of (childrenOf.get(node.id) ?? []).sort(byOldest)) walk(child);
    row.buried = out.length - before;
  };
  roots.forEach(walk);
  return out;
}

/** Say what the delete takes with it, rather than only what was clicked. */
function deleteQuestion(buried) {
  if (buried === 0) return 'Delete this comment?';
  return `Delete this and ${buried} ${buried === 1 ? 'reply' : 'replies'}?`;
}

function Comments({ websiteId }) {
  const { notify } = useStore();
  const [comments, setComments] = useState(null);
  const [total, setTotal] = useState(0);
  const [cursor, setCursor] = useState(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const [removing, setRemoving] = useState(null);

  const load = useCallback(async () => {
    try {
      const data = await api.get(`/comments/comment/${websiteId}`);
      setComments(data.comments ?? []);
      setTotal(data.total_comments ?? 0);
      setCursor(data.next_cursor ?? null);
    } catch (err) {
      notify('error', err.message);
      setComments([]);
    }
  }, [websiteId, notify]);

  useEffect(() => {
    load();
  }, [load]);

  const loadMore = async () => {
    // The guard matters: two clicks with the same cursor append the same page twice.
    if (!cursor || loadingMore) return;
    setLoadingMore(true);
    try {
      const data = await api.get(
        `/comments/comment/${websiteId}?cursor=${encodeURIComponent(cursor)}`
      );
      setComments((current) => [...current, ...(data.comments ?? [])]);
      setTotal(data.total_comments ?? 0);
      setCursor(data.next_cursor ?? null);
    } catch (err) {
      notify('error', err.message);
    } finally {
      setLoadingMore(false);
    }
  };

  const remove = async (id) => {
    setRemoving(id);
    try {
      await api.delete(`/comments/comment/${id}`);

      // Removed from the list rather than reloaded. load() now means "go back to
      // page one", so a moderator deleting one comment on page four would lose
      // every page they had opened. Replies go with their parent because the
      // database deletes them by cascade.
      const doomed = new Set([id]);
      let grew = true;
      while (grew) {
        grew = false;
        for (const c of comments) {
          if (!doomed.has(c.id) && c.comment_parent && doomed.has(c.comment_parent)) {
            doomed.add(c.id);
            grew = true;
          }
        }
      }
      setComments((current) => current.filter((c) => !doomed.has(c.id)));
      setTotal((n) => Math.max(0, n - doomed.size));

      notify('success', doomed.size === 1 ? 'Comment removed.' : `Comment and ${doomed.size - 1} ${doomed.size === 2 ? 'reply' : 'replies'} removed.`);
    } catch (err) {
      notify('error', err.message);
    } finally {
      setRemoving(null);
    }
  };

  if (comments === null) {
    return (
      <Card title="Comments">
        <div className="gc-loading"><span className="gc-spinner" /> Loading…</div>
      </Card>
    );
  }

  const rows = inThreadOrder(comments);

  return (
    <Card title={`Comments (${total})`}>
      {comments.length === 0 ? (
        <EmptyState title="Nothing yet">
          Comments left on this website will appear here, and you can remove any of them.
        </EmptyState>
      ) : (
        <ul className="gc-mod-list">
          {rows.map(({ comment: c, buried }) => (
            <li key={c.id} className="gc-mod-item">
              <Avatar user={c.by_user} size={32} />
              <div className="gc-mod-main">
                <div className="gc-mod-head">
                  <span className="gc-name">{c.by_user?.name || 'Deleted user'}</span>
                  <TimeAgo iso={c.created_at} />
                  {c.comment_parent ? <span className="gc-tag">reply</span> : null}
                  {c.liked_by ? (
                    <span className="gc-tag">
                      {c.liked_by} like{c.liked_by === 1 ? '' : 's'}
                    </span>
                  ) : null}
                </div>
                <p className="gc-mod-body">{c.comment_description}</p>
              </div>
              <ConfirmButton
                question={deleteQuestion(buried)}
                busy={removing === c.id}
                onConfirm={() => remove(c.id)}
              >
                <IconTrash />
                Delete
              </ConfirmButton>
            </li>
          ))}
        </ul>
      )}

      {cursor ? (
        <div className="gc-load-more">
          <Button onClick={loadMore} busy={loadingMore}>
            Load more comments
          </Button>
        </div>
      ) : null}
    </Card>
  );
}

export default function WebsiteDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { notify } = useStore();
  const [website, setWebsite] = useState(null);
  const [error, setError] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    api
      .get(`/websites/${id}`)
      .then((data) => setWebsite(data.website))
      .catch((err) => setError(err.message));
  }, [id]);

  const removeWebsite = async () => {
    setDeleting(true);
    try {
      await api.delete(`/websites/${id}`);
      notify('success', 'Website removed.');
      navigate('/overview');
    } catch (err) {
      notify('error', err.message);
      setDeleting(false);
    }
  };

  if (error) {
    return (
      <>
        <Link to="/overview" className="gc-back"><IconBack /> Overview</Link>
        <div className="gc-alert">{error}</div>
      </>
    );
  }

  if (!website) {
    return <div className="gc-loading"><span className="gc-spinner" /> Loading…</div>;
  }

  const snippet = embedSnippet(website.id);

  return (
    <>
      <Link to="/overview" className="gc-back"><IconBack /> Overview</Link>

      <div className="gc-page-head">
        <div>
          <h1 className="gc-page-title">{website.website_name}</h1>
          <p className="gc-page-sub">
            <a href={website.website_url} target="_blank" rel="noopener noreferrer">
              {website.website_url}
            </a>
          </p>
        </div>
      </div>

      <Card title="Embed code" action={<CopyButton value={snippet} />}>
        <p className="gc-card-note">
          Paste these two lines where the comments should appear. The div is the box;
          the script loads the widget.
        </p>
        <pre className="gc-code">{snippet}</pre>
      </Card>

      <Settings website={website} onSaved={setWebsite} />

      <Comments websiteId={website.id} />

      <Card title="Danger zone">
        <p className="gc-card-note">
          Removing this website deletes every comment left on it. The embed code will
          stop working.
        </p>
        <ConfirmButton
          question={`Delete ${website.website_name} and all its comments?`}
          busy={deleting}
          onConfirm={removeWebsite}
        >
          <IconTrash />
          Delete website
        </ConfirmButton>
      </Card>
    </>
  );
}
