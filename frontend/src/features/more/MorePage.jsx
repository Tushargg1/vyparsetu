import { useNavigate } from 'react-router-dom';
import { authApi } from '../../lib/api';
import { useAuthStore } from '../../stores/authStore';
import { useDistributor } from '../../hooks/useShop';
import Icon from '../../components/Icon';

function Row({ icon, label, onClick }) {
  return (
    <button onClick={onClick} className="w-full flex items-center gap-md px-md py-4 bg-surface-container-lowest border border-surface-variant rounded-2xl hover:bg-surface-container text-left">
      <div className="w-11 h-11 rounded-xl bg-primary-fixed flex items-center justify-center text-primary">
        <Icon name={icon} className="text-[24px]" />
      </div>
      <span className="flex-1 text-body-lg font-semibold text-on-surface">{label}</span>
      <Icon name="chevron_right" className="text-on-surface-variant" />
    </button>
  );
}

export default function MorePage() {
  const navigate = useNavigate();
  const { user, refreshToken, clearAuth } = useAuthStore();
  const distQ = useDistributor();

  const logout = async () => {
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch {
      // The local session must still end if the token is already invalid or the API is unavailable.
    } finally {
      clearAuth();
      navigate('/login', { replace: true });
    }
  };

  return (
    <div className="space-y-md">
      <h2 className="text-headline-md">More</h2>

      <div className="bg-primary text-on-primary rounded-2xl p-lg">
        <p className="text-label-md text-primary-fixed-dim">My shop</p>
        <p className="text-headline-md font-bold">{user?.name}</p>
        <p className="text-label-md text-primary-fixed-dim mt-xs">{user?.phone}</p>
        {distQ.data && (
          <div className="mt-md pt-md border-t border-on-primary/20 flex items-center gap-2">
            <Icon name="handshake" />
            <span className="text-label-md">Distributor: <b>{distQ.data.businessName}</b></span>
          </div>
        )}
      </div>

      <div className="space-y-sm">
        <Row icon="notifications" label="Notifications" onClick={() => navigate('/notifications')} />
        <Row icon="monitoring" label="Reports" onClick={() => navigate('/reports')} />
        <Row icon="inventory_2" label="My Stock" onClick={() => navigate('/inventory')} />
      </div>

      <button
        onClick={logout}
        className="w-full flex items-center justify-center gap-2 py-4 rounded-2xl border-2 border-error text-error font-bold mt-lg"
      >
        <Icon name="logout" /> Logout
      </button>
    </div>
  );
}
