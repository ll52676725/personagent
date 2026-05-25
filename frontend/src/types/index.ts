export interface User {
  id: number;
  username: string;
  email: string;
  avatar?: string;
  status: number;
  createdAt: string;
  updatedAt: string;
}

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  avatar?: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserInfo;
}

export interface Agent {
  id: number;
  name: string;
  code: string;
  description: string;
  moduleName: string;
  icon?: string;
  status: number;
  createdAt: string;
}

export interface AgentPermission {
  id: number;
  userId: number;
  agentId: number;
  status: number;
  applyReason: string;
  rejectReason?: string;
  approvedAt?: string;
  createdAt: string;
}

export interface Article {
  id: number;
  userId: number;
  title: string;
  summary?: string;
  content?: string;
  coverImage?: string;
  tags?: string;
  status: number;
  publishLinks?: string;
  createdAt: string;
  updatedAt: string;
  publishedAt?: string;
}

export interface GenerateResult {
  type: string;
  items?: string[];
  content?: string;
  model?: string;
  tokens?: number;
}

export interface Result<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}
