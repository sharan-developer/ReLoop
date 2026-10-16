// Central API layer: JSON handling, auth header, one silent refresh on 401, uniform errors.
// The access token lives ONLY in this module's memory (never localStorage/sessionStorage),
// so XSS cannot read a long-lived credential. The refresh token is an HttpOnly cookie.
let accessToken = null;
let refreshing = null;
let onAuthLost = () => {};

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || null;
  }
}

export const setAccessToken = (t) => { accessToken = t; };
export const setAuthLostHandler = (fn) => { onAuthLost = fn; };

export const qs = (params = {}) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.append(k, v);
  });
  const s = p.toString();
  return s ? `?${s}` : '';
};

async function parseError(res) {
  let body = null;
  try { body = await res.json(); } catch { /* not json */ }
  return new ApiError(res.status, body?.message || `Request failed (${res.status})`, body?.fieldErrors);
}

export async function refreshSession() {
  if (!refreshing) {
    refreshing = fetch('/api/auth/refresh', { method: 'POST', credentials: 'same-origin' })
      .then(async (res) => {
        if (!res.ok) return null;
        const data = await res.json();
        accessToken = data.accessToken;
        return data;
      })
      .catch(() => null)
      .finally(() => { refreshing = null; });
  }
  return refreshing;
}

async function request(method, path, { body, form, blob, retry = true } = {}) {
  const headers = {};
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  let res;
  try {
    res = await fetch(path, { method, headers, body: payload, credentials: 'same-origin' });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and that the backend is running.');
  }
  if (res.status === 401 && retry && !path.startsWith('/api/auth/')) {
    const s = await refreshSession();
    if (s) return request(method, path, { body, form, blob, retry: false });
    accessToken = null;
    onAuthLost();
  }
  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return null;
  if (blob) return res.blob();
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, body) => request('POST', p, { body }),
  put: (p, body) => request('PUT', p, { body }),
  patch: (p, body) => request('PATCH', p, { body }),
  del: (p) => request('DELETE', p),
  upload: (p, file) => { const f = new FormData(); f.append('file', file); return request('POST', p, { form: f }); },
  blob: (p) => request('GET', p, { blob: true }),
};
