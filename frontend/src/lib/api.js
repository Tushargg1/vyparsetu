import client, { unwrap } from './axiosClient';

export const authApi = {
  register: (body) => unwrap(client.post('/auth/register', body)),
  sendOtp: (body) => unwrap(client.post('/auth/otp/send', body)),
  verifyOtp: (body) => unwrap(client.post('/auth/otp/verify', body)),
  logout: (refreshToken) => unwrap(client.post('/auth/logout', { refreshToken })),
  devLogin: (role) => unwrap(client.post('/auth/dev-login', { role })),
};

export const adminApi = {
  dashboard: () => unwrap(client.get('/admin/dashboard')),
  users: (params) => unwrap(client.get('/admin/users', { params })),
};

export const userApi = {
  me: () => unwrap(client.get('/users/me')),
};

export const productApi = {
  search: (params) => unwrap(client.get('/products', { params })),
  byBarcode: (barcode) => unwrap(client.get(`/products/barcode/${barcode}`)),
  create: (body) => unwrap(client.post('/products', body)),
  update: (id, body) => unwrap(client.put(`/products/${id}`, body)),
  remove: (id) => unwrap(client.delete(`/products/${id}`)),
};

export const inventoryApi = {
  list: () => unwrap(client.get('/inventory')),
  lowStock: () => unwrap(client.get('/inventory/low-stock')),
  movement: (body) => unwrap(client.post('/inventory/movements', body)),
  expiring: (days = 30) => unwrap(client.get('/inventory/expiring', { params: { days } })),
  scanSale: (barcode, quantity) => unwrap(client.post('/inventory/scan-sale', { barcode, quantity })),
};

export const cartApi = {
  get: (supplierId) => unwrap(client.get('/cart', { params: { supplierId } })),
  all: () => unwrap(client.get('/cart/all')),
  addItem: (body) => unwrap(client.post('/cart/items', body)),
  removeItem: (id) => unwrap(client.delete(`/cart/items/${id}`)),
};

export const orderApi = {
  place: (body) => unwrap(client.post('/orders', body)),
  mine: (params) => unwrap(client.get('/orders', { params })),
  byId: (id) => unwrap(client.get(`/orders/${id}`)),
  history: (id) => unwrap(client.get(`/orders/${id}/history`)),
  updateStatus: (id, body) => unwrap(client.patch(`/orders/${id}/status`, body)),
  modify: (id, items) => unwrap(client.put(`/orders/${id}/items`, { items })),
  repeat: () => unwrap(client.post('/orders/repeat')),
  supplier: (params) => unwrap(client.get('/orders/supplier', { params })),
};

export const policyApi = {
  get: () => unwrap(client.get('/distributor/policy')),
  update: (body) => unwrap(client.put('/distributor/policy', body)),
};

export const analyticsApi = {
  summary: () => unwrap(client.get('/distributor/analytics/summary')),
};

export const creditApi = {
  get: (retailerId) => unwrap(client.get(`/distributor/credit/retailer/${retailerId}`)),
  setLimit: (retailerId, body) => unwrap(client.put(`/distributor/credit/retailer/${retailerId}`, body)),
  setStatus: (retailerId, status) => unwrap(client.post(`/distributor/credit/retailer/${retailerId}/status`, { status })),
};

export const customerPriceApi = {
  listForRetailer: (retailerId) => unwrap(client.get(`/distributor/customer-prices/retailer/${retailerId}`)),
  upsert: (body) => unwrap(client.put('/distributor/customer-prices', body)),
  remove: (id) => unwrap(client.delete(`/distributor/customer-prices/${id}`)),
};

export const aiApi = {
  textToOrder: (body) => unwrap(client.post('/ai/order/text', body)),
  voiceToOrder: (body) => unwrap(client.post('/ai/order/voice', body)),
  imageToOrder: (body) => unwrap(client.post('/ai/order/image', body)),
  reorder: () => unwrap(client.get('/ai/recommendations/reorder')),
  forecast: () => unwrap(client.get('/ai/forecast')),
  chat: (message) => unwrap(client.post('/ai/chat', { message })),
};

export const notificationApi = {
  list: (params) => unwrap(client.get('/notifications', { params })),
  unreadCount: () => unwrap(client.get('/notifications/unread-count')),
  markRead: (id) => unwrap(client.patch(`/notifications/${id}/read`)),
};

