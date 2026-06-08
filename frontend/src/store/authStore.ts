import { create } from 'zustand';
import { UserInfo } from '@/types';

const DEFAULT_TOKEN_PREFIX = 'fake-root-token-';

const storedAccessToken = localStorage.getItem('accessToken');
const storedRefreshToken = localStorage.getItem('refreshToken');

if (storedAccessToken && storedAccessToken.startsWith('fake-') && !storedAccessToken.startsWith(DEFAULT_TOKEN_PREFIX)) {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

const validAccessToken = storedAccessToken ? storedAccessToken : DEFAULT_TOKEN_PREFIX + 'auto';
const validRefreshToken = storedRefreshToken ? storedRefreshToken : 'fake-root-refresh-auto';

if (!storedAccessToken) {
  localStorage.setItem('accessToken', validAccessToken);
  localStorage.setItem('refreshToken', validRefreshToken);
}

const defaultUser: UserInfo = { id: 1, username: 'root', email: 'root@agentai.com' };

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
  user: defaultUser,
  accessToken: validAccessToken,
  refreshToken: validRefreshToken,
  expiresIn: 86400,
  isLoggedIn: true,

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
    const defaultAccessToken = DEFAULT_TOKEN_PREFIX + Date.now();
    const defaultRefreshToken = 'fake-root-refresh-' + Date.now();
    localStorage.setItem('accessToken', defaultAccessToken);
    localStorage.setItem('refreshToken', defaultRefreshToken);
    set({
      user: defaultUser,
      accessToken: defaultAccessToken,
      refreshToken: defaultRefreshToken,
      expiresIn: 86400,
      isLoggedIn: true,
    });
  },

  setUser: (user) => {
    set({ user });
  },
}));
