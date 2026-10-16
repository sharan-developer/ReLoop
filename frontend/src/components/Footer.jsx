import { Link } from 'react-router-dom';
import { LogoMark } from './Icons.jsx';

export default function Footer() {
  const col = 'space-y-2 text-sm text-neutral-300';
  const a = 'hover:text-white';
  return (
    <footer className="mt-16 bg-ink-900 text-neutral-300">
      <div className="container-x grid gap-8 py-12 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="flex items-center gap-2 text-white"><LogoMark size={30} /><span className="text-lg font-bold">ReLoop</span></div>
          <p className="mt-3 text-sm">Buy, repair, resell and recycle electronics with a transparent lifecycle record for every product.</p>
        </div>
        <div><h4 className="mb-3 font-semibold text-white">Shop</h4><ul className={col}>
          <li><Link className={a} to="/products">All products</Link></li>
          <li><Link className={a} to="/products?condition=REFURBISHED">Refurbished deals</Link></li>
          <li><Link className={a} to="/marketplace">Resale marketplace</Link></li>
        </ul></div>
        <div><h4 className="mb-3 font-semibold text-white">Your products</h4><ul className={col}>
          <li><Link className={a} to="/my-products">My Products &amp; Passports</Link></li>
          <li><Link className={a} to="/repair-services">Repair services</Link></li>
          <li><Link className={a} to="/sell">Sell your product</Link></li>
          <li><Link className={a} to="/calculator">Repair vs replace</Link></li>
        </ul></div>
        <div><h4 className="mb-3 font-semibold text-white">Support</h4><ul className={col}>
          <li>support@reloop.example</li><li>Mon–Sat, 9am–6pm IST</li>
          <li><Link className={a} to="/orders">Track an order</Link></li>
        </ul></div>
      </div>
      <div className="border-t border-white/10 py-4 text-center text-xs text-neutral-500">© {new Date().getFullYear()} ReLoop. Demonstration project.</div>
    </footer>
  );
}
