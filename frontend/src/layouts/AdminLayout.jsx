import { Link, NavLink, Outlet } from 'react-router-dom';
import { LogoMark } from '../components/Icons.jsx';

const items = [
  ['/admin', 'Dashboard', true], ['/admin/users', 'Users'], ['/admin/products', 'Products'], ['/admin/categories', 'Categories'],
  ['/admin/orders', 'Orders'], ['/admin/partners', 'Repair partners'], ['/admin/listings', 'Listings'], ['/admin/reports', 'Reports'],
  ['/admin/reviews', 'Reviews'], ['/admin/lifecycle', 'Lifecycle'], ['/admin/audit', 'Audit log'],
];

export default function AdminLayout() {
  return (
    <div className="min-h-screen bg-neutral-100">
      <div className="flex items-center justify-between bg-ink-900 px-4 py-3 text-white">
        <Link to="/admin" className="flex items-center gap-2 font-bold"><LogoMark size={26} />ReLoop Admin</Link>
        <Link to="/" className="text-sm text-neutral-300 hover:text-white">← Back to store</Link>
      </div>
      <div className="mx-auto flex max-w-7xl flex-col gap-4 p-4 lg:flex-row">
        <nav className="flex gap-1 overflow-x-auto lg:w-52 lg:shrink-0 lg:flex-col" aria-label="Admin">
          {items.map(([to, text, end]) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => `whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-brand-600 text-white' : 'text-ink-800 hover:bg-white'}`}>{text}</NavLink>
          ))}
        </nav>
        <div className="min-w-0 flex-1"><Outlet /></div>
      </div>
    </div>
  );
}
