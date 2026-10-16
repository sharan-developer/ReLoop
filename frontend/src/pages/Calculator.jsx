import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Field } from '../components/ui.jsx';
import { inr } from '../utils/format.js';

const num = (v) => (v === '' || isNaN(Number(v)) ? null : Number(v));

/** Pure arithmetic on the user's own inputs. It is an estimate, not a prediction or advice. */
export function compute({ repair, replace, months }) {
  const r = num(repair), p = num(replace), m = num(months);
  if (r === null || p === null || r < 0 || p <= 0) return null;
  const pct = (r / p) * 100;
  const perMonthRepair = m && m > 0 ? r / m : null;
  return { pct, perMonthRepair, saving: p - r };
}

export default function Calculator() {
  const [sp] = useSearchParams();
  const [f, setF] = useState({ repair: '', replace: '', months: '', newMonths: '' });
  const res = compute(f);
  const newM = num(f.newMonths);
  const perMonthNew = res && newM > 0 ? num(f.replace) / newM : null;
  return (
    <div className="container-x max-w-3xl py-8">
      <h1 className="text-2xl">Repair vs replace{sp.get('product') ? `: ${sp.get('product')}` : ''}</h1>
      <p className="mb-5 mt-1 text-sm text-neutral-600">Enter your own figures to compare costs. This is a simple estimate based only on what you type. It is not an AI prediction or a guaranteed recommendation.</p>
      <div className="card grid gap-4 p-5 sm:grid-cols-2">
        <Field label="Estimated repair cost (₹)"><input className="input" type="number" min="0" value={f.repair} onChange={(e) => setF({ ...f, repair: e.target.value })} /></Field>
        <Field label="Replacement product cost (₹)"><input className="input" type="number" min="1" value={f.replace} onChange={(e) => setF({ ...f, replace: e.target.value })} /></Field>
        <Field label="Extra useful life after repair (months)" hint="Your estimate"><input className="input" type="number" min="1" value={f.months} onChange={(e) => setF({ ...f, months: e.target.value })} /></Field>
        <Field label="Expected life of a replacement (months)" hint="Optional, for a like-for-like comparison"><input className="input" type="number" min="1" value={f.newMonths} onChange={(e) => setF({ ...f, newMonths: e.target.value })} /></Field>
      </div>
      {res ? (
        <div className="card mt-5 p-5" aria-live="polite">
          <h2 className="text-lg">Comparison (estimate)</h2>
          <div className="mt-3 overflow-x-auto"><table className="w-full text-left text-sm">
            <thead><tr className="border-b text-neutral-500"><th className="py-2"></th><th>Repair</th><th>Replace</th></tr></thead>
            <tbody>
              <tr className="border-b"><td className="py-2">Upfront cost</td><td>{inr(num(f.repair))}</td><td>{inr(num(f.replace))}</td></tr>
              <tr className="border-b"><td className="py-2">Cost per month of use</td><td>{res.perMonthRepair ? inr(Math.round(res.perMonthRepair)) : 'Enter extra months'}</td><td>{perMonthNew ? inr(Math.round(perMonthNew)) : 'Enter expected life'}</td></tr>
            </tbody>
          </table></div>
          <p className="mt-4 text-sm">Repair costs <strong>{res.pct.toFixed(0)}%</strong> of the replacement price{res.saving > 0 ? `, ${inr(res.saving)} less upfront.` : ', which is more than replacing.'}</p>
          <p className="mt-2 text-xs text-neutral-500">Not included: resale value of the old device, warranty on a new product, data transfer effort, environmental impact.</p>
        </div>
      ) : <p className="mt-5 text-sm text-neutral-500">Enter the repair and replacement costs to see the comparison.</p>}
    </div>
  );
}
