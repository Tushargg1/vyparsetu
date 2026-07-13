import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { orderApi, directoryApi } from '../../lib/api';
import Icon from '../../components/Icon';
import BarChart from '../../components/BarChart';
import PageHeader from '../../components/PageHeader';
import { isoDate, TREND_TOGGLE, buildBuckets, effectiveIndex, activePeriod, rangeWord } from '../../lib/trend';

const DEAD = new Set(['REJECTED', 'CANCELLED']);
const money = (v) => (v == null ? '—' : `₹${Number(v).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`);
const dateOnly = (v) => (v ? new Date(v).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—');

const STATUS = {
  PENDING: { label: 'Placed', tone: 'bg-surface-variant text-on-surface-variant' },
  ACCEPTED: { label: 'Accepted', tone: 'bg-primary-fixed text-on-primary-fixed' },
  PACKED: { label: 'Packed', tone: 'bg-secondary-fixed-dim text-on-secondary-fixed' },
  OUT_FOR_DELIVERY: { label: 'On the way', tone: 'bg-secondary-fixed-dim text-on-secondary-fixed' },
  DELIVERED: { label: 'Delivered', tone: 'bg-tertiary-fixed-dim text-on-tertiary-container' },
  CASH_COLLECTED: { label: 'Paid', tone: 'bg-tertiary-fixed-dim text-on-tertiary-container' },
  COMPLETED: { label: 'Completed', tone: 'bg-tertiary-fixed-dim text-on-tertiary-container' },
  REJECTED: { label: 'Rejected', tone: 'bg-error-container text-error' },
  CANCELLED: { label: 'Cancelled', tone: 'bg-error-container text-error' },
  RETURNED: { label: 'Returned', tone: 'bg-error-container text-error' },
};
const badgeOf = (s) => STATUS[s] || { label: s, tone: 'bg-surface-variant text-on-surface-variant' };
const paidOf = (o) => (o.amountPaid != null ? Number(o.amountPaid) : o.paymentStatus === 'PAID' ? Number(o.totalAmount || 0) : 0);
const pendingOf = (o) => Math.max(0, Number(o.totalAmount || 0) - paidOf(o));

function MiniStat({ label, value, accent }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-md shadow-sm">
      <div className="text-label-sm text-on-surface-variant">{label}</div>
      <div className={`text-headline-md font-bold ${accent || 'text-on-surface'}`}>{value}</div>
    </div>
  );
}

function TimelineStep({ label, value, done }) {
  return (
    <div className="flex items-center gap-sm">
      <Icon name={done ? 'check_circle' : 'radio_button_unchecked'} className={`text-[18px] ${done ? 'text-on-tertiary-container' : 'text-outline-variant'}`} />
      <span className="text-label-sm text-on-surface-variant">{label}</span>
      <span className={`text-label-sm font-medium ${done ? 'text-on-surface' : 'text-on-surface-variant'}`}>{value}</span>
    </div>
  );
}

