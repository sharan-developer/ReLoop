import { useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ConfirmDialog, EmptyState, ErrorState, Field, PageLoader, StatusBadge } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';
import { dateTimeFmt, inr } from '../utils/format.js';

export function RepairServices() {
  const { data, loading, error, reload } = useAsync(() => api.get('/api/partners'), []);
  return (
    <div className="container-x py-8">
      <h1 className="text-2xl">Repair services</h1>
      <p className="mb-6 mt-1 text-sm text-neutral-600">Request a repair for any product you have registered. Approved partners send you a quotation; you decide whether to accept.</p>
      {loading && <PageLoader />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.length === 0 && <EmptyState title="No approved partners yet" />}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map((p) => <div key={p.id} className="card p-5"><h2 className="text-base">{p.businessName}</h2><p className="text-sm text-neutral-500">{p.city}</p></div>)}
      </div>
      <div className="mt-8 flex flex-wrap gap-3"><Link to="/repairs/new" className="btn-primary">Request a repair</Link><Link to="/calculator" className="btn-outline">Repair vs replace calculator</Link></div>
    </div>
  );
}

export function MyRepairs() {
  const { data, loading, error, reload } = useAsync(() => api.get('/api/repairs'), []);
  if (loading && !data) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;
  return (
    <div className="container-x py-8">
      <div className="mb-5 flex items-center justify-between"><h1 className="text-2xl">My repairs</h1><Link to="/repairs/new" className="btn-primary">+ New request</Link></div>
      {data.length === 0 ? <EmptyState title="No repair requests" text="Start a repair from one of your registered products." action={<Link to="/repairs/new" className="btn-primary">Request a repair</Link>} /> : (
        <div className="space-y-3">{data.map((r) => (
          <Link key={r.id} to={`/repairs/${r.id}`} className="card flex flex-wrap items-center justify-between gap-3 p-4 hover:border-brand-600">
            <div><p className="font-semibold">{r.productName}</p><p className="line-clamp-1 text-sm text-neutral-500">{r.problem}</p><p className="text-xs text-neutral-400">{r.partnerName} · {dateTimeFmt(r.createdAt)}</p></div>
            <StatusBadge value={r.status} />
          </Link>
        ))}</div>
      )}
    </div>
  );
}

