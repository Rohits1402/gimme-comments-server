import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useStore } from '../store.jsx';
import { Button, Card, EmptyState, Field, IconExternal, IconPlus, TimeAgo } from '../ui.jsx';

/**
 * People type "www.example.com", not "https://www.example.com". Refusing that is
 * pedantry — the scheme is obvious. Normalising also stops the same site being
 * registered twice under two spellings, which the unique constraint on the URL
 * would otherwise happily allow.
 *
 * Returns null when the input is not an address at all.
 */
function normaliseUrl(raw) {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const url = new URL(withScheme);
    // "localhost" aside, an address without a dot is a typo, not a host.
    if (!url.hostname.includes('.') && url.hostname !== 'localhost') return null;
    const path = url.pathname.replace(/\/+$/, '');
    return url.origin + path;
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
            // Show what will actually be saved rather than saving something
            // different from what is on screen.
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

export default function Websites() {
  const { notify } = useStore();
  const [websites, setWebsites] = useState(null);
  const [counts, setCounts] = useState({});
  const [adding, setAdding] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await api.get('/websites');
      const list = data.websites ?? [];
      setWebsites(list);

      // One request per website. Fine for a handful, and they run concurrently —
      // but the right fix is for the API to return the count with the website.
      const entries = await Promise.all(
        list.map(async (w) => {
          try {
            const res = await api.get(`/comments/comment/${w.id}`);
            return [w.id, res.comments?.length ?? 0];
          } catch {
            return [w.id, null];
          }
        })
      );
      setCounts(Object.fromEntries(entries));
    } catch (err) {
      notify('error', err.message);
      setWebsites([]);
    }
  }, [notify]);

  useEffect(() => {
    load();
  }, [load]);

  if (websites === null) {
    return <div className="gc-loading"><span className="gc-spinner" /> Loading your websites…</div>;
  }

  return (
    <>
      <div className="gc-page-head">
        <div>
          <h1 className="gc-page-title">Websites</h1>
          <p className="gc-page-sub">Every site using your comment widget.</p>
        </div>
        {!adding ? (
          <Button variant="primary" onClick={() => setAdding(true)}>
            <IconPlus />
            Add website
          </Button>
        ) : null}
      </div>

      {adding ? (
        <CreateForm
          onCancel={() => setAdding(false)}
          onCreated={(created) => {
            setAdding(false);
            setWebsites((current) => [created, ...(current ?? [])]);
            setCounts((current) => ({ ...current, [created.id]: 0 }));
          }}
        />
      ) : null}

      {websites.length === 0 && !adding ? (
        <EmptyState
          title="No websites yet"
          action={
            <Button variant="primary" onClick={() => setAdding(true)}>
              <IconPlus />
              Add your first website
            </Button>
          }
        >
          Register a site here, then paste two lines into its HTML and it has comments.
        </EmptyState>
      ) : null}

      <div className="gc-website-list">
        {websites.map((w) => (
          <Link key={w.id} to={`/websites/${w.id}`} className="gc-website">
            <div className="gc-website-main">
              <span className="gc-website-name">{w.website_name}</span>
              <span className="gc-website-url">{w.website_url}</span>
              {w.website_description ? (
                <span className="gc-website-desc">{w.website_description}</span>
              ) : null}
            </div>
            <div className="gc-website-meta">
              <span className="gc-badge">
                {counts[w.id] === null
                  ? '—'
                  : counts[w.id] === undefined
                    ? '…'
                    : `${counts[w.id]} comment${counts[w.id] === 1 ? '' : 's'}`}
              </span>
              <span className="gc-muted gc-small">
                added <TimeAgo iso={w.created_at} />
              </span>
            </div>
            <IconExternal />
          </Link>
        ))}
      </div>
    </>
  );
}
