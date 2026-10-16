import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Timeline from '../components/Timeline.jsx';
import { ErrorState, Field, Modal, PageLoader, StatusBadge } from '../components/ui.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';
import { dateFmt } from '../utils/format.js';

function QrImage({ token }) {
  const [url, setUrl] = useState(null);
  useEffect(() => {
    let u; api.blob(`/api/passports/public/${token}/qr`).then((b) => { u = URL.createObjectURL(b); setUrl(u); }).catch(() => {});
    return () => u && URL.revokeObjectURL(u);
  }, [token]);
  return url ? <img src={url} alt="QR code linking to the public passport" className="h-36 w-36" /> : null;
}

export default function Passport() {
  const { id } = useParams();
  const { data, loading, error, reload } = useAsync(() => api.get(`/api/my-products/${id}/passport`), [id]);
  const { push } = useToast();
  const [modal, setModal] = useState(null);
  if (loading && !data) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} title={error.status === 404 ? 'Product not found' : undefined} /></div>;
  const p = data.product;
  const publicUrl = `${window.location.origin}/passport/${p.publicToken}`;
  const copy = async () => { try { await navigator.clipboard.writeText(publicUrl); push('Public link copied'); } catch { push(publicUrl); } };
  const viewReceipt = async () => {
    try { const b = await api.blob(`/api/my-products/${id}/receipt`); window.open(URL.createObjectURL(b), '_blank', 'noopener'); } catch (e) { push(e.message, 'error'); }
  };
  const upload = async (e) => {
    const file = e.target.files[0]; if (!file) return;
    try { await api.upload(`/api/my-products/${id}/receipt`, file); push('Receipt saved privately'); reload(); } catch (x) { push(x.message, 'error'); }
    e.target.value = '';
  };

  return (
    <div className="container-x py-8">
      <Link to="/my-products" className="text-sm text-brand-700 hover:underline">← My products</Link>
      <div className="mt-2 flex flex-wrap items-start justify-between gap-3">
        <div><h1 className="text-2xl">{p.name}</h1><p className="text-sm text-neutral-500">{[p.brand, p.model, p.category].filter(Boolean).join(' · ')}</p></div>
        <StatusBadge value={p.status} />
      </div>
      <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_320px]">
        <section className="card p-5">
          <h2 className="mb-1 text-lg">Lifecycle passport</h2>
          <p className="mb-5 text-xs text-neutral-500">Events are append-only. “Self reported” entries were entered by the owner and are not independently verified; only “Partner confirmed” or “Admin confirmed” entries were confirmed by an authorised party.</p>
          <Timeline events={data.events} />
        </section>
        <aside className="space-y-4">
          <div className="card space-y-2 p-5 text-sm">
            <h2 className="text-base">Details</h2>
            <p><span className="text-neutral-500">Purchased:</span> {dateFmt(p.purchaseDate)}</p>
            <p><span className="text-neutral-500">Serial:</span> {p.serialNumber || '—'} <span className="text-xs text-neutral-400">(private)</span></p>
            <p className="flex items-center gap-2"><span className="text-neutral-500">Warranty:</span> {p.warrantyEnd ? dateFmt(p.warrantyEnd) : 'not recorded'} <StatusBadge value={p.warrantyStatus} /></p>
            <p className="flex items-center gap-2"><span className="text-neutral-500">Receipt:</span> {p.hasReceipt ? <button className="font-semibold text-brand-700 hover:underline" onClick={viewReceipt}>View (private)</button> : 'none'}</p>
            {p.status !== 'RECYCLED' && <label className="btn-outline mt-1 w-full cursor-pointer">{p.hasReceipt ? 'Replace receipt' : 'Upload receipt'}<input type="file" className="sr-only" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={upload} /></label>}
          </div>
          <div className="card space-y-2 p-5">
            <h2 className="text-base">Actions</h2>
            {p.status === 'ACTIVE' && <Link className="btn-primary w-full" to={`/repairs/new?product=${p.id}`}>Request a repair</Link>}
            {p.status === 'ACTIVE' && <Link className="btn-outline w-full" to={`/sell?product=${p.id}`}>Sell this product</Link>}
            {p.status !== 'RECYCLED' && <button className="btn-outline w-full" onClick={() => setModal('warranty')}>Update warranty</button>}
            {p.status === 'ACTIVE' && <button className="btn-outline w-full" onClick={() => setModal('recycle')}>Record recycling</button>}
            <Link className="btn-outline w-full" to={`/calculator?product=${encodeURIComponent(p.name)}`}>Repair vs replace</Link>
          </div>
          <div className="card flex flex-col items-center p-5 text-center text-sm">
            <h2 className="mb-2 text-base">Public passport</h2>
            <QrImage token={p.publicToken} />
            <p className="mt-2 text-xs text-neutral-500">Shows the product and event types only. No owner identity, serial number, receipt or notes.</p>
            <button className="btn-outline mt-3" onClick={copy}>Copy link</button>
          </div>
        </aside>
      </div>
      {modal === 'warranty' && <WarrantyModal id={id} onClose={() => setModal(null)} onDone={() => { setModal(null); push('Warranty updated'); reload(); }} />}
      {modal === 'recycle' && <RecycleModal id={id} onClose={() => setModal(null)} onDone={() => { setModal(null); push('Recycling recorded (self-reported)'); reload(); }} />}
    </div>
  );
}

