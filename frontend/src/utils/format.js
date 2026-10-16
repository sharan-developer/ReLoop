export const inr = (n) =>
  n === null || n === undefined ? '' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n);

export const dateFmt = (d) => (d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '');
export const dateTimeFmt = (d) => (d ? new Date(d).toLocaleString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '');
export const label = (s) => (s ? s.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase()) : '');

export const CONDITION_STYLE = {
  NEW: 'bg-blue-50 text-blue-700',
  REFURBISHED: 'bg-brand-100 text-brand-700',
  USED: 'bg-amber-50 text-amber-700',
};

// Recently viewed product ids are non-sensitive and stored in the browser only.
const RV_KEY = 'reloop_recent';
export function getRecent() {
  try { return JSON.parse(localStorage.getItem(RV_KEY) || '[]'); } catch { return []; }
}
export function pushRecent(id) {
  try {
    const list = [id, ...getRecent().filter((x) => x !== id)].slice(0, 8);
    localStorage.setItem(RV_KEY, JSON.stringify(list));
  } catch { /* storage unavailable */ }
}
