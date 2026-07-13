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
        className="fixed bottom-5 left-5 z-50 w-16 h-16 rounded-full bg-secondary text-on-secondary shadow-xl flex items-center justify-center hover:scale-105 transition-transform"
      >
        <Icon name="barcode_scanner" className="text-[30px]" />
      </button>
      <button
        onClick={() => navigate('/cart')}
        aria-label="Cart"
        className="fixed bottom-5 right-5 z-50 w-16 h-16 rounded-full bg-primary text-on-primary shadow-xl flex items-center justify-center hover:scale-105 transition-transform"
      >
        <Icon name="shopping_cart" className="text-[28px]" />
        {count > 0 && (
          <span className="absolute -top-1 -right-1 bg-error text-on-error text-[12px] font-bold min-w-[22px] h-[22px] px-1 rounded-full flex items-center justify-center">
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
