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

export interface RegistryAISuggestion {
  category: string;
  categoryLabel: string;
  title: string;
  description: string;
  registryPaths: string[];
  issueCount: number;
  riskLevel: string;
  action: string;
  reason: string;
  priority: number;
  impact: string;
  precaution: string;
}

export interface RegistryAIAnalysisResult {
  summary: string;
  systemHealthScore: string;
  systemHealthLevel: string;
  totalIssues: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  suggestions: RegistryAISuggestion[];
  analysisInsight: string;
  optimizationAdvice: string;
  model?: string;
  tokens?: number;
  analysisDurationMs: number;
}

export interface NetworkInterfaceInfo {
  name: string;
  displayName: string;
  ipv4Address: string;
  ipv6Address: string;
  subnetMask: string;
  macAddress: string;
  up: boolean;
  loopback: boolean;
  mtu: number;
}

export interface CurrentIpInfo {
  publicIp: string;
  publicIpSource: string;
  hostname: string;
  networkInterfaces: NetworkInterfaceInfo[];
  defaultGateway: string;
  dnsServer: string;
}

export interface PingResult {
  target: string;
  reachable: boolean;
  ipAddress: string;
  pingTimeMs: number;
  ttl: number | null;
  packetsSent: number;
  packetsReceived: number;
  packetLossRate: number;
  errorMessage?: string;
}

export interface TracerouteHop {
  hop: number;
  host: string;
  ip: string;
  latency1: number;
  latency2: number;
  latency3: number;
  timeout: boolean;
}

export interface TracerouteResult {
  target: string;
  targetIp: string;
  hops: TracerouteHop[];
  totalHops: number;
  reachedTarget: boolean;
  blockHop: number | null;
  blockIp: string | null;
  blockAnalysis: string | null;
  durationMs: number;
  errorMessage?: string;
}

export interface DnsResult {
  domain: string;
  resolvedIp: string;
  success: boolean;
  dnsServer?: string;
  queryTimeMs: number;
  errorMessage?: string;
}

export interface ArpEntry {
  ipAddress: string;
  macAddress: string;
  type: string;
  interfaceName: string;
}

export interface LanScanResult {
  subnet: string;
  interfaceName: string;
  arpEntries: ArpEntry[];
  totalDevices: number;
  scanDurationMs: number;
}

export interface ConnectivityAnalysis {
  sourceIp: string;
  targetIp: string;
  targetHost: string;
  isLan: boolean;
  pingable: boolean;
  pingResult: PingResult;
  tracerouteResult: TracerouteResult | null;
  dnsResult: DnsResult | null;
  overallStatus: string;
  diagnosis: string;
  suggestions: string[];
}

export interface PhotoSize {
  code: string;
  name: string;
  widthMm: string;
  heightMm: string;
  widthPx: number;
  heightPx: number;
  description: string;
  usage: string;
}

export interface PhotoStandardizationResult {
  originalFileName: string;
  convertedFileName: string;
  originalSize: number;
  convertedSize: number;
  width: number;
  height: number;
  photoSize: string;
  backgroundColor: string;
  dpi: string;
  mimeType: string;
  imageDataBase64: string;
}

export interface PhotoStandardizationRequest {
  file?: File;
  photoSize: string;
  backgroundColor: string;
  jpegOutput: boolean;
  quality?: number;
  autoDetectFace?: boolean;
  headTopMargin?: number;
  headBottomMargin?: number;
}

export interface IconDesignRequest {
  brandName?: string;
  description?: string;
  iconType?: string;
  iconCategory?: string;
  industry?: string;
  stylePreference?: string;
  colorPreference?: string;
}

export interface IconDesignResult {
  brandName: string;
  initial: string;
  iconCategory: 'text' | 'graphic';
  graphicShape: string;
  suggestedStyle: string;
  suggestedShape: string;
  primaryColor: string;
  secondaryColor: string;
  bgColor: string;
  textColor: string;
  subText: string;
  designRationale: string;
  decorativeElements: string[];
}

