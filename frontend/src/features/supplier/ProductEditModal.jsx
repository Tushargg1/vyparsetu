import { useState } from 'react';
import { productApi } from '../../lib/api';
import Icon from '../../components/Icon';
import GstSelect from './GstSelect';

/** Edit an existing product: name, brand, prices, GST slab, stock threshold, tracking. */
export default function ProductEditModal({ product, onClose, onSaved }) {
  const [f, setF] = useState({
    name: product.name || '', brand: product.brand || '', barcode: product.barcode || '',
    mrp: product.mrp ?? '', sellingPrice: product.sellingPrice ?? '', gstRate: product.gstRate ?? 0,
    lowStockThreshold: product.lowStockThreshold ?? '', trackStock: !!product.trackStock,
  });
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const save = async () => {
    if (!f.name || f.mrp === '' || f.sellingPrice === '') { setMsg('Name, MRP and selling price are required'); return; }
    setBusy(true); setMsg('');
    try {
      await productApi.update(product.id, {
        name: f.name, brand: f.brand, barcode: f.barcode, sku: product.sku, unit: product.unit,
        mrp: Number(f.mrp), sellingPrice: Number(f.sellingPrice), gstRate: Number(f.gstRate || 0),
        stockQty: Number(product.stockQty || 0),
        lowStockThreshold: Number(f.lowStockThreshold || 0), trackStock: !!f.trackStock,
      });
      onSaved?.();
      onClose();
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Could not save'); }
    finally { setBusy(false); }
  };

  const base = 'border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary w-full';
  const previewTax = Number(f.sellingPrice || 0) * (Number(f.gstRate || 0) / 100);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-margin-mobile">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-surface-container-lowest rounded-2xl shadow-xl w-full max-w-md p-lg space-y-md max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between">
          <h3 className="text-headline-md text-on-surface">Edit product</h3>
          <button onClick={onClose} className="text-on-surface-variant hover:text-on-surface"><Icon name="close" /></button>
        </div>

        <div className="grid grid-cols-2 gap-sm">
          <label className="col-span-2"><span className="text-label-sm text-on-surface-variant">Name</span>
            <input className={base} value={f.name} onChange={set('name')} /></label>
          <label><span className="text-label-sm text-on-surface-variant">Brand</span>
            <input className={base} value={f.brand} onChange={set('brand')} /></label>
          <label><span className="text-label-sm text-on-surface-variant">Barcode</span>
            <input className={base} value={f.barcode} onChange={set('barcode')} /></label>
          <label><span className="text-label-sm text-on-surface-variant">MRP (₹)</span>
            <input className={base} inputMode="decimal" value={f.mrp} onChange={set('mrp')} /></label>
          <label><span className="text-label-sm text-on-surface-variant">Selling price (₹)</span>
            <input className={base} inputMode="decimal" value={f.sellingPrice} onChange={set('sellingPrice')} /></label>
          <label><span className="text-label-sm text-on-surface-variant">GST</span>
            <GstSelect value={f.gstRate} onChange={(v) => setF({ ...f, gstRate: v })} /></label>
          <label><span className="text-label-sm text-on-surface-variant">Low-stock alert at</span>
            <input className={base} inputMode="decimal" value={f.lowStockThreshold} onChange={set('lowStockThreshold')} /></label>
        </div>

        <label className="flex items-center gap-sm text-label-md text-on-surface">
          <input type="checkbox" checked={f.trackStock} onChange={(e) => setF({ ...f, trackStock: e.target.checked })} />
          Track stock for this product
        </label>

        <div className="text-label-sm text-on-surface-variant bg-surface-container rounded-lg p-sm">
          GST is added on top of the selling price. At {Number(f.gstRate || 0)}%, a {`₹${Number(f.sellingPrice || 0)}`} item adds
          {` ₹${previewTax.toFixed(2)}`} tax → {`₹${(Number(f.sellingPrice || 0) + previewTax).toFixed(2)}`} per unit.
        </div>

        {msg && <p className="text-label-md text-error">{msg}</p>}
        <div className="flex justify-end gap-sm">
          <button onClick={onClose} className="px-lg py-2.5 rounded-xl text-label-md font-semibold text-on-surface hover:bg-surface-container">Cancel</button>
          <button onClick={save} disabled={busy} className="bg-primary text-on-primary px-lg py-2.5 rounded-xl text-label-md font-semibold disabled:opacity-50">
            {busy ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
}
