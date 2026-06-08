import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { LoginResult, Agent, Article, Collection, CollectionOutline, GenerateResult, Result, SectionImageResult, SectionImageGenerateRequest, KnowledgeBase, KnowledgeItem, KnowledgeQueryResult, SourceReference, ImageFormatInfo, ImageConvertResult, FileFormatInfo, FileConvertResult, JsonFormatResult, JsonFormatRequest, DriveInfo, DriveAnalysisResult, AIAnalysisResult, RegistryAnalysisResult, CleanupScript, GenerateScriptRequest, RegistryAIAnalysisResult, CurrentIpInfo, PingResult, TracerouteResult, DnsResult, LanScanResult, ConnectivityAnalysis, PhotoSize, PhotoStandardizationResult, PhotoStandardizationRequest, IconDesignRequest, IconDesignResult, VirusScanResult, VirusAIAnalysisResult, RemediationScript, GenerateRemediationScriptRequest, RuleTemplate, RuleConfig, RulePullLog, RulePullResult, AIRuleGenerateRequest, CronGenerateRequest, CronGenerateResult, CronParseRequest, CronParseResult, CronNextTimesRequest, CronNextTimesResult, CronNLRequest, CronNLResult, RegexValidateRequest, RegexValidateResult, RegexGenerateRequest, RegexGenerateResult, RegexFixRequest, RegexFixResult, SqlFormatRequest, SqlFormatResult, AudioFormatInfo, SpeechToTextResult, ImageModerationRequest, ImageModerationResult, TechReportRequest, TechReportResult, SceneTemplate, AudienceType, DockerGenerateRequest, DockerGenerateResult, DockerDeployResult, DesktopShortcutRequest, DesktopShortcutResult } from '@/types';

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

const KNOWLEDGE_BASE = `${API_BASE}/agent-knowledge`;

const knowledgeClient: AxiosInstance = axios.create({
  baseURL: KNOWLEDGE_BASE,
  timeout: 120000,
});

const TOOLS_BASE = `${API_BASE}/agent-tools`;

const toolsClient: AxiosInstance = axios.create({
  baseURL: TOOLS_BASE,
  timeout: 120000,
});

const RULES_BASE = `${API_BASE}/agent-rules`;

const rulesClient: AxiosInstance = axios.create({
  baseURL: RULES_BASE,
  timeout: 120000,
});

const TECHREPORT_BASE = `${API_BASE}/agent-techreport`;

const techReportClient: AxiosInstance = axios.create({
  baseURL: TECHREPORT_BASE,
  timeout: 180000,
});

const attachAuth = (config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
};

const handleResponse = (response: AxiosResponse) => response;

const isFakeToken = (token: string | null) => token?.startsWith('fake-') && !token?.startsWith('fake-root-token-');

const handleError = async (error: any) => {
  const originalRequest = error.config;
  const currentAccessToken = localStorage.getItem('accessToken');
  if (currentAccessToken?.startsWith('fake-root-token-')) {
    return Promise.reject(error);
  }
  if (isFakeToken(currentAccessToken)) {
    return Promise.reject(error);
  }
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
      return Promise.reject(error);
    }
  }
  return Promise.reject(error);
};

