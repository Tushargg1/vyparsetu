import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { analyticsApi, productApi, distributorApi } from '../../lib/api';
import Icon from '../../components/Icon';
import { money } from './supplierUtils';

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

function RankList({ title, icon, rows, emptyText }) {
  const max = Math.max(1, ...rows.map((r) => Number(r.count) || 0));
  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm">
      <div className="px-lg py-md border-b border-outline-variant flex items-center gap-sm">
        <Icon name={icon} className="text-primary" />
        <h3 className="text-headline-md text-on-background">{title}</h3>
      </div>
      <div className="p-lg space-y-sm">
        {rows.length === 0 && <p className="text-on-surface-variant text-label-md">{emptyText}</p>}
        {rows.map((r, i) => (
          <div key={i} className="space-y-1">
            <div className="flex items-center justify-between text-label-md">
              <span className="text-on-surface truncate pr-sm">{r.label}</span>
              <span className="text-on-surface-variant font-semibold shrink-0">{r.count}</span>
            </div>
            <div className="h-2 rounded-full bg-surface-variant overflow-hidden">
              <div className="h-full bg-primary rounded-full" style={{ width: `${(Number(r.count) / max) * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function SupplierAnalyticsPage() {
  const { data: summary, isLoading, isError, refetch } = useQuery({
    queryKey: ['sup-analytics'],
    queryFn: analyticsApi.summary,
  });
  const { data: productsPage } = useQuery({
    queryKey: ['sup-products-all'],
    queryFn: () => productApi.search({ size: 200 }),
  });
  const { data: retailers = [] } = useQuery({ queryKey: ['sup-retailers'], queryFn: distributorApi.retailers });

  const productNames = useMemo(() => {
    const m = new Map();
    (productsPage?.content || []).forEach((p) => m.set(p.id, p.name));
    return m;
  }, [productsPage]);
  const retailerNames = useMemo(() => {
    const m = new Map();
    retailers.forEach((r) => m.set(r.retailerId, r.shopName || r.ownerName || `Retailer #${r.retailerId}`));
    return m;
  }, [retailers]);

  const topProducts = (summary?.topProducts || []).map((r) => ({
    label: productNames.get(r.productId) || `Product #${r.productId}`,
    count: r.count,
  }));
  const repeatCustomers = (summary?.repeatCustomers || []).map((r) => ({
    label: retailerNames.get(r.retailerId) || `Retailer #${r.retailerId}`,
    count: r.count,
  }));
  const topAliases = (summary?.topAliases || []).map((r) => ({ label: r.value, count: r.count }));
  const topFailures = (summary?.topValidationFailures || []).map((r) => ({ label: r.value, count: r.count }));

  return (
    <div className="space-y-lg">
      <div className="flex items-center justify-between flex-wrap gap-sm">
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">Analytics</h2>
          <p className="text-body-md text-on-surface-variant">Ordering metrics across your retailers.</p>
        </div>
        <button onClick={() => refetch()} className="flex items-center gap-1 text-label-md text-primary font-semibold hover:underline">
          <Icon name="refresh" className="text-[18px]" /> Refresh
        </button>
      </div>

      {isLoading && <p className="text-on-surface-variant">Loading analytics…</p>}
      {isError && (
        <div className="bg-error-container text-error rounded-xl p-lg flex items-center justify-between">
          <span>Couldn't load analytics.</span>
          <button onClick={() => refetch()} className="font-semibold underline">Retry</button>
        </div>
      )}

      {summary && (
        <>
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-gutter">
            <Stat title="Average order value" value={money(summary.averageOrderValue)} icon="receipt_long" accent="text-primary" />
            <Stat title="AI extraction failures" value={summary.aiExtractionFailures ?? 0} icon="smart_toy" accent="text-error" />
            <Stat title="Validation failures" value={summary.validationFailures ?? 0} icon="rule" accent="text-tertiary-container" />
          </div>

          <div className="grid lg:grid-cols-2 gap-gutter">
            <RankList title="Frequently ordered products" icon="local_fire_department" rows={topProducts}
              emptyText="No product orders recorded yet." />
            <RankList title="Repeat customers" icon="groups" rows={repeatCustomers}
              emptyText="No repeat orders yet." />
            <RankList title="Most common aliases" icon="sell" rows={topAliases}
              emptyText="No aliases used yet." />
            <RankList title="Most common validation failures" icon="report" rows={topFailures}
              emptyText="No validation failures — great!" />
          </div>
          <p className="text-label-sm text-on-surface-variant flex items-center gap-1">
            <Icon name="info" className="text-[16px]" />
            Metrics accumulate as retailers place orders through WhatsApp and the app.
          </p>
        </>
      )}
    </div>
  );
}
