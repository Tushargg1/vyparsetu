import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import Icon from '../../components/Icon';

function Stat({ title, value, icon }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl border border-surface-variant p-lg shadow-sm">
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
    <div className="min-h-screen bg-surface-container-low p-margin-mobile md:p-margin-desktop">
      <div className="flex items-center justify-between mb-xl">
        <div>
          <h1 className="text-headline-lg font-bold text-primary">VyaparMantra · Admin</h1>
          <p className="text-body-md text-on-surface-variant mt-xs">Platform overview</p>
        </div>
        <button onClick={() => { clearAuth(); navigate('/login'); }} className="text-error flex items-center gap-1 text-label-md">
          <Icon name="logout" /> Logout
        </button>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-gutter mb-xl">
        <Stat title="Users" value={counts?.users} icon="group" />
        <Stat title="Retailers" value={counts?.retailers} icon="store" />
        <Stat title="Suppliers" value={counts?.suppliers} icon="inventory_2" />
        <Stat title="Products" value={counts?.products} icon="category" />
        <Stat title="Orders" value={counts?.orders} icon="receipt_long" />
      </div>

      <div className="bg-surface-container-lowest rounded-xl border border-surface-variant divide-y divide-surface-variant">
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
      </div>
    </div>
  );
}
