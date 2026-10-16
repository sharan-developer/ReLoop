import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ConfirmDialog, EmptyState } from '../components/ui.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { inr } from '../utils/format.js';

export default function Cart() {
  const { cart, loading, setQty, remove, clear } = useCart();
  const { push } = useToast();
  const nav = useNavigate();
  const [confirm, setConfirm] = useState(false);
  const run = async (fn) => { try { await fn(); } catch (e) { push(e.message, 'error'); } };

  if (!loading && cart.items.length === 0) {
    return <div className="container-x py-16"><EmptyState title="Your cart is empty" text="Add refurbished deals or new gadgets to get started." action={<Link to="/products" className="btn-primary">Browse products</Link>} /></div>;
  }
  return (
    <div className="container-x py-8">
      <h1 className="mb-5 text-2xl">Shopping cart</h1>
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-3">
          {cart.items.map((i) => (
            <div key={i.id} className={`card flex gap-4 p-3 ${i.available ? '' : 'border-red-200 bg-red-50/40'}`}>
              <img src={i.imageUrl} alt="" className="h-24 w-28 rounded-lg object-cover" />
              <div className="flex-1">
                <Link to={`/products/${i.productId}`} className="font-semibold hover:text-brand-700">{i.name}</Link>
                <p className="text-sm text-neutral-600">{inr(i.unitPrice)} each</p>
                {!i.available && <p className="mt-1 text-sm font-semibold text-red-600">{i.stock === 0 ? 'Out of stock' : `Only ${i.stock} available. Reduce quantity or remove.`}</p>}
                <div className="mt-2 flex flex-wrap items-center gap-3">
                  <div className="flex items-center rounded-lg border border-neutral-300">
                    <button className="px-3 py-1 hover:bg-neutral-100" aria-label="Decrease" disabled={i.quantity <= 1} onClick={() => run(() => setQty(i.id, i.quantity - 1))}>−</button>
                    <span className="min-w-[2rem] text-center text-sm" aria-live="polite">{i.quantity}</span>
                    <button className="px-3 py-1 hover:bg-neutral-100" aria-label="Increase" disabled={i.quantity >= 20} onClick={() => run(() => setQty(i.id, i.quantity + 1))}>+</button>
                  </div>
                  <button className="text-sm text-red-600 hover:underline" onClick={() => run(() => remove(i.id))}>Remove</button>
                </div>
              </div>
              <div className="text-right font-bold">{inr(i.lineTotal)}</div>
            </div>
          ))}
          <button className="text-sm text-neutral-500 hover:text-red-600" onClick={() => setConfirm(true)}>Clear cart</button>
        </div>
        <aside className="card h-fit p-5">
          <h2 className="text-lg">Order summary</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between"><dt>Items ({cart.itemCount})</dt><dd>{inr(cart.subtotal)}</dd></div>
            <div className="flex justify-between"><dt>Delivery</dt><dd className="text-brand-700">Free</dd></div>
            <div className="flex justify-between border-t pt-3 text-base font-bold"><dt>Subtotal</dt><dd>{inr(cart.subtotal)}</dd></div>
          </dl>
          {cart.hasUnavailable && <p className="mt-3 text-sm text-red-600">Resolve unavailable items to continue.</p>}
          <button className="btn-primary mt-4 w-full" disabled={cart.hasUnavailable} onClick={() => nav('/checkout')}>Proceed to checkout</button>
          <p className="mt-2 text-xs text-neutral-500">Prices and stock are re-checked by our server at checkout.</p>
        </aside>
      </div>
      {confirm && <ConfirmDialog danger title="Clear cart?" message="All items will be removed from your cart." confirmText="Clear cart" onCancel={() => setConfirm(false)} onConfirm={async () => { await run(clear); setConfirm(false); }} />}
    </div>
  );
}
