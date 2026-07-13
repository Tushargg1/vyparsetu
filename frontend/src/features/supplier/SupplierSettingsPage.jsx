import { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { policyApi } from '../../lib/api';
import Icon from '../../components/Icon';

const OOS_OPTIONS = [
  ['REJECT', 'Reject order', 'Block items that exceed available stock'],
  ['PARTIAL', 'Partial fulfilment', 'Cap quantities to what is in stock'],
  ['BACKORDER', 'Allow backorder', 'Accept full quantity, fulfil later'],
];

function Field({ label, hint, children }) {
  return (
    <label className="block space-y-1">
      <span className="text-label-md font-medium text-on-surface">{label}</span>
      {children}
      {hint && <span className="block text-label-sm text-on-surface-variant">{hint}</span>}
    </label>
  );
}

export default function SupplierSettingsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ['sup-policy'], queryFn: policyApi.get });
  const [form, setForm] = useState(null);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  useEffect(() => { if (data) setForm({ ...data }); }, [data]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const save = async () => {
    setBusy(true); setMsg('');
    try {
      await policyApi.update({
        outOfStockBehavior: form.outOfStockBehavior,
        minOrderValue: Number(form.minOrderValue) || 0,
        minOrderQty: Number(form.minOrderQty) || 0,
        autoCancelHours: Number(form.autoCancelHours) || 0,
        enforceCreditLimit: !!form.enforceCreditLimit,
        creditOverLimitAction: form.creditOverLimitAction,
        largeOrderThreshold: Number(form.largeOrderThreshold) || 0,
        allowOrderingWithoutApproval: !!form.allowOrderingWithoutApproval,
      });
      await qc.invalidateQueries({ queryKey: ['sup-policy'] });
      setMsg('Saved.');
    } catch (e) {
      setMsg(e.response?.data?.error?.message || 'Could not save settings');
    } finally { setBusy(false); }
  };

  if (isLoading || !form) return <p className="text-on-surface-variant">Loading settings…</p>;

  const inputCls = 'block w-full mt-1 border border-outline-variant rounded-lg py-2.5 px-3 outline-none focus:border-primary text-body-md';

  return (
    <div className="space-y-lg max-w-3xl">
      <div>
        <h2 className="text-headline-lg font-bold text-on-background">Ordering rules</h2>
        <p className="text-body-md text-on-surface-variant">How orders from your retailers are validated and processed.</p>
      </div>

      {/* Out of stock behaviour */}
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm p-lg space-y-md">
        <h3 className="text-headline-md text-on-background">When stock is short</h3>
        <div className="grid sm:grid-cols-3 gap-sm">
          {OOS_OPTIONS.map(([val, title, desc]) => (
            <button key={val} onClick={() => set('outOfStockBehavior', val)}
              className={`text-left rounded-xl border p-md transition-colors ${
                form.outOfStockBehavior === val
                  ? 'border-primary bg-primary-fixed'
                  : 'border-outline-variant hover:bg-surface-container'
              }`}>
              <div className="flex items-center gap-1 font-semibold text-on-surface">
                {form.outOfStockBehavior === val && <Icon name="check_circle" className="text-primary text-[18px]" />}
                {title}
              </div>
              <div className="text-label-sm text-on-surface-variant mt-1">{desc}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Minimums + large order */}
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm p-lg grid sm:grid-cols-2 gap-md">
        <Field label="Minimum order value (₹)" hint="0 = no minimum">
          <input type="number" min="0" value={form.minOrderValue} onChange={(e) => set('minOrderValue', e.target.value)} className={inputCls} />
        </Field>
        <Field label="Minimum order quantity" hint="0 = no minimum">
          <input type="number" min="0" value={form.minOrderQty} onChange={(e) => set('minOrderQty', e.target.value)} className={inputCls} />
        </Field>
        <Field label="Large-order alert threshold (₹)" hint="Notify me for orders at/above this value. 0 = off">
          <input type="number" min="0" value={form.largeOrderThreshold} onChange={(e) => set('largeOrderThreshold', e.target.value)} className={inputCls} />
        </Field>
        <Field label="Auto-cancel unaccepted orders after (hours)" hint="0 = never auto-cancel">
          <input type="number" min="0" value={form.autoCancelHours} onChange={(e) => set('autoCancelHours', e.target.value)} className={inputCls} />
        </Field>
      </div>

      {/* Credit */}
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm p-lg space-y-md">
        <h3 className="text-headline-md text-on-background">Credit</h3>
        <label className="flex items-center gap-sm">
          <input type="checkbox" checked={!!form.enforceCreditLimit} onChange={(e) => set('enforceCreditLimit', e.target.checked)} className="w-5 h-5 accent-[color:var(--primary,#4f46e5)]" />
          <span className="text-body-md text-on-surface">Enforce credit limits on new orders</span>
        </label>
        {form.enforceCreditLimit && (
          <Field label="When an order exceeds available credit">
            <select value={form.creditOverLimitAction} onChange={(e) => set('creditOverLimitAction', e.target.value)} className={inputCls}>
              <option value="BLOCK">Block the order</option>
              <option value="REQUIRE_APPROVAL">Allow but require my approval (stays pending)</option>
            </select>
          </Field>
        )}
      </div>

      {/* Approval */}
      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm p-lg">
        <label className="flex items-center gap-sm">
          <input type="checkbox" checked={!!form.allowOrderingWithoutApproval} onChange={(e) => set('allowOrderingWithoutApproval', e.target.checked)} className="w-5 h-5" />
          <span className="text-body-md text-on-surface">Allow retailers to order without pre-approval</span>
        </label>
      </div>

      <div className="flex items-center gap-md">
        <button onClick={save} disabled={busy} className="bg-primary text-on-primary px-xl py-2.5 rounded-xl text-label-md font-semibold disabled:opacity-50">
          {busy ? 'Saving…' : 'Save settings'}
        </button>
        {msg && <span className="text-label-md text-on-surface-variant">{msg}</span>}
      </div>
    </div>
  );
}
