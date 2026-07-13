import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { dashboardApi, salesApi, orderApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import Icon from '../../components/Icon';

const money = (v) => `₹${Number(v ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
const moneyShort = (v) => `₹${Number(v ?? 0).toLocaleString('en-IN')}`;

const isToday = (value) => {
  if (!value) return false;
  const d = new Date(value);
  const n = new Date();
  return d.getFullYear() === n.getFullYear() && d.getMonth() === n.getMonth() && d.getDate() === n.getDate();
};

const time = (value) =>
  value ? new Date(value).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' }) : '';

/** Big gradient hero metric. */
function Hero({ label, value, icon, gradient, sub }) {
  return (
    <div className={`relative overflow-hidden rounded-2xl p-lg text-white shadow-sm ${gradient}`}>
      <div className="relative z-10">
        <span className="text-label-sm uppercase tracking-wide opacity-80">{label}</span>
        <div className="text-display-lg font-bold mt-sm leading-none">{value}</div>
        {sub && <p className="text-label-sm opacity-80 mt-sm">{sub}</p>}
      </div>
      <span className="absolute top-lg right-lg w-12 h-12 rounded-2xl bg-white/15 flex items-center justify-center z-10">
        <Icon name={icon} className="text-[26px]" />
      </span>
      <span className="absolute -bottom-6 -right-6 w-32 h-32 rounded-full bg-white/10" />
    </div>
  );
}

/** Compact stat tile. */
function Stat({ label, value, icon, accent, onClick }) {
  return (
    <button
      onClick={onClick}
      disabled={!onClick}
      className={`bg-surface-container-lowest rounded-2xl border border-surface-variant p-md flex items-center gap-md shadow-sm text-left w-full ${onClick ? 'hover:border-primary hover:shadow-md transition-all' : ''}`}
    >
      <span className={`w-12 h-12 rounded-full flex items-center justify-center shrink-0 ${accent}`}>
        <Icon name={icon} className="text-[24px]" />
      </span>
      <div className="min-w-0">
        <p className="text-label-sm text-on-surface-variant uppercase tracking-wide truncate">{label}</p>
        <p className="text-headline-md font-bold text-on-surface truncate">{value}</p>
      </div>
    </button>
  );
}

export default function RetailerDashboard() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  const { data, isLoading } = useQuery({ queryKey: ['retailer-dashboard'], queryFn: dashboardApi.retailer });
  const { data: sales = [] } = useQuery({
    queryKey: ['sales-history-all'],
    queryFn: () => salesApi.history({ page: 0, size: 200 }),
  });
  const { data: ordersPage } = useQuery({
    queryKey: ['my-orders-count'],
    queryFn: () => orderApi.mine({ page: 0, size: 1 }),
  });

  if (isLoading) return <p className="text-on-surface-variant">Loading…</p>;
  const d = data || {};

  const placedOrders = ordersPage?.totalElements ?? (ordersPage?.content?.length || 0);
  const lowCount = (d.lowStock || []).length;
  const todaysBills = (sales || []).filter((s) => isToday(s.createdAt));

  return (
    <div className="space-y-lg">
      {/* Greeting */}
      <div>
        <h2 className="text-headline-lg font-bold text-on-background">Hello, {user?.name || 'there'}</h2>
        <p className="text-body-md text-on-surface-variant mt-xs">Here's what's happening with your store today.</p>
      </div>

      {/* Hero metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
        <Hero
          label="Total sales today"
          value={money(d.todaySales)}
          icon="trending_up"
          gradient="bg-gradient-to-br from-[#5b53f0] to-[#8b5cf6]"
          sub="From your counter sales today"
        />
        <Hero
          label="Profit today"
          value={money(d.todayProfit)}
          icon="savings"
          gradient="bg-gradient-to-br from-primary to-primary-container"
          sub="Sales minus cost of goods"
        />
      </div>

      {/* Stat tiles */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-gutter">
        <Stat
          label="Placed orders"
          value={`${placedOrders} ${placedOrders === 1 ? 'order' : 'orders'}`}
          icon="receipt_long"
          accent="bg-secondary-fixed text-secondary"
          onClick={() => navigate('/orders')}
        />
        <Stat
          label="Pending to pay"
          value={moneyShort(d.pendingPaymentTotal)}
          icon="account_balance_wallet"
          accent="bg-error-container text-error"
          onClick={() => navigate('/orders')}
        />
        <Stat
          label="Low stock"
          value={`${lowCount} ${lowCount === 1 ? 'item' : 'items'}`}
          icon="warning"
          accent="bg-primary-fixed text-primary"
          onClick={() => navigate('/inventory')}
        />
        <Stat
          label="Bills today"
          value={`${todaysBills.length}`}
          icon="point_of_sale"
          accent="bg-tertiary-fixed-dim text-on-tertiary-container"
        />
      </div>

      {/* AI insights — coming soon */}
      <button
        onClick={() => navigate('/insights')}
        className="w-full text-left rounded-2xl border border-surface-variant bg-gradient-to-r from-secondary-fixed to-primary-fixed p-lg flex items-center gap-md hover:shadow-md transition-all"
      >
        <span className="w-12 h-12 rounded-2xl bg-secondary text-on-secondary flex items-center justify-center shrink-0">
          <Icon name="auto_awesome" className="text-[26px]" />
        </span>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-sm flex-wrap">
            <h3 className="text-headline-md text-on-surface">AI insights to boost your sales</h3>
            <span className="text-label-sm font-semibold bg-secondary text-on-secondary px-2 py-0.5 rounded-full">Coming soon</span>
          </div>
          <p className="text-label-md text-on-surface-variant mt-xs">
            Smart suggestions on what to stock and sell more. Stay alerted, we'll switch this on soon.
          </p>
        </div>
        <Icon name="chevron_right" className="text-on-surface-variant" />
      </button>

      {/* Today's bills */}
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm overflow-hidden">
        <div className="px-lg py-md border-b border-outline-variant flex items-center justify-between gap-sm">
          <div>
            <h3 className="text-headline-md text-on-surface">Today's sales</h3>
            <p className="text-label-sm text-on-surface-variant">{todaysBills.length} bills · {money(d.todaySales)}</p>
          </div>
          <button
            onClick={() => navigate('/revenue')}
            className="flex items-center gap-1 text-primary text-label-md font-semibold hover:underline"
          >
            View all <Icon name="chevron_right" className="text-[18px]" />
          </button>
        </div>

        {todaysBills.length === 0 ? (
          <div className="flex flex-col items-center justify-center text-center py-2xl text-on-surface-variant">
            <Icon name="point_of_sale" className="text-[40px] opacity-40 mb-sm" />
            <p className="text-label-md">No sales yet today.</p>
            <button onClick={() => navigate('/scan')} className="mt-md bg-primary text-on-primary px-lg py-2.5 rounded-xl text-label-md font-semibold">
              Start a sale
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="text-label-sm text-on-surface-variant uppercase tracking-wide border-b border-surface-variant">
                  <th className="px-lg py-sm font-semibold">Bill</th>
                  <th className="px-lg py-sm font-semibold">Items</th>
                  <th className="px-lg py-sm font-semibold text-right">Total</th>
                  <th className="px-lg py-sm font-semibold">Time</th>
                  <th className="px-lg py-sm font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-variant">
                {todaysBills.map((s) => (
                  <tr key={s.id} className="hover:bg-surface-container-low">
                    <td className="px-lg py-md font-semibold text-primary">#BILL-{String(s.id).padStart(4, '0')}</td>
                    <td className="px-lg py-md text-on-surface-variant">{(s.items || []).length} items · {String(s.totalItems ?? 0)} units</td>
                    <td className="px-lg py-md text-right font-bold text-on-surface">{money(s.totalAmount)}</td>
                    <td className="px-lg py-md text-on-surface-variant">{time(s.createdAt)}</td>
                    <td className="px-lg py-md">
                      <span className="inline-flex items-center gap-1 text-label-sm font-semibold bg-tertiary-fixed-dim text-on-tertiary-container px-3 py-1 rounded-full">
                        <Icon name="check_circle" className="text-[14px]" /> Completed
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
