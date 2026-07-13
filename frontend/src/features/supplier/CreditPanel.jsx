import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { creditApi } from '../../lib/api';
import Icon from '../../components/Icon';
import { money } from './supplierUtils';

export default function CreditPanel({ retailerId }) {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['credit', retailerId],
    queryFn: () => creditApi.get(retailerId),
    enabled: !!retailerId,
  });
  const [limit, setLimit] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const refresh = () => qc.invalidateQueries({ queryKey: ['credit', retailerId] });

  const saveLimit = async () => {
    const v = Number(limit);
    if (v < 0 || Number.isNaN(v)) { setMsg('Enter a valid amount'); return; }
    setBusy(true); setMsg('');
    try {
      await creditApi.setLimit(retailerId, { creditLimit: v, approved: true });
      await refresh(); setLimit(''); setMsg('Credit limit updated.');
    } catch (e) { setMsg(e.response?.data?.error?.message || 'Could not update'); }
    finally { setBusy(false); }
  };

  const toggleApproval = async (approved) => {
    setBusy(true); setMsg('');
    try { await creditApi.setLimit(retailerId, { approved }); await refresh(); }
    catch (e) { setMsg(e.response?.data?.error?.message || 'Could not update'); }
    finally { setBusy(false); }
  };

  const setStatus = async (status) => {
    setBusy(true); setMsg('');
    try { await creditApi.setStatus(retailerId, status); await refresh(); }
    catch (e) { setMsg(e.response?.data?.error?.message || 'Could not update'); }
    finally { setBusy(false); }
  };

  const usedPct = data && Number(data.creditLimit) > 0
    ? Math.min(100, (Number(data.outstanding) / Number(data.creditLimit)) * 100) : 0;

  return (
    <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant shadow-sm">
      <div className="px-lg py-md border-b border-outline-variant flex items-center gap-sm">
        <Icon name="credit_score" className="text-primary" />
        <h3 className="text-headline-md text-on-background">Credit line</h3>
        {data && (
          <span className={`ml-auto px-sm py-xs rounded-full text-label-sm font-medium ${
            data.status === 'ACTIVE' ? 'bg-tertiary-fixed-dim text-on-tertiary-container'
              : data.status === 'SUSPENDED' ? 'bg-error-container text-error'
              : 'bg-surface-variant text-on-surface-variant'}`}>
            {data.status === 'NONE' ? 'No account' : data.status}
          </span>
        )}
      </div>

      <div className="p-lg space-y-md">
        {isLoading && <p className="text-on-surface-variant text-label-md">Loading…</p>}
        {data && (
          <>
            <div className="grid grid-cols-3 gap-sm text-center">
              <div>
                <div className="text-label-sm text-on-surface-variant">Limit</div>
                <div className="font-bold text-on-surface">{money(data.creditLimit)}</div>
              </div>
              <div>
                <div className="text-label-sm text-on-surface-variant">Outstanding</div>
                <div className="font-bold text-error">{money(data.outstanding)}</div>
              </div>
              <div>
                <div className="text-label-sm text-on-surface-variant">Available</div>
                <div className="font-bold text-on-tertiary-container">{money(data.available)}</div>
              </div>
            </div>
            <div className="h-2 rounded-full bg-surface-variant overflow-hidden">
              <div className={`h-full rounded-full ${usedPct > 90 ? 'bg-error' : 'bg-primary'}`} style={{ width: `${usedPct}%` }} />
            </div>

            <div className="flex items-end gap-sm flex-wrap">
              <label className="flex-1 min-w-[160px]">
                <span className="text-label-sm text-on-surface-variant">Set credit limit (₹)</span>
                <input type="number" min="0" value={limit} onChange={(e) => setLimit(e.target.value)}
                  placeholder={String(data.creditLimit)}
                  className="block w-full mt-1 border border-outline-variant rounded-lg py-2 px-3 outline-none focus:border-primary" />
              </label>
              <button onClick={saveLimit} disabled={busy}
                className="bg-primary text-on-primary px-lg py-2 rounded-lg text-label-md font-semibold disabled:opacity-50">
                Save
              </button>
            </div>

            <div className="flex items-center gap-sm flex-wrap">
              {!data.approved ? (
                <button onClick={() => toggleApproval(true)} disabled={busy}
                  className="px-3 py-1.5 rounded-lg border border-outline-variant text-label-md font-semibold hover:bg-surface-container">
                  Approve for credit
                </button>
              ) : (
                <span className="text-label-sm text-on-tertiary-container flex items-center gap-1">
                  <Icon name="verified" className="text-[16px]" /> Credit approved
                </span>
              )}
              {data.status === 'ACTIVE' && (
                <button onClick={() => setStatus('SUSPENDED')} disabled={busy}
                  className="px-3 py-1.5 rounded-lg border border-outline-variant text-error text-label-md font-semibold hover:bg-error-container">
                  Suspend
                </button>
              )}
              {data.status === 'SUSPENDED' && (
                <button onClick={() => setStatus('ACTIVE')} disabled={busy}
                  className="px-3 py-1.5 rounded-lg border border-outline-variant text-label-md font-semibold hover:bg-surface-container">
                  Reactivate
                </button>
              )}
            </div>
            {msg && <p className="text-label-md text-on-surface-variant">{msg}</p>}
          </>
        )}
      </div>
    </div>
  );
}
