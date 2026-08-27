import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useStore } from '../store.jsx';
import {
  Avatar,
  Button,
  Card,
  ConfirmButton,
  EmptyState,
  Field,
  IconExternal,
  IconPlus,
  IconTrash,
  TimeAgo,
} from '../ui.jsx';

/**
 * People type "www.example.com", not "https://www.example.com". Refusing that is
 * pedantry — the scheme is obvious. Normalising also stops the same site being
 * registered twice under two spellings, which the unique constraint on the URL
 * would otherwise happily allow. Returns null when the input is not an address.
 */
function normaliseUrl(raw) {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(withScheme);
    if (!url.hostname.includes('.') && url.hostname !== 'localhost') return null;
    return url.origin + url.pathname.replace(/\/+$/, '');
  } catch {
    return null;
  }
}

function CreateForm({ onCreated, onCancel }) {
  const { notify } = useStore();
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');
  const [description, setDescription] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    const normalised = normaliseUrl(url);
    if (!normalised) {
      setError('That does not look like a web address.');
      return;
    }

    setBusy(true);
    setError(null);
    try {
      const data = await api.post('/websites', {
        website_name: name,
        website_url: normalised,
        website_description: description,
      });
      notify('success', 'Website added.');
      onCreated(data.website);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card title="Add a website">
      <form onSubmit={submit}>
        <Field
          id="w-name"
          label="Name"
          required
          placeholder="My blog"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <Field
          id="w-url"
          label="Address"
          type="text"
          inputMode="url"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck="false"
          required
          placeholder="www.example.com"
          hint="https:// is added for you. Each address can only be registered once."
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          onBlur={() => {
            const normalised = normaliseUrl(url);
            if (normalised) setUrl(normalised);
          }}
        />
        <Field
          id="w-desc"
          label="Description"
          textarea
          rows={2}
          placeholder="Optional"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        {error ? <p className="gc-form-error">{error}</p> : null}
        <div className="gc-row">
          <Button variant="primary" type="submit" busy={busy}>
            Add website
          </Button>
          <Button onClick={onCancel}>Cancel</Button>
        </div>
      </form>
    </Card>
  );
}

function Stat({ label, value, note }) {
  return (
    <div className="gc-stat">
      <div className="gc-stat-label">{label}</div>
      <div className="gc-stat-value">{value}</div>
      {note ? <div className="gc-stat-note">{note}</div> : null}
    </div>
  );
}

function Activity({ daily }) {
  const busiest = Math.max(1, ...daily.map((d) => d.count));
  const fmt = (iso) =>
    new Date(iso + 'T00:00:00Z').toLocaleDateString(undefined, {
      day: 'numeric',
      month: 'short',
      timeZone: 'UTC',
    });

  return (
    <Card title="Last 14 days" action={<span className="gc-hint">comments per day</span>}>
      <div className="gc-bars">
        {daily.map((d) => (
          <div
            key={d.day}
            className={`gc-bar${d.count ? '' : ' gc-bar-empty'}`}
            // The tallest bar is the busiest day rather than a fixed ceiling, so a
            // quiet fortnight still shows its own shape instead of a flat line.
            style={{ height: `${Math.max(6, (d.count / busiest) * 100)}%` }}
            title={`${fmt(d.day)}: ${d.count} comment${d.count === 1 ? '' : 's'}`}
          />
        ))}
      </div>
      <div className="gc-bars-axis">
        <span>{fmt(daily[0].day)}</span>
        <span>{fmt(daily[daily.length - 1].day)}</span>
      </div>
    </Card>
  );
}

