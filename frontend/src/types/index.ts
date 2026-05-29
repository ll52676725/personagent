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

export interface KnowledgeBase {
  id: number;
  userId: number;
  name: string;
  description?: string;
  icon?: string;
  knowledgeCount: number;
  chunkCount: number;
  status: number;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeItem {
  id: number;
  userId: number;
  baseId: number;
  title: string;
  content?: string;
  sourceType: string;
  sourceUrl?: string;
  fileName?: string;
  fileType?: string;
  fileSize?: number;
  tags?: string;
  category?: string;
  chunkStatus: number;
  chunkCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeQueryResult {
  question: string;
  answer: string;
  sources: SourceReference[];
  model?: string;
  tokens?: number;
}

export interface SourceReference {
  knowledgeId: number;
  title: string;
  content: string;
  similarity: number;
}

export interface ImageFormatInfo {
  formatName: string;
  extensions: string[];
  mimeType: string;
  readable: boolean;
  writable: boolean;
  description: string;
}

export interface ImageConvertResult {
  originalFormat: string;
  targetFormat: string;
  originalFileName: string;
  convertedFileName: string;
  originalSize: number;
  convertedSize: number;
  width: number;
  height: number;
  mimeType: string;
  imageDataBase64: string;
}

export interface ImageConvertRequest {
  targetFormat: string;
  quality?: number;
  width?: number;
  height?: number;
}

// 文件格式转换相关类型
export interface FileFormatInfo {
  formatName: string;
  extensions: string[];
  mimeType: string;
  readable: boolean;
  writable: boolean;
  description: string;
}

export interface FileConvertResult {
  originalFormat: string;
  targetFormat: string;
  originalFileName: string;
  convertedFileName: string;
  originalSize: number;
  convertedSize: number;
  mimeType: string;
  fileDataBase64: string;
}

export interface FileConvertRequest {
  targetFormat: string;
}

// JSON格式化相关类型

/**
 * JSON错误详情
 * 描述JSON解析错误的详细信息，包括错误位置、原因和修复建议
 */
export interface JsonErrorDetail {
  /** 错误类型，如：语法错误、缺少引号、多余逗号等 */
  errorType: string;
  /** 错误发生的行号（从1开始） */
  lineNumber: number;
  /** 错误发生的列号（从1开始） */
  columnNumber: number;
  /** 错误位置附近的原始内容片段 */
  errorContext: string;
  /** 具体的错误描述信息 */
  message: string;
  /** 针对该错误的修复建议 */
  suggestion: string;
}

/**
 * JSON格式化结果
 * 包含格式化后的内容、错误信息和AI修复建议
 */
export interface JsonFormatResult {
  /** 格式化是否成功 */
  success: boolean;
  /** 格式化后的JSON字符串 */
  formattedJson: string;
  /** 原始JSON字符串（压缩格式） */
  compactJson: string;
  /** 错误详情列表，格式化失败时返回 */
  errors: JsonErrorDetail[];
  /** AI修复后的JSON字符串 */
  aiFixedJson: string;
  /** AI修复说明，描述AI对JSON进行了哪些修改 */
  aiFixDescription: string;
  /** AI修复是否成功 */
  aiFixSuccess: boolean;
  /** 格式化使用的缩进空格数 */
  indentSize: number;
  /** JSON数据结构类型，如：object（对象）、array（数组）等 */
  jsonType: string;
  /** 数据统计信息，包含键值对数量、数组长度等 */
  statistics: string;
}

/**
 * JSON格式化请求参数
 */
export interface JsonFormatRequest {
  content: string;
  indentSize?: number;
  sortKeys?: boolean;
}

export interface DriveInfo {
  driveLetter: string;
  displayName: string;
  totalSpace: number;
  usedSpace: number;
  freeSpace: number;
  fileSystem: string;
  driveType: string;
}

export interface FileTypeStat {
  category: string;
  label: string;
  size: number;
  fileCount: number;
  percentage: number;
  color: string;
}

export interface FolderStat {
  path: string;
  name: string;
  size: number;
  fileCount: number;
  percentage: number;
}

export interface CleanupSuggestion {
  type: string;
  title: string;
  description: string;
  path: string;
  size: number;
  riskLevel: string;
  action: string;
}

export interface DriveAnalysisResult {
  driveInfo: DriveInfo;
  fileTypeStats: FileTypeStat[];
  topFolders: FolderStat[];
  cleanupSuggestions: CleanupSuggestion[];
  totalScannedSize: number;
  totalScannedFiles: number;
  scanDurationMs: number;
}

export interface AICleanupSuggestion {
  category: string;
  title: string;
  description: string;
  path: string;
  estimatedSize: number;
  riskLevel: string;
  action: string;
  reason: string;
  priority: number;
}

export interface AIAnalysisResult {
  driveLetter: string;
  summary: string;
  totalReclaimableSpace: number;
  suggestions: AICleanupSuggestion[];
  analysisInsight: string;
  model?: string;
  tokens?: number;
  analysisDurationMs: number;
}

export interface RegistryIssue {
  id: string;
  category: string;
  categoryLabel: string;
  registryPath: string;
  valueName: string;
  issueType: string;
  issueTypeLabel: string;
  description: string;
  reason: string;
  riskLevel: string;
  severity: string;
  selected: boolean;
  regCommand: string;
}

export interface RegistryAnalysisResult {
  summary: string;
  totalIssues: number;
  analysisDurationMs: number;
  categoryStats: Record<string, number>;
  severityStats: Record<string, number>;
  issues: RegistryIssue[];
  disclaimer: string;
}

export interface CleanupScript {
  scriptName: string;
  scriptContent: string;
  scriptType: string;
  encoding: string;
  issueCount: number;
  warning: string;
  usageInstructions: string;
}

export interface GenerateScriptRequest {
  selectedIssueIds: string[];
  scriptType: string;
  includeBackup: boolean;
}
