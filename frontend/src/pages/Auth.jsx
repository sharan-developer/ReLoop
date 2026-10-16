import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Field } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';

const emailOk = (s) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s);

function Shell({ title, subtitle, children }) {
  return (
    <div className="container-x flex justify-center py-12">
      <div className="card w-full max-w-md p-6 sm:p-8">
        <h1 className="text-2xl">{title}</h1>
        <p className="mb-6 mt-1 text-sm text-neutral-500">{subtitle}</p>
        {children}
      </div>
    </div>
  );
}

export function Login() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ email: '', password: '' });
  const [errs, setErrs] = useState({});
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);
  const from = loc.state?.from || '/';
  if (user) return <Navigate to={from} replace />;

  const submit = async (e) => {
    e.preventDefault();
    const er = {};
    if (!emailOk(f.email)) er.email = 'Enter a valid email address';
    if (!f.password) er.password = 'Enter your password';
    setErrs(er);
    setMsg('');
    if (Object.keys(er).length) return;
    setBusy(true);
    try { await login(f.email, f.password); nav(from, { replace: true }); } catch (x) { setMsg(x.message); } finally { setBusy(false); }
  };
  return (
    <Shell title="Sign in" subtitle="Welcome back to ReLoop.">
      <form onSubmit={submit} className="space-y-4" noValidate>
        {msg && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700" role="alert">{msg}</div>}
        <Field label="Email" error={errs.email}><input className="input" type="email" autoComplete="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></Field>
        <Field label="Password" error={errs.password}><input className="input" type="password" autoComplete="current-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="mt-5 text-center text-sm text-neutral-600">New to ReLoop? <Link to="/register" state={loc.state} className="font-semibold text-brand-700 hover:underline">Create an account</Link></p>
    </Shell>
  );
}

export function Register() {
  const { user, register } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '', confirm: '' });
  const [errs, setErrs] = useState({});
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);
  const from = loc.state?.from || '/';
  if (user) return <Navigate to={from} replace />;
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const er = {};
    if (!f.fullName.trim()) er.fullName = 'Enter your name';
    if (!emailOk(f.email)) er.email = 'Enter a valid email address';
    if (f.password.length < 8) er.password = 'Use at least 8 characters';
    if (f.confirm !== f.password) er.confirm = 'Passwords do not match';
    setErrs(er);
    setMsg('');
    if (Object.keys(er).length) return;
    setBusy(true);
    try {
      await register({ fullName: f.fullName, email: f.email, phone: f.phone || null, password: f.password });
      nav(from, { replace: true });
    } catch (x) {
      setMsg(x.message);
      if (x.fieldErrors) setErrs(x.fieldErrors);
    } finally { setBusy(false); }
  };
  return (
    <Shell title="Create your account" subtitle="Buy, repair, resell and track your electronics.">
      <form onSubmit={submit} className="space-y-4" noValidate>
        {msg && <div className="rounded-lg bg-red-50 p-3 text-sm text-red-700" role="alert">{msg}</div>}
        <Field label="Full name" error={errs.fullName}><input className="input" autoComplete="name" value={f.fullName} onChange={set('fullName')} /></Field>
        <Field label="Email" error={errs.email}><input className="input" type="email" autoComplete="email" value={f.email} onChange={set('email')} /></Field>
        <Field label="Phone (optional)" error={errs.phone}><input className="input" type="tel" autoComplete="tel" value={f.phone} onChange={set('phone')} /></Field>
        <Field label="Password" error={errs.password} hint="At least 8 characters"><input className="input" type="password" autoComplete="new-password" value={f.password} onChange={set('password')} /></Field>
        <Field label="Confirm password" error={errs.confirm}><input className="input" type="password" autoComplete="new-password" value={f.confirm} onChange={set('confirm')} /></Field>
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</button>
      </form>
      <p className="mt-5 text-center text-sm text-neutral-600">Already registered? <Link to="/login" state={loc.state} className="font-semibold text-brand-700 hover:underline">Sign in</Link></p>
    </Shell>
  );
}
