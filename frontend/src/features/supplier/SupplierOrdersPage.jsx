import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { supplierOrderApi, distributorApi } from '../../lib/api';
import Icon from '../../components/Icon';
import BarChart from '../../components/BarChart';
import PageHeader from '../../components/PageHeader';
import { LoadingState } from '../../components/StatePanel';
import { money, statusBadge, nextStatuses, retailerMap, paymentBadge } from './supplierUtils';
import { isoDate, TREND_TOGGLE, buildBuckets, effectiveIndex, activePeriod, rangeWord } from '../../lib/trend';

const DELIVERED_SET = new Set(['DELIVERED', 'CASH_COLLECTED', 'COMPLETED']);
const DEAD = new Set(['REJECTED', 'CANCELLED']);
const isToday = (v) => v && isoDate(v) === isoDate(Date.now());
const dateOnly = (v) => (v ? new Date(v).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—');

const paidOf = (o) => (o.amountPaid != null ? Number(o.amountPaid) : o.paymentStatus === 'PAID' ? Number(o.totalAmount || 0) : 0);
const pendingOf = (o) => Math.max(0, Number(o.totalAmount || 0) - paidOf(o));

function TimelineStep({ label, value, done }) {
  return (
    <div className="flex items-center gap-sm">
      <Icon name={done ? 'check_circle' : 'radio_button_unchecked'} className={`text-[18px] ${done ? 'text-on-tertiary-container' : 'text-outline-variant'}`} />
      <span className="text-label-sm text-on-surface-variant">{label}</span>
      <span className={`text-label-sm font-medium ${done ? 'text-on-surface' : 'text-on-surface-variant'}`}>{value}</span>
    </div>
  );
}

function OrderCard({ order, retailerName, open, onToggle, onMove }) {
  const badge = statusBadge(order.status);
  const pb = paymentBadge(order.paymentStatus);
  const items = order.items || [];
  const total = Number(order.totalAmount || 0);
  const pending = pendingOf(order);
  return (
    <div className="overflow-hidden rounded-2xl border border-surface-variant bg-surface-container-lowest shadow-sm">
      <button onClick={onToggle} className="flex w-full flex-col gap-sm px-md py-md text-left hover:bg-surface-container-low sm:flex-row sm:items-center sm:justify-between sm:px-lg">
        <div className="flex items-center gap-sm min-w-0">
          <Icon name={open ? 'expand_less' : 'expand_more'} className="text-on-surface-variant" />
          <div className="min-w-0">
            <div className="font-semibold text-on-surface truncate">{order.orderNumber}</div>
            <div className="text-label-sm text-on-surface-variant truncate">{retailerName} · {dateOnly(order.placedAt)} · {items.length} items</div>
          </div>
        </div>
        <div className="flex w-full items-center justify-between gap-sm pl-xl sm:w-auto sm:justify-end sm:pl-0">
          <div className="text-right">
            <div className="font-bold text-on-surface">{money(total)}</div>
            {pending > 0 ? <div className="text-label-sm font-semibold text-error">{money(pending)} due</div> : <div className="text-label-sm font-semibold text-on-tertiary-container">Paid</div>}
          </div>
          <span className={`px-sm py-xs rounded-full text-label-sm font-medium ${badge.tone}`}>{badge.label}</span>
        </div>
      </button>
      {open && (
        <div className="px-lg py-md border-t border-surface-variant space-y-md">
          <div className="overflow-x-auto">
          <table className="w-full min-w-[520px] text-label-md">
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
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-sm bg-surface-container rounded-lg p-md">
            <TimelineStep label="Received" value={dateOnly(order.placedAt)} done />
            <TimelineStep label="Packed" value={order.packedAt ? dateOnly(order.packedAt) : 'Pending'} done={!!order.packedAt} />
            <TimelineStep label="Delivered" value={order.deliveredAt ? dateOnly(order.deliveredAt) : 'Pending'} done={!!order.deliveredAt} />
          </div>
          <div className="flex items-center justify-between gap-sm flex-wrap">
            <div className="flex items-center gap-sm flex-wrap">
              <span className={`px-sm py-xs rounded-full text-label-sm font-medium ${pb.tone}`}>Payment: {pb.label}</span>
              {pending > 0 ? <span className="text-label-sm text-error">{money(pending)} pending</span> : <span className="text-label-sm text-on-tertiary-container">Fully paid</span>}
            </div>
            {nextStatuses(order.status).length > 0 && (
              <div className="flex gap-sm">
                {nextStatuses(order.status).map((s) => {
                  const reject = s === 'REJECTED';
                  return (
                    <button key={s} onClick={() => onMove(order.id, s)}
                      className={`px-3 py-1 rounded-md text-label-sm font-semibold ${reject ? 'border border-outline-variant text-error hover:bg-error-container' : 'bg-primary text-on-primary hover:opacity-90'}`}>
                      {statusBadge(s).label}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Section({ icon, title, accent, count, children }) {
  return (
    <div className="space-y-sm">
      <div className="flex items-center gap-sm">
        <Icon name={icon} className={accent} />
        <h3 className="text-headline-md text-on-background">{title}</h3>
        <span className="text-label-sm font-semibold bg-surface-container text-on-surface-variant px-2 py-0.5 rounded-full">{count}</span>
      </div>
      {children}
    </div>
  );
}

function Empty({ text, icon }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-xl text-center text-on-surface-variant">
      <Icon name={icon} className="text-[32px] opacity-40" />
      <p className="mt-sm text-label-md">{text}</p>
    </div>
  );
}

function Summary({ icon, label, value, accent }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-md flex items-center gap-sm shadow-sm">
      <span className={`w-10 h-10 rounded-full bg-surface-container flex items-center justify-center ${accent}`}><Icon name={icon} className="text-[20px]" /></span>
      <div>
        <div className="text-headline-md font-bold text-on-surface">{value}</div>
        <div className="text-label-sm text-on-surface-variant">{label}</div>
      </div>
    </div>
  );
}

export default function SupplierOrdersPage() {
  const qc = useQueryClient();
  const [view, setView] = useState('live');
  const [openId, setOpenId] = useState(null);
  const [range, setRange] = useState('monthly');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [selIdx, setSelIdx] = useState(null);

  const { data, isLoading } = useQuery({ queryKey: ['sup-orders-all'], queryFn: () => supplierOrderApi.list({ page: 0, size: 200 }) });
  const { data: retailers = [] } = useQuery({ queryKey: ['sup-retailers'], queryFn: distributorApi.retailers });

  const orders = data?.content || [];
  const rmap = useMemo(() => retailerMap(retailers), [retailers]);
  const retailerName = (o) => rmap.get(o.retailerId)?.shopName || `Retailer #${o.retailerId}`;

  const pending = useMemo(() => orders.filter((o) => o.status === 'PENDING').sort((a, b) => new Date(a.placedAt) - new Date(b.placedAt)), [orders]);
  const packedToday = useMemo(() => orders.filter((o) => o.status === 'PACKED' && isToday(o.packedAt)).sort((a, b) => new Date(b.packedAt) - new Date(a.packedAt)), [orders]);
  const deliveredToday = useMemo(() => orders.filter((o) => DELIVERED_SET.has(o.status) && isToday(o.deliveredAt)).sort((a, b) => new Date(b.deliveredAt) - new Date(a.deliveredAt)), [orders]);

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

  const historyList = useMemo(
    () => orders.filter((o) => o.placedAt && new Date(o.placedAt).getTime() >= period.from && new Date(o.placedAt).getTime() < period.to)
      .sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)),
    [orders, period.from, period.to],
  );
  const periodStats = useMemo(() => {
    const inP = (v) => v && new Date(v).getTime() >= period.from && new Date(v).getTime() < period.to;
    let received = 0, packed = 0, delivered = 0;
    orders.forEach((o) => { if (inP(o.placedAt)) received += 1; if (inP(o.packedAt)) packed += 1; if (inP(o.deliveredAt)) delivered += 1; });
    return { received, packed, delivered };
  }, [orders, period.from, period.to]);

  const move = async (id, status) => {
    await supplierOrderApi.updateStatus(id, { status, note: status });
    qc.invalidateQueries({ queryKey: ['sup-orders-all'] });
    qc.invalidateQueries({ queryKey: ['sup-orders'] });
  };
  const cardProps = (o) => ({ key: o.id, order: o, retailerName: retailerName(o), open: openId === o.id, onToggle: () => setOpenId(openId === o.id ? null : o.id), onMove: move });

  return (
    <div className="space-y-lg">
      <PageHeader icon="receipt_long" title="Orders" subtitle="Accept, pack, dispatch, and review retailer orders." />

      <div className="flex bg-surface-container rounded-lg overflow-hidden text-label-md w-fit">
        {[['live', 'Live orders'], ['history', 'All orders']].map(([k, l]) => (
          <button key={k} onClick={() => setView(k)} className={`px-lg py-2 ${view === k ? 'bg-primary text-on-primary font-semibold' : 'text-on-surface-variant'}`}>{l}</button>
        ))}
      </div>

      {isLoading && <LoadingState compact label="Loading retailer orders…" />}

      {view === 'live' && !isLoading && (
        <div className="space-y-lg">
          <Section icon="pending_actions" title="Pending orders" accent="text-error" count={pending.length}>
            {pending.length === 0 ? <Empty text="No pending orders. You're all caught up." icon="task_alt" /> : <div className="space-y-sm">{pending.map((o) => <OrderCard {...cardProps(o)} />)}</div>}
          </Section>
          <Section icon="inventory_2" title="Packed today" accent="text-secondary" count={packedToday.length}>
            {packedToday.length === 0 ? <Empty text="Nothing packed today yet." icon="package_2" /> : <div className="space-y-sm">{packedToday.map((o) => <OrderCard {...cardProps(o)} />)}</div>}
          </Section>
          <Section icon="local_shipping" title="Delivered today" accent="text-on-tertiary-container" count={deliveredToday.length}>
            {deliveredToday.length === 0 ? <Empty text="No deliveries today yet." icon="local_shipping" /> : <div className="space-y-sm">{deliveredToday.map((o) => <OrderCard {...cardProps(o)} />)}</div>}
          </Section>
        </div>
      )}

      {view === 'history' && !isLoading && (
        <div className="space-y-lg">
          <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm">
            <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between flex-wrap gap-sm">
              <h3 className="text-headline-md text-on-background">Order history</h3>
              <div className="flex items-center gap-sm flex-wrap">
                <span className="inline-flex items-center gap-1 text-label-md font-semibold text-primary bg-primary-fixed px-3 py-1.5 rounded-full"><Icon name="calendar_month" className="text-[18px]" /> {period.label}</span>
                <div className="flex bg-surface-container rounded-lg overflow-hidden text-label-md flex-wrap">
                  {TREND_TOGGLE.map(([k, l]) => (
                    <button key={k} onClick={() => changeRange(k)} className={`px-3 py-1.5 ${range === k ? 'bg-primary text-on-primary font-semibold' : 'text-on-surface-variant'}`}>{l}</button>
                  ))}
                </div>
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
                  {range !== 'alltime' && <p className="text-label-sm text-on-surface-variant flex items-center gap-1"><Icon name="touch_app" className="text-[16px]" /> Tap a bar to view that {rangeWord(range)}.</p>}
                </>
              ) : (
                <div className="h-[160px] flex items-center justify-center text-on-surface-variant text-label-md">Select a start and end date.</div>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 gap-md sm:grid-cols-3">
            <Summary icon="receipt_long" label="Received" value={periodStats.received} accent="text-primary" />
            <Summary icon="inventory_2" label="Packed" value={periodStats.packed} accent="text-secondary" />
            <Summary icon="local_shipping" label="Delivered" value={periodStats.delivered} accent="text-on-tertiary-container" />
          </div>

          <div className="space-y-md">
            <div className="flex items-center justify-between flex-wrap gap-sm">
              <h3 className="text-headline-md text-on-background">Orders · {period.label}</h3>
              <span className="text-label-md text-on-surface-variant">{historyList.length} orders</span>
            </div>
            {historyList.length === 0 ? <Empty text="No orders in this period." icon="receipt_long" /> : <div className="space-y-sm">{historyList.map((o) => <OrderCard {...cardProps(o)} />)}</div>}
          </div>
        </div>
      )}
    </div>
  );
}
