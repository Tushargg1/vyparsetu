import { create } from 'zustand';

const ACCESS = 'vs_access';
const REFRESH = 'vs_refresh';
const USER = 'vs_user';

export const useAuthStore = create((set) => ({
  accessToken: localStorage.getItem(ACCESS) || null,
  refreshToken: localStorage.getItem(REFRESH) || null,
  user: JSON.parse(localStorage.getItem(USER) || 'null'),

  setAuth: ({ accessToken, refreshToken, user }) => {
    if (accessToken) localStorage.setItem(ACCESS, accessToken);
    if (refreshToken) localStorage.setItem(REFRESH, refreshToken);
    if (user) localStorage.setItem(USER, JSON.stringify(user));
    set({ accessToken, refreshToken, user });
  },

  clearAuth: () => {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
    localStorage.removeItem(USER);
    set({ accessToken: null, refreshToken: null, user: null });
  },
}));

export const getAccessToken = () => localStorage.getItem(ACCESS);
export const getRefreshToken = () => localStorage.getItem(REFRESH);
