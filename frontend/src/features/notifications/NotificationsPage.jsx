import { useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../lib/api';
import PageHeader from '../../components/PageHeader';
import Icon from '../../components/Icon';
import { EmptyState } from '../../components/StatePanel';

export default function NotificationsPage() {
  const qc = useQueryClient();
  const { data } = useQuery({ queryKey: ['notifications'], queryFn: () => notificationApi.list({ page: 0, size: 30 }) });
  const items = data?.content || [];

  const markRead = async (id) => {
    await notificationApi.markRead(id);
    qc.invalidateQueries({ queryKey: ['notifications'] });
  };

  return (
    <div className="space-y-lg">
      <PageHeader icon="notifications" title="Notifications" subtitle="Updates about your orders, stock, and account activity." />
      {items.length === 0 && <EmptyState icon="notifications_off" title="You’re all caught up" description="New business updates will appear here." />}
      <div className="space-y-sm">
      {items.map((n) => (
        <button
          type="button"
          key={n.id}
          onClick={() => !n.readAt && markRead(n.id)}
          className={`ui-card flex w-full items-start gap-md p-md text-left transition hover:border-primary/40 hover:shadow-md ${n.readAt ? 'opacity-60' : ''}`}
        >
          <span className={`mt-xs flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${n.readAt ? 'bg-surface-container text-on-surface-variant' : 'bg-primary-fixed text-primary'}`}>
            <Icon name={n.readAt ? 'notifications' : 'notifications_active'} className="text-[20px]" />
          </span>
          <span className="min-w-0">
            <span className="block font-semibold text-on-surface">{n.title}</span>
            <span className="mt-xs block text-label-sm text-on-surface-variant">{n.body}</span>
          </span>
        </button>
      ))}
      </div>
    </div>
  );
}
