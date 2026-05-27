import type { LucideIcon } from 'lucide-react';

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
  collectionId?: number;
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

export interface Collection {
  id: number;
  userId: number;
  title: string;
  coverImage?: string;
  description?: string;
  outlines?: string;
  articleCount: number;
  status: number;
  createdAt: string;
  updatedAt: string;
}

export interface OutlineItem {
  title: string;
  summary: string;
  keyPoints: string;
  order: number;
}

export interface CollectionOutline {
  collectionId: number;
  title: string;
  summary: string;
  content?: string;
  outlines: OutlineItem[];
}

export interface GenerateResult {
  type: string;
  items?: string[];
  content?: string;
  model?: string;
  tokens?: number;
}

export interface SectionImageResult {
  sectionTitle: string;
  imageUrl: string;
  caption: string;
  insertPosition: string;
  success: boolean;
}

export interface SectionImageGenerateRequest {
  articleTitle: string;
  content: string;
  platform?: string;
  imageCount?: number;
  style?: string;
}

export interface Result<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface DisplayField {
  label: string;
  value: string;
  icon: LucideIcon;
}

export interface InputField {
  label: string;
  value: string;
  onChange: (value: string) => void;
  icon: LucideIcon;
  type?: 'text' | 'password';
  placeholder?: string;
}

export interface ToggleField {
  label: string;
  value: boolean;
  onChange: (value: boolean) => void;
}

export interface SettingsSection {
  title: string;
  icon: LucideIcon;
  gradient: string;
  displayFields?: DisplayField[];
  inputFields?: InputField[];
  toggles?: ToggleField[];
}