export function NewRepair() {
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const { push } = useToast();
  const prods = useAsync(() => api.get('/api/my-products'), []);
  const partners = useAsync(() => api.get('/api/partners'), []);
  const [f, setF] = useState({ ownedProductId: sp.get('product') || '', partnerId: '', problem: '' });
  const [images, setImages] = useState([]);
  const [errs, setErrs] = useState({});
  const [busy, setBusy] = useState(false);
  if (prods.loading || partners.loading) return <PageLoader />;
  if (prods.error) return <div className="container-x py-10"><ErrorState error={prods.error} onRetry={prods.reload} /></div>;
  const eligible = prods.data.filter((p) => p.status === 'ACTIVE' && p.openRepairs === 0);

  const addImage = async (e) => {
    const file = e.target.files[0]; e.target.value = '';
    if (!file) return;
    if (images.length >= 5) return push('Maximum 5 images', 'error');
    try { const { url } = await api.upload('/api/uploads', file); setImages((l) => [...l, url]); } catch (x) { push(x.message, 'error'); }
  };
  const submit = async (e) => {
    e.preventDefault();
    const er = {};
    if (!f.ownedProductId) er.ownedProductId = 'Select a product';
    if (!f.partnerId) er.partnerId = 'Select a repair partner';
    if (!f.problem.trim()) er.problem = 'Describe the problem';
    setErrs(er);
    if (Object.keys(er).length) return;
    setBusy(true);
    try {
      const r = await api.post('/api/repairs', { ownedProductId: Number(f.ownedProductId), partnerId: Number(f.partnerId), problem: f.problem, imageUrls: images });
      push('Repair request sent'); nav(`/repairs/${r.id}`, { replace: true });
    } catch (x) { push(x.message, 'error'); setErrs(x.fieldErrors || {}); } finally { setBusy(false); }
  };
  return (
    <div className="container-x max-w-2xl py-8">
      <h1 className="mb-5 text-2xl">Request a repair</h1>
      {eligible.length === 0 ? <EmptyState title="No eligible products" text="Register a product first. Products with an open repair or listed for sale can’t be repaired right now." action={<Link to="/my-products" className="btn-primary">My products</Link>} /> : (
        <form onSubmit={submit} className="card space-y-4 p-5" noValidate>
          <Field label="Product" error={errs.ownedProductId}><select className="input" value={f.ownedProductId} onChange={(e) => setF({ ...f, ownedProductId: e.target.value })}><option value="">Select…</option>{eligible.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select></Field>
          <Field label="Repair partner" error={errs.partnerId}><select className="input" value={f.partnerId} onChange={(e) => setF({ ...f, partnerId: e.target.value })}><option value="">Select…</option>{partners.data?.map((p) => <option key={p.id} value={p.id}>{p.businessName} — {p.city}</option>)}</select></Field>
          <Field label="What’s wrong?" error={errs.problem}><textarea className="input" rows={4} maxLength={2000} value={f.problem} onChange={(e) => setF({ ...f, problem: e.target.value })} /></Field>
          <div>
            <span className="mb-1 block text-sm font-medium">Photos (optional, up to 5)</span>
            <div className="flex flex-wrap gap-2">{images.map((u) => <img key={u} src={u} alt="" className="h-16 w-16 rounded-lg object-cover" />)}</div>
            <label className="btn-outline mt-2 cursor-pointer">Add photo<input type="file" className="sr-only" accept="image/jpeg,image/png,image/webp" onChange={addImage} /></label>
          </div>
          <button className="btn-primary w-full" disabled={busy}>{busy ? 'Sending…' : 'Send request'}</button>
        </form>
      )}
    </div>
  );
}

export function RepairHistory({ history }) {
  return (
    <ol className="space-y-3 border-l-2 border-brand-200 pl-4">
      {history.map((h, i) => (
        <li key={i} className="text-sm"><StatusBadge value={h.to} /> <span className="text-xs text-neutral-500">{dateTimeFmt(h.createdAt)}</span>{h.note && <p className="mt-0.5 text-neutral-600">{h.note}</p>}</li>
      ))}
    </ol>
  );
}

export function RepairDetail() {
  const { id } = useParams();
  const { push } = useToast();
  const { data: r, loading, error, reload } = useAsync(() => api.get(`/api/repairs/${id}`), [id]);
  const [confirm, setConfirm] = useState(null);
  if (loading && !r) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;
  const act = async (path, msg) => { try { await api.post(path); push(msg); reload(); } catch (e) { push(e.message, 'error'); } setConfirm(null); };
  const pending = r.quotes.find((q) => q.status === 'PENDING');
  return (
    <div className="container-x py-8">
      <Link to="/repairs" className="text-sm text-brand-700 hover:underline">← My repairs</Link>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-3"><h1 className="text-2xl">{r.productName}</h1><StatusBadge value={r.status} /></div>
      <p className="text-sm text-neutral-500">Partner: {r.partnerName} · Requested {dateTimeFmt(r.createdAt)}</p>
      <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_320px]">
        <div className="space-y-5">
          <section className="card p-5"><h2 className="text-base">Problem</h2><p className="mt-1 whitespace-pre-line text-sm">{r.problem}</p>{r.imageUrls.length > 0 && <div className="mt-3 flex flex-wrap gap-2">{r.imageUrls.map((u) => <a key={u} href={u} target="_blank" rel="noreferrer"><img src={u} alt="Issue" className="h-20 w-20 rounded-lg object-cover" /></a>)}</div>}</section>
          {r.quotes.length > 0 && (
            <section className="card p-5"><h2 className="mb-3 text-base">Quotations</h2>
              <div className="space-y-3">{r.quotes.map((q) => (
                <div key={q.id} className="rounded-lg border border-neutral-200 p-3 text-sm">
                  <div className="flex items-center justify-between"><strong className="text-lg">{inr(q.amount)}</strong><StatusBadge value={q.status} /></div>
                  <p className="text-neutral-600">Estimated {q.estimatedDays} day{q.estimatedDays > 1 ? 's' : ''}{q.notes ? ` · ${q.notes}` : ''}</p>
                  {q.status === 'PENDING' && r.status === 'QUOTED' && <div className="mt-3 flex gap-2"><button className="btn-primary" onClick={() => setConfirm({ type: 'accept', q })}>Accept</button><button className="btn-outline" onClick={() => setConfirm({ type: 'reject', q })}>Reject</button></div>}
                </div>
              ))}</div>
            </section>
          )}
          {r.status === 'COMPLETED' && (
            <section className="card border-brand-200 bg-brand-50 p-5"><h2 className="text-base">Completed repair</h2><p className="mt-1 text-sm">{r.workNotes}</p><p className="mt-1 text-sm font-semibold">Final cost: {inr(r.finalCost)}</p><p className="mt-2 text-xs text-neutral-500">Confirmed by the repair partner and added to the product’s lifecycle passport.</p></section>
          )}
        </div>
        <aside className="space-y-4">
          <section className="card p-5"><h2 className="mb-3 text-base">Progress</h2><RepairHistory history={r.history} /></section>
          {['REQUESTED', 'REVIEWED', 'QUOTED', 'APPROVED'].includes(r.status) && <button className="btn-outline w-full text-red-600" onClick={() => setConfirm({ type: 'cancel' })}>Cancel request</button>}
        </aside>
      </div>
      {confirm?.type === 'accept' && <ConfirmDialog title="Accept this quotation?" message={`You approve a repair costing ${inr(confirm.q.amount)}. The partner can then start the work.`} confirmText="Accept quotation" onCancel={() => setConfirm(null)} onConfirm={() => act(`/api/repairs/${id}/quotes/${confirm.q.id}/accept`, 'Quotation accepted')} />}
      {confirm?.type === 'reject' && <ConfirmDialog danger title="Reject this quotation?" message="The partner may send a new quotation." confirmText="Reject" onCancel={() => setConfirm(null)} onConfirm={() => act(`/api/repairs/${id}/quotes/${confirm.q.id}/reject`, 'Quotation rejected')} />}
      {confirm?.type === 'cancel' && <ConfirmDialog danger title="Cancel this repair?" message="This can’t be undone." confirmText="Cancel repair" onCancel={() => setConfirm(null)} onConfirm={() => act(`/api/repairs/${id}/cancel`, 'Repair cancelled')} />}
    </div>
  );
}
