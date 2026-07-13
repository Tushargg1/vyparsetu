import { useEffect, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { salesApi } from '../../lib/api';
import Icon from '../../components/Icon';

// Apply selected discount percents to MRP (additive, capped at 100%).
function priceAfter(mrp, percents) {
  const total = Math.min(100, percents.reduce((s, p) => s + Number(p), 0));
  return Math.round(Number(mrp) * (1 - total / 100) * 100) / 100;
}

function DiscountManager({ discounts, refetch }) {
  const [label, setLabel] = useState('');
  const [percent, setPercent] = useState('');

  const add = async () => {
    if (!label.trim() || !Number(percent)) return;
    await salesApi.addDiscount(label.trim(), Number(percent));
    setLabel('');
    setPercent('');
    refetch();
  };
  const del = async (id) => { await salesApi.deleteDiscount(id); refetch(); };

  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant p-lg">
      <h3 className="text-headline-md mb-sm flex items-center gap-sm"><Icon name="percent" className="text-secondary" /> Discounts</h3>
      <p className="text-label-sm text-on-surface-variant mb-md">Create reusable discounts, then apply one or more to a product's MRP.</p>
      <div className="flex flex-wrap gap-sm mb-md">
        {discounts.map((d) => (
          <span key={d.id} className="inline-flex items-center gap-1 bg-secondary-fixed-dim text-on-secondary-fixed px-3 py-1.5 rounded-full text-label-md">
            {d.label} · {Number(d.percent)}%
            <button onClick={() => del(d.id)} className="ml-1"><Icon name="close" className="text-[16px]" /></button>
          </span>
        ))}
        {discounts.length === 0 && <span className="text-on-surface-variant text-label-md">No discounts yet.</span>}
      </div>
      <div className="flex gap-sm">
        <input className="flex-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Label (e.g. Festive)" value={label} onChange={(e) => setLabel(e.target.value)} />
        <input className="w-24 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="%" inputMode="decimal" value={percent} onChange={(e) => setPercent(e.target.value)} />
        <button onClick={add} className="bg-secondary text-on-secondary px-md rounded-lg font-semibold">Add</button>
      </div>
    </div>
  );
}

function RateRow({ item, discounts, onSave, saved }) {
  const [price, setPrice] = useState(item.myPrice ?? item.mrp ?? '');
  const [selected, setSelected] = useState([]); // discount ids

  const toggle = (d) => {
    setSelected((prev) => {
      const next = prev.includes(d.id) ? prev.filter((x) => x !== d.id) : [...prev, d.id];
      const percents = discounts.filter((x) => next.includes(x.id)).map((x) => x.percent);
      setPrice(priceAfter(item.mrp, percents));
      return next;
    });
  };

  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-md">
      <div className="flex items-center gap-md">
        <div className="flex-1 min-w-0">
          <p className="font-semibold text-on-surface line-clamp-1">{item.productName}</p>
          <p className="text-label-sm text-on-surface-variant">
            {item.brand} · MRP ₹{item.mrp} · Distributor ₹{item.distributorPrice} · {String(item.inStock)} in stock
          </p>
        </div>
        <div className="flex items-center gap-1">
          <span className="text-on-surface-variant">₹</span>
          <input
            className="w-24 border border-outline-variant rounded-lg py-2 px-2 text-right outline-none focus:border-primary"
            inputMode="decimal"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
          />
        </div>
        <button onClick={() => onSave(item.productId, price)} className="bg-primary text-on-primary px-md py-2 rounded-lg font-semibold">
          {saved ? <Icon name="check" /> : 'Save'}
        </button>
      </div>
      {discounts.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mt-sm">
          {discounts.map((d) => (
            <button
              key={d.id}
              onClick={() => toggle(d)}
              className={`px-2.5 py-1 rounded-full text-label-sm border ${
                selected.includes(d.id) ? 'bg-secondary text-on-secondary border-secondary' : 'border-outline-variant text-on-surface-variant'
              }`}
            >
              {d.label} {Number(d.percent)}%
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function RateListPage() {
  const qc = useQueryClient();
  const { data = [], isLoading, refetch } = useQuery({ queryKey: ['rate-list'], queryFn: salesApi.rateList });
  const { data: discounts = [], refetch: refetchD } = useQuery({ queryKey: ['discounts'], queryFn: salesApi.discounts });
  const [savedId, setSavedId] = useState(null);
  const [q, setQ] = useState('');

  const filtered = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) return data;
    return data.filter((r) => (r.productName || '').toLowerCase().includes(term) || (r.brand || '').toLowerCase().includes(term));
  }, [data, q]);

  const save = async (productId, price) => {
    const p = Number(price);
    if (!p || p <= 0) return;
    await salesApi.setRate(productId, p);
    setSavedId(productId);
    setTimeout(() => setSavedId(null), 1200);
    refetch();
  };

  return (
    <div className="space-y-lg max-w-3xl">
      <div>
        <h2 className="text-headline-lg font-bold text-on-background">Set Rate List</h2>
        <p className="text-body-md text-on-surface-variant mt-xs">Prices default to MRP. Apply discounts or edit the price. Sales use this price.</p>
      </div>

      <DiscountManager discounts={discounts} refetch={refetchD} />

      <div className="relative">
        <Icon name="search" className="absolute left-4 top-1/2 -translate-y-1/2 text-outline text-[24px]" />
        <input
          className="w-full pl-12 pr-4 py-3 bg-surface-container-lowest border border-outline-variant rounded-2xl outline-none focus:border-primary"
          placeholder="Search by product or brand"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
      </div>

      {isLoading && <p className="text-on-surface-variant">Loading…</p>}
      {!isLoading && data.length === 0 && <p className="text-on-surface-variant">No stock yet — products appear here once you have inventory.</p>}
      {!isLoading && data.length > 0 && filtered.length === 0 && <p className="text-on-surface-variant">No products match “{q}”.</p>}

      <div className="space-y-sm">
        {filtered.map((r) => (
          <RateRow key={r.productId} item={r} discounts={discounts} onSave={save} saved={savedId === r.productId} />
        ))}
      </div>
    </div>
  );
}
