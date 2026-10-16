import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import AddressForm from '../components/AddressForm.jsx';
import Timeline from '../components/Timeline.jsx';
import { ConditionBadge, EmptyState, ErrorState, Field, Modal, PageLoader, Pagination, StatusBadge } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api, qs } from '../services/api.js';
import { openRazorpay } from '../services/razorpay.js';
import { inr } from '../utils/format.js';

export function Marketplace() {
  const [sp, setSp] = useSearchParams();
  const get = (k) => sp.get(k) || '';
  const page = Number(get('page') || 0);
  const [q, setQ] = useState(get('q'));
  const { data, loading, error, reload } = useAsync(
    () => api.get('/api/marketplace/listings' + qs({ q: get('q'), condition: get('condition'), minPrice: get('minPrice'), maxPrice: get('maxPrice'), sort: get('sort'), page, size: 12 })), [sp.toString()]);
  const update = (c) => { const n = new URLSearchParams(sp); Object.entries(c).forEach(([k, v]) => (v ? n.set(k, v) : n.delete(k))); if (!('page' in c)) n.delete('page'); setSp(n); };
  return (
    <div className="container-x py-8">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div><h1 className="text-2xl">Resale marketplace</h1><p className="text-sm text-neutral-500">Pre-owned products listed by ReLoop customers, each with its recorded history.</p></div>
        <Link to="/sell" className="btn-primary">Sell a product</Link>
      </div>
      <form className="mb-5 flex flex-wrap gap-2" onSubmit={(e) => { e.preventDefault(); update({ q }); }} role="search">
        <input className="input max-w-xs" placeholder="Search listings" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search listings" />
        <select className="input w-auto" value={get('condition')} onChange={(e) => update({ condition: e.target.value })} aria-label="Condition"><option value="">Any condition</option><option value="REFURBISHED">Refurbished</option><option value="USED">Used</option><option value="NEW">New</option></select>
        <select className="input w-auto" value={get('sort')} onChange={(e) => update({ sort: e.target.value })} aria-label="Sort"><option value="">Newest</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option></select>
        <button className="btn-outline">Search</button>
      </form>
      {loading && !data && <PageLoader />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.items.length === 0 && <EmptyState title="No listings found" text="Try different filters, or list your own product." />}
      <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 lg:grid-cols-4">
        {data?.items.map((l) => (
          <Link key={l.id} to={`/marketplace/${l.id}`} className="card overflow-hidden transition hover:shadow-md">
            <div className="aspect-[4/3] bg-brand-50">{l.imageUrl ? <img src={l.imageUrl} alt={l.title} loading="lazy" className="h-full w-full object-cover" /> : <div className="flex h-full items-center justify-center text-sm text-neutral-400">No photo</div>}</div>
            <div className="space-y-1 p-3"><ConditionBadge value={l.condition} /><p className="line-clamp-2 min-h-[2.5rem] text-sm font-semibold">{l.title}</p><p className="text-lg font-bold">{inr(l.price)}</p><p className="text-xs text-neutral-500">Sold by {l.sellerName}</p></div>
          </Link>
        ))}
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => update({ page: p ? String(p) : '' })} />}
    </div>
  );
}

