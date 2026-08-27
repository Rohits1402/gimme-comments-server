// The API origin cannot be baked in at build time. The same bundle is served from
// localhost during development and from Render in production, and it runs inside a
// third-party page whose own origin is irrelevant. initialize-gimme-comments.js is
// the only code that knows the answer — it reads the URL it was itself loaded from
// — so it leaves the value on window before loading this bundle.
const BASE =
  (typeof window !== 'undefined' && window.__GIMME_COMMENTS_API__) ||
  import.meta.env.VITE_API_BASE ||
  'http://localhost:8080/api/v1';

const TOKEN_KEY = 'gimme_comment_access_token';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

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

async function request(method, path, body) {
  const headers = {};

  // Read the token on every request. The old client built one axios instance and
  // captured the token at that moment, so after logging in it kept sending the
  // header it had at page load until something forced a reload.
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
    // fetch only rejects when the request never completed — DNS, offline, CORS.
    // There is no response to read here, which is where the old client crashed.
    throw new ApiError('Could not reach the server. Check your connection.', 0);
  }

  // A token the server refuses is a token worth forgetting, whether it expired or
  // was issued by an older version of this service.
  if (res.status === 401) clearToken();

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
