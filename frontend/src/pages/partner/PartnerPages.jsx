import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ConfirmDialog, EmptyState, ErrorState, Field, Modal, PageLoader, StatusBadge } from '../../components/ui.jsx';
import { RepairHistory } from '../Repairs.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import useAsync from '../../hooks/useAsync.js';
import { api } from '../../services/api.js';
import { dateTimeFmt, inr } from '../../utils/format.js';

export function PartnerDashboard() {
  const { data, loading, error, reload } = useAsync(() => api.get('/api/partner/repairs'), []);
  if (loading && !data) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} title={error.status === 403 ? 'Your partner account is not approved yet' : undefined} /></div>;
  const open = data.filter((r) => !['COMPLETED', 'CANCELLED', 'REJECTED'].includes(r.status));
  return (
    <div className="container-x py-8">
      <h1 className="text-2xl">Assigned repair requests</h1>
      <p className="mb-5 text-sm text-neutral-500">{open.length} open · {data.length} total</p>
      {data.length === 0 ? <EmptyState title="No requests assigned yet" /> : (
        <div className="space-y-3">{data.map((r) => (
          <Link key={r.id} to={`/partner/repairs/${r.id}`} className="card flex flex-wrap items-center justify-between gap-3 p-4 hover:border-brand-600">
            <div><p className="font-semibold">{r.productName}</p><p className="line-clamp-1 text-sm text-neutral-500">{r.problem}</p><p className="text-xs text-neutral-400">{r.customerName} · {dateTimeFmt(r.createdAt)}</p></div>
            <StatusBadge value={r.status} />
          </Link>
        ))}</div>
      )}
    </div>
  );
}

export function PartnerRepair() {
  const { id } = useParams();
  const { push } = useToast();
  const { data: r, loading, error, reload } = useAsync(() => api.get(`/api/partner/repairs/${id}`), [id]);
  const [modal, setModal] = useState(null);
  const [q, setQ] = useState({ amount: '', estimatedDays: '', notes: '' });
  const [c, setC] = useState({ workNotes: '', finalCost: '' });
  const [err, setErr] = useState('');
  if (loading && !r) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;

  const call = async (path, body, msg) => {
    setErr('');
    try { await api.post(path, body); push(msg); setModal(null); reload(); } catch (e) { setErr(e.message); if (!modal) push(e.message, 'error'); }
  };
  const s = r.status;
  return (
    <div className="container-x py-8">
      <Link to="/partner" className="text-sm text-brand-700 hover:underline">← Assigned requests</Link>
      <div className="mt-2 flex flex-wrap items-center justify-between gap-3"><h1 className="text-2xl">{r.productName}</h1><StatusBadge value={s} /></div>
      <p className="text-sm text-neutral-500">Customer: {r.customerName} · {dateTimeFmt(r.createdAt)}</p>
      <div className="mt-5 grid gap-5 lg:grid-cols-[1fr_320px]">
        <div className="space-y-5">
          <section className="card p-5"><h2 className="text-base">Reported problem</h2><p className="mt-1 whitespace-pre-line text-sm">{r.problem}</p>{r.imageUrls.length > 0 && <div className="mt-3 flex flex-wrap gap-2">{r.imageUrls.map((u) => <a key={u} href={u} target="_blank" rel="noreferrer"><img src={u} alt="Issue" className="h-20 w-20 rounded-lg object-cover" /></a>)}</div>}</section>
          {r.quotes.length > 0 && <section className="card p-5"><h2 className="mb-2 text-base">Quotations</h2>{r.quotes.map((x) => <p key={x.id} className="text-sm">{inr(x.amount)} · {x.estimatedDays} day(s) · <StatusBadge value={x.status} /></p>)}</section>}
          <section className="card space-y-2 p-5">
            <h2 className="text-base">Actions</h2>
            <div className="flex flex-wrap gap-2">
              {s === 'REQUESTED' && <button className="btn-outline" onClick={() => call(`/api/partner/repairs/${id}/review`, {}, 'Marked as reviewed')}>Mark reviewed</button>}
              {['REQUESTED', 'REVIEWED'].includes(s) && <button className="btn-primary" onClick={() => { setErr(''); setModal('quote'); }}>Submit quotation</button>}
              {['REQUESTED', 'REVIEWED', 'QUOTED'].includes(s) && <button className="btn-outline text-red-600" onClick={() => setModal('reject')}>Decline</button>}
              {s === 'APPROVED' && <button className="btn-primary" onClick={() => call(`/api/partner/repairs/${id}/start`, undefined, 'Repair started')}>Start repair</button>}
              {s === 'IN_PROGRESS' && <button className="btn-primary" onClick={() => { setErr(''); setModal('complete'); }}>Mark complete</button>}
              {['COMPLETED', 'CANCELLED', 'REJECTED'].includes(s) && <p className="text-sm text-neutral-500">No further actions.</p>}
              {s === 'QUOTED' && <p className="text-sm text-neutral-500">Waiting for the customer’s decision.</p>}
            </div>
            {s === 'COMPLETED' && <p className="text-sm">Final cost {inr(r.finalCost)} · {r.workNotes}</p>}
          </section>
        </div>
        <aside className="card h-fit p-5"><h2 className="mb-3 text-base">History</h2><RepairHistory history={r.history} /></aside>
      </div>
      {modal === 'quote' && (
        <Modal title="Submit quotation" onClose={() => setModal(null)}>
          <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); if (!(Number(q.amount) > 0) || !(Number(q.estimatedDays) >= 1)) return setErr('Enter an amount and estimated days'); call(`/api/partner/repairs/${id}/quote`, { amount: Number(q.amount), estimatedDays: Number(q.estimatedDays), notes: q.notes || null }, 'Quotation sent'); }}>
            {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
            <Field label="Amount (₹)"><input className="input" type="number" min="1" value={q.amount} onChange={(e) => setQ({ ...q, amount: e.target.value })} /></Field>
            <Field label="Estimated days"><input className="input" type="number" min="1" value={q.estimatedDays} onChange={(e) => setQ({ ...q, estimatedDays: e.target.value })} /></Field>
            <Field label="Notes"><textarea className="input" rows={3} value={q.notes} onChange={(e) => setQ({ ...q, notes: e.target.value })} /></Field>
            <button className="btn-primary w-full">Send quotation</button>
          </form>
        </Modal>
      )}
      {modal === 'complete' && (
        <Modal title="Complete repair" onClose={() => setModal(null)}>
          <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); if (!c.workNotes.trim() || c.finalCost === '' || Number(c.finalCost) < 0) return setErr('Describe the work and enter the final cost'); call(`/api/partner/repairs/${id}/complete`, { workNotes: c.workNotes, finalCost: Number(c.finalCost) }, 'Repair marked complete'); }}>
            {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
            <Field label="Work performed"><textarea className="input" rows={4} value={c.workNotes} onChange={(e) => setC({ ...c, workNotes: e.target.value })} /></Field>
            <Field label="Final cost (₹)"><input className="input" type="number" min="0" value={c.finalCost} onChange={(e) => setC({ ...c, finalCost: e.target.value })} /></Field>
            <p className="text-xs text-neutral-500">This is recorded on the product’s lifecycle passport as partner-confirmed.</p>
            <button className="btn-primary w-full">Confirm completion</button>
          </form>
        </Modal>
      )}
      {modal === 'reject' && <ConfirmDialog danger title="Decline this request?" message="The customer will see the request as rejected." confirmText="Decline" onCancel={() => setModal(null)} onConfirm={() => call(`/api/partner/repairs/${id}/reject`, {}, 'Request declined')} />}
    </div>
  );
}