function BuyModal({ listing, onClose }) {
  const { user } = useAuth();
  const { push } = useToast();
  const nav = useNavigate();
  const addrs = useAsync(() => api.get('/api/addresses'), []);
  const cfg = useAsync(() => api.get('/api/payments/config'), []);
  const [addressId, setAddressId] = useState(null);
  const [method, setMethod] = useState('COD');
  const [adding, setAdding] = useState(false);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  useEffect(() => { if (addrs.data && !addressId) setAddressId((addrs.data.find((a) => a.defaultAddress) || addrs.data[0])?.id ?? null); }, [addrs.data]); // eslint-disable-line

  const buy = async () => {
    setErr('');
    if (!addressId) return setErr('Select a delivery address');
    setBusy(true);
    let order;
    try { order = await api.post(`/api/marketplace/listings/${listing.id}/buy`, { addressId, paymentMethod: method }); } catch (e) { setErr(e.message); setBusy(false); return; }
    try {
      if (method === 'RAZORPAY') { const r = await openRazorpay(order, user); order = await api.post('/api/payments/razorpay/verify', r); push('Payment verified. The product is now yours.'); }
      else push('Item reserved for you. Ownership transfers once the seller confirms handover.');
    } catch (e) { push(e.message, 'error'); }
    nav(`/orders/${order.id}`, { replace: true });
  };
  return (
    <Modal title={`Buy: ${listing.title}`} onClose={onClose} wide>
      {err && <p className="mb-3 rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
      {adding ? <AddressForm onCancel={() => setAdding(false)} onSubmit={async (v) => { const a = await api.post('/api/addresses', v); await addrs.reload(); setAddressId(a.id); setAdding(false); }} /> : (
        <div className="space-y-4">
          <div>
            <div className="mb-2 flex justify-between text-sm font-semibold">Delivery address<button className="font-normal text-brand-700 hover:underline" onClick={() => setAdding(true)}>+ Add</button></div>
            {addrs.data?.map((a) => <label key={a.id} className="mb-2 flex gap-2 rounded-lg border p-2 text-sm"><input type="radio" checked={addressId === a.id} onChange={() => setAddressId(a.id)} /><span>{a.label}: {a.line1}, {a.city} {a.postalCode}</span></label>)}
          </div>
          <div className="text-sm">
            <label className="mb-1 flex gap-2"><input type="radio" checked={method === 'COD'} onChange={() => setMethod('COD')} />Cash on handover (seller confirms receipt)</label>
            <label className={`flex gap-2 ${cfg.data?.razorpayEnabled ? '' : 'text-neutral-400'}`}><input type="radio" disabled={!cfg.data?.razorpayEnabled} checked={method === 'RAZORPAY'} onChange={() => setMethod('RAZORPAY')} />Pay online{!cfg.data?.razorpayEnabled && ' (not configured)'}</label>
          </div>
          <p className="rounded-lg bg-neutral-100 p-3 text-xs text-neutral-600">The item is reserved for you immediately. Ownership is only transferred after payment is confirmed, never from a screen message alone.</p>
          <button className="btn-primary w-full" disabled={busy} onClick={buy}>{busy ? 'Working…' : `Reserve for ${inr(listing.price)}`}</button>
        </div>
      )}
    </Modal>
  );
}

export function ListingDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const { push } = useToast();
  const nav = useNavigate();
  const { data: l, loading, error, reload } = useAsync(() => api.get(`/api/marketplace/listings/${id}`), [id, user?.id]);
  const [buying, setBuying] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [reason, setReason] = useState('');
  if (loading) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} title={error.status === 404 ? 'Listing not found' : undefined} /></div>;
  const report = async (e) => {
    e.preventDefault();
    try { await api.post(`/api/marketplace/listings/${id}/report`, { reason }); push('Thanks, our team will review this listing'); setReporting(false); setReason(''); } catch (x) { push(x.message, 'error'); }
  };
  return (
    <div className="container-x py-8">
      <Link to="/marketplace" className="text-sm text-brand-700 hover:underline">← Marketplace</Link>
      <div className="mt-3 grid gap-8 md:grid-cols-2">
        <div className="aspect-[4/3] overflow-hidden rounded-xl border bg-brand-50">{l.imageUrl ? <img src={l.imageUrl} alt={l.title} className="h-full w-full object-cover" /> : <div className="flex h-full items-center justify-center text-neutral-400">No photo provided</div>}</div>
        <div>
          <div className="flex items-center gap-2"><ConditionBadge value={l.condition} />{l.status !== 'PUBLISHED' && <StatusBadge value={l.status} />}</div>
          <h1 className="mt-2 text-2xl">{l.title}</h1>
          <p className="text-sm text-neutral-500">{[l.brand, l.productName, l.category].filter(Boolean).join(' · ')}</p>
          <p className="mt-3 text-3xl font-extrabold">{inr(l.price)}</p>
          <p className="mt-2 text-sm">Warranty: <StatusBadge value={l.warrantyStatus} /> <span className="text-xs text-neutral-500">(as reported by the seller)</span></p>
          <p className="mt-1 text-sm text-neutral-500">Sold by {l.sellerName}</p>
          {l.description && <p className="mt-4 whitespace-pre-line text-sm text-neutral-700">{l.description}</p>}
          <div className="mt-6 flex flex-wrap gap-2">
            {l.mine ? <Link to="/my-listings" className="btn-outline">Manage your listing</Link>
              : l.status === 'PUBLISHED' ? <button className="btn-primary px-6" onClick={() => (user ? setBuying(true) : nav('/login', { state: { from: `/marketplace/${id}` } }))}>Buy this item</button>
              : <button className="btn-outline" disabled>{l.status === 'RESERVED' ? 'Reserved' : 'No longer available'}</button>}
            {!l.mine && <button className="btn-outline" onClick={() => (user ? setReporting(true) : nav('/login', { state: { from: `/marketplace/${id}` } }))}>Report listing</button>}
          </div>
        </div>
      </div>
      <section className="card mt-8 p-5">
        <h2 className="text-lg">Product history</h2>
        <p className="mb-4 text-xs text-neutral-500">Only event types and verification levels are shown. The seller’s documents and personal details are never shared. “Self reported” means not independently verified.</p>
        {l.history.length ? <Timeline events={l.history} /> : <p className="text-sm text-neutral-500">No history available.</p>}
      </section>
      {buying && <BuyModal listing={l} onClose={() => setBuying(false)} />}
      {reporting && (
        <Modal title="Report this listing" onClose={() => setReporting(false)}>
          <form onSubmit={report} className="space-y-3"><Field label="What is wrong with it?"><textarea className="input" rows={4} required maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} /></Field><button className="btn-primary w-full">Submit report</button></form>
        </Modal>
      )}
    </div>
  );
}

