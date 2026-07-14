import { create } from 'zustand';

const ACCESS = 'vs_access';
const REFRESH = 'vs_refresh';
const USER = 'vs_user';

function readStoredUser() {
  const value = localStorage.getItem(USER);
  if (!value) return null;

  try {
    return JSON.parse(value);
  } catch {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
    localStorage.removeItem(USER);
    return null;
  }
}

const storedUser = readStoredUser();

export const useAuthStore = create((set) => ({
  accessToken: localStorage.getItem(ACCESS) || null,
  refreshToken: localStorage.getItem(REFRESH) || null,
  user: storedUser,

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