apiClient.interceptors.request.use(attachAuth);
apiClient.interceptors.response.use(handleResponse, handleError);
articleClient.interceptors.request.use(attachAuth);
articleClient.interceptors.response.use(handleResponse, handleError);
knowledgeClient.interceptors.request.use(attachAuth);
knowledgeClient.interceptors.response.use(handleResponse, handleError);
toolsClient.interceptors.request.use(attachAuth);
toolsClient.interceptors.response.use(handleResponse, handleError);
rulesClient.interceptors.request.use(attachAuth);
rulesClient.interceptors.response.use(handleResponse, handleError);
techReportClient.interceptors.request.use(attachAuth);
techReportClient.interceptors.response.use(handleResponse, handleError);

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

  sectionImages: async (request: SectionImageGenerateRequest) => {
    const response = await articleClient.post('/generate/section-images', request);
    return response.data as Result<{ images: SectionImageResult[]; imageCount: number; successCount: number }>;
  },

  insertImages: async (request: SectionImageGenerateRequest) => {
    const response = await articleClient.post('/generate/insert-images', request);
    return response.data as Result<{ content: string; images: SectionImageResult[]; imageCount: number; successCount: number }>;
  },

  getImageCache: async () => {
    const response = await articleClient.get('/generate/image-cache');
    return response.data as Result<{ cacheSize: number }>;
  },

  clearImageCache: async () => {
    const response = await articleClient.delete('/generate/image-cache');
    return response.data as Result<void>;
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

export const knowledgeApi = {
  getBases: async () => {
    const response = await knowledgeClient.get('/bases');
    return response.data as Result<KnowledgeBase[]>;
  },

  createBase: async (name: string, description?: string, icon?: string) => {
    const response = await knowledgeClient.post('/bases', { name, description, icon });
    return response.data as Result<KnowledgeBase>;
  },

  getBase: async (id: number) => {
    const response = await knowledgeClient.get(`/bases/${id}`);
    return response.data as Result<KnowledgeBase>;
  },

  updateBase: async (id: number, data: { name?: string; description?: string; icon?: string }) => {
    const response = await knowledgeClient.put(`/bases/${id}`, data);
    return response.data as Result<KnowledgeBase>;
  },

  deleteBase: async (id: number) => {
    const response = await knowledgeClient.delete(`/bases/${id}`);
    return response.data as Result<void>;
  },

  getItems: async (baseId: number) => {
    const response = await knowledgeClient.get(`/bases/${baseId}/items`);
    return response.data as Result<KnowledgeItem[]>;
  },

  getAllItems: async () => {
    const response = await knowledgeClient.get('/items');
    return response.data as Result<KnowledgeItem[]>;
  },

  createItem: async (baseId: number, data: { title: string; content?: string; sourceType?: string; sourceUrl?: string; tags?: string; category?: string }) => {
    const response = await knowledgeClient.post(`/bases/${baseId}/items`, data);
    return response.data as Result<KnowledgeItem>;
  },

  getItem: async (id: number) => {
    const response = await knowledgeClient.get(`/items/${id}`);
    return response.data as Result<KnowledgeItem>;
  },

  updateItem: async (id: number, data: { title?: string; content?: string; tags?: string; category?: string }) => {
    const response = await knowledgeClient.put(`/items/${id}`, data);
    return response.data as Result<KnowledgeItem>;
  },

  deleteItem: async (id: number) => {
    const response = await knowledgeClient.delete(`/items/${id}`);
    return response.data as Result<void>;
  },

  importUrl: async (url: string, baseId: number, title?: string, category?: string, tags?: string) => {
    const response = await knowledgeClient.post('/import/url', { url, baseId, title, category, tags });
    return response.data as Result<KnowledgeItem>;
  },

  importFile: async (baseId: number, file: File, category?: string, tags?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('baseId', baseId.toString());
    if (category) formData.append('category', category);
    if (tags) formData.append('tags', tags);
    const response = await knowledgeClient.post('/import/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Result<KnowledgeItem>;
  },

  importFiles: async (baseId: number, files: File[], category?: string, tags?: string) => {
    const formData = new FormData();
    files.forEach(f => formData.append('files', f));
    formData.append('baseId', baseId.toString());
    if (category) formData.append('category', category);
    if (tags) formData.append('tags', tags);
    const response = await knowledgeClient.post('/import/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Result<KnowledgeItem[]>;
  },

  reprocess: async (id: number) => {
    const response = await knowledgeClient.post(`/items/${id}/reprocess`);
    return response.data as Result<void>;
  },

  query: async (question: string, baseId?: number | null, topK?: number, similarityThreshold?: number) => {
    const response = await knowledgeClient.post('/query', {
      question,
      baseId: baseId || undefined,
      topK,
      similarityThreshold,
    });
    return response.data as Result<KnowledgeQueryResult>;
  },

  queryStream: (
    question: string,
    baseId: number | null | undefined,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ) => {
    return streamKnowledgeQuery(
      '/query/stream',
      { question, baseId: baseId || undefined },
      onChunk,
      onComplete,
      onError
    );
  },

  search: async (keyword: string, baseId?: number | null, topK?: number) => {
    const response = await knowledgeClient.post('/search', {
      keyword,
      baseId: baseId || undefined,
      topK,
    });
    return response.data as Result<SourceReference[]>;
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
      const currentAccessToken = localStorage.getItem('accessToken');
      if (currentAccessToken?.startsWith('fake-root-token-')) {
        onError('后端认证失败，此功能暂不可用');
        return;
      }
      if (isFakeToken(currentAccessToken)) {
        onError('后端服务未启动，此功能暂不可用');
        return;
      }
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
          onError('登录已过期，请重新登录');
          return;
        }
      } catch (e) {
        onError('登录已过期，请重新登录');
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

const streamKnowledgeQuery = async (
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

    const response = await fetch(`${KNOWLEDGE_BASE}${endpoint}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorText = await response.text();
      let errorMessage = '查询失败';
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

    onComplete();
  } catch (error: any) {
    console.error('Knowledge stream error:', error);
    onError(error.message || '查询失败，请稍后重试');
  }
};

export const toolsApi = {
  getImageFormats: async () => {
    const response = await toolsClient.get('/image/formats');
    return response.data as Result<ImageFormatInfo[]>;
  },

  convertImage: async (file: File, targetFormat: string, quality?: number, width?: number, height?: number) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('targetFormat', targetFormat);
    if (quality !== undefined) formData.append('quality', quality.toString());
    if (width !== undefined) formData.append('width', width.toString());
    if (height !== undefined) formData.append('height', height.toString());
    const response = await toolsClient.post('/image/convert', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Result<ImageConvertResult>;
  },

  downloadConvertedImage: async (file: File, targetFormat: string, quality?: number, width?: number, height?: number) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('targetFormat', targetFormat);
    if (quality !== undefined) formData.append('quality', quality.toString());
    if (width !== undefined) formData.append('width', width.toString());
    if (height !== undefined) formData.append('height', height.toString());
    const response = await toolsClient.post('/image/convert-download', formData, {
      responseType: 'blob',
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Blob;
  },

  // 文件格式转换API
  getFileFormats: async () => {
    const response = await toolsClient.get('/file/formats');
    return response.data as Result<FileFormatInfo[]>;
  },

  convertFile: async (file: File, targetFormat: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('targetFormat', targetFormat);
    const response = await toolsClient.post('/file/convert', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Result<FileConvertResult>;
  },

  downloadConvertedFile: async (file: File, targetFormat: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('targetFormat', targetFormat);
    const response = await toolsClient.post('/file/convert-download', formData, {
      responseType: 'blob',
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Blob;
  },

  // JSON格式化工具API

  /**
   * 格式化JSON字符串
   * @param request 格式化请求参数
   */
  formatJson: async (request: JsonFormatRequest) => {
    const response = await toolsClient.post('/json/format', request);
    return response.data as Result<JsonFormatResult>;
  },

  /**
   * 压缩JSON字符串
   * @param request 压缩请求参数
   */
  compactJson: async (request: JsonFormatRequest) => {
    const response = await toolsClient.post('/json/compact', request);
    return response.data as Result<JsonFormatResult>;
  },

  /**
   * 校验JSON语法
   * @param request 校验请求参数
   */
  validateJson: async (request: JsonFormatRequest) => {
    const response = await toolsClient.post('/json/validate', request);
    return response.data as Result<JsonFormatResult>;
  },

  /**
   * 使用AI修复JSON格式错误
   * @param request 修复请求参数
   */
  fixJsonWithAI: async (request: JsonFormatRequest) => {
    const response = await toolsClient.post('/json/fix', request);
    return response.data as Result<JsonFormatResult>;
  },

  getAvailableDrives: async () => {
    const response = await toolsClient.get('/disk/drives');
    return response.data as Result<DriveInfo[]>;
  },

  analyzeDrive: async (drive: string, maxDepth?: number) => {
    const params: Record<string, string | number> = { drive };
    if (maxDepth !== undefined) params.maxDepth = maxDepth;
    const response = await toolsClient.get('/disk/analyze', {
      params,
      timeout: 600000,
    });
    return response.data as Result<DriveAnalysisResult>;
  },

  aiAnalyzeDrive: async (drive: string, maxDepth?: number) => {
    const params: Record<string, string | number> = { drive };
    if (maxDepth !== undefined) params.maxDepth = maxDepth;
    const response = await toolsClient.get('/disk/ai-analyze', {
      params,
      timeout: 600000,
    });
    return response.data as Result<AIAnalysisResult>;
  },

  analyzeRegistry: async () => {
    const response = await toolsClient.get('/registry/analyze', {
      timeout: 600000,
    });
    return response.data as Result<RegistryAnalysisResult>;
  },

  generateCleanupScript: async (request: GenerateScriptRequest) => {
    const response = await toolsClient.post('/registry/generate-script', request, {
      timeout: 60000,
    });
    return response.data as Result<CleanupScript>;
  },

  downloadCleanupScript: async (request: GenerateScriptRequest) => {
    const response = await toolsClient.post('/registry/download-script', request, {
      responseType: 'blob',
      timeout: 60000,
    });
    return response.data as Blob;
  },

  aiAnalyzeRegistry: async () => {
    const response = await toolsClient.get('/registry/ai-analyze', {
      timeout: 600000,
    });
    return response.data as Result<RegistryAIAnalysisResult>;
  },

  getCurrentIpInfo: async () => {
    const response = await toolsClient.get('/ip/current', {
      timeout: 30000,
    });
    return response.data as Result<CurrentIpInfo>;
  },

  ping: async (target: string, count?: number) => {
    const params: Record<string, string | number> = { target };
    if (count !== undefined) params.count = count;
    const response = await toolsClient.get('/ip/ping', {
      params,
      timeout: 60000,
    });
    return response.data as Result<PingResult>;
  },

  traceroute: async (target: string) => {
    const response = await toolsClient.get('/ip/traceroute', {
      params: { target },
      timeout: 180000,
    });
    return response.data as Result<TracerouteResult>;
  },

  resolveDns: async (domain: string) => {
    const response = await toolsClient.get('/ip/dns', {
      params: { domain },
      timeout: 30000,
    });
    return response.data as Result<DnsResult>;
  },

  scanLan: async () => {
    const response = await toolsClient.get('/ip/lan-scan', {
      timeout: 30000,
    });
    return response.data as Result<LanScanResult>;
  },

  analyzeConnectivity: async (target: string) => {
    const response = await toolsClient.get('/ip/analyze', {
      params: { target },
      timeout: 180000,
    });
    return response.data as Result<ConnectivityAnalysis>;
  },

  getPhotoSizes: async () => {
    const response = await toolsClient.get('/photo/sizes');
    return response.data as Result<PhotoSize[]>;
  },

  getPhotoBackgrounds: async () => {
    const response = await toolsClient.get('/photo/backgrounds');
    return response.data as Result<string[]>;
  },

  standardizePhoto: async (request: PhotoStandardizationRequest) => {
    const formData = new FormData();
    if (request.file) {
      formData.append('file', request.file);
    }
    formData.append('photoSize', request.photoSize);
    formData.append('backgroundColor', request.backgroundColor);
    formData.append('jpegOutput', request.jpegOutput.toString());
    if (request.quality !== undefined) {
      formData.append('quality', request.quality.toString());
    }
    if (request.autoDetectFace !== undefined) {
      formData.append('autoDetectFace', request.autoDetectFace.toString());
    }
    if (request.headTopMargin !== undefined) {
      formData.append('headTopMargin', request.headTopMargin.toString());
    }
    if (request.headBottomMargin !== undefined) {
      formData.append('headBottomMargin', request.headBottomMargin.toString());
    }
    const response = await toolsClient.post('/photo/standardize', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Result<PhotoStandardizationResult>;
  },

  downloadStandardizedPhoto: async (request: PhotoStandardizationRequest) => {
    const formData = new FormData();
    if (request.file) {
      formData.append('file', request.file);
    }
    formData.append('photoSize', request.photoSize);
    formData.append('backgroundColor', request.backgroundColor);
    formData.append('jpegOutput', request.jpegOutput.toString());
    if (request.quality !== undefined) {
      formData.append('quality', request.quality.toString());
    }
    if (request.autoDetectFace !== undefined) {
      formData.append('autoDetectFace', request.autoDetectFace.toString());
    }
    if (request.headTopMargin !== undefined) {
      formData.append('headTopMargin', request.headTopMargin.toString());
    }
    if (request.headBottomMargin !== undefined) {
      formData.append('headBottomMargin', request.headBottomMargin.toString());
    }
    const response = await toolsClient.post('/photo/standardize-download', formData, {
      responseType: 'blob',
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data as Blob;
  },

  generateIconDesign: async (request: IconDesignRequest) => {
    const response = await toolsClient.post('/icon/design', request);
    return response.data as Result<IconDesignResult>;
  },

  /**
   * 执行系统安全扫描
   * <p>扫描内容包括：可疑进程、启动项、临时目录、系统服务、计划任务、
   * 系统漏洞、开放端口、过期软件、安全设置等9个维度
   * <p>超时时间：10分钟（扫描可能需要较长时间）
   * 
   * @returns 扫描结果，包含可疑程序列表、漏洞列表、健康评分等
   */
  scanVirus: async () => {
    const response = await toolsClient.get('/virus/scan', {
      timeout: 600000,
    });
    return response.data as Result<VirusScanResult>;
  },

  /**
   * AI智能分析系统安全状况
   * <p>先执行完整的系统扫描，然后调用AI对扫描结果进行深度分析，
   * 提供专业的安全评估、问题诊断和修复建议
   * <p>当AI服务不可用时，自动降级为本地规则引擎分析
   * <p>超时时间：10分钟（AI分析可能需要较长时间）
   * 
   * @returns AI分析结果，包含安全评估、建议列表、优化方案等
   */
  aiAnalyzeVirus: async () => {
    const response = await toolsClient.get('/virus/ai-analyze', {
      timeout: 600000,
    });
    return response.data as Result<VirusAIAnalysisResult>;
  },

  /**
   * 生成修复脚本
   * <p>根据用户选择的安全问题ID列表，生成Windows批处理修复脚本
   * <p>脚本内容包含自动修复命令、使用说明和警告信息
   * <p>超时时间：1分钟
   * 
   * @param request 请求参数，包含选中的问题ID列表
   * @returns 修复脚本信息，包含脚本内容、使用说明、警告信息等
   */
  generateRemediationScript: async (request: GenerateRemediationScriptRequest) => {
    const response = await toolsClient.post('/virus/generate-script', request, {
      timeout: 60000,
    });
    return response.data as Result<RemediationScript>;
  },

  /**
   * 下载修复脚本文件
   * <p>生成修复脚本并以Blob形式返回，支持浏览器直接下载
   * <p>脚本文件编码：UTF-8，文件名包含时间戳避免重复
   * <p>超时时间：1分钟
   * 
   * @param request 请求参数，包含选中的问题ID列表
   * @returns Blob对象，可用于创建下载链接
   */
  downloadRemediationScript: async (request: GenerateRemediationScriptRequest) => {
    const response = await toolsClient.post('/virus/download-script', request, {
      responseType: 'blob',
      timeout: 60000,
    });
    return response.data as Blob;
  },

  generateCron: async (request: CronGenerateRequest) => {
    const response = await toolsClient.post('/cron/generate', request);
    return response.data as Result<CronGenerateResult>;
  },

  parseCron: async (request: CronParseRequest) => {
    const response = await toolsClient.post('/cron/parse', request);
    return response.data as Result<CronParseResult>;
  },

  getCronNextTimes: async (request: CronNextTimesRequest) => {
    const response = await toolsClient.post('/cron/next-times', request);
    return response.data as Result<CronNextTimesResult>;
  },

  parseCronNaturalLanguage: async (request: CronNLRequest) => {
    const response = await toolsClient.post('/cron/parse-natural-language', request, {
      timeout: 60000,
    });
    return response.data as Result<CronNLResult>;
  },

  validateRegex: async (request: RegexValidateRequest) => {
    const response = await toolsClient.post('/regex/validate', request);
    return response.data as Result<RegexValidateResult>;
  },

  generateRegex: async (request: RegexGenerateRequest) => {
    const response = await toolsClient.post('/regex/generate', request, {
      timeout: 60000,
    });
    return response.data as Result<RegexGenerateResult>;
  },

  fixRegex: async (request: RegexFixRequest) => {
    const response = await toolsClient.post('/regex/fix', request, {
      timeout: 60000,
    });
    return response.data as Result<RegexFixResult>;
  },

  /**
   * 格式化SQL语句
   * @param request 格式化请求参数
   */
  formatSql: async (request: SqlFormatRequest) => {
    const response = await toolsClient.post('/sql/format', request);
    return response.data as Result<SqlFormatResult>;
  },

  /**
   * 压缩SQL语句
   * @param request 压缩请求参数
   */
  compactSql: async (request: SqlFormatRequest) => {
    const response = await toolsClient.post('/sql/compact', request);
    return response.data as Result<SqlFormatResult>;
  },

  /**
   * 校验SQL语法
   * @param request 校验请求参数
   */
  validateSql: async (request: SqlFormatRequest) => {
    const response = await toolsClient.post('/sql/validate', request);
    return response.data as Result<SqlFormatResult>;
  },

  /**
   * 使用AI修复SQL语法错误
   * @param request 修复请求参数
   */
  fixSqlWithAI: async (request: SqlFormatRequest) => {
    const response = await toolsClient.post('/sql/fix', request, {
      timeout: 60000,
    });
    return response.data as Result<SqlFormatResult>;
  },

  /**
   * 使用AI优化SQL性能
   * @param request 优化请求参数
   */
  optimizeSqlWithAI: async (request: SqlFormatRequest) => {
    const response = await toolsClient.post('/sql/optimize', request, {
      timeout: 60000,
    });
    return response.data as Result<SqlFormatResult>;
  },

  getAudioFormats: async () => {
    const response = await toolsClient.get('/speech/formats');
    return response.data as Result<AudioFormatInfo[]>;
  },

  speechToText: async (file: File, language: string = 'zh') => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('language', language);
    const response = await toolsClient.post('/speech/transcribe', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000,
    });
    return response.data as Result<SpeechToTextResult>;
  },

  /**
   * 图片内容检测
   * <p>检测图片中的涉黄、涉政、涉爆等违规内容
   * <p>使用多模态AI模型进行智能分析，支持降级模式
   * 
   * @param request 检测请求参数，包含图片Base64数据和检测配置
   * @returns 检测结果，包含各分类检测详情、总体结论、处理建议等
   */
  moderateImage: async (request: ImageModerationRequest) => {
    console.log('[ImageModeration] 开始图片内容检测:', {
      fileName: request.fileName,
      fileSize: request.fileSize,
      mimeType: request.mimeType
    });
    const response = await toolsClient.post('/image-moderation/detect', request, {
      timeout: 120000,
    });
    return response.data as Result<ImageModerationResult>;
  },

  /**
   * 下载图片内容检测报告
   * <p>将检测结果生成为文本报告文件，支持浏览器直接下载
   * 
   * @param result 检测结果数据
   * @returns Blob对象，可用于创建下载链接
   */
  downloadModerationReport: async (result: ImageModerationResult) => {
    console.log('[ImageModeration] 下载检测报告, 任务ID:', result.taskId);
    const response = await toolsClient.post('/image-moderation/download-report', result, {
      responseType: 'blob',
      timeout: 30000,
    });
    return response.data as Blob;
  },

  generateDockerfile: async (request: DockerGenerateRequest) => {
    const response = await toolsClient.post('/docker/generate', request, {
      timeout: 60000,
    });
    return response.data as Result<DockerGenerateResult>;
  },

  deployDocker: async (request: DockerGenerateRequest) => {
    const response = await toolsClient.post('/docker/deploy', request, {
      timeout: 600000,
    });
    return response.data as Result<DockerDeployResult>;
  },

  checkDocker: async () => {
    const response = await toolsClient.get('/docker/check-docker', {
      timeout: 10000,
    });
    return response.data as Result<boolean>;
  },

  createDesktopShortcut: async (request: DesktopShortcutRequest) => {
    const response = await toolsClient.post('/desktop-shortcut/create', request, {
      timeout: 30000,
    });
    return response.data as Result<DesktopShortcutResult>;
  },

  getDesktopPath: async () => {
    const response = await toolsClient.get('/desktop-shortcut/desktop-path', {
      timeout: 10000,
    });
    return response.data as Result<string>;
  },
};

export const rulesApi = {
  getTemplates: async () => {
    const response = await rulesClient.get('/templates');
    return response.data as Result<RuleTemplate[]>;
  },

  getTemplatesByCategory: async (category: string) => {
    const response = await rulesClient.get(`/templates/category/${category}`);
    return response.data as Result<RuleTemplate[]>;
  },

  getSystemTemplates: async () => {
    const response = await rulesClient.get('/templates/system');
    return response.data as Result<RuleTemplate[]>;
  },

  getPublicTemplates: async () => {
    const response = await rulesClient.get('/templates/public');
    return response.data as Result<RuleTemplate[]>;
  },

  getTemplate: async (id: number) => {
    const response = await rulesClient.get(`/templates/${id}`);
    return response.data as Result<RuleTemplate>;
  },

  createTemplate: async (data: Partial<RuleTemplate>) => {
    const response = await rulesClient.post('/templates', data);
    return response.data as Result<RuleTemplate>;
  },

  updateTemplate: async (id: number, data: Partial<RuleTemplate>) => {
    const response = await rulesClient.put(`/templates/${id}`, data);
    return response.data as Result<RuleTemplate>;
  },

  deleteTemplate: async (id: number) => {
    const response = await rulesClient.delete(`/templates/${id}`);
    return response.data as Result<void>;
  },

  copyTemplate: async (id: number) => {
    const response = await rulesClient.post(`/templates/${id}/copy`);
    return response.data as Result<RuleTemplate>;
  },

  getConfigs: async () => {
    const response = await rulesClient.get('/configs');
    return response.data as Result<RuleConfig[]>;
  },

  getConfigsByCategory: async (category: string) => {
    const response = await rulesClient.get(`/configs/category/${category}`);
    return response.data as Result<RuleConfig[]>;
  },

  getConfigsByProject: async (projectPath: string) => {
    const response = await rulesClient.get(`/configs/project`, { params: { projectPath } });
    return response.data as Result<RuleConfig[]>;
  },

  getConfig: async (id: number) => {
    const response = await rulesClient.get(`/configs/${id}`);
    return response.data as Result<RuleConfig>;
  },

  createConfig: async (data: Partial<RuleConfig>) => {
    const response = await rulesClient.post('/configs', data);
    return response.data as Result<RuleConfig>;
  },

  updateConfig: async (id: number, data: Partial<RuleConfig>) => {
    const response = await rulesClient.put(`/configs/${id}`, data);
    return response.data as Result<RuleConfig>;
  },

  deleteConfig: async (id: number) => {
    const response = await rulesClient.delete(`/configs/${id}`);
    return response.data as Result<void>;
  },

  pullRules: async (data: { templateId?: number; configId?: number; conflictStrategy?: string; previewOnly?: boolean }) => {
    const response = await rulesClient.post('/configs/pull', data);
    return response.data as Result<RulePullResult>;
  },

  getConflicts: async () => {
    const response = await rulesClient.get('/conflicts');
    return response.data as Result<RulePullLog[]>;
  },

  getConflict: async (id: number) => {
    const response = await rulesClient.get(`/conflicts/${id}`);
    return response.data as Result<RulePullLog>;
  },

  resolveConflict: async (data: { logId: number; resolutionStrategy: string; mergedContent?: string; renameSuffix?: string }) => {
    const response = await rulesClient.post('/conflicts/resolve', data);
    return response.data as Result<RulePullResult>;
  },

  generateRules: async (data: AIRuleGenerateRequest) => {
    const response = await rulesClient.post('/ai/generate', data);
    return response.data as Result<string>;
  },

  generateAndSaveRules: async (data: AIRuleGenerateRequest) => {
    const response = await rulesClient.post('/ai/generate/save', data);
    return response.data as Result<RuleTemplate>;
  },

  quickGenerateRules: async (category: string, description: string, targetTool?: string) => {
    const params: Record<string, string> = { category, description };
    if (targetTool) params.targetTool = targetTool;
    const response = await rulesClient.get('/ai/quick', { params });
    return response.data as Result<string>;
  },
};

export const techReportApi = {
  generate: async (request: TechReportRequest) => {
    const response = await techReportClient.post('/generate', request);
    return response.data as Result<TechReportResult>;
  },

  generateOutline: async (request: TechReportRequest) => {
    const response = await techReportClient.post('/generate/outline', request);
    return response.data as Result<TechReportResult>;
  },

  getScenes: async () => {
    const response = await techReportClient.get('/scenes');
    return response.data as Result<SceneTemplate[]>;
  },

  getAudiences: async () => {
    const response = await techReportClient.get('/audiences');
    return response.data as Result<AudienceType[]>;
  },

  exportMarkdown: async (result: TechReportResult) => {
    const response = await techReportClient.post('/export/markdown', result);
    return response.data as Result<string>;
  },

  exportPptOutline: async (result: TechReportResult) => {
    const response = await techReportClient.post('/export/ppt-outline', result);
    return response.data as Result<string>;
  },

  generateStream: (
    request: TechReportRequest,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: string) => void
  ) => {
    return streamTechReport(
      '/generate/stream',
      request,
      onChunk,
      onComplete,
      onError
    );
  },

  downloadMarkdown: (result: TechReportResult, filename: string = 'tech-report.md') => {
    const markdown = result.contentMarkdown || '';
    const blob = new Blob([markdown], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  },

  downloadPptOutline: (result: TechReportResult, filename: string = 'ppt-outline.txt') => {
    techReportClient.post('/export/ppt-outline', result)
      .then(response => {
        const content = response.data.data;
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
      })
      .catch(error => console.error('下载PPT大纲失败:', error));
  },
};

const streamTechReport = async (
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

    let response = await fetch(`${TECHREPORT_BASE}${endpoint}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });

    if (response.status === 401) {
      const currentAccessToken = localStorage.getItem('accessToken');
      if (currentAccessToken?.startsWith('fake-root-token-')) {
        onError('后端认证失败，此功能暂不可用');
        return;
      }
      if (isFakeToken(currentAccessToken)) {
        onError('后端服务未启动，此功能暂不可用');
        return;
      }
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

          response = await fetch(`${TECHREPORT_BASE}${endpoint}`, {
            method: 'POST',
            headers: retryHeaders,
            body: JSON.stringify(body),
          });
        } else {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          onError('登录已过期，请重新登录');
          return;
        }
      } catch (e) {
        onError('登录已过期，请重新登录');
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

    onComplete();
  } catch (error: any) {
    console.error('Tech report stream error:', error);
    onError(error.message || '生成失败，请稍后重试');
  }
};
