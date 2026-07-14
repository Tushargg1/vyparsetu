import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { distributorApi, supplierOrderApi } from '../../lib/api';
import Icon from '../../components/Icon';
import PageHeader from '../../components/PageHeader';
import { EmptyState } from '../../components/StatePanel';
import { money, retailerStats, timeAgo } from './supplierUtils';

export default function RetailersPage() {
  const qc = useQueryClient();
  const { data: retailers = [] } = useQuery({ queryKey: ['sup-retailers'], queryFn: distributorApi.retailers });
  const { data: ordersPage } = useQuery({
    queryKey: ['sup-orders-all'],
    queryFn: () => supplierOrderApi.list({ page: 0, size: 200 }),
  });
  const orders = ordersPage?.content || [];
  const stats = useMemo(() => retailerStats(orders), [orders]);

  const [form, setForm] = useState({ name: '', phone: '', shopName: '', city: '', address: '', altPhones: '', locationUrl: '' });
  const [msg, setMsg] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState('');
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const add = async () => {
    setMsg('');
    try {
      await distributorApi.addRetailer(form);
      setForm({ name: '', phone: '', shopName: '', city: '', address: '', altPhones: '', locationUrl: '' });
      setMsg('Retailer added. They received a login OTP.');
      qc.invalidateQueries({ queryKey: ['sup-retailers'] });
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not add retailer');
    }
  };

  const visible = retailers.filter((r) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    return [r.shopName, r.ownerName, r.phone, r.city].filter(Boolean).join(' ').toLowerCase().includes(q);
  });

  return (
    <div className="space-y-lg">
      <PageHeader
        icon="groups"
        title="Retailers"
        subtitle={`${retailers.length} linked to your distribution network`}
        action={
          <button onClick={() => setShowForm((v) => !v)} className="ui-button-primary">
            <Icon name={showForm ? 'close' : 'person_add'} className="text-[18px]" />
            {showForm ? 'Close' : 'Add retailer'}
          </button>
        }
      />

      {showForm && (
        <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-sm">
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Owner name" value={form.name} onChange={set('name')} />
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Primary phone" inputMode="numeric" value={form.phone} onChange={set('phone')} />
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Shop name" value={form.shopName} onChange={set('shopName')} />
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="More numbers (comma separated)" value={form.altPhones} onChange={set('altPhones')} />
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="City" value={form.city} onChange={set('city')} />
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary md:col-span-2" placeholder="Address" value={form.address} onChange={set('address')} />
            <input className="border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" placeholder="Shared location link" value={form.locationUrl} onChange={set('locationUrl')} />
          </div>
          <button onClick={add} className="mt-md bg-primary text-on-primary px-lg py-2 rounded-lg text-label-md font-semibold">Send invite</button>
          {msg && <p className="mt-sm text-label-sm text-on-surface-variant">{msg}</p>}
        </div>
      )}

      <div className="relative">
        <Icon name="search" className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[20px]" />
        <input
          className="w-full border border-outline-variant rounded-lg py-2 pl-10 pr-3 outline-none focus:border-primary bg-surface-container-lowest"
          placeholder="Search retailers"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
        {visible.length === 0 && (
          <div className="col-span-full"><EmptyState compact icon="group_off" title="No retailers found" description="Try a different name, phone number, or city." /></div>
        )}
        {visible.map((r) => {
          const s = stats.get(r.retailerId) || { orders: 0, revenue: 0, pending: 0, due: 0, lastAt: null };
          return (
            <Link
              key={r.retailerId}
              to={`/supplier/retailers/${r.retailerId}`}
              className="ui-card p-lg transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-md"
            >
              <div className="flex items-start justify-between gap-sm">
                <div className="flex items-center gap-md min-w-0">
                  <div className="w-11 h-11 rounded-full bg-primary-fixed text-primary flex items-center justify-center font-bold text-headline-md shrink-0">
                    {(r.shopName || r.ownerName || 'R')[0]}
                  </div>
                  <div className="min-w-0">
                    <div className="font-bold text-on-surface truncate">{r.shopName || 'Unnamed shop'}</div>
                    <div className="text-label-sm text-on-surface-variant truncate">{r.ownerName} · {r.phone}</div>
                  </div>
                </div>
                <div className="flex items-center gap-sm shrink-0">
                  <div className="flex flex-col items-end gap-1">
                    {r.creditApproved && (
                      <span className="text-label-sm text-on-tertiary-container bg-tertiary-fixed-dim px-2 py-0.5 rounded-md">Credit</span>
                    )}
                    {s.pending > 0 && (
                      <span className="text-label-sm text-error bg-error-container px-2 py-0.5 rounded-md">{s.pending} pending</span>
                    )}
                  </div>
                  <Icon name="chevron_right" className="text-on-surface-variant" />
                </div>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-sm mt-md pt-md border-t border-surface-variant">
                <Stat label="Orders" value={s.orders} />
                <Stat label="Revenue" value={money(s.revenue)} />
                <Stat label="Outstanding due" value={money(s.due)} due={s.due > 0} />
                <Stat label="Last order" value={timeAgo(s.lastAt)} />
              </div>

              <div className="mt-md flex items-center justify-end gap-1 text-label-sm text-primary font-semibold">
                Click to see full details <Icon name="arrow_forward" className="text-[16px]" />
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}

function Stat({ label, value, due }) {
  return (
    <div>
      <div className={`font-semibold ${due ? 'text-error' : 'text-on-surface'}`}>{value}</div>
      <div className="text-label-sm text-on-surface-variant">{label}</div>
    </div>
  );
}
