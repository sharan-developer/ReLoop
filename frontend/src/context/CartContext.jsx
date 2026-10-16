import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from '../services/api.js';
import { useAuth } from './AuthContext.jsx';

const CartCtx = createContext(null);
export const useCart = () => useContext(CartCtx);
const EMPTY = { items: [], itemCount: 0, subtotal: 0, hasUnavailable: false };

export function CartProvider({ children }) {
  const { user } = useAuth();
  const [cart, setCart] = useState(EMPTY);
  const [loading, setLoading] = useState(false);

  const reload = useCallback(async () => {
    if (!user) { setCart(EMPTY); return; }
    setLoading(true);
    try { setCart(await api.get('/api/cart')); } catch { /* shown on cart page */ } finally { setLoading(false); }
  }, [user]);
  useEffect(() => { reload(); }, [reload]);

  const add = async (productId, quantity = 1) => setCart(await api.post('/api/cart/items', { productId, quantity }));
  const setQty = async (itemId, quantity) => setCart(await api.put(`/api/cart/items/${itemId}`, { quantity }));
  const remove = async (itemId) => setCart(await api.del(`/api/cart/items/${itemId}`));
  const clear = async () => setCart(await api.del('/api/cart'));

  return <CartCtx.Provider value={{ cart, loading, add, setQty, remove, clear, reload }}>{children}</CartCtx.Provider>;
}
