import { useEffect, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { getCategories } from '../services/catalog.js';

export default function CategoryNav() {
  const [cats, setCats] = useState([]);
  const loc = useLocation();
  useEffect(() => { getCategories().then(setCats).catch(() => {}); }, []);
  const sp = new URLSearchParams(loc.search);
  const active = (test) => (test ? 'border-brand-600 text-brand-700' : 'border-transparent text-ink-800 hover:text-brand-700');
  const onProducts = loc.pathname === '/products';
  const cls = 'whitespace-nowrap border-b-2 px-3 py-3 text-sm font-medium transition';
  return (
    <nav className="border-b border-neutral-200 bg-white" aria-label="Categories">
      <div className="container-x flex overflow-x-auto">
        <NavLink to="/products" end className={cls + ' ' + active(onProducts && !sp.get('category') && !sp.get('condition'))}>All Categories</NavLink>
        {cats.map((c) => (
          <NavLink key={c.id} to={`/products?category=${c.slug}`} className={cls + ' ' + active(onProducts && sp.get('category') === c.slug)}>{c.name}</NavLink>
        ))}
        <NavLink to="/products?condition=REFURBISHED&sort=price_asc" className={cls + ' font-semibold ' + active(onProducts && sp.get('condition') === 'REFURBISHED')}>Refurbished Deals</NavLink>
        <NavLink to="/repair-services" className={cls + ' ' + active(loc.pathname === '/repair-services')}>Repair Services</NavLink>
        <NavLink to="/sell" className={cls + ' ' + active(loc.pathname === '/sell')}>Sell Your Product</NavLink>
      </div>
    </nav>
  );
}
