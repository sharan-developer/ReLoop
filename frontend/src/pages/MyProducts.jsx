import { useState } from 'react';
import { Link } from 'react-router-dom';
import { EmptyState, ErrorState, Field, Modal, PageLoader, StatusBadge } from '../components/ui.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';
import { dateFmt } from '../utils/format.js';

const CATS = ['Smartphones', 'Laptops', 'Tablets', 'Audio', 'Smartwatches', 'Gaming', 'Accessories', 'Other'];
const today = () => new Date().toISOString().slice(0, 10);

export function RegisterProductModal({ onClose, onDone }) {
  const [f, setF] = useState({ name: '', category: 'Smartphones', brand: '', model: '', serialNumber: '', purchaseDate: '', warrantyStart: '', warrantyEnd: '', warrantyProvider: '' });
  const [errs, setErrs] = useState({});
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    const er = {};
    if (!f.name.trim()) er.name = 'Required';
    if (!f.purchaseDate) er.purchaseDate = 'Required';
    else if (f.purchaseDate > today()) er.purchaseDate = 'Cannot be in the future';
    if (!!f.warrantyStart !== !!f.warrantyEnd) er.warrantyEnd = 'Provide both warranty dates';
    else if (f.warrantyStart && f.warrantyEnd < f.warrantyStart) er.warrantyEnd = 'Must be after the start date';
    setErrs(er); setMsg('');
    if (Object.keys(er).length) return;
    setBusy(true);
    const body = Object.fromEntries(Object.entries(f).map(([k, v]) => [k, v === '' ? null : v]));
    try { onDone(await api.post('/api/my-products', body)); } catch (x) { setMsg(x.message); setErrs(x.fieldErrors || {}); } finally { setBusy(false); }
  };
  return (
    <Modal title="Register a product" onClose={onClose} wide>
      <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2" noValidate>
        {msg && <p className="rounded bg-red-50 p-2 text-sm text-red-700 sm:col-span-2" role="alert">{msg}</p>}
        <div className="sm:col-span-2"><Field label="Product name" error={errs.name}><input className="input" value={f.name} onChange={set('name')} placeholder="e.g. Samsung Galaxy S21" /></Field></div>
        <Field label="Category"><select className="input" value={f.category} onChange={set('category')}>{CATS.map((c) => <option key={c}>{c}</option>)}</select></Field>
        <Field label="Brand"><input className="input" value={f.brand} onChange={set('brand')} /></Field>
        <Field label="Model"><input className="input" value={f.model} onChange={set('model')} /></Field>
        <Field label="Serial number (kept private)"><input className="input" value={f.serialNumber} onChange={set('serialNumber')} /></Field>
        <Field label="Purchase date" error={errs.purchaseDate}><input className="input" type="date" max={today()} value={f.purchaseDate} onChange={set('purchaseDate')} /></Field>
        <Field label="Warranty provider"><input className="input" value={f.warrantyProvider} onChange={set('warrantyProvider')} /></Field>
        <Field label="Warranty start"><input className="input" type="date" value={f.warrantyStart} onChange={set('warrantyStart')} /></Field>
        <Field label="Warranty end" error={errs.warrantyEnd}><input className="input" type="date" value={f.warrantyEnd} onChange={set('warrantyEnd')} /></Field>
        <p className="text-xs text-neutral-500 sm:col-span-2">Details you enter are recorded as self-reported. They are not independently verified.</p>
        <div className="flex justify-end gap-2 sm:col-span-2"><button type="button" className="btn-outline" onClick={onClose}>Cancel</button><button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Register product'}</button></div>
      </form>
    </Modal>
  );
}

export default function MyProducts() {
  const { data, loading, error, reload } = useAsync(() => api.get('/api/my-products'), []);
  const [adding, setAdding] = useState(false);
  const { push } = useToast();
  if (loading && !data) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} /></div>;
  return (
    <div className="container-x py-8">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-2xl">My products</h1><p className="text-sm text-neutral-500">Register what you own to track warranty, repairs, resale and recycling.</p></div>
        <button className="btn-primary" onClick={() => setAdding(true)}>+ Register product</button>
      </div>
      {data.length === 0 ? <EmptyState title="No products registered" text="Add a product to start its lifecycle passport." action={<button className="btn-primary" onClick={() => setAdding(true)}>Register your first product</button>} /> : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.map((p) => (
            <div key={p.id} className="card flex flex-col p-4">
              <div className="flex items-start justify-between gap-2"><div><h2 className="text-base">{p.name}</h2><p className="text-sm text-neutral-500">{[p.brand, p.model].filter(Boolean).join(' · ') || p.category}</p></div><StatusBadge value={p.status} /></div>
              <dl className="mt-3 space-y-1 text-sm">
                <div className="flex justify-between"><dt className="text-neutral-500">Purchased</dt><dd>{dateFmt(p.purchaseDate)}</dd></div>
                <div className="flex items-center justify-between"><dt className="text-neutral-500">Warranty</dt><dd className="flex items-center gap-2">{p.warrantyEnd && <span>until {dateFmt(p.warrantyEnd)}</span>}<StatusBadge value={p.warrantyStatus} /></dd></div>
                {p.openRepairs > 0 && <div className="flex justify-between"><dt className="text-neutral-500">Open repairs</dt><dd>{p.openRepairs}</dd></div>}
              </dl>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link to={`/my-products/${p.id}`} className="btn-primary">Passport</Link>
                {p.status === 'ACTIVE' && <Link to={`/repairs/new?product=${p.id}`} className="btn-outline">Repair</Link>}
                {p.status === 'ACTIVE' && <Link to={`/sell?product=${p.id}`} className="btn-outline">Sell</Link>}
              </div>
            </div>
          ))}
        </div>
      )}
      {adding && <RegisterProductModal onClose={() => setAdding(false)} onDone={() => { setAdding(false); push('Product registered'); reload(); }} />}
    </div>
  );
}
