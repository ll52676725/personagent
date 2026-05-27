import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { LoginResult, Agent, Article, Collection, CollectionOutline, GenerateResult, Result } from '@/types';

/** 与后端同域部署时使用相对路径；开发模式可通过 VITE_API_BASE_URL 覆盖 */
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const ARTICLE_BASE = `${API_BASE}/agent-article`;

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 10000,
});

const articleClient: AxiosInstance = axios.create({
  baseURL: ARTICLE_BASE,
  timeout: 120000,
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

export const collectionApi = {
  create: async (title: string, description?: string, coverImage?: string) => {
    const response = await articleClient.post('/collections', {
      title,
      description,
      coverImage,
    });
    return response.data as Result<Collection>;
  },

  getAll: async () => {
    const response = await articleClient.get('/collections');
    return response.data as Result<Collection[]>;
  },

  getById: async (id: number) => {
    const response = await articleClient.get(`/collections/${id}`);
    return response.data as Result<Collection>;
  },

  update: async (id: number, data: Partial<Pick<Collection, 'title' | 'description' | 'coverImage' | 'status'>>) => {
    const response = await articleClient.put(`/collections/${id}`, data);
    return response.data as Result<Collection>;
  },

  delete: async (id: number) => {
    const response = await articleClient.delete(`/collections/${id}`);
    return response.data as Result<void>;
  },

  generateOutlines: async (id: number, articleCount?: number, topic?: string, keywords?: string[]) => {
    const response = await articleClient.post(`/collections/${id}/generate-outlines`, {
      collectionId: id,
      articleCount,
      topic,
      keywords,
    });
    return response.data as Result<CollectionOutline>;
  },

  saveOutlines: async (id: number, outlines: CollectionOutline) => {
    const response = await articleClient.post(`/collections/${id}/outlines`, outlines);
    return response.data as Result<Collection>;
  },

  generateArticle: async (id: number, outlineIndex: number) => {
    const response = await articleClient.post(`/collections/${id}/articles`, {
      collectionId: id,
      outlineIndex,
    });
    return response.data as Result<Article>;
  },

  generateAllArticles: async (id: number) => {
    const response = await articleClient.post(`/collections/${id}/articles/all`);
    return response.data as Result<{ collectionId: number; articleCount: number; articles: Article[] }>;
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

  outline: async (topic: string, keywords?: string[]) => {
    const response = await articleClient.post('/generate/outline', {
      topic,
      keywords,
    });
    return response.data as Result<GenerateResult>;
  },

  coverImage: async (title: string, style?: string) => {
    const response = await articleClient.post('/generate/cover-image', {
      title,
      style,
    });
    return response.data as Result<GenerateResult>;
  },

  titleStream: (
    topic: string,
    keywords: string[] | undefined,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ) => {
    return streamGenerate(
      '/generate/title/stream',
      { topic, keywords },
      onChunk,
      onComplete,
      onError
    );
  },

  summaryStream: (
    title: string,
    content: string | undefined,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ) => {
    return streamGenerate(
      '/generate/summary/stream',
      { title, content },
      onChunk,
      onComplete,
      onError
    );
  },

  contentStream: (
    title: string,
    outline: string | undefined,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ) => {
    return streamGenerate(
      '/generate/content/stream',
      { title, outline },
      onChunk,
      onComplete,
      onError
    );
  },

  outlineStream: (
    topic: string,
    keywords: string[] | undefined,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ) => {
    return streamGenerate(
      '/generate/outline/stream',
      { topic, keywords },
      onChunk,
      onComplete,
      onError
    );
  },
};

const getAuthHeader = (): Record<string, string> => {
  const token = localStorage.getItem('accessToken');
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
};

const streamGenerate = async (
  endpoint: string,
  body: any,
  onChunk: (chunk: string) => void,
  onComplete: () => void,
  onError: (error: string) => void
) => {
  try {
    const headers = {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    } as Record<string, string>;

    let response = await fetch(`${ARTICLE_BASE}${endpoint}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });

    if (response.status === 401) {
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) throw new Error('登录已过期，请重新登录');

        const refreshResponse = await fetch(`${API_BASE}/auth/refresh`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${refreshToken}`,
          },
          body: JSON.stringify({ refreshToken }),
        });

        const data = await refreshResponse.json();
        if (data.code === 200) {
          localStorage.setItem('accessToken', data.data.accessToken);
          localStorage.setItem('refreshToken', data.data.refreshToken);

          const retryHeaders = {
            'Content-Type': 'application/json',
            ...getAuthHeader(),
          } as Record<string, string>;

          response = await fetch(`${ARTICLE_BASE}${endpoint}`, {
            method: 'POST',
            headers: retryHeaders,
            body: JSON.stringify(body),
          });
        } else {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          window.location.href = '/login';
          return;
        }
      } catch (e) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return;
      }
    }

    if (!response.ok) {
      const errorText = await response.text();
      let errorMessage = '生成失败';
      try {
        const errorData = JSON.parse(errorText);
        errorMessage = errorData.message || errorMessage;
      } catch (e) {
        errorMessage = errorText || errorMessage;
      }
      throw new Error(errorMessage);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('无法读取响应流');
    }

    const decoder = new TextDecoder();
    let isFallback = false;
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const data = line.slice(6);
          if (data === '[DONE]') continue;

          if (data.includes('[FALLBACK]')) {
            isFallback = true;
            continue;
          }

          try {
            const parsed = JSON.parse(data);
            const content = parsed.choices?.[0]?.delta?.content;
            if (content) {
              onChunk(content);
            }
          } catch (e) {
            onChunk(data);
          }
        }
      }
    }

    if (buffer.length > 0) {
      if (buffer.startsWith('data: ')) {
        const data = buffer.slice(6);
        if (data !== '[DONE]') {
          try {
            const parsed = JSON.parse(data);
            const content = parsed.choices?.[0]?.delta?.content;
            if (content) {
              onChunk(content);
            }
          } catch (e) {
            onChunk(data);
          }
        }
      }
    }

    if (isFallback) {
      onChunk('\n\n[系统提示：当前使用模拟数据，配置 OpenAI API Key 后可获得真实 AI 生成内容]');
    }

    onComplete();
  } catch (error: any) {
    console.error('Stream error:', error);
    onError(error.message || '生成失败，请稍后重试');
  }
};
