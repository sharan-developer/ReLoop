import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, refreshSession, setAccessToken, setAuthLostHandler } from '../services/api.js';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setAuthLostHandler(() => setUser(null));
    let alive = true;
    // Restore the session from the HttpOnly refresh cookie (if any).
    refreshSession().then((s) => { if (alive) { setUser(s?.user || null); setLoading(false); } });
    return () => { alive = false; };
  }, []);

  const finish = (data) => { setAccessToken(data.accessToken); setUser(data.user); return data.user; };
  const login = useCallback(async (email, password) => finish(await api.post('/api/auth/login', { email, password })), []);
  const register = useCallback(async (payload) => finish(await api.post('/api/auth/register', payload)), []);
  const logout = useCallback(async () => {
    try { await api.post('/api/auth/logout'); } catch { /* ignore */ }
    setAccessToken(null);
    setUser(null);
  }, []);

  return <AuthCtx.Provider value={{ user, loading, login, register, logout, setUser }}>{children}</AuthCtx.Provider>;
}
