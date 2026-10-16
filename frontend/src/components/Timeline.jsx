import { StatusBadge } from './ui.jsx';
import { dateTimeFmt, label } from '../utils/format.js';

const ICON = {
  PRODUCT_REGISTERED: '📝', PURCHASE_RECORDED: '🧾', WARRANTY_UPDATED: '🛡️', REPAIR_REQUESTED: '🔧', REPAIR_APPROVED: '👍',
  REPAIR_STARTED: '🛠️', REPAIR_COMPLETED: '✅', RESALE_LISTED: '🏷️', OWNERSHIP_TRANSFERRED: '🔁', RECYCLING_RECORDED: '♻️',
};

/** Chronological lifecycle timeline. Verification level is always visible next to each event. */
export default function Timeline({ events }) {
  return (
    <ol className="relative ml-3 border-l-2 border-brand-200">
      {events.map((e) => (
        <li key={e.id} className="mb-6 ml-6">
          <span className="absolute -left-[17px] flex h-8 w-8 items-center justify-center rounded-full bg-white text-base ring-2 ring-brand-200" aria-hidden="true">{ICON[e.type] || '•'}</span>
          <div className="flex flex-wrap items-center gap-2"><h3 className="text-sm">{label(e.type)}</h3><StatusBadge value={e.verification} /></div>
          <p className="text-xs text-neutral-500">{dateTimeFmt(e.createdAt)} · {e.actorLabel}</p>
          {e.details && <p className="mt-1 text-sm text-neutral-700">{e.details}</p>}
        </li>
      ))}
    </ol>
  );
}