/**
 * 可疑程序数据接口
 * <p>描述扫描发现的可疑程序的详细信息，包括基本信息、风险评估、
 * 可疑行为分析和处理建议等
 */
export interface SuspiciousProgram {
  /** 唯一标识符，用于前端选择和操作 */
  id: string;
  /** 程序名称 */
  programName: string;
  /** 程序文件完整路径 */
  programPath: string;
  /** 进程名称（如果正在运行） */
  processName: string;
  /** 进程ID（如果正在运行） */
  processId: number;
  /** 分类编码：suspicious_process/unknown_startup等 */
  category: string;
  /** 分类中文名称，用于界面显示 */
  categoryLabel: string;
  /** 风险等级：high（高危）/medium（中危）/low（低危） */
  severity: string;
  /** 程序描述信息 */
  description: string;
  /** 风险原因分析，说明为什么被判定为可疑 */
  riskReason: string;
  /** 可疑行为列表，列举该程序的可疑行为特征 */
  suspiciousBehaviors: string[];
  /** 关联的注册表路径（如适用） */
  registryPath: string;
  /** 启动类型：自动/手动/禁用等（如适用） */
  startupType: string;
  /** 程序发行公司名称 */
  company: string;
  /** 文件版本号 */
  fileVersion: string;
  /** 文件大小，单位：字节 */
  fileSize: number;
  /** 最后修改时间，ISO格式字符串 */
  lastModified: string;
  /** 数字签名信息 */
  digitalSignature: string;
  /** 是否已签名：true-已签名，false-未签名 */
  isSigned: boolean;
  /** 前端是否选中：true-已选中，false-未选中 */
  selected: boolean;
  /** 处理建议，说明应该如何处理此程序 */
  recommendation: string;
}

/**
 * 安全漏洞数据接口
 * <p>描述扫描发现的系统安全漏洞的详细信息，包括漏洞描述、
 * 受影响组件、风险评估和修复步骤等
 */
export interface Vulnerability {
  /** 唯一标识符，用于前端选择和操作 */
  id: string;
  /** 漏洞官方编号，如CVE编号等 */
  vulnerabilityId: string;
  /** 漏洞标题，简要描述漏洞内容 */
  title: string;
  /** 分类编码：os_vulnerability/open_port等 */
  category: string;
  /** 分类中文名称，用于界面显示 */
  categoryLabel: string;
  /** 风险等级：high（高危）/medium（中危）/low（低危） */
  severity: string;
  /** 漏洞详细描述，说明漏洞原理和影响 */
  description: string;
  /** 受影响的组件名称，如操作系统、软件名称等 */
  affectedComponent: string;
  /** 当前安装的版本号 */
  installedVersion: string;
  /** 修复此漏洞的版本号 */
  fixedVersion: string;
  /** CVSS评分，0-10分，分数越高越严重 */
  cvssScore: string;
  /** CVE漏洞编号，如CVE-2025-0001 */
  cveId: string;
  /** 漏洞发布日期，ISO格式字符串 */
  publishDate: string;
  /** 检测方法，说明如何发现此漏洞 */
  detectionMethod: string;
  /** 修复步骤列表，按顺序执行可修复此漏洞 */
  remediationSteps: string[];
  /** 风险等级说明：critical/high/medium/low */
  riskLevel: string;
  /** 漏洞利用状态：是否已有公开利用代码 */
  exploitStatus: string;
  /** 前端是否选中：true-已选中，false-未选中 */
  selected: boolean;
}

/**
 * 病毒扫描结果数据接口
 * <p>包含完整的系统安全扫描结果，包括可疑程序列表、安全漏洞列表、
 * 统计信息、健康评分等
 */
