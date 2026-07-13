import { useState, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { distributorApi, supplierOrderApi, whatsappApi } from '../../lib/api';
import Icon from '../../components/Icon';
import { money, retailerMap, statusBadge, REVENUE_STATUSES, formatDate, paymentDues } from './supplierUtils';

const ONGOING = new Set(['PENDING', 'PACKED']);

function Metric({ title, value, sub, icon, accent, to }) {
  const body = (
    <>
      <div className="flex items-center justify-between mb-sm">
        <span className="text-label-md text-on-surface-variant">{title}</span>
        <Icon name={icon} className={accent} />
      </div>
      <div className="text-display-lg text-on-surface">{value}</div>
      {sub && <p className="text-label-sm text-on-surface-variant mt-xs">{sub}</p>}
    </>
  );
  const cls =
    'bg-surface-container-lowest p-lg rounded-xl border border-surface-variant shadow-sm transition-colors hover:border-primary block';
  return to ? <Link to={to} className={cls}>{body}</Link> : <div className={cls}>{body}</div>;
}

export default function SupplierDashboard() {
  const qc = useQueryClient();
  const { data: invite } = useQuery({ queryKey: ['invite'], queryFn: distributorApi.inviteCode });
  const { data: profile } = useQuery({ queryKey: ['my-profile'], queryFn: distributorApi.profile });
  const { data: retailers = [] } = useQuery({ queryKey: ['sup-retailers'], queryFn: distributorApi.retailers });
  const { data: ordersPage } = useQuery({ queryKey: ['sup-orders-all'], queryFn: () => supplierOrderApi.list({ page: 0, size: 200 }) });
  const { data: wa } = useQuery({ queryKey: ['wa-settings'], queryFn: whatsappApi.settings, retry: false });

  const orders = ordersPage?.content || [];
  const rmap = useMemo(() => retailerMap(retailers), [retailers]);
  const retailerName = (o) => rmap.get(o.retailerId)?.shopName || `Retailer #${o.retailerId}`;

  const [copied, setCopied] = useState(false);
  const code = profile?.inviteCode || invite?.inviteCode;

  const copyCode = () => {
    if (code) {
      navigator.clipboard?.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    }
  };

  const move = async (id, status) => {
    await supplierOrderApi.updateStatus(id, { status, note: status });
    qc.invalidateQueries({ queryKey: ['sup-orders-all'] });
  };

  const pending = orders.filter((o) => o.status === 'PENDING');
  const ongoing = orders
    .filter((o) => ONGOING.has(o.status))
    .sort((a, b) => (a.status === b.status ? new Date(a.placedAt) - new Date(b.placedAt) : a.status === 'PENDING' ? -1 : 1));
  const dues = useMemo(() => paymentDues(orders, retailers), [orders, retailers]);
  const totalDue = dues.reduce((s, d) => s + d.amount, 0);

  const today = new Date().toDateString();
  const { todayRevenue, todayProfit } = useMemo(() => {
    let rev = 0;
    let prof = 0;
    orders.forEach((o) => {
      if (!o.placedAt || new Date(o.placedAt).toDateString() !== today) return;
      if (!REVENUE_STATUSES.has(o.status)) return;
      rev += Number(o.totalAmount || 0);
      prof += Number(o.totalAmount || 0) - Number(o.taxAmount || 0);
    });
    return { todayRevenue: rev, todayProfit: prof };
  }, [orders, today]);

  const connected = !!wa?.connected;
  const place = [profile?.address, profile?.city, profile?.state, profile?.pincode].filter(Boolean).join(', ');
  const phones = [profile?.phone, ...(profile?.altPhones ? profile.altPhones.split(/[,\n]/).map((x) => x.trim()).filter(Boolean) : [])];

  return (
    <div className="space-y-xl">
      {/* Distributor details + unique code + WhatsApp status */}
      <div className="bg-primary text-on-primary rounded-2xl shadow-lg p-lg">
        <div className="flex items-start justify-between gap-md flex-wrap">
          <div className="flex items-start gap-md min-w-0">
            <div className="w-14 h-14 rounded-2xl bg-on-primary/15 flex items-center justify-center shrink-0">
              <Icon name="storefront" className="text-[28px]" />
            </div>
            <div className="min-w-0">
              <h2 className="text-headline-lg font-bold">{profile?.displayName || 'Your distribution'}</h2>
              <p className="text-label-md text-primary-fixed-dim">{profile?.ownerName || '—'}</p>
              {phones.length > 0 && (
                <p className="text-label-sm text-primary-fixed-dim mt-xs flex items-center gap-1">
                  <Icon name="call" className="text-[16px]" /> {phones.join(' · ')}
                </p>
              )}
              {place && (
                <p className="text-label-sm text-primary-fixed-dim mt-xs flex items-center gap-1">
                  <Icon name="location_on" className="text-[16px]" /> {place}
                </p>
              )}
            </div>
          </div>

          <div className="flex flex-col items-end gap-sm">
            <Link
              to="/supplier/whatsapp"
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-label-md font-semibold ${
                connected ? 'bg-tertiary-fixed-dim text-on-tertiary-container' : 'bg-error-container text-error'
              }`}
            >
              <span className={`w-2.5 h-2.5 rounded-full ${connected ? 'bg-on-tertiary-container' : 'bg-error'}`} />
              {connected ? 'WhatsApp connected' : 'WhatsApp not connected'}
            </Link>
          </div>
        </div>

        {/* Unique distributor code */}
        <div className="mt-lg pt-md border-t border-on-primary/15 flex items-center justify-between gap-md flex-wrap">
          <div>
            <p className="text-label-sm text-primary-fixed-dim">Your unique distributor code</p>
            <div className="flex items-center gap-md mt-1">
              <span className="text-headline-md font-bold tracking-wide">{code || '...'}</span>
              <button onClick={copyCode} className="bg-on-primary/10 hover:bg-on-primary/20 rounded-lg px-sm py-xs flex items-center gap-1 text-label-sm">
                <Icon name={copied ? 'check' : 'content_copy'} className="text-[16px]" />
                {copied ? 'Copied' : 'Copy'}
              </button>
            </div>
          </div>
          <p className="text-label-sm text-primary-fixed-dim max-w-xs">
            Share this code with retailers — they join you at signup or on WhatsApp by sending “JOIN {code || 'CODE'}”.
          </p>
        </div>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-gutter">
        <Metric title="My Retailers" value={retailers.length} sub="Linked to your code" icon="groups" accent="text-secondary" to="/supplier/retailers" />
        <Metric title="Pending Orders" value={pending.length} sub="Need action" icon="pending_actions" accent="text-error" to="/supplier/orders" />
        <Metric title="Today's Revenue" value={money(todayRevenue)} sub="Fulfilled today" icon="payments" accent="text-tertiary-container" to="/supplier/revenue" />
        <Metric title="Today's Profit" value={money(todayProfit)} sub="Net of GST" icon="savings" accent="text-primary" to="/supplier/revenue" />
      </div>

      {/* Ongoing orders (pending + packed), scrollable */}
      <div className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm overflow-hidden">
        <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between">
          <div>
            <h3 className="text-headline-md text-on-background">Ongoing Orders</h3>
            <p className="text-body-md text-on-surface-variant mt-1">Pending &amp; packed orders</p>
          </div>
          <Link to="/supplier/orders" className="text-label-md text-primary font-semibold hover:underline">View all</Link>
        </div>
        <div className="divide-y divide-surface-variant max-h-[360px] overflow-y-auto">
          {ongoing.length === 0 && <p className="p-lg text-on-surface-variant">No ongoing orders.</p>}
          {ongoing.map((o) => {
            const badge = statusBadge(o.status);
            return (
              <div key={o.id} className="flex items-center justify-between gap-sm px-lg py-md">
                <Link to="/supplier/orders" className="min-w-0 flex-1">
                  <div className="font-semibold text-on-surface">{o.orderNumber}</div>
                  <div className="text-label-sm text-on-surface-variant truncate">
                    {retailerName(o)} · {money(o.totalAmount)} · {formatDate(o.placedAt)}
                  </div>
                </Link>
                {o.status === 'PENDING' ? (
                  <div className="flex gap-sm shrink-0">
                    <button onClick={() => move(o.id, 'REJECTED')} className="px-3 py-1.5 rounded-md border border-outline-variant text-error text-label-sm font-semibold hover:bg-error-container">Reject</button>
                    <button onClick={() => move(o.id, 'ACCEPTED')} className="px-3 py-1.5 rounded-md bg-tertiary-container text-on-tertiary-container text-label-sm font-semibold hover:opacity-90">Accept</button>
                  </div>
                ) : (
                  <span className={`px-sm py-xs rounded-full text-label-sm font-medium shrink-0 ${badge.tone}`}>{badge.label}</span>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Pending payments from retailers, scrollable */}
      <div className="bg-surface-container-lowest rounded-xl border border-surface-variant shadow-sm overflow-hidden">
        <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between gap-sm flex-wrap">
          <div className="flex items-center gap-sm">
            <Icon name="account_balance_wallet" className="text-error" />
            <h3 className="text-headline-md text-on-background">Pending Payments</h3>
          </div>
          <div className="text-right">
            <div className="text-label-sm text-on-surface-variant">Total outstanding</div>
            <div className="font-bold text-error">{money(totalDue)}</div>
          </div>
        </div>
        <div className="divide-y divide-surface-variant max-h-[360px] overflow-y-auto">
          {dues.length === 0 && <p className="p-lg text-on-surface-variant">No pending payments. All settled!</p>}
          {dues.map((d) => (
            <Link
              key={d.retailerId}
              to={`/supplier/retailers/${d.retailerId}`}
              className="flex items-center justify-between gap-sm px-lg py-md hover:bg-surface-container"
            >
              <div className="min-w-0">
                <div className="font-semibold text-on-surface truncate">{d.name}</div>
                <div className="text-label-sm text-on-surface-variant">{d.count} unpaid order{d.count > 1 ? 's' : ''}</div>
              </div>
              <div className="text-right shrink-0">
                <div className="font-bold text-error">{money(d.amount)}</div>
                <div className="text-label-sm text-primary">View history →</div>
              </div>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
