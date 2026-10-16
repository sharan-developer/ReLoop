import { useState } from 'react';
import { Field } from './ui.jsx';

const blank = { label: 'Home', line1: '', line2: '', city: '', state: '', postalCode: '', phone: '', defaultAddress: false };

export default function AddressForm({ initial, onSubmit, onCancel, submitText = 'Save address' }) {
  const [f, setF] = useState({ ...blank, ...(initial || {}) });
  const [errs, setErrs] = useState({});
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const er = {};
    if (!f.line1.trim()) er.line1 = 'Required';
    if (!f.city.trim()) er.city = 'Required';
    if (!f.state.trim()) er.state = 'Required';
    if (!/^\d{6}$/.test(f.postalCode)) er.postalCode = 'Enter a 6-digit PIN code';
    if (!f.phone.trim()) er.phone = 'Required';
    setErrs(er);
    if (Object.keys(er).length) return;
    setBusy(true); setMsg('');
    try { await onSubmit({ ...f, line2: f.line2 || null }); } catch (x) { setMsg(x.message); if (x.fieldErrors) setErrs(x.fieldErrors); } finally { setBusy(false); }
  };

  return (
    <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2" noValidate>
      {msg && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700 sm:col-span-2" role="alert">{msg}</div>}
      <Field label="Label" error={errs.label}><input className="input" value={f.label} onChange={set('label')} /></Field>
      <Field label="Phone" error={errs.phone}><input className="input" type="tel" value={f.phone} onChange={set('phone')} /></Field>
      <div className="sm:col-span-2"><Field label="Address line 1" error={errs.line1}><input className="input" value={f.line1} onChange={set('line1')} /></Field></div>
      <div className="sm:col-span-2"><Field label="Address line 2 (optional)" error={errs.line2}><input className="input" value={f.line2 || ''} onChange={set('line2')} /></Field></div>
      <Field label="City" error={errs.city}><input className="input" value={f.city} onChange={set('city')} /></Field>
      <Field label="State" error={errs.state}><input className="input" value={f.state} onChange={set('state')} /></Field>
      <Field label="PIN code" error={errs.postalCode}><input className="input" inputMode="numeric" maxLength={6} value={f.postalCode} onChange={set('postalCode')} /></Field>
      <label className="flex items-center gap-2 self-end pb-2 text-sm"><input type="checkbox" checked={f.defaultAddress} onChange={(e) => setF({ ...f, defaultAddress: e.target.checked })} />Make default</label>
      <div className="flex justify-end gap-2 sm:col-span-2">
        {onCancel && <button type="button" className="btn-outline" onClick={onCancel}>Cancel</button>}
        <button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : submitText}</button>
      </div>
    </form>
  );
}
