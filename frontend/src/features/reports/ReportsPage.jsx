import { useQuery } from '@tanstack/react-query';
import { reportApi } from '../../lib/api';

function Stat({ title, value }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
      <div className="text-label-md text-on-surface-variant">{title}</div>
      <div className="text-display-lg text-primary mt-xs">{value}</div>
    </div>
  );
}

export default function ReportsPage() {
  const { data: sales } = useQuery({ queryKey: ['rpt-sales'], queryFn: reportApi.sales });
  const { data: purchase } = useQuery({ queryKey: ['rpt-purchase'], queryFn: reportApi.purchase });
  const { data: inventory } = useQuery({ queryKey: ['rpt-inv'], queryFn: reportApi.inventory });
  const money = (v) => `₹${Number(v ?? 0).toLocaleString('en-IN')}`;

  return (
    <div className="space-y-md">
      <h2 className="text-headline-md">Reports</h2>
      <div className="grid grid-cols-2 gap-md">
        <Stat title="Sales (30d)" value={money(sales?.salesValue)} />
        <Stat title="Profit (30d)" value={money(sales?.profit)} />
        <Stat title="Purchases (30d)" value={money(purchase?.purchaseValue)} />
        <Stat title="Inventory value" value={money(inventory?.inventoryValueAtCost)} />
      </div>
    </div>
  );
}
