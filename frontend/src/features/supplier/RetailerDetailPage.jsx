import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { distributorApi, supplierOrderApi } from '../../lib/api';
import Icon from '../../components/Icon';
import BarChart from '../../components/BarChart';
import ContactCard from '../../components/ContactCard';
import { money, statusBadge, nextStatuses, formatDate, REVENUE_STATUSES, paymentBadge } from './supplierUtils';
import { isoDate, TREND_TOGGLE, buildBuckets, effectiveIndex, activePeriod, rangeWord } from '../../lib/trend';
import CreditPanel from './CreditPanel';
import CustomerPricesPanel from './CustomerPricesPanel';
import OrderTimeline from './OrderTimeline';
import OrderModifyModal from './OrderModifyModal';
import InvoiceModal from './InvoiceModal';

const MODIFIABLE = new Set(['PENDING', 'ACCEPTED']);

const IN_PROGRESS = new Set(['PENDING', 'ACCEPTED', 'PACKED', 'OUT_FOR_DELIVERY']);
const DEAD = new Set(['REJECTED', 'CANCELLED']);

const paidOf = (o) => (o.amountPaid != null ? Number(o.amountPaid) : o.paymentStatus === 'PAID' ? Number(o.totalAmount || 0) : 0);
const pendingOf = (o) => Math.max(0, Number(o.totalAmount || 0) - paidOf(o));
const dateTime = (v) => (v ? new Date(v).toLocaleString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : null);

function Stat({ title, value, icon, accent }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
      <div className="flex items-center justify-between mb-sm">
        <span className="text-label-md text-on-surface-variant">{title}</span>
        <Icon name={icon} className={accent} />
      </div>
      <div className="text-headline-lg font-bold text-on-surface">{value}</div>
    </div>
  );
}

function OrderRow({ order, open, onToggle, onMove, onEdit, onInvoice }) {
  const badge = statusBadge(order.status);
  const pb = paymentBadge(order.paymentStatus);
  const items = order.items || [];
  const total = Number(order.totalAmount || 0);
  const paid = paidOf(order);
  const pending = pendingOf(order);
  return (
    <div className="border border-surface-variant rounded-xl overflow-hidden">
      <button onClick={onToggle} className="w-full flex items-center justify-between gap-sm px-lg py-md text-left hover:bg-surface-container">
        <div className="flex items-center gap-sm min-w-0">
          <Icon name={open ? 'expand_less' : 'expand_more'} className="text-on-surface-variant" />
          <div className="min-w-0">
            <div className="font-semibold text-on-surface truncate">{order.orderNumber}</div>
            <div className="text-label-sm text-on-surface-variant">{formatDate(order.placedAt)} · {items.length} items</div>
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
        <div className="px-lg py-md border-t border-surface-variant bg-surface-container-lowest space-y-md">
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
          <div className="grid grid-cols-3 gap-sm text-center pt-sm border-t border-surface-variant">
            <div><div className="text-label-sm text-on-surface-variant">Order total</div><div className="font-bold text-on-surface">{money(total)}</div></div>
            <div><div className="text-label-sm text-on-surface-variant">Amount paid</div><div className="font-bold text-on-tertiary-container">{money(paid)}</div></div>
            <div><div className="text-label-sm text-on-surface-variant">Amount pending</div><div className={`font-bold ${pending > 0 ? 'text-error' : 'text-on-surface'}`}>{money(pending)}</div></div>
          </div>
          <div className="flex items-center justify-between gap-sm flex-wrap">
            <div className="flex items-center gap-sm flex-wrap">
              <span className={`px-sm py-xs rounded-full text-label-sm font-medium ${pb.tone}`}>Payment: {pb.label}</span>
              {order.lastPaymentAt && <span className="text-label-sm text-on-surface-variant flex items-center gap-1"><Icon name="event_available" className="text-[16px]" /> Last paid {dateTime(order.lastPaymentAt)}</span>}
            </div>
            <div className="flex gap-sm items-center">
              <button onClick={() => onInvoice(order)}
                className="px-3 py-1 rounded-md text-label-sm font-semibold border border-outline-variant text-on-surface hover:bg-surface-container flex items-center gap-1">
                <Icon name="receipt" className="text-[16px]" /> Invoice
              </button>
              {MODIFIABLE.has(order.status) && (
                <button onClick={() => onEdit(order)}
                  className="px-3 py-1 rounded-md text-label-sm font-semibold border border-outline-variant text-on-surface hover:bg-surface-container flex items-center gap-1">
                  <Icon name="edit" className="text-[16px]" /> Edit items
                </button>
              )}
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
          </div>

          {/* Status timeline */}
          <div className="pt-sm border-t border-surface-variant">
            <div className="text-label-sm font-semibold text-on-surface-variant mb-sm flex items-center gap-1">
              <Icon name="timeline" className="text-[16px]" /> Timeline
            </div>
            <OrderTimeline orderId={order.id} />
          </div>
        </div>
      )}
    </div>
  );
}

