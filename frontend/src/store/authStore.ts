import { create } from 'zustand';
import { UserInfo } from '@/types';

const storedAccessToken = localStorage.getItem('accessToken');
const storedRefreshToken = localStorage.getItem('refreshToken');

if (storedAccessToken && storedAccessToken.startsWith('fake-')) {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

const validAccessToken = storedAccessToken && !storedAccessToken.startsWith('fake-') ? storedAccessToken : null;
const validRefreshToken = storedRefreshToken && !storedRefreshToken.startsWith('fake-') ? storedRefreshToken : null;

interface AuthStore {
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  expiresIn: number;
  isLoggedIn: boolean;
  
  login: (user: UserInfo, accessToken: string, refreshToken: string, expiresIn: number) => void;
  logout: () => void;
  setUser: (user: UserInfo) => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  accessToken: validAccessToken,
  refreshToken: validRefreshToken,
  expiresIn: 0,
  isLoggedIn: !!validAccessToken,

  login: (user, accessToken, refreshToken, expiresIn) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    set({
      user,
      accessToken,
      refreshToken,
      expiresIn,
      isLoggedIn: true,
    });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      expiresIn: 0,
      isLoggedIn: false,
    });
  },

  setUser: (user) => {
    set({ user });
  },
}));
