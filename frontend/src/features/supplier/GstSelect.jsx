import { useState } from 'react';

// Standard Indian GST slabs. Distributors usually pick one of these per product.
export const GST_SLABS = [0, 5, 12, 18, 28];

/**
 * GST rate picker: a dropdown of the standard slabs plus a "Custom %" escape hatch.
 * `value` is a number/string percentage; `onChange` receives the new percentage.
 */
export default function GstSelect({ value, onChange, className = '' }) {
  const num = value === '' || value == null ? '' : Number(value);
  const isSlab = GST_SLABS.includes(Number(num));
  const [custom, setCustom] = useState(!isSlab && num !== '');

  const base = 'border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary';

  if (custom) {
    return (
      <div className={`flex gap-1 ${className}`}>
        <input
          type="number" min="0" step="0.5" inputMode="decimal"
          className={`${base} flex-1`}
          placeholder="GST %"
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
        <button type="button" onClick={() => { setCustom(false); onChange('0'); }}
          className="text-label-sm text-primary px-1" title="Use a standard slab">slabs</button>
      </div>
    );
  }

  return (
    <select
      className={`${base} ${className}`}
      value={isSlab ? String(Number(num)) : ''}
      onChange={(e) => {
        if (e.target.value === '__custom') { setCustom(true); return; }
        onChange(e.target.value);
      }}
    >
      <option value="" disabled>GST %</option>
      {GST_SLABS.map((s) => <option key={s} value={String(s)}>{s}% GST</option>)}
      <option value="__custom">Custom…</option>
    </select>
  );
}
