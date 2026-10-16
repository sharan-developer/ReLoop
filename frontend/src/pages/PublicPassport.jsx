import { Link, useParams } from 'react-router-dom';
import Timeline from '../components/Timeline.jsx';
import { ErrorState, PageLoader, StatusBadge } from '../components/ui.jsx';
import useAsync from '../hooks/useAsync.js';
import { api } from '../services/api.js';

export default function PublicPassport() {
  const { token } = useParams();
  const { data, loading, error, reload } = useAsync(() => api.get(`/api/passports/public/${token}`), [token]);
  if (loading) return <PageLoader />;
  if (error) return <div className="container-x py-10"><ErrorState error={error} onRetry={reload} title={error.status === 404 ? 'Passport not found' : undefined} /></div>;
  return (
    <div className="container-x max-w-3xl py-8">
      <p className="text-sm font-semibold uppercase tracking-wide text-brand-700">ReLoop Product Passport</p>
      <h1 className="mt-1 text-2xl">{data.name}</h1>
      <p className="text-sm text-neutral-500">{[data.brand, data.model, data.category].filter(Boolean).join(' · ')}</p>
      <div className="mt-3 flex items-center gap-2 text-sm"><StatusBadge value={data.status} /> Warranty <StatusBadge value={data.warrantyStatus} /></div>
      <p className="mt-4 rounded-lg bg-neutral-100 p-3 text-xs text-neutral-600">{data.disclaimer}</p>
      <section className="card mt-5 p-5"><Timeline events={data.events} /></section>
      <p className="mt-6 text-sm"><Link to="/" className="text-brand-700 hover:underline">Visit ReLoop</Link></p>
    </div>
  );
}