export interface VirusScanResult {
  /** 扫描结果摘要，简要说明发现的问题和整体安全状况 */
  summary: string;
  /** 系统健康等级：优秀/良好/一般/较差/危险 */
  systemHealthLevel: string;
  /** 系统健康评分，0-100分，分数越高越安全 */
  systemHealthScore: number;
  /** 扫描耗时，单位：毫秒 */
  scanDurationMs: number;
  /** 发现的可疑程序总数 */
  totalSuspiciousPrograms: number;
  /** 发现的安全漏洞总数 */
  totalVulnerabilities: number;
  /** 高危问题数量 */
  highRiskCount: number;
  /** 中危问题数量 */
  mediumRiskCount: number;
  /** 低危问题数量 */
  lowRiskCount: number;
  /** 可疑程序按分类统计，key为分类编码，value为数量 */
  programCategoryStats: Record<string, number>;
  /** 安全漏洞按分类统计，key为分类编码，value为数量 */
  vulnerabilityCategoryStats: Record<string, number>;
  /** 按风险等级统计，key为等级（high/medium/low），value为数量 */
  severityStats: Record<string, number>;
  /** 可疑程序详细列表 */
  suspiciousPrograms: SuspiciousProgram[];
  /** 安全漏洞详细列表 */
  vulnerabilities: Vulnerability[];
  /** 免责声明，提示用户工具的局限性 */
  disclaimer: string;
  /** 扫描完成时间，ISO格式字符串 */
  scanTime: string;
}

/**
 * AI安全建议数据接口
 * <p>AI针对某一类安全问题给出的专业建议，包括问题描述、
 * 风险分析、处理建议和修复步骤等
 */
export interface VirusAISuggestion {
  /** 问题分类编码 */
  category: string;
  /** 分类中文名称，用于界面显示 */
  categoryLabel: string;
  /** 建议标题，简要说明需要处理的问题 */
  title: string;
  /** 问题详细描述，说明具体存在哪些安全问题 */
  description: string;
  /** 受影响的程序或组件列表 */
  affectedPrograms: string[];
  /** 此类问题的数量 */
  issueCount: number;
  /** 风险等级：high（高危）/medium（中危）/low（低危） */
  riskLevel: string;
  /** 建议采取的行动：立即处理/尽快处理/建议优化 */
  action: string;
  /** 建议理由，说明为什么需要处理此问题 */
  reason: string;
  /** 优先级，1-10，数字越大优先级越高 */
  priority: number;
  /** 处理后的预期效果说明 */
  impact: string;
  /** 注意事项，处理此问题时需要注意的风险点 */
  precaution: string;
  /** 具体修复步骤列表，按顺序执行可修复此类问题 */
  remediationSteps: string[];
}

/**
 * AI安全分析结果数据接口
 * <p>AI对系统安全状况的完整分析报告，包括整体评估、
 * 安全建议列表、优化方案等
 */
export interface VirusAIAnalysisResult {
  /** 分析结果摘要，简要说明系统安全状况 */
  summary: string;
  /** 系统健康评分，0-100的字符串 */
  systemHealthScore: string;
  /** 系统健康等级：优秀/良好/一般/较差/危险 */
  systemHealthLevel: string;
  /** AI深度分析见解，说明发现的主要安全问题及其可能的影响 */
  analysisInsight: string;
  /** 全面的安全评估，说明当前系统面临的主要威胁 */
  securityAssessment: string;
  /** 系统安全优化建议，包括安全配置、防护软件、安全习惯等 */
  optimizationAdvice: string;
  /** 安全问题总数 */
  totalIssues: number;
  /** 高危问题数量 */
  highRiskCount: number;
  /** 中危问题数量 */
  mediumRiskCount: number;
  /** 低危问题数量 */
  lowRiskCount: number;
  /** AI安全建议列表，按优先级排序 */
  suggestions: VirusAISuggestion[];
  /** 使用的AI模型名称，fallback表示使用本地规则引擎 */
  model?: string;
  /** Token消耗数量（AI模式下） */
  tokens?: number;
  /** 分析耗时，单位：毫秒 */
  analysisDurationMs: number;
}

/**
 * 修复脚本数据接口
 * <p>包含自动生成的系统修复脚本信息，包括脚本内容、
 * 使用说明、警告信息等
 */
