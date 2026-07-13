import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { customerPriceApi, productApi } from '../../lib/api';
import Icon from '../../components/Icon';
import { money } from './supplierUtils';

export default function CustomerPricesPanel({ retailerId }) {
  const qc = useQueryClient();
  const { data: prices = [], isLoading } = useQuery({
    queryKey: ['cust-prices', retailerId],
    queryFn: () => customerPriceApi.listForRetailer(retailerId),
    enabled: !!retailerId,
  });
  const { data: productsPage } = useQuery({
    queryKey: ['sup-products-all'],
    queryFn: () => productApi.search({ size: 200 }),
  });
  const products = productsPage?.content || [];
  const productById = useMemo(() => {
    const m = new Map();
    products.forEach((p) => m.set(p.id, p));
    return m;
  }, [products]);

  const [productId, setProductId] = useState('');
  const [price, setPrice] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const refresh = () => qc.invalidateQueries({ queryKey: ['cust-prices', retailerId] });

  const add = async () => {
    if (!productId || !price) { setMsg('Pick a product and price'); return; }
    setBusy(true); setMsg('');
    try {
      await customerPriceApi.upsert({ retailerId, productId: Number(productId), unitPrice: Number(price), active: true });
      await refresh(); setProductId(''); setPrice(''); setMsg('Saved.');
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Could not save'); }
    finally { setBusy(false); }
  };

  const remove = async (id) => {
    setBusy(true);
    try { await customerPriceApi.remove(id); await refresh(); }
    finally { setBusy(false); }
  };

  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm">
      <div className="px-lg py-md border-b border-outline-variant flex items-center gap-sm">
        <Icon name="sell" className="text-primary" />
        <h3 className="text-headline-md text-on-background">Special prices</h3>
        <span className="ml-auto text-label-sm text-on-surface-variant">Override default selling price for this retailer</span>
      </div>

      <div className="p-lg space-y-md">
        {/* add row */}
        <div className="flex items-end gap-sm flex-wrap">
          <label className="flex-1 min-w-[200px]">
            <span className="text-label-sm text-on-surface-variant">Product</span>
            <select value={productId} onChange={(e) => setProductId(e.target.value)}
              className="block w-full mt-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary">
              <option value="">Select a product…</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>{p.name} (default {money(p.sellingPrice)})</option>
              ))}
            </select>
          </label>
          <label className="w-32">
            <span className="text-label-sm text-on-surface-variant">Price (₹)</span>
            <input type="number" min="0" value={price} onChange={(e) => setPrice(e.target.value)}
              className="block w-full mt-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" />
          </label>
          <button onClick={add} disabled={busy}
            className="bg-primary text-on-primary px-lg py-2 rounded-lg text-label-md font-semibold disabled:opacity-50">
            Add
          </button>
        </div>
        {msg && <p className="text-label-md text-on-surface-variant">{msg}</p>}

        {/* list */}
        {isLoading && <p className="text-on-surface-variant text-label-md">Loading…</p>}
        {!isLoading && prices.length === 0 && (
          <p className="text-on-surface-variant text-label-md">No special prices. This retailer pays the default catalog price.</p>
        )}
        <div className="divide-y divide-surface-variant">
          {prices.map((cp) => {
            const p = productById.get(cp.productId);
            return (
              <div key={cp.id} className="flex items-center justify-between py-2">
                <div className="min-w-0">
                  <div className="font-medium text-on-surface truncate">{p?.name || `Product #${cp.productId}`}</div>
                  <div className="text-label-sm text-on-surface-variant">
                    {money(cp.unitPrice)} {p ? `· default ${money(p.sellingPrice)}` : ''}
                  </div>
                </div>
                <button onClick={() => remove(cp.id)} disabled={busy}
                  className="text-error hover:bg-error-container rounded-lg p-1.5" title="Remove">
                  <Icon name="delete" className="text-[20px]" />
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
