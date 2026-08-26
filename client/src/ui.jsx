import { useEffect } from 'react';
import { useStore } from './store.jsx';

// Icons are inline SVG rather than an icon package. A package would pull in files
// that Vite emits as assets, and asset URLs would resolve against the host page's
// origin instead of ours. Inline markup has no URL to get wrong.
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

export const IconHeart = ({ filled }) =>
  icon(
    <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1L12 21l7.7-7.6 1.1-1a5.5 5.5 0 0 0 0-7.8z" />,
    filled ? { fill: 'currentColor' } : {}
  );

export const IconReply = () =>
  icon(
    <>
      <polyline points="9 17 4 12 9 7" />
      <path d="M20 18v-2a4 4 0 0 0-4-4H4" />
    </>
  );

export const IconEdit = () =>
  icon(
    <>
      <path d="M12 20h9" />
      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" />
    </>
  );

export const IconTrash = () =>
  icon(
    <>
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    </>
  );

export const IconClose = () =>
  icon(
    <>
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </>
  );

export function Button({ variant = 'ghost', size, busy, children, ...rest }) {
  return (
    <button
      type="button"
      className={`gc-btn gc-btn-${variant}${size ? ` gc-btn-${size}` : ''}`}
      disabled={busy || rest.disabled}
      {...rest}
    >
      {busy ? <span className="gc-spinner" /> : children}
    </button>
  );
}

export function Field({ label, hint, error, id, ...rest }) {
  return (
    <label className="gc-field" htmlFor={id}>
      <span className="gc-field-label">{label}</span>
      <input id={id} className={`gc-input${error ? ' gc-input-bad' : ''}`} {...rest} />
      {error ? <span className="gc-field-error">{error}</span> : null}
      {hint && !error ? <span className="gc-field-hint">{hint}</span> : null}
    </label>
  );
}

export function Avatar({ user, size = 36 }) {
  const name = user?.name?.trim() || '?';
  const initials = name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  const style = { width: size, height: size, fontSize: Math.round(size * 0.4) };

  if (user?.profile_image) {
    return (
      <img
        className="gc-avatar"
        style={style}
        src={user.profile_image}
        alt=""
        loading="lazy"
      />
    );
  }
  // A deleted author has no name, so it renders as a neutral circle rather than a
  // broken image — which is what the old widget showed.
  return (
    <span className="gc-avatar gc-avatar-initials" style={style} aria-hidden="true">
      {initials}
    </span>
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
  for (const [unit, secondsPer] of UNITS) {
    if (Math.abs(seconds) >= secondsPer) {
      text = rtf.format(-Math.round(seconds / secondsPer), unit);
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
      <button
        type="button"
        className="gc-toast-close"
        onClick={dismissToast}
        aria-label="Dismiss"
      >
        <IconClose />
      </button>
    </div>
  );
}
