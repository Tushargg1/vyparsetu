import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { productApi } from '../../lib/api';
import Icon from '../../components/Icon';
import GstSelect from './GstSelect';
import ProductEditModal from './ProductEditModal';

const EMPTY = { name: '', brand: '', barcode: '', mrp: '', sellingPrice: '', gstRate: 0, stockQty: '', lowStockThreshold: '', trackStock: true };

export default function SupplierStockPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ['my-products'], queryFn: () => productApi.search({ page: 0, size: 100 }) });
  const products = data?.content || [];

  const [form, setForm] = useState(EMPTY);
  const [msg, setMsg] = useState('');
  const [lowOnly, setLowOnly] = useState(false);
  const [restock, setRestock] = useState(null); // { product, qty }
  const [restockBusy, setRestockBusy] = useState(false);
  const [editProduct, setEditProduct] = useState(null);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const lowCount = products.filter((p) => p.trackStock && p.lowStock).length;
  const shown = lowOnly ? products.filter((p) => p.trackStock && p.lowStock) : products;

  const submitRestock = async () => {
    const add = Number(restock.qty);
    if (!add || add <= 0) return;
    setRestockBusy(true);
    try {
      const p = restock.product;
      await productApi.update(p.id, {
        name: p.name, brand: p.brand, barcode: p.barcode, sku: p.sku, unit: p.unit,
        mrp: Number(p.mrp || 0), sellingPrice: Number(p.sellingPrice || 0), gstRate: Number(p.gstRate || 0),
        stockQty: Number(p.stockQty || 0) + add,
        lowStockThreshold: Number(p.lowStockThreshold || 0), trackStock: true,
      });
      await qc.invalidateQueries({ queryKey: ['my-products'] });
      setRestock(null);
    } finally { setRestockBusy(false); }
  };

  const add = async () => {
    setMsg('');
    try {
      await productApi.create({
        name: form.name,
        brand: form.brand,
        barcode: form.barcode,
        mrp: Number(form.mrp || 0),
        sellingPrice: Number(form.sellingPrice || 0),
        gstRate: Number(form.gstRate || 0),
        stockQty: Number(form.stockQty || 0),
        lowStockThreshold: Number(form.lowStockThreshold || 0),
        trackStock: !!form.trackStock,
      });
      setForm(EMPTY);
      setMsg('Product added.');
      qc.invalidateQueries({ queryKey: ['my-products'] });
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not add product');
    }
  };

  return (
    <div className="space-y-lg">
      <div className="flex items-center justify-between flex-wrap gap-sm">
        <h2 className="text-headline-lg font-bold text-on-background">Stock / Products</h2>
        <button onClick={() => setLowOnly((v) => !v)}
          className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-label-md font-semibold border transition-colors ${
            lowOnly ? 'bg-error text-on-error border-error' : 'border-outline-variant text-on-surface hover:bg-surface-container'}`}>
          <Icon name="warning" className="text-[18px]" />
          {lowOnly ? 'Showing low stock' : `Needs restock${lowCount ? ` (${lowCount})` : ''}`}
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
        <h3 className="text-headline-md mb-md flex items-center gap-sm"><Icon name="add_box" className="text-secondary" /> Add a product</h3>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-sm">
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary col-span-2 md:col-span-1" placeholder="Name" value={form.name} onChange={set('name')} />
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Brand" value={form.brand} onChange={set('brand')} />
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Barcode" value={form.barcode} onChange={set('barcode')} />
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="MRP" inputMode="decimal" value={form.mrp} onChange={set('mrp')} />
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Selling price" inputMode="decimal" value={form.sellingPrice} onChange={set('sellingPrice')} />
          <GstSelect value={form.gstRate} onChange={(v) => setForm({ ...form, gstRate: v })} />
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Stock qty" inputMode="decimal" value={form.stockQty} onChange={set('stockQty')} />
          <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Low-stock alert at" inputMode="decimal" value={form.lowStockThreshold} onChange={set('lowStockThreshold')} />
          <label className="flex items-center gap-sm text-label-md text-on-surface-variant px-1">
            <input type="checkbox" checked={form.trackStock} onChange={(e) => setForm({ ...form, trackStock: e.target.checked })} />
            Track stock
          </label>
        </div>
        <button onClick={add} className="mt-md bg-primary text-on-primary px-lg py-2 rounded-lg font-semibold">Add product</button>
        {msg && <p className="mt-sm text-label-md text-on-surface-variant">{msg}</p>}
      </div>

      {isLoading && <p className="text-on-surface-variant">Loading…</p>}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-sm">
        {shown.map((p) => (
          <div key={p.id} className={`bg-surface-container-lowest rounded-xl border p-md flex justify-between items-center gap-sm ${p.lowStock ? 'border-error/40' : 'border-surface-variant'}`}>
            <div className="min-w-0">
              <p className="font-semibold text-on-surface truncate">{p.name}</p>
              <p className="text-label-sm text-on-surface-variant">{p.brand} · GST {String(p.gstRate)}%</p>
              {p.trackStock ? (
                <p className="text-label-sm mt-0.5">
                  <span className={p.lowStock ? 'text-error font-semibold' : 'text-on-surface-variant'}>
                    {String(p.availableQty)} available
                  </span>
                  {Number(p.reservedQty) > 0 && <span className="text-on-surface-variant"> · {String(p.reservedQty)} reserved</span>}
                  {p.lowStock && <span className="ml-1 text-error">· low</span>}
                </p>
              ) : (
                <p className="text-label-sm text-on-surface-variant mt-0.5">Stock not tracked</p>
              )}
            </div>
            <div className="flex flex-col items-end gap-1 shrink-0">
              <span className="font-bold text-primary">₹{p.sellingPrice}</span>
              <span className="text-label-sm text-on-surface-variant">{String(p.gstRate)}% GST</span>
              <div className="flex items-center gap-sm">
                <button onClick={() => setEditProduct(p)} className="text-label-sm font-semibold text-on-surface-variant hover:text-on-surface flex items-center gap-0.5">
                  <Icon name="edit" className="text-[16px]" /> Edit
                </button>
                {p.trackStock && (
                  <button onClick={() => setRestock({ product: p, qty: '' })}
                    className="text-label-sm font-semibold text-primary hover:underline flex items-center gap-0.5">
                    <Icon name="add" className="text-[16px]" /> Restock
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
        {!isLoading && shown.length === 0 && (
          <p className="text-on-surface-variant">{lowOnly ? 'Nothing needs restocking right now.' : 'No products yet. Add your first above.'}</p>
        )}
      </div>

      {restock && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-margin-mobile">
          <div className="absolute inset-0 bg-black/40" onClick={() => setRestock(null)} />
          <div className="relative bg-surface-container-lowest rounded-2xl shadow-xl w-full max-w-sm p-lg space-y-md">
            <div className="flex items-center justify-between">
              <h3 className="text-headline-md text-on-surface">Restock</h3>
              <button onClick={() => setRestock(null)} className="text-on-surface-variant hover:text-on-surface"><Icon name="close" /></button>
            </div>
            <p className="text-label-md text-on-surface-variant">
              <span className="font-semibold text-on-surface">{restock.product.name}</span> — currently {String(restock.product.stockQty)} on hand.
            </p>
            <label className="block">
              <span className="text-label-md text-on-surface-variant">Add quantity</span>
              <input type="number" min="1" autoFocus value={restock.qty} onChange={(e) => setRestock({ ...restock, qty: e.target.value })}
                className="block w-full mt-1 border border-outline-variant rounded-lg py-2.5 px-3 outline-none focus:border-primary text-headline-md font-bold" />
            </label>
            <div className="flex justify-end gap-sm">
              <button onClick={() => setRestock(null)} className="px-lg py-2.5 rounded-xl text-label-md font-semibold text-on-surface hover:bg-surface-container">Cancel</button>
              <button onClick={submitRestock} disabled={restockBusy} className="bg-primary text-on-primary px-lg py-2.5 rounded-xl text-label-md font-semibold disabled:opacity-50">
                {restockBusy ? 'Saving…' : 'Add stock'}
              </button>
            </div>
          </div>
        </div>
      )}

      {editProduct && (
        <ProductEditModal
          product={editProduct}
          onClose={() => setEditProduct(null)}
          onSaved={() => qc.invalidateQueries({ queryKey: ['my-products'] })}
        />
      )}
    </div>
  );
}
