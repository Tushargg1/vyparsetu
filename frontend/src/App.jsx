import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import LoginPage from './features/auth/LoginPage';
import RetailerLayout from './components/RetailerLayout';
import SupplierLayout from './components/SupplierLayout';

import RetailerDashboard from './features/dashboard/RetailerDashboard';
import ScanPage from './features/scan/ScanPage';
import InventoryPage from './features/inventory/InventoryPage';
import RateListPage from './features/rates/RateListPage';
import BuyProductsPage from './features/shop/BuyProductsPage';
import CartPage from './features/shop/CartPage';
import DistributorsPage from './features/distributors/DistributorsPage';
import DistributorCatalogPage from './features/distributors/DistributorCatalogPage';
import OrdersPage from './features/orders/OrdersPage';
import RevenuePage from './features/revenue/RevenuePage';
import InsightsPage from './features/insights/InsightsPage';
import NotificationsPage from './features/notifications/NotificationsPage';
import ProfilePage from './features/profile/ProfilePage';

import SupplierDashboard from './features/supplier/SupplierDashboard';
import SupplierOrdersPage from './features/supplier/SupplierOrdersPage';
import RetailersPage from './features/supplier/RetailersPage';
import RetailerDetailPage from './features/supplier/RetailerDetailPage';
import SupplierStockPage from './features/supplier/SupplierStockPage';
import SupplierRevenuePage from './features/supplier/SupplierRevenuePage';
import SupplierInsightsPage from './features/supplier/SupplierInsightsPage';
import SupplierAnalyticsPage from './features/supplier/SupplierAnalyticsPage';
import SupplierSettingsPage from './features/supplier/SupplierSettingsPage';
import WhatsAppAiPage from './features/supplier/WhatsAppAiPage';
import AdminPage from './features/admin/AdminPage';

function hasRole(user, role) {
  return (user?.roles || []).includes(role);
}

function homeFor(user) {
  if (hasRole(user, 'SUPPLIER')) return '/supplier';
  if (hasRole(user, 'ADMIN')) return '/admin';
  return '/';
}

function RequireRole({ role, children }) {
  const { accessToken, user } = useAuthStore();
  if (!accessToken) return <Navigate to="/login" replace />;
  if (!hasRole(user, role)) return <Navigate to={homeFor(user)} replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />

      {/* Retailer */}
      <Route
        path="/"
        element={
          <RequireRole role="RETAILER">
            <RetailerLayout />
          </RequireRole>
        }
      >
        <Route index element={<RetailerDashboard />} />
        <Route path="scan" element={<ScanPage />} />
        <Route path="inventory" element={<InventoryPage />} />
        <Route path="rate-list" element={<RateListPage />} />
        <Route path="buy" element={<BuyProductsPage />} />
        <Route path="distributors" element={<DistributorsPage />} />
        <Route path="distributors/:id" element={<DistributorCatalogPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="revenue" element={<RevenuePage />} />
        <Route path="insights" element={<InsightsPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="notifications" element={<NotificationsPage />} />
      </Route>

      {/* Supplier / Distributor */}
      <Route
        path="/supplier"
        element={
          <RequireRole role="SUPPLIER">
            <SupplierLayout />
          </RequireRole>
        }
      >
        <Route index element={<SupplierDashboard />} />
        <Route path="orders" element={<SupplierOrdersPage />} />
        <Route path="retailers" element={<RetailersPage />} />
        <Route path="retailers/:id" element={<RetailerDetailPage />} />
        <Route path="stock" element={<SupplierStockPage />} />
        <Route path="revenue" element={<SupplierRevenuePage />} />
        <Route path="analytics" element={<SupplierAnalyticsPage />} />
        <Route path="insights" element={<SupplierInsightsPage />} />
        <Route path="whatsapp" element={<WhatsAppAiPage />} />
        <Route path="settings" element={<SupplierSettingsPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      {/* Admin */}
      <Route
        path="/admin"
        element={
          <RequireRole role="ADMIN">
            <AdminPage />
          </RequireRole>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
