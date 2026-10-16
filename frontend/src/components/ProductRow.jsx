import { Link } from 'react-router-dom';
import useAsync from '../hooks/useAsync.js';
import ProductCard from './ProductCard.jsx';
import { ErrorState, SectionTitle, Spinner } from './ui.jsx';

/** A titled grid of products loaded from the backend. `loader` must return an array of products. */
export default function ProductRow({ title, loader, link, tone, deps = [], hideIfEmpty }) {
  const { data, loading, error, reload } = useAsync(loader, deps);
  if (!loading && !error && hideIfEmpty && (!data || data.length === 0)) return null;
  return (
    <section className="container-x mt-10">
      <SectionTitle action={link && <Link to={link} className="text-sm font-semibold text-brand-700 hover:underline">View all →</Link>}>{title}</SectionTitle>
      {loading && <div className="flex justify-center py-10"><Spinner /></div>}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && (
        <div className="grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 lg:grid-cols-4">
          {data.map((p) => <ProductCard key={p.id} product={p} tone={tone} />)}
        </div>
      )}
    </section>
  );
}
