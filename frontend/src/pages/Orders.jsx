import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { ConfirmDialog, EmptyState, ErrorState, Modal, PageLoader, Pagination, StatusBadge } from '../components/ui.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';
import { openRazorpay } from '../services/razorpay.js';
import { useAuth } from '../context/AuthContext.jsx';
import { dateTimeFmt, inr } from '../utils/format.js';

export function MyOrders() {
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useAsync(() => api.get(`/api/orders?page=${page}&size=10`), [page]);
  if (loading && !data) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;
  return (
    <div className="container-x py-8">
      <h1 className="mb-5 text-2xl">My orders</h1>
      {data.items.length === 0 ? <EmptyState title="No orders yet" action={<Link to="/products" className="btn-primary">Start shopping</Link>} /> : (
        <div className="space-y-3">
          {data.items.map((o) => (
            <Link key={o.id} to={`/orders/${o.id}`} className="card flex flex-wrap items-center justify-between gap-3 p-4 hover:border-brand-600">
              <div><p className="font-semibold">{o.orderNumber}</p><p className="text-sm text-neutral-500">{dateTimeFmt(o.createdAt)} · {o.items.length} item{o.items.length === 1 ? '' : 's'}{o.type === 'RESALE' ? ' · Marketplace' : ''}</p></div>
              <div className="flex items-center gap-3"><StatusBadge value={o.status} /><span className="font-bold">{inr(o.total)}</span></div>
            </Link>
          ))}
        </div>
      )}
      <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </div>
  );
}

function ReviewModal({ item, onClose, onDone }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setErr('');
    try { await api.post(`/api/products/${item.productId}/reviews`, { rating, comment, orderItemId: item.id }); onDone(); } catch (x) { setErr(x.message); } finally { setBusy(false); }
  };
  return (
    <Modal title={`Review: ${item.name}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
        <div className="flex gap-1" role="radiogroup" aria-label="Rating">
          {[1, 2, 3, 4, 5].map((n) => <button type="button" key={n} onClick={() => setRating(n)} aria-label={`${n} star${n > 1 ? 's' : ''}`} aria-pressed={rating === n} className={`text-3xl ${n <= rating ? 'text-amber-500' : 'text-neutral-300'}`}>★</button>)}
        </div>
        <textarea className="input" rows={4} maxLength={2000} placeholder="Share your experience (optional)" value={comment} onChange={(e) => setComment(e.target.value)} />
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Submitting…' : 'Submit review'}</button>
      </form>
    </Modal>
  );
}

export function OrderDetail() {
  const { id } = useParams();
  const loc = useLocation();
  const { user } = useAuth();
  const { push } = useToast();
  const { data: o, loading, error, reload } = useAsync(() => api.get(`/api/orders/${id}`), [id]);
  const [cancel, setCancel] = useState(false);
  const [review, setReview] = useState(null);
  const [busy, setBusy] = useState(false);

  if (loading && !o) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;

  const doCancel = async () => {
    setBusy(true);
    try {
      await api.post(o.type === 'RESALE' ? `/api/marketplace/orders/${o.id}/cancel` : `/api/orders/${o.id}/cancel`);
      push('Order cancelled'); setCancel(false); reload();
    } catch (e) { push(e.message, 'error'); setCancel(false); } finally { setBusy(false); }
  };
  const payNow = async () => {
    try {
      const full = await api.get(`/api/orders/${o.id}`);
      if (!full.razorpayOrderId) return push('This order is not awaiting payment', 'error');
      const result = await openRazorpay(full, user);
      await api.post('/api/payments/razorpay/verify', result);
      push('Payment verified'); reload();
    } catch (e) { push(e.message, 'error'); }
  };
  const cancellable = o.canCancel || (o.type === 'RESALE' && ['PLACED', 'PENDING_PAYMENT'].includes(o.status) && o.paymentStatus !== 'PAID');

  return (
    <div className="container-x py-8">
      {loc.state?.confirmed && (
        <div className="mb-5 rounded-xl border border-brand-200 bg-brand-50 p-4 text-brand-700" role="status">
          <strong>Thank you! Your order {o.orderNumber} has been placed.</strong> {o.paymentMethod === 'COD' ? 'Pay when it is delivered.' : 'Your payment was verified.'}
        </div>
      )}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-2xl">Order {o.orderNumber}</h1><p className="text-sm text-neutral-500">Placed {dateTimeFmt(o.createdAt)}{o.type === 'RESALE' ? ' · Marketplace purchase' : ''}</p></div>
        <div className="flex items-center gap-2"><StatusBadge value={o.status} /><StatusBadge value={o.paymentStatus} /></div>
      </div>
      {o.type === 'RESALE' && o.status === 'PLACED' && <p className="mb-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">Ownership transfers to you after the seller confirms handover and payment.</p>}
      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <div className="card divide-y">
          {o.items.map((i) => (
            <div key={i.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
              <div><p className="font-semibold">{i.name}</p><p className="text-sm text-neutral-500">{inr(i.unitPrice)} × {i.quantity}</p></div>
              <div className="flex items-center gap-3">
                {i.canReview && <button className="btn-outline" onClick={() => setReview(i)}>Write a review</button>}
                <span className="font-bold">{inr(i.lineTotal)}</span>
              </div>
            </div>
          ))}
        </div>
        <aside className="card h-fit space-y-3 p-5 text-sm">
          <div><h2 className="text-base">Delivery to</h2><p className="mt-1 text-neutral-600">{o.shipName}<br />{o.shipAddress}</p></div>
          <div className="flex justify-between border-t pt-3 text-base font-bold"><span>Total</span><span>{inr(o.total)}</span></div>
          <p className="text-neutral-500">Payment: {o.paymentMethod === 'COD' ? 'Cash on Delivery' : 'Razorpay'}</p>
          {o.status === 'PENDING_PAYMENT' && o.paymentMethod === 'RAZORPAY' && <button className="btn-primary w-full" onClick={payNow}>Complete payment</button>}
          {cancellable && <button className="btn-outline w-full text-red-600" onClick={() => setCancel(true)}>Cancel order</button>}
        </aside>
      </div>
      {cancel && <ConfirmDialog danger busy={busy} title="Cancel this order?" message="Reserved stock will be released. This cannot be undone." confirmText="Cancel order" onCancel={() => setCancel(false)} onConfirm={doCancel} />}
      {review && <ReviewModal item={review} onClose={() => setReview(null)} onDone={() => { setReview(null); push('Thanks for your review!'); reload(); }} />}
    </div>
  );
}
