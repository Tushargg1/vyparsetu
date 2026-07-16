import axios from 'axios';
import { getAccessToken, getRefreshToken, useAuthStore } from '../stores/authStore';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '');

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing = null;

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const refreshToken = getRefreshToken();
    const isAuthRequest = original?.url?.includes('/auth/');
    if (error.response?.status === 401 && original && !original._retry && refreshToken && !isAuthRequest) {
      original._retry = true;
      try {
        refreshing =
          refreshing ||
          axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
        const { data } = await refreshing;
        refreshing = null;
        const payload = data.data;
        useAuthStore.getState().setAuth({
          accessToken: payload.accessToken,
          refreshToken: payload.refreshToken,
          user: payload.user,
        });
        original.headers.Authorization = `Bearer ${payload.accessToken}`;
        return client(original);
      } catch (e) {
        refreshing = null;
        useAuthStore.getState().clearAuth();
        window.location.href = '/login';
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  }
);

// unwrap the ApiResponse envelope
export const unwrap = (promise) => promise.then((r) => r.data.data);

export default client;
