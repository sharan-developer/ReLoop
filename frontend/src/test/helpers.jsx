import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext.jsx';
import { CartProvider } from '../context/CartContext.jsx';
import { ToastProvider } from '../context/ToastContext.jsx';
import { WishlistProvider } from '../context/WishlistContext.jsx';

export const json = (body, status = 200) => Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));

/** Mocks fetch with a route table: { 'METHOD /path': (url, init) => Response | body }. Unmatched calls return 404. */
export function mockApi(routes) {
  const calls = [];
  globalThis.fetch = vi.fn((url, init = {}) => {
    const method = (init.method || 'GET').toUpperCase();
    const path = String(url);
    calls.push({ method, path, body: init.body ? JSON.parse(init.body) : undefined });
    const key = Object.keys(routes).find((k) => {
      const [m, p] = k.split(' ');
      return m === method && (path === p || path.startsWith(p + '?'));
    });
    if (!key) return json({ message: 'not found' }, key === undefined && path.startsWith('/api/auth/refresh') ? 401 : 404);
    const r = routes[key];
    const res = typeof r === 'function' ? r(path, init) : r;
    return res instanceof Promise ? res : json(res);
  });
  return calls;
}

export function renderApp(ui, { route = '/' } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ToastProvider><AuthProvider><CartProvider><WishlistProvider>{ui}</WishlistProvider></CartProvider></AuthProvider></ToastProvider>
    </MemoryRouter>
  );
}

export const user = { id: 1, email: 'a@b.co', fullName: 'Asha Test', phone: null, role: 'CUSTOMER' };
