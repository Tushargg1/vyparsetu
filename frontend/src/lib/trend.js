// Shared time-bucketing + active-period helpers for all trend graphs.
export const DAY = 86_400_000;

export const isoDate = (value) => {
  const d = new Date(value);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
export const shortDay = (d) => new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
export const prettyDate = (v) => new Date(v).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
export const parseLocal = (s) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); };
export const mondayOf = (d) => { const x = new Date(d); const diff = (x.getDay() + 6) % 7; x.setHours(0, 0, 0, 0); x.setDate(x.getDate() - diff); return x; };

// Toggle order — "All time" sits just before "Custom".
export const TREND_TOGGLE = [
  ['daily', 'Day'],
  ['weekly', 'Week'],
  ['monthly', 'Month'],
  ['yearly', 'Year'],
  ['alltime', 'All time'],
  ['custom', 'Custom'],
];

export const GRANULAR = new Set(['daily', 'weekly', 'monthly', 'yearly']);

export const rangeWord = (range) =>
  range === 'monthly' ? 'month' : range === 'weekly' ? 'week' : range === 'yearly' ? 'year' : 'day';

/**
 * Build empty buckets ({key,label,value,from,to}) plus a keyOf(date) mapper.
 * Returns { buckets, keyOf, customReady }.
 */
export function buildBuckets(range, start, end) {
  const now = Date.now();
  if (range === 'custom') {
    if (!start || !end || parseLocal(end) < parseLocal(start)) return { buckets: [], keyOf: null, customReady: false };
    const buckets = [];
    for (let t = parseLocal(start); t <= parseLocal(end); t = new Date(t.getTime() + DAY)) {
      buckets.push({ key: isoDate(t), label: shortDay(t), value: 0, from: t.getTime(), to: t.getTime() + DAY });
    }
    return { buckets, keyOf: (d) => isoDate(d), customReady: true };
  }
  if (range === 'daily') {
    const buckets = [];
    for (let i = 13; i >= 0; i--) {
      const d = new Date(now - i * DAY);
      const f = parseLocal(isoDate(d)).getTime();
      buckets.push({ key: isoDate(d), label: shortDay(d), value: 0, from: f, to: f + DAY });
    }
    return { buckets, keyOf: (d) => isoDate(d), customReady: true };
  }
  if (range === 'weekly') {
    const startMon = mondayOf(new Date(now - 7 * 7 * DAY));
    const buckets = [];
    for (let i = 0; i < 8; i++) {
      const wk = new Date(startMon.getTime() + i * 7 * DAY);
      buckets.push({ key: isoDate(wk), label: shortDay(wk), value: 0, from: wk.getTime(), to: wk.getTime() + 7 * DAY });
    }
    return { buckets, keyOf: (d) => isoDate(mondayOf(d)), customReady: true };
  }
  if (range === 'monthly') {
    const ref = new Date();
    const buckets = [];
    for (let i = 11; i >= 0; i--) {
      const d = new Date(ref.getFullYear(), ref.getMonth() - i, 1);
      const next = new Date(ref.getFullYear(), ref.getMonth() - i + 1, 1);
      buckets.push({ key: `${d.getFullYear()}-${d.getMonth()}`, label: d.toLocaleDateString('en-IN', { month: 'short' }), value: 0, from: d.getTime(), to: next.getTime() });
    }
    return { buckets, keyOf: (d) => `${d.getFullYear()}-${d.getMonth()}`, customReady: true };
  }
  // yearly or alltime → year buckets (alltime spans a bit more)
  const y = new Date().getFullYear();
  const span = range === 'alltime' ? 6 : 5;
  const buckets = [];
  for (let i = span - 1; i >= 0; i--) {
    const yr = y - i;
    buckets.push({ key: String(yr), label: String(yr), value: 0, from: new Date(yr, 0, 1).getTime(), to: new Date(yr + 1, 0, 1).getTime() });
  }
  return { buckets, keyOf: (d) => String(new Date(d).getFullYear()), customReady: true };
}

const periodLabel = (range, b) => {
  if (range === 'monthly') return new Date(b.from).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  if (range === 'yearly') return b.label;
  if (range === 'weekly') return `${prettyDate(b.from)} – ${prettyDate(b.to - DAY)}`;
  return prettyDate(b.from); // single day
};

/**
 * The effective selected bar index: an explicit selection, or the current
 * (last) bucket for granular ranges, or none.
 */
export function effectiveIndex(range, buckets, selIdx) {
  if (selIdx != null) return selIdx;
  if (GRANULAR.has(range) && buckets.length) return buckets.length - 1;
  return null;
}

/**
 * Active period { from, to, label } given the range, built buckets, and the
 * effective selected index. Numbers/lists should reflect this window.
 */
export function activePeriod(range, buckets, effIdx, start, end) {
  if (effIdx != null && buckets[effIdx]) {
    const b = buckets[effIdx];
    return { from: b.from, to: b.to, label: periodLabel(range, b) };
  }
  if (range === 'custom') {
    if (!start || !end) return { from: 0, to: Date.now(), label: 'Select dates' };
    return { from: parseLocal(start).getTime(), to: parseLocal(end).getTime() + DAY, label: `${prettyDate(start)} – ${prettyDate(end)}` };
  }
  return { from: 0, to: Date.now(), label: 'All time' };
}