export interface RemediationScript {
  /** 脚本文件名，如：virus_remediation_123456.bat */
  scriptName: string;
  /** 脚本内容，完整的批处理脚本代码 */
  scriptContent: string;
  /** 脚本类型：BAT（Windows批处理）/PS1（PowerShell）/SH（Linux Shell） */
  scriptType: string;
  /** 脚本编码格式：UTF-8/GBK等 */
  encoding: string;
  /** 本次脚本修复的问题数量 */
  issueCount: number;
  /** 修复的问题列表，包含所有将被此脚本处理的问题描述 */
  fixedIssues: string[];
  /** 警告信息，提示用户执行脚本前需要注意的事项 */
  warning: string;
  /** 使用说明，指导用户如何正确使用此修复脚本 */
  usageInstructions: string;
}

/**
 * 生成修复脚本请求数据接口
 * <p>用于封装前端生成修复脚本的请求参数，指定需要修复的问题ID列表
 */
export interface GenerateRemediationScriptRequest {
  /** 选中的问题ID列表，包含需要生成修复脚本的所有问题的唯一标识 */
  selectedIssueIds: string[];
}

/**
 * 规则分类
 */
export type RuleCategory = 'global' | 'project' | 'coding_standard' | 'documentation' | 'ai_tool';

/**
 * 规则模板数据接口
 */
