import { useEffect } from 'react';
import { CONDITION_STYLE, label } from '../utils/format.js';

export function Spinner({ size = 24 }) {
  return (
    <svg className="animate-spin text-brand-600" width={size} height={size} viewBox="0 0 24 24" fill="none" role="status" aria-label="Loading">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" opacity=".2" />
      <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export const PageLoader = ({ text = 'Loading…' }) => (
  <div className="flex flex-col items-center justify-center gap-3 py-20 text-sm text-neutral-500"><Spinner size={32} />{text}</div>
);

export function ErrorState({ error, onRetry, title = 'Something went wrong' }) {
  return (
    <div className="mx-auto max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-center" role="alert">
      <h3 className="text-base text-red-800">{title}</h3>
      <p className="mt-1 text-sm text-red-700">{error?.message || 'Please try again.'}</p>
      {onRetry && <button className="btn-outline mt-4" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function EmptyState({ title, text, action }) {
  return (
    <div className="mx-auto max-w-md rounded-xl border border-dashed border-neutral-300 bg-white p-10 text-center">
      <h3 className="text-lg">{title}</h3>
      {text && <p className="mt-1 text-sm text-neutral-500">{text}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export const Badge = ({ children, className = 'bg-neutral-100 text-neutral-700' }) => (
  <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${className}`}>{children}</span>
);

export const ConditionBadge = ({ value }) => <Badge className={CONDITION_STYLE[value] || ''}>{label(value)}</Badge>;

const STATUS_STYLE = {
  ACTIVE: 'bg-brand-100 text-brand-700', PUBLISHED: 'bg-brand-100 text-brand-700', COMPLETED: 'bg-brand-100 text-brand-700',
  DELIVERED: 'bg-brand-100 text-brand-700', PAID: 'bg-brand-100 text-brand-700', APPROVED: 'bg-brand-100 text-brand-700',
  CONFIRMED: 'bg-brand-100 text-brand-700', PARTNER_CONFIRMED: 'bg-brand-100 text-brand-700', ADMIN_CONFIRMED: 'bg-brand-100 text-brand-700',
  PLACED: 'bg-blue-50 text-blue-700', SHIPPED: 'bg-blue-50 text-blue-700', IN_PROGRESS: 'bg-blue-50 text-blue-700', REVIEWED: 'bg-blue-50 text-blue-700',
  QUOTED: 'bg-amber-50 text-amber-700', PENDING: 'bg-amber-50 text-amber-700', PENDING_PAYMENT: 'bg-amber-50 text-amber-700',
  REQUESTED: 'bg-amber-50 text-amber-700', DRAFT: 'bg-neutral-100 text-neutral-700', PAUSED: 'bg-neutral-100 text-neutral-700',
  RESERVED: 'bg-amber-50 text-amber-700', SELF_REPORTED: 'bg-neutral-100 text-neutral-600', LISTED: 'bg-blue-50 text-blue-700',
  EXPIRED: 'bg-red-50 text-red-700', NOT_RECORDED: 'bg-neutral-100 text-neutral-600',
  CANCELLED: 'bg-red-50 text-red-700', REJECTED: 'bg-red-50 text-red-700', FAILED: 'bg-red-50 text-red-700', RECYCLED: 'bg-neutral-200 text-neutral-700', SOLD: 'bg-neutral-200 text-neutral-700',
};
export const StatusBadge = ({ value }) => <Badge className={STATUS_STYLE[value] || ''}>{label(value)}</Badge>;

export function Stars({ value, count }) {
  if (value === null || value === undefined) return null;
  return (
    <span className="inline-flex items-center gap-1 text-xs text-neutral-600" aria-label={`Rated ${value} out of 5`}>
      <span className="text-amber-500">★</span><strong>{value}</strong>{count !== undefined && <span>({count})</span>}
    </span>
  );
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <nav className="mt-6 flex items-center justify-center gap-3" aria-label="Pagination">
      <button className="btn-outline" disabled={page <= 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span className="text-sm text-neutral-600">Page {page + 1} of {totalPages}</span>
      <button className="btn-outline" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </nav>
  );
}

export function Modal({ title, onClose, children, wide }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div role="dialog" aria-modal="true" aria-label={title} className={`max-h-[90vh] w-full overflow-y-auto rounded-xl bg-white p-6 shadow-xl ${wide ? 'max-w-2xl' : 'max-w-md'}`}>
        <div className="mb-4 flex items-start justify-between gap-4">
          <h2 className="text-lg">{title}</h2>
          <button onClick={onClose} className="text-neutral-400 hover:text-ink-900" aria-label="Close">✕</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function ConfirmDialog({ title, message, confirmText = 'Confirm', danger, onConfirm, onCancel, busy }) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p className="text-sm text-neutral-600">{message}</p>
      <div className="mt-6 flex justify-end gap-2">
        <button className="btn-outline" onClick={onCancel} disabled={busy}>Cancel</button>
        <button className={danger ? 'btn-danger' : 'btn-primary'} onClick={onConfirm} disabled={busy}>{busy ? 'Please wait…' : confirmText}</button>
      </div>
    </Modal>
  );
}

export function Field({ label: text, error, children, hint }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-ink-800">{text}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-neutral-500">{hint}</span>}
      {error && <span className="mt-1 block text-xs text-red-600" role="alert">{error}</span>}
    </label>
  );
}

export const SectionTitle = ({ children, action }) => (
  <div className="mb-4 flex items-end justify-between gap-4">
    <h2 className="text-xl sm:text-2xl">{children}</h2>
    {action}
  </div>
);
