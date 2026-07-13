import SidebarLayout from './SidebarLayout';

const NAV = [
  { to: '/supplier', label: 'Dashboard', icon: 'dashboard', end: true },
  { to: '/supplier/orders', label: 'Orders', icon: 'shopping_cart' },
  { to: '/supplier/retailers', label: 'Retailers', icon: 'groups' },
  { to: '/supplier/stock', label: 'Stock', icon: 'inventory_2' },
  { to: '/supplier/revenue', label: 'Revenue', icon: 'payments' },
  { to: '/supplier/whatsapp', label: 'WhatsApp AI', icon: 'forum' },
  { to: '/supplier/analytics', label: 'Analytics', icon: 'bar_chart' },
  { to: '/supplier/insights', label: 'AI Insights', icon: 'auto_awesome' },
  { to: '/supplier/settings', label: 'Ordering Rules', icon: 'tune' },
  { to: '/supplier/profile', label: 'My Profile', icon: 'badge' },
];

export default function SupplierLayout() {
  return <SidebarLayout subtitle="Distributor Dashboard" nav={NAV} />;
}
