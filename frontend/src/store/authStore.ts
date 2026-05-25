import { create } from 'zustand';
import { UserInfo } from '@/types';

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
  accessToken: localStorage.getItem('accessToken') || null,
  refreshToken: localStorage.getItem('refreshToken') || null,
  expiresIn: 0,
  isLoggedIn: !!localStorage.getItem('accessToken'),

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
