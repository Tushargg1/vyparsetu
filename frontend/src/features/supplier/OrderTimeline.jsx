import { useQuery } from '@tanstack/react-query';
import { orderApi } from '../../lib/api';
import Icon from '../../components/Icon';
import { statusBadge } from './supplierUtils';

const when = (v) =>
  v ? new Date(v).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '';

export default function OrderTimeline({ orderId }) {
  const { data: history = [], isLoading } = useQuery({
    queryKey: ['order-history', orderId],
    queryFn: () => orderApi.history(orderId),
    enabled: !!orderId,
  });

  if (isLoading) return (
    <div className="flex items-center gap-sm rounded-xl bg-surface-container-low p-md text-label-sm text-on-surface-variant" role="status">
      <Icon name="progress_activity" className="animate-spin text-[18px]" /> Loading timeline…
    </div>
  );
  if (history.length === 0) return (
    <div className="flex items-center gap-sm rounded-xl border border-dashed border-outline-variant p-md text-label-sm text-on-surface-variant">
      <Icon name="history" className="text-[18px]" /> No order history yet.
    </div>
  );

  return (
    <div className="pl-1">
      {history.map((h, i) => {
        const badge = statusBadge(h.toStatus);
        const last = i === history.length - 1;
        return (
          <div key={i} className="flex gap-sm">
            <div className="flex flex-col items-center">
              <div className={`w-2.5 h-2.5 rounded-full mt-1 ${last ? 'bg-primary' : 'bg-outline-variant'}`} />
              {!last && <div className="w-px flex-1 bg-outline-variant" />}
            </div>
            <div className="pb-md">
              <div className="flex items-center gap-sm">
                <span className={`px-sm py-xs rounded-full text-label-sm font-medium ${badge.tone}`}>{badge.label}</span>
                <span className="text-label-sm text-on-surface-variant">{when(h.at)}</span>
              </div>
              {h.note && <div className="text-label-sm text-on-surface-variant mt-0.5">{h.note}</div>}
            </div>
          </div>
        );
      })}
    </div>
  );
}
