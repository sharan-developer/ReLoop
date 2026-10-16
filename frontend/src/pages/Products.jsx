import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ProductCard from '../components/ProductCard.jsx';
import { EmptyState, ErrorState, Pagination, PageLoader } from '../components/ui.jsx';
import useAsync from '../hooks/useAsync.js';
import { api, qs } from '../services/api.js';
import { getCategories } from '../services/catalog.js';

const SORTS = [['', 'Newest'], ['popular', 'Most popular'], ['price_asc', 'Price: low to high'], ['price_desc', 'Price: high to low']];

export default function Products() {
  const [sp, setSp] = useSearchParams();
  const get = (k) => sp.get(k) || '';
  const [min, setMin] = useState(get('minPrice'));
  const [max, setMax] = useState(get('maxPrice'));
  useEffect(() => { setMin(get('minPrice')); setMax(get('maxPrice')); }, [sp]); // eslint-disable-line react-hooks/exhaustive-deps

  const cats = useAsync(getCategories, []);
  const page = Number(get('page') || 0);
  const key = sp.toString();
  const { data, loading, error, reload } = useAsync(
    () => api.get('/api/products' + qs({ q: get('q'), category: get('category'), condition: get('condition'), minPrice: get('minPrice'), maxPrice: get('maxPrice'), sort: get('sort'), page, size: 12 })),
    [key]
  );

  const update = (changes) => {
    const n = new URLSearchParams(sp);
    Object.entries(changes).forEach(([k, v]) => (v ? n.set(k, v) : n.delete(k)));
    if (!('page' in changes)) n.delete('page');
    setSp(n);
  };
  const applyPrice = (e) => { e.preventDefault(); update({ minPrice: min, maxPrice: max }); };
  const clear = () => setSp({});
  const hasFilters = ['q', 'category', 'condition', 'minPrice', 'maxPrice'].some(get);

  return (
    <div className="container-x py-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl">{get('q') ? `Results for “${get('q')}”` : 'All products'}</h1>
        <label className="flex items-center gap-2 text-sm">Sort by
          <select className="input w-auto" value={get('sort')} onChange={(e) => update({ sort: e.target.value })}>
            {SORTS.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
          </select>
        </label>
      </div>
      <div className="grid gap-6 lg:grid-cols-[230px_1fr]">
        <aside className="card h-fit space-y-5 p-4 text-sm">
          <div>
            <h2 className="mb-2 text-sm">Category</h2>
            <ul className="space-y-1">
              <li><button className={`hover:text-brand-700 ${!get('category') ? 'font-bold text-brand-700' : ''}`} onClick={() => update({ category: '' })}>All</button></li>
              {(cats.data || []).map((c) => (
                <li key={c.id}><button className={`hover:text-brand-700 ${get('category') === c.slug ? 'font-bold text-brand-700' : ''}`} onClick={() => update({ category: c.slug })}>{c.name}</button></li>
              ))}
            </ul>
          </div>
          <div>
            <h2 className="mb-2 text-sm">Condition</h2>
            {['', 'NEW', 'REFURBISHED', 'USED'].map((c) => (
              <label key={c} className="mb-1 flex items-center gap-2">
                <input type="radio" name="cond" checked={get('condition') === c} onChange={() => update({ condition: c })} />{c ? c.charAt(0) + c.slice(1).toLowerCase() : 'Any'}
              </label>
            ))}
          </div>
          <form onSubmit={applyPrice}>
            <h2 className="mb-2 text-sm">Price (₹)</h2>
            <div className="flex gap-2">
              <input className="input" type="number" min="0" placeholder="Min" value={min} onChange={(e) => setMin(e.target.value)} aria-label="Minimum price" />
              <input className="input" type="number" min="0" placeholder="Max" value={max} onChange={(e) => setMax(e.target.value)} aria-label="Maximum price" />
            </div>
            <button className="btn-outline mt-2 w-full">Apply</button>
          </form>
          {hasFilters && <button className="text-sm font-semibold text-brand-700 hover:underline" onClick={clear}>Clear all filters</button>}
        </aside>
        <div>
          {loading && !data && <PageLoader />}
          {error && <ErrorState error={error} onRetry={reload} />}
          {data && data.items.length === 0 && <EmptyState title="No products found" text="Try different keywords or remove some filters." action={<button className="btn-primary" onClick={clear}>Clear filters</button>} />}
          {data && data.items.length > 0 && (
            <>
              <p className="mb-3 text-sm text-neutral-500">{data.totalElements} product{data.totalElements === 1 ? '' : 's'}</p>
              <div className={`grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 ${loading ? 'opacity-60' : ''}`}>
                {data.items.map((p) => <ProductCard key={p.id} product={p} />)}
              </div>
              <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => update({ page: p ? String(p) : '' })} />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
