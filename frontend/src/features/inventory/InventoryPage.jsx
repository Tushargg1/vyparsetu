import { useQuery } from '@tanstack/react-query';
import { inventoryApi } from '../../lib/api';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';

export default function InventoryPage() {
  const { data: items = [], isLoading } = useQuery({ queryKey: ['inventory'], queryFn: inventoryApi.list });
  const { data: expiring = [] } = useQuery({ queryKey: ['expiring'], queryFn: () => inventoryApi.expiring(30) });

  const today = new Date();
  const expired = expiring.filter((e) => e.expiryDate && new Date(e.expiryDate) < today);
  const soon = expiring.filter((e) => !e.expiryDate || new Date(e.expiryDate) >= today);

  return (
    <div className="space-y-lg">
      <PageHeader icon="inventory_2" title="Inventory" subtitle="Track your stock and watch expiry dates." />

      {(expired.length > 0 || soon.length > 0) && (
        <div className="bg-error-container/30 border border-error-container rounded-2xl p-lg">
          <div className="flex items-center gap-sm mb-md text-error">
            <Icon name="warning" filled />
            <h3 className="text-headline-md">Expiry alerts</h3>
          </div>
          <div className="space-y-sm">
            {expired.map((e, i) => (
              <div key={`x${i}`} className="bg-surface-container-lowest rounded-xl border border-error p-md flex justify-between items-center">
                <div>
                  <p className="font-semibold text-on-surface">Product #{e.productId} {e.batchNumber ? `· ${e.batchNumber}` : ''}</p>
                  <p className="text-label-sm text-error">Expired on {e.expiryDate}</p>
                </div>
                <span className="text-label-sm font-bold text-on-error-container bg-error-container px-2 py-1 rounded-md">Qty {String(e.quantity)}</span>
              </div>
            ))}
            {soon.map((e, i) => (
              <div key={`s${i}`} className="bg-surface-container-lowest rounded-xl border border-surface-variant p-md flex justify-between items-center">
                <div>
                  <p className="font-semibold text-on-surface">Product #{e.productId} {e.batchNumber ? `· ${e.batchNumber}` : ''}</p>
                  <p className="text-label-sm text-on-surface-variant">Expires {e.expiryDate}</p>
                </div>
                <span className="text-label-sm font-bold px-2 py-1 rounded-md bg-secondary-fixed-dim text-on-secondary-fixed">Qty {String(e.quantity)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <h3 className="text-headline-md">All stock</h3>
      {isLoading && <p className="text-on-surface-variant">Loading…</p>}
      {!isLoading && items.length === 0 && (
        <p className="text-on-surface-variant">No stock yet. It builds up as your orders are delivered.</p>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-sm">
        {items.map((it) => (
          <div key={it.id} className="bg-surface-container-lowest rounded-xl border border-surface-variant p-md flex justify-between items-center">
            <div>
              <div className="font-semibold text-on-surface">Product #{it.productId}</div>
              <div className="text-label-sm text-on-surface-variant">In stock: {String(it.quantity)}</div>
            </div>
            {it.lowStock && <span className="text-label-sm text-on-error-container bg-error-container px-2 py-1 rounded-md">Low</span>}
          </div>
        ))}
      </div>
    </div>
  );
}