export default function RetailerDetailPage() {
  const { id } = useParams();
  const retailerId = Number(id);
  const qc = useQueryClient();

  const { data: retailers = [] } = useQuery({ queryKey: ['sup-retailers'], queryFn: distributorApi.retailers });
  const { data: ordersPage, isLoading } = useQuery({ queryKey: ['sup-orders-all'], queryFn: () => supplierOrderApi.list({ page: 0, size: 200 }) });

  const [range, setRange] = useState('monthly');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const [selIdx, setSelIdx] = useState(null);
  const [openId, setOpenId] = useState(null);
  const [pendingOnly, setPendingOnly] = useState(false);
  const [payOpen, setPayOpen] = useState(false);
  const [payAmount, setPayAmount] = useState('');
  const [payBusy, setPayBusy] = useState(false);
  const [payMsg, setPayMsg] = useState('');
  const [editOrder, setEditOrder] = useState(null);
  const [invoiceOrder, setInvoiceOrder] = useState(null);

  const retailer = retailers.find((r) => r.retailerId === retailerId);
  const orders = useMemo(
    () => (ordersPage?.content || []).filter((o) => o.retailerId === retailerId).sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)),
    [ordersPage, retailerId],
  );

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

  // Period-aware stat cards.
  const totals = useMemo(() => {
    const inP = (t) => t >= period.from && t < period.to;
    let revenue = 0, due = 0, pendingOrders = 0, count = 0;
    orders.forEach((o) => {
      if (!o.placedAt || !inP(new Date(o.placedAt).getTime())) return;
      count += 1;
      if (REVENUE_STATUSES.has(o.status)) revenue += Number(o.totalAmount || 0);
      if (!DEAD.has(o.status)) due += pendingOf(o);
      if (IN_PROGRESS.has(o.status)) pendingOrders += 1;
    });
    return { revenue, due, pendingOrders, count };
  }, [orders, period.from, period.to]);

  const periodOrders = useMemo(
    () => orders.filter((o) => o.placedAt && new Date(o.placedAt).getTime() >= period.from && new Date(o.placedAt).getTime() < period.to)
      .sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)),
    [orders, period.from, period.to],
  );

  const pendingList = useMemo(
    () => orders.filter((o) => !DEAD.has(o.status) && pendingOf(o) > 0).sort((a, b) => new Date(b.placedAt) - new Date(a.placedAt)),
    [orders],
  );
  const pendingTotal = useMemo(() => pendingList.reduce((s, o) => s + pendingOf(o), 0), [pendingList]);
  const shownOrders = pendingOnly ? pendingList : periodOrders;

  const move = async (oid, status) => {
    await supplierOrderApi.updateStatus(oid, { status, note: status });
    qc.invalidateQueries({ queryKey: ['sup-orders-all'] });
  };

  const submitPayment = async () => {
    const amt = Number(payAmount);
    if (!amt || amt <= 0) { setPayMsg('Enter a valid amount'); return; }
    setPayBusy(true); setPayMsg('');
    try {
      const res = await distributorApi.recordPayment(retailerId, amt);
      await qc.invalidateQueries({ queryKey: ['sup-orders-all'] });
      const settled = res?.ordersSettled ?? 0;
      setPayMsg(`Recorded ${money(res?.applied ?? amt)}. ${settled} order${settled === 1 ? '' : 's'} cleared. Outstanding: ${money(res?.outstandingDue ?? 0)}.`);
      setPayAmount('');
    } catch (e) {
      setPayMsg(e.response?.data?.error?.message || 'Could not record payment');
    } finally { setPayBusy(false); }
  };

  const cardProps = (o) => ({ key: o.id, order: o, open: openId === o.id, onToggle: () => setOpenId(openId === o.id ? null : o.id), onMove: move, onEdit: setEditOrder, onInvoice: setInvoiceOrder });

  return (
    <div className="space-y-lg">
      <Link to="/supplier/retailers" className="inline-flex items-center gap-1 text-label-md text-on-surface-variant hover:text-on-surface">
        <Icon name="arrow_back" className="text-[18px]" /> Retailers
      </Link>

      <div className="flex items-center gap-md flex-wrap">
        <div className="w-14 h-14 rounded-full bg-primary text-on-primary flex items-center justify-center font-bold text-display-sm">
          {(retailer?.shopName || retailer?.ownerName || 'R')[0]}
        </div>
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">{retailer?.shopName || `Retailer #${retailerId}`}</h2>
          <p className="text-body-md text-on-surface-variant">
            {retailer?.ownerName || '—'} · {retailer?.phone || '—'}{retailer?.city ? ` · ${retailer.city}` : ''}{retailer?.creditApproved ? ' · Credit approved' : ''}
          </p>
        </div>
      </div>

      <ContactCard shopName={retailer?.shopName} ownerName={retailer?.ownerName} phone={retailer?.phone} altPhones={retailer?.altPhones}
        address={retailer?.address} city={retailer?.city} state={retailer?.state} pincode={retailer?.pincode} locationUrl={retailer?.locationUrl} />

      {/* Period selector */}
      <div className="flex items-center justify-between gap-md flex-wrap">
        <div className="flex bg-surface-container rounded-lg overflow-hidden text-label-md flex-wrap">
          {TREND_TOGGLE.map(([k, l]) => (
            <button key={k} onClick={() => changeRange(k)} className={`px-3 py-1.5 ${range === k ? 'bg-primary text-on-primary font-semibold' : 'text-on-surface-variant'}`}>{l}</button>
          ))}
        </div>
        <span className="inline-flex items-center gap-1 text-label-md font-semibold text-primary bg-primary-fixed px-3 py-1.5 rounded-full">
          <Icon name="calendar_month" className="text-[18px]" /> {period.label}
        </span>
      </div>

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

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-gutter">
        <Stat title="Revenue" value={money(totals.revenue)} icon="payments" accent="text-primary" />
        <Stat title="Orders" value={totals.count} icon="receipt_long" accent="text-secondary" />
        <Stat title="Outstanding due" value={money(totals.due)} icon="account_balance_wallet" accent="text-error" />
        <Stat title="Pending orders" value={totals.pendingOrders} icon="pending_actions" accent="text-tertiary-container" />
      </div>

      {/* Record payment */}
      <div className="flex items-center justify-between gap-md flex-wrap bg-surface-container-lowest rounded-xl border border-surface-variant p-md shadow-sm">
        <div className="flex items-center gap-sm">
          <Icon name="account_balance_wallet" className="text-primary" />
          <div>
            <p className="text-label-md font-semibold text-on-surface">Received a payment?</p>
            <p className="text-label-sm text-on-surface-variant">It clears the oldest pending orders first.</p>
          </div>
        </div>
        <button onClick={() => { setPayOpen(true); setPayMsg(''); }} className="flex items-center gap-sm bg-primary text-on-primary px-lg py-2.5 rounded-xl text-label-md font-semibold hover:opacity-90">
          <Icon name="add_card" className="text-[20px]" /> Record payment
        </button>
      </div>

      {/* Credit + special pricing */}
      <div className="grid lg:grid-cols-2 gap-gutter">
        <CreditPanel retailerId={retailerId} />
        <CustomerPricesPanel retailerId={retailerId} />
      </div>

      {/* Graph */}
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm">
        <div className="px-lg py-md border-b border-outline-variant">
          <h3 className="text-headline-md text-on-background">Order history</h3>
        </div>
        <div className="p-lg space-y-md">
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

      {/* Orders */}
      <div className="space-y-md">
        <div className="flex items-center justify-between flex-wrap gap-sm">
          <div className="flex items-center gap-sm flex-wrap">
            <h3 className="text-headline-md text-on-background">{pendingOnly ? 'Pending payments' : `Orders · ${period.label}`}</h3>
            {pendingOnly && pendingTotal > 0 && <span className="text-label-sm font-semibold text-error bg-error-container px-2 py-0.5 rounded-full">{money(pendingTotal)} due</span>}
          </div>
          <div className="flex items-center gap-sm">
            <span className="text-label-md text-on-surface-variant">{shownOrders.length} orders</span>
            <button onClick={() => setPendingOnly((v) => !v)}
              className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-label-md font-semibold border transition-colors ${pendingOnly ? 'bg-error text-on-error border-error' : 'border-outline-variant text-on-surface hover:bg-surface-container'}`}>
              <Icon name="account_balance_wallet" className="text-[18px]" />
              {pendingOnly ? 'Showing pending' : 'Pending payments'}
            </button>
          </div>
        </div>

        {isLoading && <p className="text-on-surface-variant">Loading…</p>}
        {!isLoading && shownOrders.length === 0 && (
          <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-xl text-center text-on-surface-variant">
            {pendingOnly ? 'No pending payments. All settled.' : 'No orders in this period.'}
          </div>
        )}
        {shownOrders.map((o) => <OrderRow {...cardProps(o)} />)}
      </div>

      {/* Record payment modal */}
      {payOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-margin-mobile">
          <div className="absolute inset-0 bg-black/40" onClick={() => setPayOpen(false)} />
          <div className="relative bg-surface-container-lowest rounded-2xl shadow-xl w-full max-w-md p-lg space-y-md">
            <div className="flex items-center justify-between">
              <h3 className="text-headline-md text-on-surface">Record payment</h3>
              <button onClick={() => setPayOpen(false)} className="text-on-surface-variant hover:text-on-surface"><Icon name="close" /></button>
            </div>
            <p className="text-label-md text-on-surface-variant">
              From <span className="font-semibold text-on-surface">{retailer?.shopName || `Retailer #${retailerId}`}</span>. Total outstanding is <span className="font-semibold text-error">{money(pendingTotal)}</span>. Applied to oldest unpaid orders first.
            </p>
            <label className="block">
              <span className="text-label-md text-on-surface-variant">Amount received (₹)</span>
              <input type="number" min="1" autoFocus value={payAmount} onChange={(e) => setPayAmount(e.target.value)} placeholder="e.g. 5000"
                className="block w-full mt-1 border border-outline-variant rounded-lg py-2.5 px-3 outline-none focus:border-primary text-headline-md font-bold" />
            </label>
            <div className="flex gap-sm flex-wrap">
              {[pendingTotal, 1000, 5000, 10000].filter((v) => v > 0).map((v, i) => (
                <button key={i} onClick={() => setPayAmount(String(Math.round(v)))} className="px-3 py-1.5 rounded-lg border border-outline-variant text-label-md text-on-surface hover:bg-surface-container">
                  {i === 0 ? 'Full due' : money(v)}
                </button>
              ))}
            </div>
            {payMsg && <p className="text-label-md text-on-surface-variant">{payMsg}</p>}
            <div className="flex justify-end gap-sm">
              <button onClick={() => setPayOpen(false)} className="px-lg py-2.5 rounded-xl text-label-md font-semibold text-on-surface hover:bg-surface-container">Close</button>
              <button onClick={submitPayment} disabled={payBusy} className="bg-primary text-on-primary px-lg py-2.5 rounded-xl text-label-md font-semibold disabled:opacity-50">{payBusy ? 'Saving…' : 'Save payment'}</button>
            </div>
          </div>
        </div>
      )}

      {editOrder && (
        <OrderModifyModal
          order={editOrder}
          onClose={() => setEditOrder(null)}
          onSaved={() => qc.invalidateQueries({ queryKey: ['sup-orders-all'] })}
        />
      )}

      {invoiceOrder && (
        <InvoiceModal order={invoiceOrder} retailer={retailer} onClose={() => setInvoiceOrder(null)} />
      )}
    </div>
  );
}
