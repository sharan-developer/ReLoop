import { useState } from 'react';
import AdminTable from '../../components/AdminTable.jsx';
import { ConfirmDialog, ErrorState, Field, Modal, PageLoader, StatusBadge, Badge } from '../../components/ui.jsx';
import { useToast } from '../../context/ToastContext.jsx';
import useAsync from '../../hooks/useAsync.js';
import { api, qs } from '../../services/api.js';
import { dateTimeFmt, inr, label } from '../../utils/format.js';

const page = (path) => (q, p) => api.get(`${path}${qs({ q, page: p, size: 10 })}`);

/** Button that asks for confirmation, runs the action, reports the outcome and refreshes the table. */
function ConfirmButton({ text, title, message, danger, run, reload, ok }) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const { push } = useToast();
  const go = async () => {
    setBusy(true);
    try { await run(); push(ok || 'Done'); reload(); } catch (e) { push(e.message, 'error'); } finally { setBusy(false); setOpen(false); }
  };
  return (
    <>
      <button className={danger ? 'btn-outline text-red-600' : 'btn-outline'} onClick={() => setOpen(true)}>{text}</button>
      {open && <ConfirmDialog busy={busy} danger={danger} title={title} message={message} confirmText={text} onCancel={() => setOpen(false)} onConfirm={go} />}
    </>
  );
}

function Stat({ label: l, value, hint }) {
  return <div className="card p-4"><p className="text-xs uppercase text-neutral-500">{l}</p><p className="mt-1 text-2xl font-bold">{value}</p>{hint && <p className="text-xs text-neutral-400">{hint}</p>}</div>;
}

