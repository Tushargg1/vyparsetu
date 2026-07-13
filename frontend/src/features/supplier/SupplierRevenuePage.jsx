import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { supplierOrderApi, distributorApi } from '../../lib/api';
import BarChart from '../../components/BarChart';
import Icon from '../../components/Icon';
import { money, statusBadge, retailerMap, REVENUE_STATUSES } from './supplierUtils';
import { isoDate, prettyDate, TREND_TOGGLE, buildBuckets, effectiveIndex, activePeriod, rangeWord } from '../../lib/trend';

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

function OrderCard({ order, retailerName, open, onToggle }) {
  const badge = statusBadge(order.status);
  const items = order.items || [];
  const total = Number(order.totalAmount || 0);
  const tax = Number(order.taxAmount || 0);
  const net = total - tax;
  return (
    <div className="border border-surface-variant rounded-lg overflow-hidden">
      <button onClick={onToggle} className="w-full flex items-center justify-between gap-sm px-md py-sm text-left hover:bg-surface-container">
        <div className="flex items-center gap-sm min-w-0">
          <Icon name={open ? 'expand_less' : 'expand_more'} className="text-on-surface-variant" />
          <div className="min-w-0">
            <div className="font-semibold text-on-surface truncate">{order.orderNumber} · {retailerName}</div>
            <div className="text-label-sm text-on-surface-variant">{items.length} items · {order.paymentMode}</div>
          </div>
        </div>
        <div className="flex items-center gap-md shrink-0">
          <span className="font-bold text-on-surface">{money(total)}</span>
          <span className={`px-sm py-xs rounded-full text-label-sm font-medium ${badge.tone}`}>{badge.label}</span>
        </div>
      </button>
      {open && (
        <div className="px-md py-sm border-t border-surface-variant bg-surface-container-lowest">
          <table className="w-full text-label-md">
            <thead>
              <tr className="text-on-surface-variant text-label-sm">
                <th className="text-left font-medium py-1">Product</th>
                <th className="text-right font-medium py-1">Qty</th>
                <th className="text-right font-medium py-1">Price</th>
                <th className="text-right font-medium py-1">GST</th>
                <th className="text-right font-medium py-1">Total</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-variant">
              {items.map((it, i) => (
                <tr key={i}>
                  <td className="py-1.5 text-on-surface">{it.productName}</td>
                  <td className="py-1.5 text-right text-on-surface-variant">{String(it.quantity)}</td>
                  <td className="py-1.5 text-right text-on-surface-variant">{money(it.unitPrice)}</td>
                  <td className="py-1.5 text-right text-on-surface-variant">{it.gstRate != null ? `${String(it.gstRate)}%` : '—'}</td>
                  <td className="py-1.5 text-right text-on-surface font-medium">{money(it.lineTotal)}</td>
                </tr>
              ))}
              {items.length === 0 && <tr><td colSpan={5} className="py-2 text-on-surface-variant">No line items.</td></tr>}
            </tbody>
          </table>
          <div className="mt-sm pt-sm border-t border-surface-variant grid grid-cols-3 gap-sm text-center">
            <div><div className="text-label-sm text-on-surface-variant">Subtotal</div><div className="font-semibold text-on-surface">{money(order.subtotal)}</div></div>
            <div><div className="text-label-sm text-on-surface-variant">GST</div><div className="font-semibold text-on-surface">{money(tax)}</div></div>
            <div><div className="text-label-sm text-on-surface-variant">Net (ex-GST)</div><div className="font-semibold text-tertiary-container">{money(net)}</div></div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function SupplierRevenuePage() {
  const { data: ordersPage, isLoading } = useQuery({ queryKey: ['sup-orders-all'], queryFn: () => supplierOrderApi.list({ page: 0, size: 200 }) });
  const { data: retailers = [] } = useQuery({ queryKey: ['sup-retailers'], queryFn: distributorApi.retailers });

  const [range, setRange] = useState('monthly');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [selIdx, setSelIdx] = useState(null);
  const [openId, setOpenId] = useState(null);

  const orders = ordersPage?.content || [];
  const rmap = useMemo(() => retailerMap(retailers), [retailers]);
  const retailerName = (o) => rmap.get(o.retailerId)?.shopName || `Retailer #${o.retailerId}`;

  const changeRange = (k) => { setRange(k); setSelIdx(null); };

  const { buckets, keyOf, customReady } = useMemo(() => buildBuckets(range, start, end), [range, start, end]);
  const series = useMemo(() => {
    if (!keyOf) return buckets;
    const idx = new Map(buckets.map((b) => [b.key, b]));
    orders.forEach((o) => { if (o.placedAt && REVENUE_STATUSES.has(o.status)) { const b = idx.get(keyOf(new Date(o.placedAt))); if (b) b.value += Number(o.totalAmount || 0); } });
    return buckets;
  }, [buckets, keyOf, orders]);

  const effIdx = effectiveIndex(range, series, selIdx);
  const period = activePeriod(range, series, effIdx, start, end);

  const stats = useMemo(() => {
    const inP = (t) => t >= period.from && t < period.to;
    let gross = 0, tax = 0, fulfilled = 0;
    orders.forEach((o) => {
      if (!o.placedAt || !REVENUE_STATUSES.has(o.status)) return;
      if (!inP(new Date(o.placedAt).getTime())) return;
      fulfilled += 1; gross += Number(o.totalAmount || 0); tax += Number(o.taxAmount || 0);
    });
    return { gross, tax, net: gross - tax, fulfilled, aov: fulfilled ? gross / fulfilled : 0 };
  }, [orders, period.from, period.to]);

  const groups = useMemo(() => {
    const map = new Map();
    orders.forEach((o) => {
      if (!o.placedAt) return;
      const t = new Date(o.placedAt).getTime();
      if (t < period.from || t >= period.to) return;
      const key = isoDate(o.placedAt);
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(o);
    });
    return [...map.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1)).map(([key, list]) => {
      let revenue = 0, profit = 0;
      list.forEach((o) => { if (!REVENUE_STATUSES.has(o.status)) return; revenue += Number(o.totalAmount || 0); profit += Number(o.totalAmount || 0) - Number(o.taxAmount || 0); });
      list.sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt));
      return { key, list, revenue, profit };
    });
  }, [orders, period.from, period.to]);

  if (isLoading) return <p className="text-on-surface-variant">Loading…</p>;

  return (
    <div className="space-y-lg">
      <div className="flex items-end justify-between gap-md flex-wrap">
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">Revenue &amp; profit</h2>
          <p className="text-body-md text-on-surface-variant mt-xs">Earnings from fulfilled retailer orders.</p>
        </div>
        <span className="inline-flex items-center gap-1 text-label-md font-semibold text-primary bg-primary-fixed px-3 py-1.5 rounded-full">
          <Icon name="calendar_month" className="text-[18px]" /> {period.label}
        </span>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-gutter">
        <Stat title="Total revenue" value={money(stats.gross)} icon="payments" accent="text-primary" sub={period.label} />
        <Stat title="Net (ex-GST)" value={money(stats.net)} icon="savings" accent="text-tertiary-container" sub="After tax collected" />
        <Stat title="GST collected" value={money(stats.tax)} icon="receipt_long" accent="text-secondary" sub="Payable to govt." />
        <Stat title="Avg order value" value={money(stats.aov)} icon="trending_up" accent="text-on-surface-variant" sub={`${stats.fulfilled} fulfilled orders`} />
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

      <div className="space-y-md">
        <h3 className="text-headline-md text-on-background">Orders · {period.label}</h3>
        {groups.length === 0 && (
          <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-xl text-center text-on-surface-variant">No orders in this period.</div>
        )}
        {groups.map((g) => (
          <div key={g.key} className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm overflow-hidden">
            <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between flex-wrap gap-sm">
              <div>
                <div className="font-bold text-on-surface">{prettyDate(g.key)}</div>
                <div className="text-label-sm text-on-surface-variant">{g.list.length} orders</div>
              </div>
              <div className="flex gap-lg text-right">
                <div><div className="text-label-sm text-on-surface-variant">Revenue</div><div className="font-bold text-primary">{money(g.revenue)}</div></div>
                <div><div className="text-label-sm text-on-surface-variant">Profit (net)</div><div className="font-bold text-tertiary-container">{money(g.profit)}</div></div>
              </div>
            </div>
            <div className="p-md space-y-sm">
              {g.list.map((o) => <OrderCard key={o.id} order={o} retailerName={retailerName(o)} open={openId === o.id} onToggle={() => setOpenId(openId === o.id ? null : o.id)} />)}
            </div>
          </div>
        ))}
      </div>

      <p className="text-label-sm text-on-surface-variant flex items-center gap-1">
        <Icon name="info" className="text-[16px]" /> Profit shown is net of GST. Per-product cost prices (add them in Stock) are needed for true margin.
      </p>
    </div>
  );
}
