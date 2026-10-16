import { Link } from 'react-router-dom';
import ProductRow from '../components/ProductRow.jsx';
import { EmptyState, SectionTitle } from '../components/ui.jsx';
import useAsync from '../hooks/useAsync.js';
import { api, qs } from '../services/api.js';
import { getCategories } from '../services/catalog.js';
import { getRecent } from '../utils/format.js';

const list = (params) => () => api.get('/api/products' + qs(params)).then((r) => r.items);

function Hero() {
  return (
    <section className="bg-ink-900 text-white">
      <div className="container-x grid items-center gap-6 py-10 md:grid-cols-2 md:py-14">
        <div>
          <p className="mb-3 inline-block rounded-full bg-brand-600/20 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-brand-200">Product afterlife network</p>
          <h1 className="text-3xl font-extrabold leading-tight sm:text-5xl">GIVE YOUR TECH A <span className="text-brand-500">SECOND LIFE</span></h1>
          <p className="mt-4 text-base text-neutral-300 sm:text-lg">Refurbished Deals • Trusted Repairs • Smarter Resale</p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Link to="/products?condition=REFURBISHED&sort=price_asc" className="btn-primary px-6 py-3 text-base">Explore Deals</Link>
            <Link to="/sell" className="btn border border-white/30 px-6 py-3 text-base text-white hover:bg-white/10">Sell Your Device</Link>
          </div>
        </div>
        <img src="/images/hero.svg" alt="" className="mx-auto w-full max-w-md" />
      </div>
    </section>
  );
}

function Categories() {
  const { data } = useAsync(getCategories, []);
  if (!data) return null;
  return (
    <section className="container-x mt-12">
      <SectionTitle>Shop by category</SectionTitle>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7">
        {data.map((c) => (
          <Link key={c.id} to={`/products?category=${c.slug}`} className="card flex items-center justify-center p-4 text-center text-sm font-semibold transition hover:border-brand-600 hover:text-brand-700">{c.name}</Link>
        ))}
      </div>
    </section>
  );
}

function Partners() {
  const { data } = useAsync(() => api.get('/api/partners'), []);
  return (
    <section className="container-x mt-12">
      <SectionTitle action={<Link to="/repair-services" className="text-sm font-semibold text-brand-700 hover:underline">All repair services →</Link>}>Trusted repair services</SectionTitle>
      {data && data.length === 0 && <EmptyState title="No repair partners yet" text="Approved partners will appear here." />}
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {(data || []).slice(0, 3).map((p) => (
          <div key={p.id} className="card flex items-center gap-4 p-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-100 text-lg font-bold text-brand-700">{p.businessName[0]}</div>
            <div><p className="font-semibold">{p.businessName}</p><p className="text-sm text-neutral-500">{p.city}</p></div>
          </div>
        ))}
      </div>
    </section>
  );
}

function RecentlyViewed() {
  const ids = getRecent();
  if (!ids.length) return null;
  return <ProductRow title="Recently viewed" loader={() => api.get('/api/products/by-ids' + qs({ ids: ids.join(',') }))} hideIfEmpty />;
}

const steps = [
  ['1', 'Register', 'Add your products and warranty to build a lifecycle passport.'],
  ['2', 'Repair', 'Request a quote from an approved repair partner and track progress.'],
  ['3', 'Resell', 'List eligible products. History travels with the item, privacy stays with you.'],
  ['4', 'Recycle', 'Record end-of-life recycling so nothing is forgotten.'],
];

export default function Home() {
  return (
    <>
      <Hero />
      <ProductRow title="Top picks for you" tone="green" loader={list({ sort: 'popular', size: 4 })} link="/products?sort=popular" />
      <ProductRow title="Popular refurbished electronics" loader={list({ condition: 'REFURBISHED', sort: 'popular', size: 4 })} link="/products?condition=REFURBISHED" />
      <ProductRow title="Best deals" tone="green" loader={() => api.get('/api/products' + qs({ size: 48 })).then((r) => [...r.items].filter((p) => p.available).sort((a, b) => (b.discountPercent || 0) - (a.discountPercent || 0)).slice(0, 4))} link="/products?sort=price_asc" />
      <ProductRow title="Recently added" loader={list({ size: 4 })} link="/products" />
      <Categories />
      <Partners />
      <section className="container-x mt-12">
        <div className="flex flex-col items-start justify-between gap-4 rounded-2xl bg-ink-900 p-8 text-white sm:flex-row sm:items-center">
          <div><h2 className="text-2xl">Sell or trade in your device</h2><p className="mt-1 text-neutral-300">List a product you own on the marketplace. Ownership transfers safely when the sale is confirmed.</p></div>
          <Link to="/sell" className="btn-primary px-6 py-3">Start selling</Link>
        </div>
      </section>
      <RecentlyViewed />
      <section className="container-x mt-12">
        <SectionTitle>How ReLoop works</SectionTitle>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {steps.map(([n, t, d]) => (
            <div key={n} className="card p-5"><span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{n}</span><h3 className="mt-3 text-base">{t}</h3><p className="mt-1 text-sm text-neutral-600">{d}</p></div>
          ))}
        </div>
      </section>
    </>
  );
}
