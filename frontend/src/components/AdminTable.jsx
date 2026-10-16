import { useState } from 'react';
import { EmptyState, ErrorState, PageLoader, Pagination } from './ui.jsx';
import useAsync from '../hooks/useAsync.js';

/**
 * Paginated, searchable admin table.
 *  load(q, page) -> PageDto ; columns: [{ header, render(row) }] ; actions(row, reload) -> JSX
 */
export default function AdminTable({ title, load, columns, actions, searchable = true, headerExtra }) {
  const [q, setQ] = useState('');
  const [term, setTerm] = useState('');
  const [page, setPage] = useState(0);
  const { data, loading, error, reload } = useAsync(() => load(term, page), [term, page]);
  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl">{title}</h1>
        <div className="flex gap-2">
          {searchable && (
            <form onSubmit={(e) => { e.preventDefault(); setPage(0); setTerm(q.trim()); }} className="flex gap-2" role="search">
              <input className="input w-48" placeholder="Search…" value={q} onChange={(e) => setQ(e.target.value)} aria-label={`Search ${title}`} />
              <button className="btn-outline">Search</button>
            </form>
          )}
          {headerExtra && headerExtra(reload)}
        </div>
      </div>
      {loading && !data && <PageLoader />}
      {error && <ErrorState error={error} onRetry={reload} />}
      {data && data.items.length === 0 && <EmptyState title="Nothing to show" />}
      {data && data.items.length > 0 && (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="border-b bg-neutral-50 text-xs uppercase text-neutral-500"><tr>{columns.map((c) => <th key={c.header} className="px-3 py-2">{c.header}</th>)}{actions && <th className="px-3 py-2">Actions</th>}</tr></thead>
            <tbody className={loading ? 'opacity-60' : ''}>
              {data.items.map((row, i) => (
                <tr key={row.id ?? i} className="border-b last:border-0">
                  {columns.map((c) => <td key={c.header} className="px-3 py-2 align-top">{c.render(row)}</td>)}
                  {actions && <td className="px-3 py-2 align-top"><div className="flex flex-wrap gap-2">{actions(row, reload)}</div></td>}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
