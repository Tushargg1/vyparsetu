import { useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../lib/api';

export default function NotificationsPage() {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ['notifications'], queryFn: () => notificationApi.list({ page: 0, size: 30 }) });
  const items = data?.content || [];

  const markRead = async (id) => {
    await notificationApi.markRead(id);
    qc.invalidateQueries({ queryKey: ['notifications'] });
  };

  return (
    <div className="space-y-md">
      <h2 className="text-headline-md">Notifications</h2>
      {items.length === 0 && <p className="text-on-surface-variant">No notifications.</p>}
      {items.map((n) => (
        <div
          key={n.id}
          onClick={() => !n.readAt && markRead(n.id)}
          className={`bg-surface-container-lowest rounded-xl border border-surface-variant p-md ${n.readAt ? 'opacity-60' : ''}`}
        >
          <div className="font-semibold text-on-surface">{n.title}</div>
          <div className="text-label-sm text-on-surface-variant">{n.body}</div>
        </div>
      ))}
    </div>
  );
}