function WarrantyModal({ id, onClose, onDone }) {
  const [f, setF] = useState({ startDate: '', endDate: '', provider: '', notes: '' });
  const [err, setErr] = useState('');
  const submit = async (e) => {
    e.preventDefault(); setErr('');
    if (!f.startDate || !f.endDate) return setErr('Enter both dates');
    if (f.endDate < f.startDate) return setErr('End date must be after the start date');
    try { await api.post(`/api/my-products/${id}/warranty`, f); onDone(); } catch (x) { setErr(x.message); }
  };
  return (
    <Modal title="Update warranty" onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
        <Field label="Start date"><input className="input" type="date" value={f.startDate} onChange={(e) => setF({ ...f, startDate: e.target.value })} /></Field>
        <Field label="End date"><input className="input" type="date" value={f.endDate} onChange={(e) => setF({ ...f, endDate: e.target.value })} /></Field>
        <Field label="Provider"><input className="input" value={f.provider} onChange={(e) => setF({ ...f, provider: e.target.value })} /></Field>
        <button className="btn-primary w-full">Save warranty</button>
      </form>
    </Modal>
  );
}

function RecycleModal({ id, onClose, onDone }) {
  const [f, setF] = useState({ recycledOn: new Date().toISOString().slice(0, 10), partnerName: '', evidenceUrl: '' });
  const [err, setErr] = useState('');
  const submit = async (e) => {
    e.preventDefault(); setErr('');
    try { await api.post(`/api/my-products/${id}/recycle`, { ...f, partnerName: f.partnerName || null, evidenceUrl: f.evidenceUrl || null }); onDone(); } catch (x) { setErr(x.message); }
  };
  return (
    <Modal title="Record recycling" onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">This marks the product as recycled and can’t be undone. The record is self-reported and is not an official recycling certificate unless an administrator later confirms it.</p>
        {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
        <Field label="Recycled on"><input className="input" type="date" max={new Date().toISOString().slice(0, 10)} value={f.recycledOn} onChange={(e) => setF({ ...f, recycledOn: e.target.value })} /></Field>
        <Field label="Recycling partner (optional)"><input className="input" value={f.partnerName} onChange={(e) => setF({ ...f, partnerName: e.target.value })} /></Field>
        <Field label="Evidence link (https, optional)"><input className="input" value={f.evidenceUrl} onChange={(e) => setF({ ...f, evidenceUrl: e.target.value })} /></Field>
        <button className="btn-danger w-full">Record recycling</button>
      </form>
    </Modal>
  );
}
