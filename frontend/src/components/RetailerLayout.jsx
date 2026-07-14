import { useNavigate } from 'react-router-dom';
import SidebarLayout from './SidebarLayout';
import Icon from './Icon';
import { useAllCarts } from '../hooks/useShop';

const NAV = [
  { to: '/', label: 'Dashboard', icon: 'dashboard', end: true },
  { to: '/scan', label: 'Scan & Sell', icon: 'barcode_scanner' },
  { to: '/inventory', label: 'Inventory', icon: 'inventory_2' },
  { to: '/rate-list', label: 'Set Rate List', icon: 'sell' },
  { to: '/buy', label: 'Buy Products', icon: 'shopping_basket' },
  { to: '/distributors', label: 'Distributors', icon: 'groups' },
  { to: '/orders', label: 'My Orders', icon: 'receipt_long' },
  { to: '/revenue', label: 'Revenue', icon: 'payments' },
  { to: '/insights', label: 'AI Insights', icon: 'auto_awesome' },
  { to: '/profile', label: 'My Profile', icon: 'badge' },
];

function FloatingButtons() {
  const navigate = useNavigate();
  const cartsQ = useAllCarts();
  const count = (cartsQ.data || []).reduce((n, c) => n + (c.items?.length || 0), 0);

  return (
    <>
      <button
        onClick={() => navigate('/scan')}
        aria-label="Scan & Sell"
        className="fixed bottom-5 left-4 z-30 flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary text-on-secondary shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl focus-visible:ring-offset-background sm:left-5 md:bottom-6 md:left-[296px]"
      >
        <Icon name="barcode_scanner" className="text-[30px]" />
      </button>
      <button
        onClick={() => navigate('/cart')}
        aria-label="Cart"
        className="fixed bottom-5 right-4 z-30 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary text-on-primary shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl focus-visible:ring-offset-background sm:right-5 md:bottom-6 md:right-6"
      >
        <Icon name="shopping_cart" className="text-[28px]" />
        {count > 0 && (
          <span className="absolute -right-1.5 -top-1.5 flex h-[22px] min-w-[22px] items-center justify-center rounded-full border-2 border-background bg-error px-1 text-[11px] font-bold text-on-error shadow-sm">
            {count}
          </span>
        )}
      </button>
    </>
  );
}

export default function RetailerLayout() {
  return (
    <>
      <SidebarLayout subtitle="Retailer Dashboard" nav={NAV} />
      <FloatingButtons />
    </>
  );
}
