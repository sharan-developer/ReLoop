import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import ProductRow from '../components/ProductRow.jsx';
import { ConditionBadge, ErrorState, PageLoader, Pagination, Stars } from '../components/ui.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { useWishlist } from '../context/WishlistContext.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';
import { dateFmt, inr, pushRecent } from '../utils/format.js';

function Reviews({ id }) {
  const [page, setPage] = useState(0);
  const { data } = useAsync(() => api.get(`/api/products/${id}/reviews?page=${page}&size=5`), [id, page]);
  return (
    <section className="mt-10">
      <h2 className="mb-3 text-xl">Customer reviews</h2>
      {data && data.items.length === 0 && <p className="text-sm text-neutral-500">No reviews yet. Customers can review products after delivery, from their order page.</p>}
      <div className="space-y-3">
        {data?.items.map((r) => (
          <div key={r.id} className="card p-4">
            <div className="flex items-center justify-between"><Stars value={r.rating} /><span className="text-xs text-neutral-500">{r.userName} · {dateFmt(r.createdAt)} · Verified purchase</span></div>
            {r.comment && <p className="mt-2 text-sm">{r.comment}</p>}
          </div>
        ))}
      </div>
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </section>
  );
}

export default function ProductDetail() {
  const { id } = useParams();
  const { data: p, loading, error, reload } = useAsync(() => api.get(`/api/products/${id}`), [id]);
  const { user } = useAuth();
  const { add } = useCart();
  const wish = useWishlist();
  const { push } = useToast();
  const nav = useNavigate();
  const loc = useLocation();
  const [qty, setQty] = useState(1);
  const [img, setImg] = useState(0);
  const [busy, setBusy] = useState(false);

  useEffect(() => { pushRecent(Number(id)); setQty(1); setImg(0); }, [id]);

  if (loading) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} title={error.status === 404 ? 'Product not found' : undefined} /></div>;

  const needLogin = () => nav('/login', { state: { from: loc.pathname } });
  const addToCart = async (goCart) => {
    if (!user) return needLogin();
    setBusy(true);
    try { await add(p.id, qty); push('Added to cart'); if (goCart) nav('/cart'); } catch (e) { push(e.message, 'error'); } finally { setBusy(false); }
  };
  const toggleWish = async () => { if (!user) return needLogin(); try { await wish.toggle(p.id); } catch (e) { push(e.message, 'error'); } };

  return (
    <div className="container-x py-6">
      <nav className="mb-4 text-sm text-neutral-500"><Link to="/" className="hover:underline">Home</Link> / <Link to={`/products`} className="hover:underline">Products</Link> / {p.name}</nav>
      <div className="grid gap-8 md:grid-cols-2">
        <div>
          <div className="aspect-[4/3] overflow-hidden rounded-xl border border-neutral-200 bg-brand-50"><img src={p.images[img] || p.imageUrl} alt={p.name} className="h-full w-full object-cover" /></div>
          {p.images.length > 1 && (
            <div className="mt-3 flex gap-2">{p.images.map((u, i) => <button key={u} onClick={() => setImg(i)} className={`h-16 w-16 overflow-hidden rounded-lg border-2 ${i === img ? 'border-brand-600' : 'border-transparent'}`}><img src={u} alt="" className="h-full w-full object-cover" /></button>)}</div>
          )}
        </div>
        <div>
          <div className="flex items-center gap-2"><ConditionBadge value={p.condition} /><span className="text-sm text-neutral-500">{p.brand} · {p.categoryName}</span></div>
          <h1 className="mt-2 text-2xl sm:text-3xl">{p.name}</h1>
          <div className="mt-2"><Stars value={p.rating} count={p.reviewCount} /></div>
          <div className="mt-4 flex items-baseline gap-3">
            <span className="text-3xl font-extrabold">{inr(p.price)}</span>
            {p.originalPrice > p.price && <><span className="text-neutral-400 line-through">{inr(p.originalPrice)}</span><span className="font-semibold text-brand-700">{p.discountPercent}% off</span></>}
          </div>
          <p className={`mt-3 text-sm font-semibold ${p.available ? (p.stock <= 3 ? 'text-amber-600' : 'text-brand-700') : 'text-red-600'}`}>
            {p.available ? (p.stock <= 3 ? `Only ${p.stock} left` : 'In stock') : 'Out of stock'}
          </p>
          <p className="mt-4 whitespace-pre-line text-sm leading-relaxed text-neutral-700">{p.description}</p>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-sm">Qty
              <select className="input w-20" value={qty} onChange={(e) => setQty(Number(e.target.value))} disabled={!p.available}>
                {Array.from({ length: Math.min(p.stock, 10) || 1 }, (_, i) => i + 1).map((n) => <option key={n}>{n}</option>)}
              </select>
            </label>
            <button className="btn-primary" disabled={!p.available || busy} onClick={() => addToCart(false)}>Add to Cart</button>
            <button className="btn-dark" disabled={!p.available || busy} onClick={() => addToCart(true)}>Buy Now</button>
            <button className="btn-outline" onClick={toggleWish}>{wish.has(p.id) ? '♥ Saved' : '♡ Wishlist'}</button>
          </div>
        </div>
      </div>
      <Reviews id={id} />
      <div className="-mx-4 sm:-mx-6 lg:-mx-8"><ProductRow title="Related products" loader={() => api.get(`/api/products/${id}/related`)} deps={[id]} hideIfEmpty /></div>
    </div>
  );
}
