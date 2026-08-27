// The dashboard is served by the same application it talks to, so unlike the widget
// there is nothing to discover: the API is always on this origin. In development
// Vite proxies /api to localhost:8080, which keeps it same-origin there too.
const BASE = '/api/v1';

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
