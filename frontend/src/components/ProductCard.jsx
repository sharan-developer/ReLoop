import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { useWishlist } from '../context/WishlistContext.jsx';
import { inr } from '../utils/format.js';
import { HeartIcon } from './Icons.jsx';
import { ConditionBadge, Stars } from './ui.jsx';

export default function ProductCard({ product: p, tone = 'white' }) {
  const { user } = useAuth();
  const { add } = useCart();
  const wish = useWishlist();
  const { push } = useToast();
  const nav = useNavigate();
  const loc = useLocation();

  const needLogin = () => { nav('/login', { state: { from: loc.pathname + loc.search } }); };
  const addToCart = async () => {
    if (!user) return needLogin();
    try { await add(p.id, 1); push(`${p.name} added to cart`); } catch (e) { push(e.message, 'error'); }
  };
  const toggleWish = async () => {
    if (!user) return needLogin();
    try { await wish.toggle(p.id); } catch (e) { push(e.message, 'error'); }
  };
  const bg = tone === 'green' ? 'bg-brand-50 border-brand-200' : 'bg-white border-neutral-200';

  return (
    <article className={`group relative flex flex-col overflow-hidden rounded-xl border shadow-sm transition hover:shadow-md ${bg}`}>
      <button onClick={toggleWish} aria-label={wish.has(p.id) ? 'Remove from wishlist' : 'Add to wishlist'}
        className={`absolute right-2 top-2 z-10 rounded-full bg-white/90 p-1.5 shadow ${wish.has(p.id) ? 'text-red-500' : 'text-neutral-500 hover:text-red-500'}`}>
        <HeartIcon filled={wish.has(p.id)} width={18} height={18} />
      </button>
      {p.discountPercent > 0 && <span className="absolute left-2 top-2 z-10 rounded-md bg-brand-600 px-2 py-0.5 text-xs font-bold text-white">{p.discountPercent}% OFF</span>}
      <Link to={`/products/${p.id}`} className="block aspect-[4/3] overflow-hidden bg-brand-50">
        <img src={p.imageUrl} alt={p.name} loading="lazy" className="h-full w-full object-cover transition group-hover:scale-105" />
      </Link>
      <div className="flex flex-1 flex-col gap-1.5 p-3">
        <div className="flex items-center justify-between gap-2"><ConditionBadge value={p.condition} /><Stars value={p.rating} count={p.reviewCount} /></div>
        <Link to={`/products/${p.id}`} className="line-clamp-2 min-h-[2.5rem] text-sm font-semibold hover:text-brand-700">{p.name}</Link>
        <div className="mt-auto flex items-baseline gap-2">
          <span className="text-lg font-bold">{inr(p.price)}</span>
          {p.originalPrice > p.price && <span className="text-xs text-neutral-400 line-through">{inr(p.originalPrice)}</span>}
        </div>
        {p.available
          ? <button className="btn-primary mt-1 w-full" onClick={addToCart}>Add to Cart</button>
          : <button className="btn-outline mt-1 w-full" disabled>Out of stock</button>}
      </div>
    </article>
  );
}
