import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import Icon from '../../components/Icon';

function Stat({ title, value, icon }) {
  return (
    <div className="ui-card p-lg transition hover:-translate-y-0.5 hover:shadow-md">
      <div className="flex items-center justify-between mb-sm">
        <span className="text-label-md text-on-surface-variant">{title}</span>
        <Icon name={icon} className="text-primary" />
      </div>
      <div className="text-display-lg text-primary">{value ?? '—'}</div>
    </div>
  );
}

export default function AdminPage() {
  const navigate = useNavigate();
  const clearAuth = useAuthStore((s) => s.clearAuth);
  const { data: counts } = useQuery({ queryKey: ['admin-dash'], queryFn: adminApi.dashboard });
  const { data: usersPage } = useQuery({ queryKey: ['admin-users'], queryFn: () => adminApi.users({ page: 0, size: 50 }) });
  const users = usersPage?.content || [];

  return (
    <div className="min-h-screen bg-background px-margin-mobile py-lg md:px-margin-desktop md:py-margin-desktop">
      <main className="mx-auto max-w-[1480px]">
      <header className="mb-xl flex flex-col gap-md border-b border-surface-variant pb-lg sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-headline-lg font-bold text-primary">VyaparMantra · Admin</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">Platform overview</p>
        </div>
        <button onClick={() => { clearAuth(); navigate('/login'); }} className="ui-button-secondary text-error hover:border-error hover:bg-error-container">
          <Icon name="logout" /> Logout
        </button>
      </header>
      <div className="mb-xl grid grid-cols-1 gap-md sm:grid-cols-2 lg:grid-cols-5">
        <Stat title="Users" value={counts?.users} icon="group" />
        <Stat title="Retailers" value={counts?.retailers} icon="store" />
        <Stat title="Suppliers" value={counts?.suppliers} icon="inventory_2" />
        <Stat title="Products" value={counts?.products} icon="category" />
        <Stat title="Orders" value={counts?.orders} icon="receipt_long" />
      </div>

      <section className="ui-card divide-y divide-surface-variant overflow-hidden">
        <div className="px-lg py-md"><h3 className="text-headline-md">Users</h3></div>
        {users.map((u) => (
          <div key={u.uuid} className="px-lg py-md flex items-center justify-between">
            <div>
              <div className="font-semibold text-on-surface">{u.name}</div>
              <div className="text-label-sm text-on-surface-variant">{u.phone} · {(u.roles || []).join(', ')}</div>
            </div>
            <span className="text-label-sm px-2 py-0.5 rounded-full bg-surface-variant text-on-surface-variant">{u.status}</span>
          </div>
        ))}
      </section>
      </main>
    </div>
  );
}