function Recent({ comments, onRemoved }) {
  const { notify } = useStore();
  const [removing, setRemoving] = useState(null);

  const remove = async (id) => {
    setRemoving(id);
    try {
      await api.delete(`/comments/comment/${id}`);
      notify('success', 'Comment removed.');
      await onRemoved();
    } catch (err) {
      notify('error', err.message);
    } finally {
      setRemoving(null);
    }
  };

  return (
    <Card title="Recent comments">
      {comments.length === 0 ? (
        <EmptyState title="Nothing yet">
          Comments left on any of your websites appear here, newest first, and you can
          remove one without leaving this page.
        </EmptyState>
      ) : (
        <ul className="gc-mod-list">
          {comments.map((c) => (
            <li key={c.id} className="gc-mod-item">
              <Avatar user={c.by_user} size={32} />
              <div className="gc-mod-main">
                <div className="gc-mod-head">
                  <span className="gc-name">{c.by_user?.name || 'Deleted user'}</span>
                  <TimeAgo iso={c.created_at} />
                  <Link to={`/websites/${c.website.id}`} className="gc-tag gc-tag-link">
                    {c.website.website_name}
                  </Link>
                  {c.is_reply ? <span className="gc-tag">reply</span> : null}
                  {c.liked_by ? (
                    <span className="gc-tag">
                      {c.liked_by} like{c.liked_by === 1 ? '' : 's'}
                    </span>
                  ) : null}
                </div>
                <p className="gc-mod-body">{c.comment_description}</p>
              </div>
              <ConfirmButton
                question="Delete this comment?"
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
    </Card>
  );
}

function WebsiteList({ websites, onAdd }) {
  return (
    <Card
      title="Your websites"
      action={
        <Button variant="primary" size="sm" onClick={onAdd}>
          <IconPlus />
          Add website
        </Button>
      }
    >
      <div className="gc-website-list">
        {websites.map((w) => (
          <Link key={w.id} to={`/websites/${w.id}`} className="gc-website">
            <div className="gc-website-main">
              <span className="gc-website-name">{w.website_name}</span>
              <span className="gc-website-url">{w.website_url}</span>
            </div>
            <span className={`gc-badge${w.comment_count ? '' : ' gc-badge-quiet'}`}>
              {w.comment_count
                ? `${w.comment_count} comment${w.comment_count === 1 ? '' : 's'}`
                : 'no comments yet'}
            </span>
            <IconExternal />
          </Link>
        ))}
      </div>
    </Card>
  );
}

export default function Overview() {
  const { user, notify } = useStore();
  const [data, setData] = useState(null);
  const [websites, setWebsites] = useState(null);
  const [adding, setAdding] = useState(false);

  const load = useCallback(async () => {
    try {
      // Two requests for the whole page. Before the overview endpoint existed this
      // was one per website, plus one for every website's comments.
      const [overview, sites] = await Promise.all([
        api.get('/overview'),
        api.get('/websites'),
      ]);
      setData(overview);
      setWebsites(sites.websites ?? []);
    } catch (err) {
      notify('error', err.message);
      setWebsites([]);
    }
  }, [notify]);

  useEffect(() => {
    load();
  }, [load]);

  if (websites === null || data === null) {
    return (
      <div className="gc-loading">
        <span className="gc-spinner" /> Loading…
      </div>
    );
  }

  const firstName = user?.name?.trim().split(/\s+/)[0];

  // Nothing has been set up yet, so the page is a single instruction rather than a
  // row of zeroes and an empty chart.
  if (websites.length === 0 && !adding) {
    return (
      <>
        <div className="gc-page-head">
          <div>
            <h1 className="gc-page-title">Welcome{firstName ? `, ${firstName}` : ''}</h1>
            <p className="gc-page-sub">One step to go.</p>
          </div>
        </div>
        <EmptyState
          title="Register your first website"
          action={
            <Button variant="primary" onClick={() => setAdding(true)}>
              <IconPlus />
              Add a website
            </Button>
          }
        >
          Add the address of a site you own. You get an id for it, you paste two lines
          into its HTML, and it has comments.
        </EmptyState>
      </>
    );
  }

  const { totals, daily, recent } = data;

  return (
    <>
      <div className="gc-page-head">
        <div>
          <h1 className="gc-page-title">Overview</h1>
          <p className="gc-page-sub">
            Everything across your {totals.websites} website
            {totals.websites === 1 ? '' : 's'}.
          </p>
        </div>
      </div>

      {websites.length > 0 ? (
        <div className="gc-stats">
          <Stat
            label="Comments"
            value={totals.comments}
            note={`${daily.reduce((n, d) => n + d.count, 0)} in the last 14 days`}
          />
          <Stat label="Likes" value={totals.likes} note="across all sites" />
          <Stat
            label="People"
            value={totals.people}
            note={totals.people === 1 ? 'has commented' : 'have commented'}
          />
        </div>
      ) : null}

      <Activity daily={daily} />

      <Recent comments={recent} onRemoved={load} />

      {adding ? (
        <CreateForm
          onCancel={() => setAdding(false)}
          onCreated={() => {
            setAdding(false);
            load();
          }}
        />
      ) : (
        <WebsiteList websites={websites} onAdd={() => setAdding(true)} />
      )}
    </>
  );
}
