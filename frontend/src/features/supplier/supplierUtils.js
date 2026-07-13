// Shared helpers for the distributor (supplier) screens.

export const money = (v) =>
  v == null || v === '' ? '—' : `₹${Number(v).toLocaleString('en-IN', { maximumFractionDigits: 0 })}`;

export const STATUS_FLOW = ['PENDING', 'ACCEPTED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED'];

export const STATUS_META = {
  PENDING: { label: 'Pending', tone: 'bg-error-container text-error' },
  ACCEPTED: { label: 'Accepted', tone: 'bg-tertiary-fixed-dim text-on-tertiary-container' },
  PACKED: { label: 'Packed', tone: 'bg-secondary-container text-on-secondary-container' },
  OUT_FOR_DELIVERY: { label: 'Out for delivery', tone: 'bg-primary-fixed text-primary' },
  DELIVERED: { label: 'Delivered', tone: 'bg-tertiary-fixed-dim text-on-tertiary-container' },
  REJECTED: { label: 'Rejected', tone: 'bg-surface-variant text-on-surface-variant line-through' },
  CANCELLED: { label: 'Cancelled', tone: 'bg-surface-variant text-on-surface-variant' },
};

// statuses that count as realised revenue for the distributor
export const REVENUE_STATUSES = new Set(['ACCEPTED', 'PACKED', 'OUT_FOR_DELIVERY', 'DELIVERED']);

export const PAYMENT_META = {
  PAID: { label: 'Paid', tone: 'bg-tertiary-fixed-dim text-on-tertiary-container' },
  PENDING: { label: 'Unpaid', tone: 'bg-error-container text-error' },
  PARTIAL: { label: 'Partial', tone: 'bg-secondary-container text-on-secondary-container' },
  REFUNDED: { label: 'Refunded', tone: 'bg-surface-variant text-on-surface-variant' },
  FAILED: { label: 'Failed', tone: 'bg-surface-variant text-on-surface-variant' },
};

// payment states that still owe the distributor money
export const OPEN_PAYMENT = new Set(['PENDING', 'PARTIAL']);

export function paymentBadge(status) {
  return PAYMENT_META[status] || { label: status || '—', tone: 'bg-surface-variant text-on-surface-variant' };
}

// A live order whose payment is still outstanding.
export function isPaymentDue(o) {
  return OPEN_PAYMENT.has(o.paymentStatus) && o.status !== 'REJECTED' && o.status !== 'CANCELLED';
}

// Per-retailer outstanding dues from the supplier's orders.
export function paymentDues(orders = [], retailers = []) {
  const names = retailerMap(retailers);
  const m = new Map();
  orders.forEach((o) => {
    if (!isPaymentDue(o)) return;
    const cur = m.get(o.retailerId) || { amount: 0, count: 0, lastAt: null };
    cur.amount += Number(o.totalAmount || 0);
    cur.count += 1;
    const at = o.placedAt ? new Date(o.placedAt).getTime() : 0;
    if (at > (cur.lastAt || 0)) cur.lastAt = at;
    m.set(o.retailerId, cur);
  });
  return [...m.entries()]
    .map(([retailerId, v]) => ({ retailerId, ...v, name: names.get(retailerId)?.shopName || `Retailer #${retailerId}` }))
    .sort((a, b) => b.amount - a.amount);
}

export function statusBadge(status) {
  return STATUS_META[status] || { label: status, tone: 'bg-surface-variant text-on-surface-variant' };
}

export function nextStatuses(status) {
  const map = {
    PENDING: ['ACCEPTED', 'REJECTED'],
    ACCEPTED: ['PACKED'],
    PACKED: ['OUT_FOR_DELIVERY'],
    OUT_FOR_DELIVERY: ['DELIVERED'],
  };
  return map[status] || [];
}

// Build retailerId -> shop name map from the retailer roster.
export function retailerMap(retailers = []) {
  const m = new Map();
  retailers.forEach((r) => m.set(r.retailerId, r));
  return m;
}

// Per-retailer aggregates computed from the supplier's orders.
export function retailerStats(orders = []) {
  const stats = new Map();
  orders.forEach((o) => {
    const cur = stats.get(o.retailerId) || { orders: 0, revenue: 0, pending: 0, due: 0, lastAt: null };
    cur.orders += 1;
    if (o.status === 'PENDING') cur.pending += 1;
    if (REVENUE_STATUSES.has(o.status)) cur.revenue += Number(o.totalAmount || 0);
    if (isPaymentDue(o)) cur.due += Number(o.totalAmount || 0);
    const at = o.placedAt ? new Date(o.placedAt).getTime() : 0;
    if (at > (cur.lastAt || 0)) cur.lastAt = at;
    stats.set(o.retailerId, cur);
  });
  return stats;
}

export function timeAgo(value) {
  if (!value) return '—';
  const d = new Date(value).getTime();
  const diff = Date.now() - d;
  const day = 86_400_000;
  if (diff < day) return 'Today';
  if (diff < 2 * day) return 'Yesterday';
  const days = Math.floor(diff / day);
  if (days < 30) return `${days}d ago`;
  return new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export const formatDate = (value) =>
  value
    ? new Date(value).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
    : '—';
