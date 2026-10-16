import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from '../services/api.js';
import { useAuth } from './AuthContext.jsx';

const Ctx = createContext(null);
export const useWishlist = () => useContext(Ctx);

export function WishlistProvider({ children }) {
  const { user } = useAuth();
  const [items, setItems] = useState([]);

  const reload = useCallback(async () => {
    if (!user) { setItems([]); return; }
    try { setItems(await api.get('/api/wishlist')); } catch { /* ignore */ }
  }, [user]);
  useEffect(() => { reload(); }, [reload]);

  const has = (id) => items.some((p) => p.id === id);
  const toggle = async (id) => {
    if (has(id)) await api.del(`/api/wishlist/${id}`); else await api.post(`/api/wishlist/${id}`);
    await reload();
  };
  return <Ctx.Provider value={{ items, has, toggle, reload }}>{children}</Ctx.Provider>;
}