export interface RuleTemplate {
  id: number;
  userId: number;
  name: string;
  description?: string;
  category: RuleCategory;
  sourceType: string;
  targetTool?: string;
  fileName: string;
  filePath?: string;
  content?: string;
  variables?: string;
  version?: string;
  isSystem: boolean;
  isPublic: boolean;
  useCount: number;
  status: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 规则配置数据接口
 */
export interface RuleConfig {
  id: number;
  userId: number;
  templateId?: number;
  projectPath?: string;
  category: RuleCategory;
  targetTool?: string;
  fileName: string;
  targetPath: string;
  content?: string;
  conflictStrategy: string;
  fileHash?: string;
  lastPullAt?: string;
  pullCount: number;
  isActive: boolean;
  status: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 规则拉取日志数据接口
 */
export interface RulePullLog {
  id: number;
  userId: number;
  configId?: number;
  templateId?: number;
  operationType: string;
  targetPath: string;
  hasConflict: boolean;
  conflictType?: string;
  conflictStrategy?: string;
  localContent?: string;
  remoteContent?: string;
  mergedContent?: string;
  diffResult?: string;
  mergeSuccessful?: boolean;
  status: string;
  errorMessage?: string;
  operator?: string;
  createdAt: string;
}

/**
 * 规则拉取结果数据接口
 */
export interface RulePullResult {
  logId: number;
  configId?: number;
  status: string;
  hasConflict: boolean;
  conflictType?: string;
  conflictStrategy?: string;
  targetPath?: string;
  localContent?: string;
  remoteContent?: string;
  mergedContent?: string;
  diffResult?: string;
  message?: string;
}

/**
 * 规则分类选项
 */
export interface RuleCategoryOption {
  code: RuleCategory;
  label: string;
  icon: string;
  description: string;
}

/**
 * 冲突处理策略选项
 */
export interface ConflictStrategyOption {
  code: string;
  label: string;
  description: string;
  color: string;
}

/**
 * AI规则生成请求数据接口
 */
export interface AIRuleGenerateRequest {
  category: RuleCategory;
  targetTool?: string;
  description: string;
  projectType?: string;
  codingLanguage?: string;
  additionalRequirements?: string;
  fileName?: string;
  saveAsTemplate?: boolean;
}

export interface CronGenerateRequest {
  second?: string;
  minute?: string;
  hour?: string;
  day?: string;
  month?: string;
  weekDay?: string;
  year?: string;
}

export interface CronGenerateResult {
  success: boolean;
  cronExpression: string;
  description: string;
  presetName: string | null;
  valid: boolean;
  warnings: string[] | null;
}

export interface CronParseRequest {
  cronExpression: string;
}

export interface CronParseResult {
  success: boolean;
  valid: boolean;
  description: string;
  fields: Record<string, string>;
  errors: string[];
}

export interface CronNextTimesRequest {
  cronExpression: string;
  count?: number;
  excludeHoliday?: boolean;
  useChinaHoliday?: boolean;
}

export interface CronNextTimesResult {
  success: boolean;
  cronExpression: string;
  nextTimes: string[];
  errors: string[];
  excludedTimes: string[] | null;
  holidayInfo: string[] | null;
}

export interface CronNLRequest {
  naturalLanguage: string;
}

export interface CronNLResult {
  success: boolean;
  cronExpression: string;
  description: string;
  confidence: number;
  aiModel: string | null;
  fallback: boolean;
  errors: string[];
}

export interface RegexMatchResult {
  matchedText: string;
  startIndex: number;
  endIndex: number;
  groups: string[];
}

export interface RegexValidateRequest {
  pattern: string;
  testString?: string;
  flags?: number;
}

export interface RegexValidateResult {
  success: boolean;
  valid: boolean;
  pattern: string;
  description: string;
  matches: RegexMatchResult[];
  matchCount: number;
  errors: string[];
  groups: string[];
}

export interface RegexGenerateRequest {
  description: string;
  testString?: string;
  category?: string;
}

export interface RegexAlternative {
  pattern: string;
  description: string;
}

export interface RegexGenerateResult {
  success: boolean;
  pattern: string;
  description: string;
  explanation: string;
  testCases: string[];
  confidence: number;
  aiModel: string | null;
  fallback: boolean;
  errors: string[];
  alternatives: RegexAlternative[];
}

export interface RegexFixRequest {
  pattern: string;
  testString?: string;
  errorMessage?: string;
  intent?: string;
}

export interface RegexFixResult {
  success: boolean;
  originalPattern: string;
  fixedPattern: string;
  fixDescription: string;
  explanation: string;
  valid: boolean;
  testMatches: RegexMatchResult[];
  aiModel: string | null;
  fallback: boolean;
  errors: string[];
  suggestions: string[];
}

export interface SqlErrorDetail {
  errorType: string;
  lineNumber: number;
  columnNumber: number;
  errorContext: string;
  message: string;
  suggestion: string;
}

export interface SqlFormatResult {
  success: boolean;
  formattedSql: string;
  compactSql: string;
  errors: SqlErrorDetail[];
  aiFixedSql: string;
  aiFixDescription: string;
  aiFixSuccess: boolean;
  indentSize: number;
  uppercase: boolean;
  dbType: string;
  sqlType: string;
  statistics: string;
  suggestions: string[];
}

export interface AudioFormatInfo {
  formatName: string;
  extension: string;
  mimeType: string;
  description: string;
  maxFileSize: number;
}

export interface SpeechToTextResult {
  text: string;
  language: string;
  detectedLanguage: string;
  duration: number;
  model: string;
  originalFileName: string;
  originalSize: number;
  audioFormat: string;
  durationMs: number;
}

/**
 * 内容检测分类详情
 * <p>描述某一类违规内容的检测结果
 */
export interface ModerationCategoryDetail {
  /** 分类编码：pornography-涉黄、political-涉政、violence-涉爆、other-其他 */
  category: string;
  /** 分类中文名称 */
  categoryLabel: string;
  /** 是否检测到违规内容 */
  violated: boolean;
  /** 置信度，0-100 */
  confidence: number;
  /** 风险等级：high-高危、medium-中危、low-低危、safe-安全 */
  riskLevel: string;
  /** 检测到的违规标签列表 */
  labels: string[];
  /** 检测说明 */
  description: string;
}

/**
 * 图片内容检测请求参数
 */
export interface ImageModerationRequest {
  /** 图片Base64编码数据 */
  imageBase64: string;
  /** 原始文件名 */
  fileName: string;
  /** 文件大小，单位：字节 */
  fileSize: number;
  /** 图片MIME类型 */
  mimeType: string;
  /** 是否检测涉黄内容，默认true */
  detectPornography?: boolean;
  /** 是否检测涉政内容，默认true */
  detectPolitical?: boolean;
  /** 是否检测涉爆内容，默认true */
  detectViolence?: boolean;
  /** 是否检测其他违规内容，默认true */
  detectOther?: boolean;
  /** 检测敏感度阈值，0-100，默认80 */
  sensitivityThreshold?: number;
}

/**
 * 图片内容检测结果
 */
export interface ImageModerationResult {
  /** 检测任务唯一标识 */
  taskId: string;
  /** 原始文件名 */
  fileName: string;
  /** 文件大小，单位：字节 */
  fileSize: number;
  /** 图片MIME类型 */
  mimeType: string;
  /** 图片宽度，单位：像素 */
  width: number;
  /** 图片高度，单位：像素 */
  height: number;
  /** 检测是否成功完成 */
  success: boolean;
  /** 总体检测结论：pass-通过、review-待复审、block-拦截 */
  conclusion: string;
  /** 总体结论中文描述 */
  conclusionLabel: string;
  /** 是否检测到违规内容 */
  hasViolation: boolean;
  /** 总体风险等级：high-高危、medium-中危、low-低危、safe-安全 */
  overallRiskLevel: string;
  /** 最高置信度，0-100 */
  maxConfidence: number;
  /** 涉黄内容检测详情 */
  pornographyResult: ModerationCategoryDetail;
  /** 涉政内容检测详情 */
  politicalResult: ModerationCategoryDetail;
  /** 涉爆内容检测详情 */
  violenceResult: ModerationCategoryDetail;
  /** 其他违规内容检测详情 */
  otherResult: ModerationCategoryDetail;
  /** 检测到的所有违规标签汇总 */
  allViolationLabels: string[];
  /** 审核建议 */
  suggestion: string;
  /** 详细检测说明 */
  auditNote: string;
  /** 使用的AI模型名称 */
  model?: string;
  /** Token消耗数量 */
  tokens?: number;
  /** 是否为降级模式返回的结果 */
  fallback?: boolean;
  /** 检测耗时，单位：毫秒 */
  detectionDurationMs: number;
  /** 检测完成时间 */
  detectionTime: string;
  /** 免责声明 */
  disclaimer: string;
}

export interface SqlFormatRequest {
  content: string;
  indentSize?: number;
  uppercase?: boolean;
  dbType?: string;
  userIntent?: string;
  enableAI?: boolean;
}

export interface ReportSlide {
  slideNumber: number;
  title: string;
  type: string;
  content: string;
  keyPoints: string[];
  speakerNotes: string;
  visualSuggestion: string;
  durationMinutes: number;
}

export interface TechReportResult {
  reportTitle: string;
  executiveSummary: string;
  slides: ReportSlide[];
  qaPreparation: string[];
  presentationTips: string[];
  totalSlides: number;
  estimatedDurationMinutes: number;
  model?: string;
  contentMarkdown?: string;
}

export interface TechReportRequest {
  scene: string;
  description?: string;
  audience: string;
  reportType?: string;
  slideCount?: number;
  keyPoints?: string[];
  industry?: string;
  companySize?: string;
  additionalInfo?: string;
}

export interface SceneTemplate {
  name: string;
  description: string;
  defaultAudience: string;
  slideCount: string;
}

export interface AudienceType {
  type: string;
  label: string;
  description: string;
}

export interface DockerGenerateRequest {
  projectPath: string;
  imageName?: string;
  imageTag?: string;
  port?: number;
  jdkVersion?: string;
  buildTool?: string;
  jvmOpts?: string;
  springProfile?: string;
  includeDockerCompose?: boolean;
}

export interface DockerGenerateResult {
  projectPath: string;
  projectName: string;
  buildTool: string;
  jdkVersion: string;
  packaging: string;
  mainClass: string | null;
  dockerfileContent: string;
  dockerignoreContent: string;
  dockerComposeContent: string | null;
  dockerfileWritten: boolean;
  dockerignoreWritten: boolean;
  dockerComposeWritten: boolean;
}

export interface DockerDeployResult {
  imageName: string;
  containerId: string;
  containerName: string;
  status: string;
  buildLog: string;
  runLog: string;
  mappedPort: number;
  success: boolean;
  errorMessage: string | null;
}
