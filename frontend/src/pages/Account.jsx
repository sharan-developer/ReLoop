import { useState } from 'react';
import { Link } from 'react-router-dom';
import AddressForm from '../components/AddressForm.jsx';
import { ConfirmDialog, ErrorState, Field, Modal, PageLoader } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';

function Profile() {
  const { user, setUser } = useAuth();
  const { push } = useToast();
  const [f, setF] = useState({ fullName: user.fullName, phone: user.phone || '' });
  const [errs, setErrs] = useState({});
  const save = async (e) => {
    e.preventDefault(); setErrs({});
    if (!f.fullName.trim()) return setErrs({ fullName: 'Enter your name' });
    try { setUser(await api.put('/api/users/me', f)); push('Profile updated'); } catch (x) { push(x.message, 'error'); setErrs(x.fieldErrors || {}); }
  };
  return (
    <form onSubmit={save} className="card space-y-3 p-5">
      <h2 className="text-lg">Profile</h2>
      <p className="text-sm text-neutral-500">{user.email} · {user.role.replace('_', ' ').toLowerCase()}</p>
      <Field label="Full name" error={errs.fullName}><input className="input" value={f.fullName} onChange={(e) => setF({ ...f, fullName: e.target.value })} /></Field>
      <Field label="Phone" error={errs.phone}><input className="input" value={f.phone} onChange={(e) => setF({ ...f, phone: e.target.value })} /></Field>
      <button className="btn-primary">Save changes</button>
    </form>
  );
}

function Password() {
  const { push } = useToast();
  const [f, setF] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [err, setErr] = useState('');
  const save = async (e) => {
    e.preventDefault(); setErr('');
    if (f.newPassword.length < 8) return setErr('New password must be at least 8 characters');
    if (f.newPassword !== f.confirm) return setErr('Passwords do not match');
    try { await api.post('/api/users/me/password', { currentPassword: f.currentPassword, newPassword: f.newPassword }); push('Password changed'); setF({ currentPassword: '', newPassword: '', confirm: '' }); } catch (x) { setErr(x.message); }
  };
  return (
    <form onSubmit={save} className="card space-y-3 p-5">
      <h2 className="text-lg">Change password</h2>
      {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700" role="alert">{err}</p>}
      <Field label="Current password"><input className="input" type="password" autoComplete="current-password" value={f.currentPassword} onChange={(e) => setF({ ...f, currentPassword: e.target.value })} /></Field>
      <Field label="New password"><input className="input" type="password" autoComplete="new-password" value={f.newPassword} onChange={(e) => setF({ ...f, newPassword: e.target.value })} /></Field>
      <Field label="Confirm new password"><input className="input" type="password" autoComplete="new-password" value={f.confirm} onChange={(e) => setF({ ...f, confirm: e.target.value })} /></Field>
      <button className="btn-primary">Update password</button>
    </form>
  );
}

function Addresses() {
  const { push } = useToast();
  const { data, loading, error, reload } = useAsync(() => api.get('/api/addresses'), []);
  const [edit, setEdit] = useState(null); // null | 'new' | address
  const [del, setDel] = useState(null);
  return (
    <section className="card p-5 lg:col-span-2">
      <div className="mb-3 flex items-center justify-between"><h2 className="text-lg">Saved addresses</h2><button className="btn-outline" onClick={() => setEdit('new')}>+ Add address</button></div>
      {loading && <PageLoader />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.length === 0 && <p className="text-sm text-neutral-500">No saved addresses.</p>}
      <div className="grid gap-3 sm:grid-cols-2">
        {data?.map((a) => (
          <div key={a.id} className="rounded-lg border border-neutral-200 p-3 text-sm">
            <p className="font-semibold">{a.label}{a.defaultAddress && <span className="ml-2 text-xs font-normal text-brand-700">Default</span>}</p>
            <p className="mt-1 text-neutral-600">{a.line1}{a.line2 ? `, ${a.line2}` : ''}, {a.city}, {a.state} {a.postalCode}</p>
            <p className="text-neutral-500">{a.phone}</p>
            <div className="mt-2 flex gap-3"><button className="text-brand-700 hover:underline" onClick={() => setEdit(a)}>Edit</button><button className="text-red-600 hover:underline" onClick={() => setDel(a)}>Delete</button></div>
          </div>
        ))}
      </div>
      {edit && (
        <Modal title={edit === 'new' ? 'Add address' : 'Edit address'} onClose={() => setEdit(null)} wide>
          <AddressForm initial={edit === 'new' ? null : edit} onCancel={() => setEdit(null)} onSubmit={async (v) => {
            if (edit === 'new') await api.post('/api/addresses', v); else await api.put(`/api/addresses/${edit.id}`, v);
            setEdit(null); push('Address saved'); reload();
          }} />
        </Modal>
      )}
      {del && <ConfirmDialog danger title="Delete address?" message={`${del.label} will be removed. Past orders are not affected.`} confirmText="Delete" onCancel={() => setDel(null)} onConfirm={async () => { try { await api.del(`/api/addresses/${del.id}`); push('Address deleted'); } catch (x) { push(x.message, 'error'); } setDel(null); reload(); }} />}
    </section>
  );
}

function PartnerApply() {
  const { user } = useAuth();
  const { push } = useToast();
  const [f, setF] = useState({ businessName: '', city: '' });
  const [sent, setSent] = useState(false);
  if (user.role !== 'CUSTOMER') return null;
  const submit = async (e) => {
    e.preventDefault();
    if (!f.businessName.trim() || !f.city.trim()) return push('Enter business name and city', 'error');
    try { await api.post('/api/partners/apply', f); setSent(true); push('Application submitted'); } catch (x) { push(x.message, 'error'); }
  };
  return (
    <form onSubmit={submit} className="card space-y-3 p-5 lg:col-span-2">
      <h2 className="text-lg">Become a repair partner</h2>
      {sent ? <p className="text-sm text-brand-700">Application received. An administrator will review it. Partner access is only granted on approval.</p> : (
        <div className="grid gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <Field label="Business name"><input className="input" value={f.businessName} onChange={(e) => setF({ ...f, businessName: e.target.value })} /></Field>
          <Field label="City"><input className="input" value={f.city} onChange={(e) => setF({ ...f, city: e.target.value })} /></Field>
          <button className="btn-outline">Apply</button>
        </div>
      )}
    </form>
  );
}

export default function Account() {
  return (
    <div className="container-x py-8">
      <h1 className="mb-5 text-2xl">My account</h1>
      <div className="grid gap-5 lg:grid-cols-2">
        <Profile /><Password /><Addresses /><PartnerApply />
      </div>
      <p className="mt-6 text-sm text-neutral-500">Shortcuts: <Link className="text-brand-700 hover:underline" to="/orders">orders</Link> · <Link className="text-brand-700 hover:underline" to="/my-products">my products</Link> · <Link className="text-brand-700 hover:underline" to="/repairs">repairs</Link></p>
    </div>
  );
}
