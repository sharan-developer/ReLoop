import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import { useWishlist } from '../context/WishlistContext.jsx';
import { CartIcon, CloseIcon, HeartIcon, LogoMark, MenuIcon, SearchIcon, UserIcon } from './Icons.jsx';

function Badge({ n }) {
  if (!n) return null;
  return <span className="absolute -right-2 -top-2 flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-brand-600 px-1 text-[11px] font-bold text-white">{n > 99 ? '99+' : n}</span>;
}

export default function Header() {
  const { user, logout } = useAuth();
  const { cart } = useCart();
  const wish = useWishlist();
  const nav = useNavigate();
  const [params] = useSearchParams();
  const [q, setQ] = useState(params.get('q') || '');
  const [menu, setMenu] = useState(false);
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const close = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const submit = (e) => { e.preventDefault(); nav(`/products?q=${encodeURIComponent(q.trim())}`); setMenu(false); };
  const doLogout = async () => { setOpen(false); await logout(); nav('/'); };

  const links = user ? [
    ['/account', 'My Account'], ['/orders', 'My Orders'], ['/my-products', 'My Products'], ['/repairs', 'My Repairs'], ['/my-listings', 'My Listings'],
    ...(user.role === 'REPAIR_PARTNER' ? [['/partner', 'Partner Dashboard']] : []),
    ...(user.role === 'ADMIN' ? [['/admin', 'Admin Dashboard']] : []),
  ] : [];

  return (
    <header className="sticky top-0 z-40 border-b border-neutral-200 bg-white">
      <div className="container-x flex h-16 items-center gap-3 sm:gap-6">
        <button className="lg:hidden" aria-label="Menu" onClick={() => setMenu((m) => !m)}>{menu ? <CloseIcon /> : <MenuIcon />}</button>
        <Link to="/" className="flex shrink-0 items-center gap-2" aria-label="ReLoop home">
          <LogoMark />
          <span className="leading-tight">
            <span className="block text-xl font-extrabold tracking-tight">Re<span className="text-brand-600">Loop</span></span>
            <span className="hidden text-[10px] font-medium text-neutral-500 sm:block">Product afterlife &amp; recovery</span>
          </span>
        </Link>
        <form onSubmit={submit} className="relative mx-auto hidden max-w-2xl flex-1 md:block" role="search">
          <input className="input h-10 rounded-full bg-neutral-50 pl-4 pr-12" placeholder="Search phones, laptops, audio…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search products" />
          <button className="absolute right-1 top-1 flex h-8 w-9 items-center justify-center rounded-full bg-brand-600 text-white hover:bg-brand-700" aria-label="Search"><SearchIcon width={18} height={18} /></button>
        </form>
        <div className="ml-auto flex items-center gap-4 sm:gap-5">
          <Link to="/wishlist" className="relative text-ink-800 hover:text-brand-700" aria-label="Wishlist"><HeartIcon /><Badge n={wish.items.length} /></Link>
          <Link to="/cart" className="relative text-ink-800 hover:text-brand-700" aria-label="Cart"><CartIcon /><Badge n={cart.itemCount} /></Link>
          {user ? (
            <div className="relative" ref={ref}>
              <button className="flex items-center gap-1.5 text-sm font-semibold hover:text-brand-700" onClick={() => setOpen((o) => !o)} aria-haspopup="menu" aria-expanded={open}>
                <UserIcon /><span className="hidden max-w-[7rem] truncate sm:block">{user.fullName.split(' ')[0]}</span>
              </button>
              {open && (
                <div role="menu" className="absolute right-0 mt-2 w-52 rounded-xl border border-neutral-200 bg-white py-1 shadow-lg">
                  {links.map(([to, text]) => <Link key={to} to={to} role="menuitem" onClick={() => setOpen(false)} className="block px-4 py-2 text-sm hover:bg-brand-50">{text}</Link>)}
                  <button role="menuitem" onClick={doLogout} className="block w-full border-t border-neutral-100 px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50">Sign out</button>
                </div>
              )}
            </div>
          ) : (
            <Link to="/login" className="btn-primary hidden sm:inline-flex">Sign In</Link>
          )}
        </div>
      </div>
      {menu && (
        <div className="border-t border-neutral-100 bg-white p-4 lg:hidden">
          <form onSubmit={submit} className="mb-3 flex gap-2" role="search">
            <input className="input" placeholder="Search products" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search products" />
            <button className="btn-primary" aria-label="Search"><SearchIcon width={18} height={18} /></button>
          </form>
          {!user && <Link to="/login" className="btn-primary w-full" onClick={() => setMenu(false)}>Sign In</Link>}
        </div>
      )}
    </header>
  );
}
