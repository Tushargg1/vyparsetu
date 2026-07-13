import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { orderApi, productApi } from '../../lib/api';
import Icon from '../../components/Icon';
import { money } from './supplierUtils';

/**
 * Edit the line items of an order (add / remove / change qty).
 * Only usable before packing — the caller decides when to show it.
 */
export default function OrderModifyModal({ order, onClose, onSaved }) {
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

  const [lines, setLines] = useState(
    () => (order.items || []).map((it) => ({ productId: it.productId, quantity: Number(it.quantity) })),
  );
  const [addId, setAddId] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const setQty = (productId, qty) =>
    setLines((ls) => ls.map((l) => (l.productId === productId ? { ...l, quantity: qty } : l)));
  const removeLine = (productId) => setLines((ls) => ls.filter((l) => l.productId !== productId));
  const addLine = () => {
    const id = Number(addId);
    if (!id || lines.some((l) => l.productId === id)) return;
    setLines((ls) => [...ls, { productId: id, quantity: 1 }]);
    setAddId('');
  };

  const estTotal = lines.reduce((sum, l) => {
    const p = productById.get(l.productId);
    return sum + (p ? Number(p.sellingPrice) * Number(l.quantity || 0) : 0);
  }, 0);

  const save = async () => {
    const items = lines
      .filter((l) => Number(l.quantity) > 0)
      .map((l) => ({ productId: l.productId, quantity: Number(l.quantity) }));
    if (items.length === 0) { setMsg('Keep at least one item, or cancel the order instead.'); return; }
    setBusy(true); setMsg('');
    try {
      await orderApi.modify(order.id, items);
      onSaved?.();
      onClose();
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not modify order');
    } finally { setBusy(false); }
  };

  const available = products.filter((p) => !lines.some((l) => l.productId === p.id));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-margin-mobile">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-surface-container-lowest rounded-2xl shadow-xl w-full max-w-lg p-lg space-y-md max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between">
          <h3 className="text-headline-md text-on-surface">Edit order {order.orderNumber}</h3>
          <button onClick={onClose} className="text-on-surface-variant hover:text-on-surface"><Icon name="close" /></button>
        </div>
        <p className="text-label-sm text-on-surface-variant">
          Prices are recalculated by the system (including any special price for this retailer).
        </p>

        <div className="divide-y divide-surface-variant">
          {lines.map((l) => {
            const p = productById.get(l.productId);
            return (
              <div key={l.productId} className="flex items-center gap-sm py-2">
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-on-surface truncate">{p?.name || `Product #${l.productId}`}</div>
                  {p && <div className="text-label-sm text-on-surface-variant">{money(p.sellingPrice)} each</div>}
                </div>
                <input type="number" min="0" value={l.quantity}
                  onChange={(e) => setQty(l.productId, e.target.value)}
                  className="w-20 border border-outline-variant rounded-lg py-1.5 px-2 text-right outline-none focus:border-primary" />
                <button onClick={() => removeLine(l.productId)} className="text-error hover:bg-error-container rounded-lg p-1.5">
                  <Icon name="delete" className="text-[20px]" />
                </button>
              </div>
            );
          })}
          {lines.length === 0 && <p className="py-2 text-on-surface-variant text-label-md">No items. Add one below.</p>}
        </div>

        <div className="flex items-end gap-sm">
          <label className="flex-1">
            <span className="text-label-sm text-on-surface-variant">Add product</span>
            <select value={addId} onChange={(e) => setAddId(e.target.value)}
              className="block w-full mt-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary">
              <option value="">Select…</option>
              {available.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          </label>
          <button onClick={addLine} className="px-lg py-2 rounded-lg border border-outline-variant text-label-md font-semibold hover:bg-surface-container">
            Add
          </button>
        </div>

        <div className="flex items-center justify-between border-t border-surface-variant pt-md">
          <span className="text-label-md text-on-surface-variant">Estimated (pre-tax)</span>
          <span className="font-bold text-on-surface">{money(estTotal)}</span>
        </div>
        {msg && <p className="text-label-md text-error">{msg}</p>}

        <div className="flex justify-end gap-sm">
          <button onClick={onClose} className="px-lg py-2.5 rounded-xl text-label-md font-semibold text-on-surface hover:bg-surface-container">Cancel</button>
          <button onClick={save} disabled={busy} className="bg-primary text-on-primary px-lg py-2.5 rounded-xl text-label-md font-semibold disabled:opacity-50">
            {busy ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </div>
    </div>
  );
}
