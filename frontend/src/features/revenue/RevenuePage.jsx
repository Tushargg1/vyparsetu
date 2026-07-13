import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { reportApi, salesApi, orderApi } from '../../lib/api';
import BarChart from '../../components/BarChart';
import Icon from '../../components/Icon';
import { isoDate, prettyDate, parseLocal, TREND_TOGGLE, buildBuckets, effectiveIndex, activePeriod, rangeWord } from '../../lib/trend';

const money = (v) => (v == null || v === '' ? '—' : `₹${Number(v).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`);
const prettyTime = (v) => (v ? new Date(v).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' }) : '');
const billProfit = (s) => (s.items || []).reduce((p, it) => p + (Number(it.lineTotal || 0) - Number(it.quantity || 0) * Number(it.costPrice || 0)), 0);

function Stat({ title, value, icon, accent, sub }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
      <div className="flex items-center justify-between mb-sm">
        <span className="text-label-md text-on-surface-variant">{title}</span>
        <Icon name={icon} className={accent} />
      </div>
      <div className="text-headline-lg font-bold text-on-surface">{value}</div>
      {sub && <p className="text-label-sm text-on-surface-variant mt-xs">{sub}</p>}
    </div>
  );
}

function BillCard({ sale, open, onToggle }) {
  const items = sale.items || [];
  const total = Number(sale.totalAmount || 0);
  return (
    <div className="border border-surface-variant rounded-lg overflow-hidden">
      <button onClick={onToggle} className="w-full flex items-center justify-between gap-sm px-md py-sm text-left hover:bg-surface-container">
        <div className="flex items-center gap-sm min-w-0">
          <Icon name={open ? 'expand_less' : 'expand_more'} className="text-on-surface-variant" />
          <div className="min-w-0">
            <div className="font-semibold text-on-surface truncate">Bill #{sale.id}</div>
            <div className="text-label-sm text-on-surface-variant">{items.length} items · {String(sale.totalItems ?? items.length)} units · {prettyTime(sale.createdAt)}</div>
          </div>
        </div>
        <span className="font-bold text-on-surface shrink-0">{money(total)}</span>
      </button>
      {open && (
        <div className="px-md py-sm border-t border-surface-variant bg-surface-container-lowest">
          <table className="w-full text-label-md">
            <thead>
              <tr className="text-on-surface-variant text-label-sm">
                <th className="text-left font-medium py-1">Product</th>
                <th className="text-right font-medium py-1">Qty</th>
                <th className="text-right font-medium py-1">Price</th>
                <th className="text-right font-medium py-1">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-variant">
              {items.map((it, i) => (
                <tr key={i}>
                  <td className="py-1.5 text-on-surface">{it.productName}</td>
                  <td className="py-1.5 text-right text-on-surface-variant">{String(it.quantity)}</td>
                  <td className="py-1.5 text-right text-on-surface-variant">{money(it.unitPrice)}</td>
                  <td className="py-1.5 text-right text-on-surface font-medium">{money(it.lineTotal)}</td>
                </tr>
              ))}
              {items.length === 0 && <tr><td colSpan={4} className="py-2 text-on-surface-variant">No line items.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default function RevenuePage() {
  const { data, isLoading } = useQuery({ queryKey: ['revenue'], queryFn: reportApi.revenue });
  const { data: sales = [] } = useQuery({ queryKey: ['sales-history-all'], queryFn: () => salesApi.history({ page: 0, size: 200 }) });
  const { data: ordersPage } = useQuery({ queryKey: ['my-orders-all'], queryFn: () => orderApi.mine({ page: 0, size: 200 }) });

  const orders = ordersPage?.content || [];
  const [range, setRange] = useState('monthly');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [selIdx, setSelIdx] = useState(null);
  const [openId, setOpenId] = useState(null);

  const changeRange = (k) => { setRange(k); setSelIdx(null); };

  const { buckets, keyOf, customReady } = useMemo(() => buildBuckets(range, start, end), [range, start, end]);
  const series = useMemo(() => {
    if (!keyOf) return buckets;
    const idx = new Map(buckets.map((b) => [b.key, b]));
    sales.forEach((s) => { if (s.createdAt) { const b = idx.get(keyOf(new Date(s.createdAt))); if (b) b.value += Number(s.totalAmount || 0); } });
    return buckets;
  }, [buckets, keyOf, sales]);

  const effIdx = effectiveIndex(range, series, selIdx);
  const period = activePeriod(range, series, effIdx, start, end);

  // Period-aware numbers
  const stats = useMemo(() => {
    const inP = (t) => t >= period.from && t < period.to;
    let salesTotal = 0, profit = 0, purchases = 0;
    sales.forEach((s) => { if (s.createdAt && inP(new Date(s.createdAt).getTime())) { salesTotal += Number(s.totalAmount || 0); profit += billProfit(s); } });
    orders.forEach((o) => { if (o.placedAt && o.status !== 'REJECTED' && o.status !== 'CANCELLED' && inP(new Date(o.placedAt).getTime())) purchases += Number(o.totalAmount || 0); });
    return { salesTotal, profit, purchases };
  }, [sales, orders, period.from, period.to]);

  const groups = useMemo(() => {
    const map = new Map();
    sales.forEach((s) => {
      if (!s.createdAt) return;
      const t = new Date(s.createdAt).getTime();
      if (t < period.from || t >= period.to) return;
      const key = isoDate(s.createdAt);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(s);
    });
    return [...map.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1)).map(([key, list]) => {
      let revenue = 0; list.forEach((s) => { revenue += Number(s.totalAmount || 0); });
      list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      return { key, list, revenue };
    });
  }, [sales, period.from, period.to]);

  if (isLoading) return <p className="text-on-surface-variant">Loading…</p>;

  return (
    <div className="space-y-lg">
      <div className="flex items-end justify-between gap-md flex-wrap">
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">Revenue &amp; profit</h2>
          <p className="text-body-md text-on-surface-variant mt-xs">Earnings from your counter sales.</p>
        </div>
        <span className="inline-flex items-center gap-1 text-label-md font-semibold text-primary bg-primary-fixed px-3 py-1.5 rounded-full">
          <Icon name="calendar_month" className="text-[18px]" /> {period.label}
        </span>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-gutter">
        <Stat title="Total sales" value={money(stats.salesTotal)} icon="payments" accent="text-primary" sub={period.label} />
        <Stat title="Profit" value={money(stats.profit)} icon="savings" accent="text-tertiary-container" sub="Sales minus cost of goods" />
        <Stat title="Investment / Bought" value={money(stats.purchases)} icon="shopping_bag" accent="text-secondary" sub="Paid to distributors" />
        <Stat title="Stock value" value={money(data?.stockValue)} icon="inventory" accent="text-on-surface-variant" sub="Current inventory at cost" />
      </div>

      <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg">
        <div className="flex items-center justify-between mb-md flex-wrap gap-sm">
          <h3 className="text-headline-md">Sales trend</h3>
          <div className="flex bg-surface-container rounded-lg overflow-hidden text-label-md flex-wrap">
            {TREND_TOGGLE.map(([k, l]) => (
              <button key={k} onClick={() => changeRange(k)} className={`px-3 py-1.5 ${range === k ? 'bg-primary text-on-primary font-semibold' : 'text-on-surface-variant'}`}>{l}</button>
            ))}
          </div>
        </div>

        {range === 'custom' && (
          <div className="flex items-center gap-sm flex-wrap mb-md">
            <Icon name="calendar_month" className="text-on-surface-variant text-[20px]" />
            <label className="text-label-sm text-on-surface-variant">From</label>
            <input type="date" value={start} max={end || isoDate(Date.now())} onChange={(e) => { setStart(e.target.value); setSelIdx(null); }}
              className="border border-outline-variant rounded-lg px-3 py-1.5 text-label-md outline-none focus:border-primary bg-surface-container-lowest" />
            <label className="text-label-sm text-on-surface-variant">To</label>
            <input type="date" value={end} min={start || undefined} max={isoDate(Date.now())} onChange={(e) => { setEnd(e.target.value); setSelIdx(null); }}
              className="border border-outline-variant rounded-lg px-3 py-1.5 text-label-md outline-none focus:border-primary bg-surface-container-lowest" />
            {(start || end) && <button onClick={() => { setStart(''); setEnd(''); setSelIdx(null); }} className="text-label-md text-primary font-semibold hover:underline">Clear</button>}
          </div>
        )}

        {customReady ? (
          <>
            <BarChart data={series} onSelect={setSelIdx} selectedIndex={effIdx} />
            {range !== 'alltime' && (
              <p className="text-label-sm text-on-surface-variant flex items-center gap-1 mt-sm">
                <Icon name="touch_app" className="text-[16px]" /> Tap a bar to view that {rangeWord(range)}.
              </p>
            )}
          </>
        ) : (
          <div className="h-[160px] flex items-center justify-center text-on-surface-variant text-label-md">Select a start and end date to view the range.</div>
        )}
      </div>

      {/* Breakdown for the active period */}
      <div className="space-y-md">
        <h3 className="text-headline-md text-on-background">Bills · {period.label}</h3>
        {groups.length === 0 && (
          <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-xl text-center text-on-surface-variant">No sales in this period.</div>
        )}
        {groups.map((g) => (
          <div key={g.key} className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm overflow-hidden">
            <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between flex-wrap gap-sm">
              <div>
                <div className="font-bold text-on-surface">{prettyDate(g.key)}</div>
                <div className="text-label-sm text-on-surface-variant">{g.list.length} bills</div>
              </div>
              <div className="text-right">
                <div className="text-label-sm text-on-surface-variant">Revenue</div>
                <div className="font-bold text-primary">{money(g.revenue)}</div>
              </div>
            </div>
            <div className="p-md space-y-sm">
              {g.list.map((s) => <BillCard key={s.id} sale={s} open={openId === s.id} onToggle={() => setOpenId(openId === s.id ? null : s.id)} />)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