export function Sell() {
  const { user } = useAuth();
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const { push } = useToast();
  const prods = useAsync(() => (user ? api.get('/api/my-products') : Promise.resolve([])), [user?.id]);
  const [f, setF] = useState({ ownedProductId: sp.get('product') || '', title: '', description: '', condition: 'USED', price: '', imageUrl: '' });
  const [errs, setErrs] = useState({});
  const [busy, setBusy] = useState(false);
  if (!user) return <div className="container-x py-16"><EmptyState title="Sell your product" text="Sign in to list a product you own. Ownership transfers safely when the sale is confirmed." action={<Link to="/login" state={{ from: '/sell' }} className="btn-primary">Sign in</Link>} /></div>;
  if (prods.loading) return <PageLoader />;
  const eligible = (prods.data || []).filter((p) => p.status === 'ACTIVE' && p.openRepairs === 0);
  const pick = (id) => { const p = eligible.find((x) => String(x.id) === String(id)); setF({ ...f, ownedProductId: id, title: f.title || (p ? p.name : '') }); };
  const upload = async (e) => { const file = e.target.files[0]; e.target.value = ''; if (!file) return; try { const { url } = await api.upload('/api/uploads', file); setF((x) => ({ ...x, imageUrl: url })); } catch (x) { push(x.message, 'error'); } };
  const submit = async (e) => {
    e.preventDefault();
    const er = {};
    if (!f.ownedProductId) er.ownedProductId = 'Select a product';
    if (!f.title.trim()) er.title = 'Required';
    if (!(Number(f.price) >= 1)) er.price = 'Enter a price';
    setErrs(er);
    if (Object.keys(er).length) return;
    setBusy(true);
    try { await api.post('/api/my-listings', { ...f, ownedProductId: Number(f.ownedProductId), price: Number(f.price), imageUrl: f.imageUrl || null }); push('Draft listing created. Publish it from My Listings.'); nav('/my-listings'); } catch (x) { push(x.message, 'error'); setErrs(x.fieldErrors || {}); } finally { setBusy(false); }
  };
  return (
    <div className="container-x max-w-2xl py-8">
      <h1 className="text-2xl">Sell your product</h1>
      {eligible.length === 0 ? <div className="mt-6"><EmptyState title="No eligible products" text="Register a product you own first. Products with open repairs or listings can’t be listed." action={<Link to="/my-products" className="btn-primary">My products</Link>} /></div> : (
        <form onSubmit={submit} className="card mt-5 space-y-4 p-5" noValidate>
          <Field label="Product" error={errs.ownedProductId}><select className="input" value={f.ownedProductId} onChange={(e) => pick(e.target.value)}><option value="">Select…</option>{eligible.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></Field>
          <Field label="Listing title" error={errs.title}><input className="input" maxLength={160} value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Condition"><select className="input" value={f.condition} onChange={(e) => setF({ ...f, condition: e.target.value })}><option value="USED">Used</option><option value="REFURBISHED">Refurbished</option><option value="NEW">New (unused)</option></select></Field>
            <Field label="Asking price (₹)" error={errs.price}><input className="input" type="number" min="1" value={f.price} onChange={(e) => setF({ ...f, price: e.target.value })} /></Field>
          </div>
          <Field label="Description"><textarea className="input" rows={4} maxLength={3000} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
          <div>{f.imageUrl && <img src={f.imageUrl} alt="" className="mb-2 h-24 rounded-lg" />}<label className="btn-outline cursor-pointer">{f.imageUrl ? 'Replace photo' : 'Add photo'}<input type="file" className="sr-only" accept="image/jpeg,image/png,image/webp" onChange={upload} /></label></div>
          <button className="btn-primary w-full" disabled={busy}>{busy ? 'Saving…' : 'Create draft listing'}</button>
        </form>
      )}
    </div>
  );
}

export function MyListings() {
  const { push } = useToast();
  const { data, loading, error, reload } = useAsync(() => api.get('/api/my-listings'), []);
  const sales = useAsync(() => api.get('/api/my-sales'), []);
  const [edit, setEdit] = useState(null);
  const act = async (path, msg) => { try { await api.post(path); push(msg); reload(); sales.reload(); } catch (e) { push(e.message, 'error'); } };
  if (loading && !data) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;
  return (
    <div className="container-x py-8">
      <div className="mb-5 flex items-center justify-between"><h1 className="text-2xl">My listings</h1><Link to="/sell" className="btn-primary">+ New listing</Link></div>
      {sales.data?.length > 0 && (
        <section className="mb-6 rounded-xl border border-amber-200 bg-amber-50 p-4">
          <h2 className="text-base">Sales awaiting you</h2>
          {sales.data.map((s) => (
            <div key={s.orderId} className="mt-2 flex flex-wrap items-center justify-between gap-2 text-sm">
              <span><strong>{s.title}</strong> · {inr(s.price)} · buyer {s.buyerFirstName} · {s.orderNumber}</span>
              <span className="flex gap-2">
                {s.paymentMethod === 'COD' && s.status === 'PLACED' && <button className="btn-primary" onClick={() => act(`/api/marketplace/orders/${s.orderId}/confirm-handover`, 'Handover confirmed. Ownership transferred.')}>Confirm handover &amp; payment received</button>}
                {s.status === 'PENDING_PAYMENT' && <span className="text-neutral-600">Waiting for buyer’s online payment</span>}
                <button className="btn-outline" onClick={() => act(`/api/marketplace/orders/${s.orderId}/cancel`, 'Reservation cancelled')}>Cancel</button>
              </span>
            </div>
          ))}
          <p className="mt-2 text-xs text-neutral-600">Only confirm after the buyer has the item and you have been paid. Ownership transfer cannot be undone.</p>
        </section>
      )}
      {data.length === 0 ? <EmptyState title="No listings yet" action={<Link to="/sell" className="btn-primary">Create a listing</Link>} /> : (
        <div className="space-y-3">{data.map((l) => (
          <div key={l.id} className="card flex flex-wrap items-center justify-between gap-3 p-4">
            <div><Link to={`/marketplace/${l.id}`} className="font-semibold hover:text-brand-700">{l.title}</Link><p className="text-sm text-neutral-500">{inr(l.price)}</p></div>
            <div className="flex flex-wrap items-center gap-2">
              <StatusBadge value={l.status} />
              {['DRAFT', 'PAUSED'].includes(l.status) && <button className="btn-outline" onClick={() => setEdit(l)}>Edit</button>}
              {['DRAFT', 'PAUSED'].includes(l.status) && <button className="btn-primary" onClick={() => act(`/api/my-listings/${l.id}/publish`, 'Listing published')}>Publish</button>}
              {l.status === 'PUBLISHED' && <button className="btn-outline" onClick={() => act(`/api/my-listings/${l.id}/pause`, 'Listing paused')}>Pause</button>}
              {['PUBLISHED', 'PAUSED'].includes(l.status) && <button className="btn-outline" title="Sold outside ReLoop. Ownership is not transferred." onClick={() => act(`/api/my-listings/${l.id}/mark-sold`, 'Marked as sold')}>Mark sold</button>}
            </div>
          </div>
        ))}</div>
      )}
      {edit && <EditListing l={edit} onClose={() => setEdit(null)} onDone={() => { setEdit(null); push('Listing updated'); reload(); }} />}
    </div>
  );
}

function EditListing({ l, onClose, onDone }) {
  const [f, setF] = useState({ title: l.title, description: l.description || '', condition: l.condition, price: l.price, imageUrl: l.imageUrl });
  const [err, setErr] = useState('');
  const submit = async (e) => { e.preventDefault(); try { await api.put(`/api/my-listings/${l.id}`, { ...f, price: Number(f.price) }); onDone(); } catch (x) { setErr(x.message); } };
  return (
    <Modal title="Edit listing" onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
        <Field label="Title"><input className="input" value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></Field>
        <Field label="Price (₹)"><input className="input" type="number" min="1" value={f.price} onChange={(e) => setF({ ...f, price: e.target.value })} /></Field>
        <Field label="Condition"><select className="input" value={f.condition} onChange={(e) => setF({ ...f, condition: e.target.value })}><option>USED</option><option>REFURBISHED</option><option>NEW</option></select></Field>
        <Field label="Description"><textarea className="input" rows={3} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></Field>
        <button className="btn-primary w-full">Save</button>
      </form>
    </Modal>
  );
}
