import { useEffect, useState } from 'react';
import { useStore } from './store.jsx';

const icon = (path, props = {}) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
    className="gc-icon"
    {...props}
  >
    {path}
  </svg>
);

export const IconPlus = () => icon(<><line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" /></>);
export const IconCopy = () => icon(<><rect x="9" y="9" width="12" height="12" rx="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" /></>);
export const IconCheck = () => icon(<polyline points="20 6 9 17 4 12" />);
export const IconTrash = () => icon(<><polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" /></>);
export const IconBack = () => icon(<><line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" /></>);
export const IconExternal = () => icon(<><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /><polyline points="15 3 21 3 21 9" /><line x1="10" y1="14" x2="21" y2="3" /></>);
export const IconClose = () => icon(<><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></>);

export function Button({ variant = 'ghost', size, busy, children, ...rest }) {
  return (
    <button
      type="button"
      className={`gc-btn gc-btn-${variant}${size ? ` gc-btn-${size}` : ''}`}
      {...rest}
      // After the spread, not before: rest carries the caller's own `disabled`,
      // and a later JSX prop wins. Written above the spread, a busy button stays
      // clickable, and a second click submits the form again.
      disabled={busy || rest.disabled}
    >
      {busy ? <span className="gc-spinner" /> : children}
    </button>
  );
}

export function Field({ label, hint, id, textarea, ...rest }) {
  const Tag = textarea ? 'textarea' : 'input';
  return (
    <label className="gc-field" htmlFor={id}>
      <span className="gc-field-label">{label}</span>
      <Tag id={id} className="gc-input" {...rest} />
      {hint ? <span className="gc-field-hint">{hint}</span> : null}
    </label>
  );
}

export function Avatar({ user, size = 36 }) {
  const name = user?.name?.trim() || '?';
  const initials = name.split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase();
  const style = { width: size, height: size, fontSize: Math.round(size * 0.4) };

  if (user?.profile_image) {
    return <img className="gc-avatar" style={style} src={user.profile_image} alt="" />;
  }
  return (
    <span className="gc-avatar gc-avatar-initials" style={style} aria-hidden="true">
      {initials}
    </span>
  );
}

export function Card({ title, action, children, className = '' }) {
  return (
    <section className={`gc-card ${className}`}>
      {title || action ? (
        <div className="gc-card-head">
          <h2 className="gc-card-title">{title}</h2>
          {action}
        </div>
      ) : null}
      {children}
    </section>
  );
}

export function EmptyState({ title, children, action }) {
  return (
    <div className="gc-empty">
      <p className="gc-empty-title">{title}</p>
      {children ? <p className="gc-empty-body">{children}</p> : null}
      {action}
    </div>
  );
}

/** Copying to the clipboard can fail — permissions, insecure origin — so it says so. */
export function CopyButton({ value, label = 'Copy' }) {
  const [state, setState] = useState('idle');

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setState('done');
    } catch {
      setState('failed');
    }
  };

  useEffect(() => {
    if (state === 'idle') return;
    const t = setTimeout(() => setState('idle'), 2000);
    return () => clearTimeout(t);
  }, [state]);

  return (
    <Button size="sm" onClick={copy}>
      {state === 'done' ? <IconCheck /> : <IconCopy />}
      {state === 'done' ? 'Copied' : state === 'failed' ? 'Press Ctrl+C' : label}
    </Button>
  );
}

const UNITS = [
  ['year', 31536000],
  ['month', 2592000],
  ['week', 604800],
  ['day', 86400],
  ['hour', 3600],
  ['minute', 60],
];

export function TimeAgo({ iso }) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return null;

  const seconds = Math.round((Date.now() - date.getTime()) / 1000);
  const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

  let text = rtf.format(-seconds, 'second');
  for (const [unit, per] of UNITS) {
    if (Math.abs(seconds) >= per) {
      text = rtf.format(-Math.round(seconds / per), unit);
      break;
    }
  }
  return (
    <time className="gc-time" dateTime={iso} title={date.toLocaleString()}>
      {text}
    </time>
  );
}

export function Toast() {
  const { toast, dismissToast } = useStore();

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(dismissToast, 4000);
    return () => clearTimeout(t);
  }, [toast, dismissToast]);

  if (!toast) return null;

  return (
    <div className={`gc-toast gc-toast-${toast.type}`} role="status">
      <span>{toast.message}</span>
      <button type="button" className="gc-toast-close" onClick={dismissToast} aria-label="Dismiss">
        <IconClose />
      </button>
    </div>
  );
}

/** Destructive actions ask first, in place, rather than through window.confirm. */
export function ConfirmButton({ onConfirm, children, question, busy }) {
  const [asking, setAsking] = useState(false);

  if (!asking) {
    return (
      <Button variant="danger" size="sm" onClick={() => setAsking(true)}>
        {children}
      </Button>
    );
  }

  return (
    <span className="gc-confirm">
      <span className="gc-confirm-q">{question}</span>
      <Button size="sm" onClick={() => setAsking(false)}>
        Keep it
      </Button>
      <Button variant="danger" size="sm" busy={busy} onClick={onConfirm}>
        Yes, delete
      </Button>
    </span>
  );
}
