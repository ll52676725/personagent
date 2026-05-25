import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { LoginResult, Agent, Article, GenerateResult, Result } from '@/types';

/** 与后端同域部署时使用相对路径；开发模式可通过 VITE_API_BASE_URL 覆盖 */
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const ARTICLE_BASE = `${API_BASE}/agent-article`;

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
});

const articleClient: AxiosInstance = axios.create({
  baseURL: ARTICLE_BASE,
  timeout: 30000,
});

const attachAuth = (config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

const handleResponse = (response: AxiosResponse) => response;

const handleError = async (error: any) => {
  const originalRequest = error.config;
  if (error.response?.status === 401 && !originalRequest._retry) {
    originalRequest._retry = true;
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      try {
        const response = await apiClient.post('/auth/refresh', { refreshToken });
        const { accessToken, refreshToken: newRefreshToken } = response.data.data;
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (e) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(e);
      }
    } else {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
    }
  }
  return Promise.reject(error);
};

apiClient.interceptors.request.use(attachAuth);
apiClient.interceptors.response.use(handleResponse, handleError);
articleClient.interceptors.request.use(attachAuth);
articleClient.interceptors.response.use(handleResponse, handleError);

export const authApi = {
  register: async (username: string, email: string, password: string) => {
    const response = await apiClient.post('/auth/register', {
      username,
      email,
      password,
    });
    return response.data as Result<unknown>;
  },

  login: async (username: string, password: string) => {
    const response = await apiClient.post('/auth/login', {
      username,
      password,
    });
    return response.data as Result<LoginResult>;
  },

  refresh: async (refreshToken: string) => {
    const response = await apiClient.post('/auth/refresh', {
      refreshToken,
    });
    return response.data as Result<LoginResult>;
  },

  getMe: async () => {
    const response = await apiClient.get('/auth/me');
    return response.data as Result<LoginResult['user']>;
  },
};

export const agentApi = {
  getAll: async () => {
    const response = await apiClient.get('/agents');
    return response.data as Result<Agent[]>;
  },

  getById: async (id: number) => {
    const response = await apiClient.get(`/agents/${id}`);
    return response.data as Result<Agent>;
  },

  applyPermission: async (id: number, reason: string) => {
    const response = await apiClient.post(`/agents/${id}/apply`, {
      applyReason: reason,
    });
    return response.data as Result<Agent>;
  },

  getPermissionStatus: async (id: number) => {
    const response = await apiClient.get(`/agents/${id}/status`);
    return response.data as Result<{ status: number; message?: string }>;
  },
};

export const articleApi = {
  create: async (title: string, summary?: string, content?: string, tags?: string[]) => {
    const response = await articleClient.post('/articles', {
      title,
      summary,
      content,
      tags,
    });
    return response.data as Result<Article>;
  },

  getAll: async () => {
    const response = await articleClient.get('/articles');
    return response.data as Result<Article[]>;
  },

  getById: async (id: number) => {
    const response = await articleClient.get(`/articles/${id}`);
    return response.data as Result<Article>;
  },

  update: async (id: number, data: Partial<Pick<Article, 'title' | 'summary' | 'content'>> & { tags?: string[] }) => {
    const response = await articleClient.put(`/articles/${id}`, data);
    return response.data as Result<Article>;
  },

  delete: async (id: number) => {
    const response = await articleClient.delete(`/articles/${id}`);
    return response.data as Result<void>;
  },

  publish: async (id: number, platforms: string[]) => {
    const response = await articleClient.post(`/articles/${id}/publish`, {
      platforms,
    });
    return response.data as Result<unknown>;
  },
};

export const generateApi = {
  title: async (topic: string, keywords?: string[]) => {
    const response = await articleClient.post('/generate/title', {
      topic,
      keywords,
    });
    return response.data as Result<GenerateResult>;
  },

  summary: async (title: string, content?: string) => {
    const response = await articleClient.post('/generate/summary', {
      title,
      content,
    });
    return response.data as Result<GenerateResult>;
  },

  content: async (title: string, outline?: string) => {
    const response = await articleClient.post('/generate/content', {
      title,
      outline,
    });
    return response.data as Result<GenerateResult>;
  },
};
