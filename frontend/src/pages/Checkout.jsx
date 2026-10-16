import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AddressForm from '../components/AddressForm.jsx';
import { EmptyState, ErrorState, Modal, PageLoader } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';
import { openRazorpay } from '../services/razorpay.js';
import { inr } from '../utils/format.js';

export default function Checkout() {
  const { user } = useAuth();
  const { cart, loading: cartLoading, reload } = useCart();
  const { push } = useToast();
  const nav = useNavigate();
  const addrs = useAsync(() => api.get('/api/addresses'), []);
  const cfg = useAsync(() => api.get('/api/payments/config'), []);
  const [addressId, setAddressId] = useState(null);
  const [method, setMethod] = useState('COD');
  const [adding, setAdding] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (addrs.data && !addressId) setAddressId((addrs.data.find((a) => a.defaultAddress) || addrs.data[0])?.id ?? null);
  }, [addrs.data]); // eslint-disable-line react-hooks/exhaustive-deps

  if (cartLoading || addrs.loading) return <PageLoader />;
  if (addrs.error) return <div className="container-x py-10"><ErrorState error={addrs.error} onRetry={addrs.reload} /></div>;
  if (cart.items.length === 0) return <div className="container-x py-16"><EmptyState title="Nothing to check out" action={<Link to="/products" className="btn-primary">Browse products</Link>} /></div>;

  const place = async () => {
    setError('');
    if (!addressId) return setError('Please add or select a delivery address.');
    setBusy(true);
    let order;
    try {
      order = await api.post('/api/orders', { addressId, paymentMethod: method });
      await reload();
    } catch (e) { setError(e.message); await reload(); setBusy(false); return; }
    try {
      if (method === 'RAZORPAY') {
        const result = await openRazorpay(order, user);
        order = await api.post('/api/payments/razorpay/verify', result); // server verifies the signature
      }
      push('Order placed successfully');
      nav(`/orders/${order.id}`, { replace: true, state: { confirmed: true } });
    } catch (e) {
      push(e.message, 'error');
      nav(`/orders/${order.id}`, { replace: true });
    } finally { setBusy(false); }
  };

  return (
    <div className="container-x py-8">
      <h1 className="mb-5 text-2xl">Checkout</h1>
      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <section className="card p-5">
            <div className="mb-3 flex items-center justify-between"><h2 className="text-lg">Delivery address</h2><button className="text-sm font-semibold text-brand-700 hover:underline" onClick={() => setAdding(true)}>+ Add new</button></div>
            {addrs.data.length === 0 && <p className="text-sm text-neutral-500">You have no saved addresses yet.</p>}
            <div className="grid gap-3 sm:grid-cols-2">
              {addrs.data.map((a) => (
                <label key={a.id} className={`cursor-pointer rounded-lg border p-3 text-sm ${addressId === a.id ? 'border-brand-600 bg-brand-50' : 'border-neutral-200'}`}>
                  <input type="radio" name="addr" className="mr-2" checked={addressId === a.id} onChange={() => setAddressId(a.id)} />
                  <strong>{a.label}</strong>{a.defaultAddress && <span className="ml-2 text-xs text-brand-700">Default</span>}
                  <p className="mt-1 text-neutral-600">{a.line1}{a.line2 ? `, ${a.line2}` : ''}, {a.city}, {a.state} {a.postalCode}</p>
                  <p className="text-neutral-500">{a.phone}</p>
                </label>
              ))}
            </div>
          </section>
          <section className="card p-5">
            <h2 className="mb-3 text-lg">Payment method</h2>
            <label className="mb-2 flex items-center gap-2 text-sm"><input type="radio" name="pay" checked={method === 'COD'} onChange={() => setMethod('COD')} />Cash on Delivery</label>
            <label className={`flex items-center gap-2 text-sm ${cfg.data?.razorpayEnabled ? '' : 'text-neutral-400'}`}>
              <input type="radio" name="pay" disabled={!cfg.data?.razorpayEnabled} checked={method === 'RAZORPAY'} onChange={() => setMethod('RAZORPAY')} />
              Pay online (Razorpay){!cfg.data?.razorpayEnabled && ' — not configured on this server'}
            </label>
          </section>
        </div>
        <aside className="card h-fit p-5">
          <h2 className="text-lg">Order summary</h2>
          <ul className="mt-3 space-y-2 text-sm">
            {cart.items.map((i) => <li key={i.id} className="flex justify-between gap-3"><span className="truncate">{i.name} × {i.quantity}</span><span>{inr(i.lineTotal)}</span></li>)}
          </ul>
          <div className="mt-4 flex justify-between border-t pt-3 text-base font-bold"><span>Total</span><span>{inr(cart.subtotal)}</span></div>
          {(error || cart.hasUnavailable) && <p className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700" role="alert">{error || 'Some items are unavailable. Please update your cart.'}</p>}
          <button className="btn-primary mt-4 w-full" disabled={busy || cart.hasUnavailable} onClick={place}>{busy ? 'Placing order…' : method === 'COD' ? 'Place order' : 'Pay & place order'}</button>
          <p className="mt-2 text-xs text-neutral-500">The final total is calculated by the server.</p>
        </aside>
      </div>
      {adding && (
        <Modal title="Add address" onClose={() => setAdding(false)} wide>
          <AddressForm onCancel={() => setAdding(false)} onSubmit={async (v) => { const a = await api.post('/api/addresses', v); await addrs.reload(); setAddressId(a.id); setAdding(false); }} />
        </Modal>
      )}
    </div>
  );
}