export const reportApi = {
  sales: () => unwrap(client.get('/reports/sales')),
  purchase: () => unwrap(client.get('/reports/purchase')),
  inventory: () => unwrap(client.get('/reports/inventory')),
  revenue: () => unwrap(client.get('/reports/revenue')),
  revenueRange: (from, to) => unwrap(client.get('/reports/revenue/range', { params: { from, to } })),
  today: () => unwrap(client.get('/reports/today')),
  topProducts: () => unwrap(client.get('/reports/top-products')),
  supplierSales: (params) => unwrap(client.get('/reports/supplier/sales', { params })),
};

export const directoryApi = {
  distributors: () => unwrap(client.get('/distributors')),
};

export const dashboardApi = {
  retailer: () => unwrap(client.get('/dashboard/retailer')),
};

export const salesApi = {
  lookup: (barcode) => unwrap(client.get('/sales/lookup', { params: { barcode } })),
  record: (items) => unwrap(client.post('/sales', { items })),
  history: (params) => unwrap(client.get('/sales', { params })),
  rateList: () => unwrap(client.get('/sales/rate-list')),
  setRate: (productId, price) => unwrap(client.put('/sales/rate-list', { productId, price })),
  discounts: () => unwrap(client.get('/sales/discounts')),
  addDiscount: (label, percent) => unwrap(client.post('/sales/discounts', { label, percent })),
  deleteDiscount: (id) => unwrap(client.delete(`/sales/discounts/${id}`)),
};

export const distributorApi = {
  inviteCode: () => unwrap(client.get('/distributor/invite-code')),
  retailers: () => unwrap(client.get('/distributor/retailers')),
  addRetailer: (body) => unwrap(client.post('/distributor/retailers', body)),
  recordPayment: (retailerId, amount) => unwrap(client.post(`/distributor/retailers/${retailerId}/payments`, { amount })),
  setWhatsApp: (body) => unwrap(client.put('/distributor/whatsapp', body)),
  profile: () => unwrap(client.get('/distributor/profile')),
  updateProfile: (body) => unwrap(client.put('/distributor/profile', body)),
};

export const retailerApi = {
  join: (inviteCode) => unwrap(client.post('/retailer/join', { inviteCode })),
  distributor: () => unwrap(client.get('/retailer/distributor')),
  profile: () => unwrap(client.get('/retailer/profile')),
  updateProfile: (body) => unwrap(client.put('/retailer/profile', body)),
};

export const supplierOrderApi = {
  list: (params) => unwrap(client.get('/orders/supplier', { params })),
  updateStatus: (id, body) => unwrap(client.patch(`/orders/${id}/status`, body)),
};

export const whatsappApi = {
  settings: () => unwrap(client.get('/distributor/whatsapp/settings')),
  updateSettings: (body) => unwrap(client.put('/distributor/whatsapp/settings', body)),
  connect: (businessNumber) => unwrap(client.post('/distributor/whatsapp/connect', { businessNumber })),
  disconnect: () => unwrap(client.post('/distributor/whatsapp/disconnect')),
  takeover: (enabled) => unwrap(client.post('/distributor/whatsapp/takeover', { enabled })),
  requests: (status) => unwrap(client.get('/distributor/whatsapp/requests', { params: status ? { status } : {} })),
  approveRequest: (id) => unwrap(client.post(`/distributor/whatsapp/requests/${id}/approve`)),
  rejectRequest: (id) => unwrap(client.post(`/distributor/whatsapp/requests/${id}/reject`)),
  simulate: (from, text) => unwrap(client.post('/distributor/whatsapp/simulate', { from, text })),
  linkedNumbers: (retailerId) => unwrap(client.get(`/distributor/whatsapp/retailers/${retailerId}/numbers`)),
  addNumber: (retailerId, phone) => unwrap(client.post(`/distributor/whatsapp/retailers/${retailerId}/numbers`, { phone })),
  verifyNumber: (retailerId, phone, code) => unwrap(client.post(`/distributor/whatsapp/retailers/${retailerId}/numbers/verify`, { phone, code })),
  removeNumber: (retailerId, numberId) => unwrap(client.delete(`/distributor/whatsapp/retailers/${retailerId}/numbers/${numberId}`)),
};
