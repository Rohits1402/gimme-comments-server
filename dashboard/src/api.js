// The dashboard is served by the same application it talks to, so unlike the widget
// there is nothing to discover: the API is always on this origin. In development
// Vite proxies /api to localhost:8080, which keeps it same-origin there too.
const BASE = '/api/v1';

const TOKEN_KEY = 'gimme_comment_access_token';
const REFRESH_KEY = 'gimme_comment_refresh_token';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY);

/** Store whatever /auth/login or /auth/refresh just returned. */
export function setTokens(data) {
  if (data?.token) localStorage.setItem(TOKEN_KEY, data.token);
  if (data?.refresh_token) localStorage.setItem(REFRESH_KEY, data.refresh_token);
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

function parseJson(text) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

// These hand out or destroy tokens. A 401 from one of them is the answer itself, not
// an access token that aged out, so retrying would only repeat the same refusal.
const NEVER_RETRY = ['/auth/login', '/auth/logout', '/auth/refresh'];

let refreshInFlight = null;

/**
 * One refresh at a time, across every tab.
 *
 * Refresh tokens are single-use: presenting one retires it and returns a replacement.
 * Two tabs refreshing with the same token therefore look exactly like a stolen token
 * being replayed, which is something the server is entitled to treat as a theft. It
 * forgives it inside a short window, but the honest fix is not to cause it.
 */
function withLock(work) {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request('gimme-comments-refresh', work);
  }
  return work();   // no Web Locks: the in-tab guard below is all there is
}

async function doRefresh(tokenWeStartedWith) {
  return withLock(async () => {
    // Another tab may have refreshed while we queued for the lock. If storage already
    // holds a newer token, ours is spent and there is nothing left to do.
    const current = getRefreshToken();
    if (!current) return false;
    if (current !== tokenWeStartedWith) return true;

    let res;
    try {
      res = await fetch(BASE + '/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refresh_token: current }),
      });
    } catch {
      return false;   // offline: keep the tokens, a later attempt may succeed
    }

    if (!res.ok) {
      // Expired, revoked, or the session was ended because the token was presented
      // twice. Whichever it was, this browser is signed out.
      clearTokens();
      return false;
    }

    setTokens(await res.json());
    return true;
  });
}

function refreshOnce() {
  const started = getRefreshToken();
  if (!started) return Promise.resolve(false);
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = doRefresh(started).finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function request(method, path, body, retry = true) {
  const headers = {};

  // Read on every request, never captured once at startup.
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let payload = body;
  if (body !== undefined && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: payload });
  } catch {
    throw new ApiError('Could not reach the server. Check your connection.', 0);
  }

  if (res.status === 401 && retry && !NEVER_RETRY.includes(path) && getRefreshToken()) {
    // Access tokens are short on purpose, so a 401 usually only means this one aged
    // out. Swap it for a fresh one and try the request once more before concluding
    // that the reader is signed out.
    if (await refreshOnce()) {
      return request(method, path, body, false);
    }
  }

  // Refused with nothing left to try: forget both tokens rather than keep sending
  // credentials the server has rejected.
  if (res.status === 401) clearTokens();
  if (res.status === 204) return null;

  const text = await res.text();
  const data = text ? parseJson(text) : null;

  if (!res.ok) {
    throw new ApiError(data?.msg || `Request failed (${res.status})`, res.status);
  }
  return data;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  patch: (path, body) => request('PATCH', path, body),
  delete: (path) => request('DELETE', path),
};