function OrderCard({ order, distributorName, open, onToggle }) {
  const badge = badgeOf(order.status);
  const items = order.items || [];
  const total = Number(order.totalAmount || 0);
  const pending = pendingOf(order);
  return (
    <div className="border border-surface-variant rounded-xl overflow-hidden bg-surface-container-lowest">
      <button onClick={onToggle} className="w-full flex items-center justify-between gap-sm px-lg py-md text-left hover:bg-surface-container">
        <div className="flex items-center gap-sm min-w-0">
          <Icon name={open ? 'expand_less' : 'expand_more'} className="text-on-surface-variant" />
          <div className="min-w-0">
            <div className="font-semibold text-on-surface truncate">{order.orderNumber}</div>
            <div className="text-label-sm text-on-surface-variant truncate">{distributorName} · {dateOnly(order.placedAt)} · {items.length} items</div>
          </div>
        </div>
        <div className="flex items-center gap-sm shrink-0">
          <div className="text-right">
            <div className="font-bold text-on-surface">{money(total)}</div>
            {pending > 0 ? <div className="text-label-sm font-semibold text-error">{money(pending)} due</div> : <div className="text-label-sm font-semibold text-on-tertiary-container">Paid</div>}
          </div>
          <span className={`px-sm py-xs rounded-full text-label-sm font-medium ${badge.tone}`}>{badge.label}</span>
        </div>
      </button>
      {open && (
        <div className="px-lg py-md border-t border-surface-variant space-y-md">
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
            </tbody>
          </table>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-sm bg-surface-container rounded-lg p-md">
            <TimelineStep label="Placed" value={dateOnly(order.placedAt)} done />
            <TimelineStep label="Packed" value={order.packedAt ? dateOnly(order.packedAt) : 'Pending'} done={!!order.packedAt} />
            <TimelineStep label="Delivered" value={order.deliveredAt ? dateOnly(order.deliveredAt) : 'Pending'} done={!!order.deliveredAt} />
          </div>
          <div className="grid grid-cols-3 gap-sm text-center pt-sm border-t border-surface-variant">
            <div><div className="text-label-sm text-on-surface-variant">Order total</div><div className="font-bold text-on-surface">{money(total)}</div></div>
            <div><div className="text-label-sm text-on-surface-variant">Paid</div><div className="font-bold text-on-tertiary-container">{money(paidOf(order))}</div></div>
            <div><div className="text-label-sm text-on-surface-variant">Pending</div><div className={`font-bold ${pending > 0 ? 'text-error' : 'text-on-surface'}`}>{money(pending)}</div></div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function OrdersPage() {
  const navigate = useNavigate();
  const [range, setRange] = useState('monthly');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [selIdx, setSelIdx] = useState(null);
  const [openId, setOpenId] = useState(null);

  const { data, isLoading } = useQuery({ queryKey: ['orders'], queryFn: () => orderApi.mine({ page: 0, size: 200 }) });
  const { data: distributors = [] } = useQuery({ queryKey: ['distributors'], queryFn: directoryApi.distributors });

  const orders = data?.content || [];
  const dmap = useMemo(() => { const m = new Map(); distributors.forEach((d) => m.set(d.id, d)); return m; }, [distributors]);
  const distributorName = (o) => dmap.get(o.supplierId)?.businessName || `Distributor #${o.supplierId}`;

  const changeRange = (k) => { setRange(k); setSelIdx(null); };

  const { buckets, keyOf, customReady } = useMemo(() => buildBuckets(range, start, end), [range, start, end]);
  const series = useMemo(() => {
    if (!keyOf) return buckets;
    const idx = new Map(buckets.map((b) => [b.key, b]));
    orders.forEach((o) => { if (o.placedAt && !DEAD.has(o.status)) { const b = idx.get(keyOf(new Date(o.placedAt))); if (b) b.value += Number(o.totalAmount || 0); } });
    return buckets;
  }, [buckets, keyOf, orders]);

  const effIdx = effectiveIndex(range, series, selIdx);
  const period = activePeriod(range, series, effIdx, start, end);

  const periodOrders = useMemo(
    () => orders.filter((o) => o.placedAt && new Date(o.placedAt).getTime() >= period.from && new Date(o.placedAt).getTime() < period.to)
      .sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)),
    [orders, period.from, period.to],
  );
  const stats = useMemo(() => {
    let spent = 0, due = 0;
    periodOrders.forEach((o) => { if (DEAD.has(o.status)) return; spent += Number(o.totalAmount || 0); due += pendingOf(o); });
    return { count: periodOrders.length, spent, due };
  }, [periodOrders]);

  if (isLoading) return <p className="text-on-surface-variant">Loading…</p>;

  return (
    <div className="space-y-lg">
      <PageHeader icon="receipt_long" title="My Orders" subtitle="Every order you've placed with distributors."
        action={<span className="inline-flex items-center gap-1 text-label-md font-semibold text-primary bg-primary-fixed px-3 py-1.5 rounded-full"><Icon name="calendar_month" className="text-[18px]" /> {period.label}</span>} />

      {orders.length === 0 ? (
        <div className="text-center py-2xl text-on-surface-variant">
          <Icon name="receipt_long" className="text-[48px] opacity-40" />
          <p className="mt-sm">No orders yet.</p>
          <button onClick={() => navigate('/distributors')} className="mt-lg bg-primary text-on-primary px-lg py-3 rounded-2xl font-bold">Place your first order</button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-3 gap-gutter">
            <MiniStat label="Orders" value={stats.count} />
            <MiniStat label="Total value" value={money(stats.spent)} accent="text-primary" />
            <MiniStat label="Pending due" value={money(stats.due)} accent={stats.due > 0 ? 'text-error' : 'text-on-surface'} />
          </div>

          <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm">
            <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between flex-wrap gap-sm">
              <h3 className="text-headline-md text-on-background">Order history</h3>
              <div className="flex bg-surface-container rounded-lg overflow-hidden text-label-md flex-wrap">
                {TREND_TOGGLE.map(([k, l]) => (
                  <button key={k} onClick={() => changeRange(k)} className={`px-3 py-1.5 ${range === k ? 'bg-primary text-on-primary font-semibold' : 'text-on-surface-variant'}`}>{l}</button>
                ))}
              </div>
            </div>
            <div className="p-lg space-y-md">
              {range === 'custom' && (
                <div className="flex items-center gap-sm flex-wrap">
                  <Icon name="calendar_month" className="text-on-surface-variant text-[20px]" />
                  <label className="text-label-sm text-on-surface-variant">From</label>
                  <input type="date" value={start} max={end || isoDate(Date.now())} onChange={(e) => { setStart(e.target.value); setSelIdx(null); }}
                    className="border border-outline-variant rounded-lg px-3 py-1.5 text-label-md outline-none focus:border-primary" />
                  <label className="text-label-sm text-on-surface-variant">To</label>
                  <input type="date" value={end} min={start || undefined} max={isoDate(Date.now())} onChange={(e) => { setEnd(e.target.value); setSelIdx(null); }}
                    className="border border-outline-variant rounded-lg px-3 py-1.5 text-label-md outline-none focus:border-primary" />
                </div>
              )}
              {customReady ? (
                <>
                  <BarChart data={series} onSelect={setSelIdx} selectedIndex={effIdx} />
                  {range !== 'alltime' && (
                    <p className="text-label-sm text-on-surface-variant flex items-center gap-1"><Icon name="touch_app" className="text-[16px]" /> Tap a bar to view that {rangeWord(range)}.</p>
                  )}
                </>
              ) : (
                <div className="h-[160px] flex items-center justify-center text-on-surface-variant text-label-md">Select a start and end date.</div>
              )}
            </div>
          </div>

          <div className="space-y-md">
            <div className="flex items-center justify-between flex-wrap gap-sm">
              <h3 className="text-headline-md text-on-background">Orders · {period.label}</h3>
              <span className="text-label-md text-on-surface-variant">{periodOrders.length} orders</span>
            </div>
            {periodOrders.length === 0 ? (
              <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-xl text-center text-on-surface-variant">No orders in this period.</div>
            ) : (
              <div className="space-y-sm">
                {periodOrders.map((o) => (
                  <OrderCard key={o.id} order={o} distributorName={distributorName(o)} open={openId === o.id} onToggle={() => setOpenId(openId === o.id ? null : o.id)} />
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