function Trend({ title, points, money }) {
  const max = Math.max(1, ...points.map((p) => (money ? Number(p.amount) : p.count)));
  return (
    <div className="card p-4">
      <h3 className="mb-3 text-sm">{title}</h3>
      <div className="flex h-32 items-end gap-2" role="img" aria-label={title}>
        {points.map((p) => {
          const v = money ? Number(p.amount) : p.count;
          return (
            <div key={p.month} className="flex flex-1 flex-col items-center gap-1">
              <span className="text-[10px] text-neutral-500">{money ? inr(v) : v}</span>
              <div className="w-full rounded-t bg-brand-500" style={{ height: `${Math.max(2, (v / max) * 80)}%` }} title={`${p.month}: ${v}`} />
              <span className="text-[10px] text-neutral-400">{p.month.slice(5)}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function AdminDashboard() {
  const { data: s, loading, error, reload } = useAsync(() => api.get('/api/admin/stats'), []);
  if (loading) return <PageLoader />;
  if (error) return <ErrorState error={error} onRetry={reload} />;
  return (
    <div className="space-y-5">
      <h1 className="text-xl">Dashboard</h1>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Stat label="Users" value={s.users} /><Stat label="Active products" value={s.products} />
        <Stat label="Active listings" value={s.activeListings} /><Stat label="Orders" value={s.orders} />
        <Stat label="Repair requests" value={s.repairRequests} /><Stat label="Completed repairs" value={s.completedRepairs} />
        <Stat label="Pending approvals" value={s.pendingApprovals} hint="Partner applications" /><Stat label="Reported listings" value={s.reportedListings} hint="Open reports" />
      </div>
      <Stat label="Total sales (non-cancelled orders)" value={inr(s.totalSales)} />
      <div className="grid gap-3 lg:grid-cols-3">
        <Trend title="Store sales (₹, last 6 months)" points={s.salesTrend} money />
        <Trend title="Repair requests" points={s.repairTrend} />
        <Trend title="Completed resale transfers" points={s.resaleTrend} />
      </div>
      <section className="card p-4">
        <h2 className="mb-2 text-base">Recent activity</h2>
        {s.recent.length === 0 ? <p className="text-sm text-neutral-500">No activity yet.</p> : (
          <ul className="divide-y text-sm">{s.recent.map((a, i) => <li key={i} className="flex flex-wrap justify-between gap-2 py-2"><span><strong>{label(a.action)}</strong> <span className="text-neutral-500">{a.entityType} #{a.entityId} {a.details}</span></span><span className="text-xs text-neutral-400">{dateTimeFmt(a.createdAt)}</span></li>)}</ul>
        )}
      </section>
    </div>
  );
}

export function AdminUsers() {
  return (
    <AdminTable title="Users" load={page('/api/admin/users')}
      columns={[{ header: 'Email', render: (u) => u.email }, { header: 'Name', render: (u) => u.fullName }, { header: 'Role', render: (u) => <Badge>{label(u.role)}</Badge> }, { header: 'Status', render: (u) => <StatusBadge value={u.enabled ? 'ACTIVE' : 'CANCELLED'} /> }, { header: 'Joined', render: (u) => dateTimeFmt(u.createdAt) }]}
      actions={(u, reload) => (
        <ConfirmButton danger={u.enabled} text={u.enabled ? 'Disable' : 'Enable'} title={`${u.enabled ? 'Disable' : 'Enable'} ${u.email}?`}
          message={u.enabled ? 'They will be unable to sign in or use the API.' : 'They will regain access.'} ok="User updated" reload={reload}
          run={() => api.patch(`/api/admin/users/${u.id}`, { enabled: !u.enabled })} />
      )} />
  );
}

const NEXT = { PENDING_PAYMENT: ['CANCELLED'], PLACED: ['CONFIRMED', 'CANCELLED'], CONFIRMED: ['SHIPPED', 'CANCELLED'], SHIPPED: ['DELIVERED'] };
export function AdminOrders() {
  return (
    <AdminTable title="Orders" load={page('/api/admin/orders')}
      columns={[{ header: 'Order', render: (o) => o.orderNumber }, { header: 'Customer', render: (o) => o.userEmail }, { header: 'Status', render: (o) => <StatusBadge value={o.status} /> }, { header: 'Payment', render: (o) => <StatusBadge value={o.paymentStatus} /> }, { header: 'Total', render: (o) => inr(o.total) }, { header: 'Placed', render: (o) => dateTimeFmt(o.createdAt) }]}
      actions={(o, reload) => (NEXT[o.status] || []).map((n) => (
        <ConfirmButton key={n} danger={n === 'CANCELLED'} text={label(n)} title={`${label(n)} order ${o.orderNumber}?`} message={n === 'CANCELLED' ? 'Stock will be restored.' : 'The customer will see the new status.'} ok="Order updated" reload={reload}
          run={() => api.patch(`/api/admin/orders/${o.id}/status`, { status: n })} />
      ))} />
  );
}

export function AdminPartners() {
  return (
    <AdminTable title="Repair partners" load={page('/api/admin/partners')}
      columns={[{ header: 'Business', render: (p) => p.businessName }, { header: 'City', render: (p) => p.city }, { header: 'Account', render: (p) => p.email }, { header: 'Approved', render: (p) => <StatusBadge value={p.approved ? 'APPROVED' : 'PENDING'} /> }]}
      actions={(p, reload) => (
        <ConfirmButton danger={p.approved} text={p.approved ? 'Revoke' : 'Approve'} title={`${p.approved ? 'Revoke' : 'Approve'} ${p.businessName}?`}
          message={p.approved ? 'They lose partner access immediately.' : 'This grants the repair partner role to the applicant.'} ok="Partner updated" reload={reload}
          run={() => api.post(`/api/admin/partners/${p.id}/${p.approved ? 'revoke' : 'approve'}`)} />
      )} />
  );
}

export function AdminListings() {
  return (
    <AdminTable title="Resale listings" load={page('/api/admin/listings')}
      columns={[{ header: 'Title', render: (l) => l.title }, { header: 'Seller', render: (l) => l.sellerEmail }, { header: 'Price', render: (l) => inr(l.price) }, { header: 'Status', render: (l) => <StatusBadge value={l.status} /> }]}
      actions={(l, reload) => (<>
        {l.status === 'PUBLISHED' && <ConfirmButton danger text="Take down" title="Take down this listing?" message="It will be paused and hidden from buyers." ok="Listing paused" reload={reload} run={() => api.patch(`/api/admin/listings/${l.id}/status`, { status: 'PAUSED' })} />}
        {l.status === 'PAUSED' && <ConfirmButton text="Re-publish" title="Re-publish this listing?" message="It becomes visible to buyers again." ok="Listing published" reload={reload} run={() => api.patch(`/api/admin/listings/${l.id}/status`, { status: 'PUBLISHED' })} />}
      </>)} />
  );
}

export function AdminReports() {
  return (
    <AdminTable title="Reported listings" searchable={false} load={(q, p) => api.get(`/api/admin/reports${qs({ page: p, size: 10, openOnly: true })}`)}
      columns={[{ header: 'Listing', render: (r) => `#${r.listingId} ${r.listingTitle}` }, { header: 'Reason', render: (r) => r.reason }, { header: 'Reported', render: (r) => dateTimeFmt(r.createdAt) }]}
      actions={(r, reload) => <ConfirmButton text="Mark resolved" title="Resolve this report?" message="Take down the listing first if needed (Listings page)." ok="Report resolved" reload={reload} run={() => api.post(`/api/admin/reports/${r.id}/resolve`)} />} />
  );
}

export function AdminReviews() {
  return (
    <AdminTable title="Reviews" load={page('/api/admin/reviews')}
      columns={[{ header: 'Product', render: (r) => r.productName }, { header: 'By', render: (r) => r.userName }, { header: 'Rating', render: (r) => `${r.rating}★` }, { header: 'Comment', render: (r) => r.comment || '—' }, { header: 'Visible', render: (r) => <StatusBadge value={r.hidden ? 'CANCELLED' : 'ACTIVE'} /> }]}
      actions={(r, reload) => <ConfirmButton danger={!r.hidden} text={r.hidden ? 'Restore' : 'Hide'} title={`${r.hidden ? 'Restore' : 'Hide'} this review?`} message={r.hidden ? 'It will show on the product page again.' : 'It will no longer be shown or counted.'} ok="Review updated" reload={reload} run={() => api.post(`/api/admin/reviews/${r.id}/${r.hidden ? 'restore' : 'hide'}`)} />} />
  );
}

export function AdminLifecycle() {
  return (
    <div className="space-y-3">
      <p className="rounded-lg bg-neutral-100 p-3 text-xs text-neutral-600">Read-only oversight. Lifecycle events are append-only and cannot be edited or deleted, even by administrators. Recycling reports can be confirmed, which adds a new admin-confirmed event.</p>
      <AdminTable title="Lifecycle events" load={page('/api/admin/lifecycle')}
        columns={[{ header: 'Product', render: (e) => `#${e.ownedProductId} ${e.productName}` }, { header: 'Event', render: (e) => label(e.type) }, { header: 'Verification', render: (e) => <StatusBadge value={e.verification} /> }, { header: 'Actor', render: (e) => e.actorLabel }, { header: 'When', render: (e) => dateTimeFmt(e.createdAt) }]}
        actions={(e, reload) => e.type === 'RECYCLING_RECORDED' && e.verification === 'SELF_REPORTED' ? <ConfirmButton text="Confirm recycling" title="Confirm this recycling record?" message="Only confirm after reviewing the evidence. This adds an admin-confirmed event." ok="Recycling confirmed" reload={reload} run={() => api.post(`/api/admin/lifecycle/products/${e.ownedProductId}/confirm-recycling`)} /> : null} />
    </div>
  );
}

export function AdminAudit() {
  return <AdminTable title="Audit log" searchable={false} load={(q, p) => api.get(`/api/admin/audit${qs({ page: p, size: 20 })}`)}
    columns={[{ header: 'Action', render: (a) => label(a.action) }, { header: 'Entity', render: (a) => `${a.entityType} #${a.entityId ?? ''}` }, { header: 'Details', render: (a) => a.details || '' }, { header: 'When', render: (a) => dateTimeFmt(a.createdAt) }]} />;
}

export function AdminCategories() {
  const { push } = useToast();
  const { data, loading, error, reload } = useAsync(() => api.get('/api/admin/categories'), []);
  const [edit, setEdit] = useState(null);
  const [name, setName] = useState('');
  if (loading && !data) return <PageLoader />;
  if (error) return <ErrorState error={error} onRetry={reload} />;
  const save = async (e) => {
    e.preventDefault();
    if (!name.trim()) return push('Enter a name', 'error');
    try { if (edit.id) await api.put(`/api/admin/categories/${edit.id}`, { name, active: edit.active }); else await api.post('/api/admin/categories', { name }); push('Category saved'); setEdit(null); reload(); } catch (x) { push(x.message, 'error'); }
  };
  return (
    <div>
      <div className="mb-4 flex items-center justify-between"><h1 className="text-xl">Categories</h1><button className="btn-primary" onClick={() => { setName(''); setEdit({}); }}>+ Add category</button></div>
      <div className="card divide-y">{data.map((c) => (
        <div key={c.id} className="flex items-center justify-between p-3 text-sm"><span>{c.name} <span className="text-neutral-400">/{c.slug}</span></span>
          <span className="flex items-center gap-2"><StatusBadge value={c.active ? 'ACTIVE' : 'CANCELLED'} />
            <button className="btn-outline" onClick={() => { setName(c.name); setEdit(c); }}>Rename</button>
            <ConfirmButton text={c.active ? 'Deactivate' : 'Activate'} danger={c.active} title={`${c.active ? 'Deactivate' : 'Activate'} ${c.name}?`} message="Inactive categories disappear from the store navigation." ok="Category updated" reload={reload} run={() => api.put(`/api/admin/categories/${c.id}`, { name: c.name, active: !c.active })} /></span></div>
      ))}</div>
      {edit && <Modal title={edit.id ? 'Rename category' : 'New category'} onClose={() => setEdit(null)}><form onSubmit={save} className="space-y-3"><Field label="Name"><input className="input" value={name} onChange={(e) => setName(e.target.value)} /></Field><button className="btn-primary w-full">Save</button></form></Modal>}
      <p className="mt-3 text-xs text-neutral-500">Store navigation updates after the page is reloaded.</p>
    </div>
  );
}

function ProductForm({ product, cats, onClose, onDone }) {
  const [f, setF] = useState({ name: product?.name || '', brand: product?.brand || '', description: product?.description || '', condition: product?.condition || 'NEW', price: product?.price ?? '', originalPrice: product?.originalPrice ?? '', imageUrl: product?.imageUrl || '', categoryId: product?.categoryId || cats[0]?.id || '', stock: product?.stock ?? 0, active: product?.active ?? true });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault(); setErr('');
    if (!f.name.trim()) return setErr('Name is required');
    if (!(Number(f.price) > 0)) return setErr('Enter a valid price');
    const body = { ...f, price: Number(f.price), originalPrice: f.originalPrice === '' ? null : Number(f.originalPrice), categoryId: Number(f.categoryId), stock: Number(f.stock), imageUrl: f.imageUrl || null };
    try { if (product) await api.put(`/api/admin/products/${product.id}`, body); else await api.post('/api/admin/products', body); onDone(); } catch (x) { setErr(x.message); }
  };
  return (
    <Modal title={product ? 'Edit product' : 'New product'} onClose={onClose} wide>
      <form onSubmit={submit} className="grid gap-3 sm:grid-cols-2">
        {err && <p className="rounded bg-red-50 p-2 text-sm text-red-700 sm:col-span-2" role="alert">{err}</p>}
        <div className="sm:col-span-2"><Field label="Name"><input className="input" value={f.name} onChange={set('name')} /></Field></div>
        <Field label="Brand"><input className="input" value={f.brand} onChange={set('brand')} /></Field>
        <Field label="Category"><select className="input" value={f.categoryId} onChange={set('categoryId')}>{cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></Field>
        <Field label="Condition"><select className="input" value={f.condition} onChange={set('condition')}><option>NEW</option><option>REFURBISHED</option><option>USED</option></select></Field>
        <Field label="Stock"><input className="input" type="number" min="0" value={f.stock} onChange={set('stock')} /></Field>
        <Field label="Price (₹)"><input className="input" type="number" min="1" value={f.price} onChange={set('price')} /></Field>
        <Field label="Original price (₹)"><input className="input" type="number" min="1" value={f.originalPrice} onChange={set('originalPrice')} /></Field>
        <div className="sm:col-span-2"><Field label="Image URL" hint="e.g. /images/products/ps5.svg or an uploaded image URL"><input className="input" value={f.imageUrl} onChange={set('imageUrl')} /></Field></div>
        <div className="sm:col-span-2"><Field label="Description"><textarea className="input" rows={3} value={f.description} onChange={set('description')} /></Field></div>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} />Visible in store</label>
        <div className="flex justify-end gap-2 sm:col-span-2"><button type="button" className="btn-outline" onClick={onClose}>Cancel</button><button className="btn-primary">Save product</button></div>
      </form>
    </Modal>
  );
}

export function AdminProducts() {
  const { push } = useToast();
  const cats = useAsync(() => api.get('/api/admin/categories'), []);
  const [edit, setEdit] = useState(null);
  const [tick, setTick] = useState(0);
  return (
    <>
      <AdminTable key={tick} title="Products" load={page('/api/admin/products')}
        headerExtra={() => <button className="btn-primary" disabled={!cats.data} onClick={() => setEdit('new')}>+ Add</button>}
        columns={[{ header: 'Name', render: (p) => p.name }, { header: 'Category', render: (p) => p.categoryName }, { header: 'Condition', render: (p) => label(p.condition) }, { header: 'Price', render: (p) => inr(p.price) }, { header: 'Stock', render: (p) => p.stock }, { header: 'Visible', render: (p) => <StatusBadge value={p.active ? 'ACTIVE' : 'CANCELLED'} /> }]}
        actions={(p, reload) => (<>
          <button className="btn-outline" onClick={() => setEdit(p)}>Edit</button>
          {p.active && <ConfirmButton danger text="Deactivate" title={`Deactivate ${p.name}?`} message="It is hidden from the store. Past orders keep their records." ok="Product deactivated" reload={reload} run={() => api.del(`/api/admin/products/${p.id}`)} />}
        </>)} />
      {edit && cats.data && <ProductForm product={edit === 'new' ? null : edit} cats={cats.data.filter((c) => c.active)} onClose={() => setEdit(null)} onDone={() => { setEdit(null); push('Product saved'); setTick((t) => t + 1); }} />}
    </>
  );
}
