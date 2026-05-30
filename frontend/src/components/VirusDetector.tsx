import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ShieldAlert,
  ArrowLeft,
  Loader2,
  AlertCircle,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Info,
  Shield,
  Download,
  FileCode,
  Terminal,
  CheckSquare,
  Square,
  Zap,
  AlertTriangle,
  FileText,
  Brain,
  TrendingUp,
  Lightbulb,
  Gauge,
  Clock,
  Sparkles,
  Bug,
  Lock,
  Unlock,
  Activity,
} from 'lucide-react';
import { toolsApi } from '@/api';
import { VirusScanResult, SuspiciousProgram, Vulnerability, RemediationScript, GenerateRemediationScriptRequest, VirusAIAnalysisResult, VirusAISuggestion } from '@/types';

/**
 * 可疑程序分类标签映射
 * <p>将分类编码映射为中文显示名称，用于界面展示
 */
const PROGRAM_CATEGORY_LABELS: Record<string, string> = {
  suspicious_process: '可疑进程',
  unknown_startup: '未知启动项',
  unsigned_exe: '未签名程序',
  temp_executable: '临时目录程序',
  browser_hijacker: '浏览器劫持',
  service_anomaly: '服务异常',
  scheduled_task: '计划任务',
};

/**
 * 安全漏洞分类标签映射
 * <p>将分类编码映射为中文显示名称，用于界面展示
 */
const VULN_CATEGORY_LABELS: Record<string, string> = {
  os_vulnerability: '系统漏洞',
  software_vulnerability: '软件漏洞',
  weak_password: '弱密码检测',
  open_port: '开放端口',
  outdated_software: '过时软件',
  security_setting: '安全设置',
};

/**
 * 风险等级样式配置
 * <p>定义不同风险等级的标签样式，包括文字颜色、背景色和边框色
 * <p>high-高危（红色）、medium-中危（琥珀色）、low-低危（翠绿色）
 */
const SEVERITY_CONFIG: Record<string, { label: string; color: string; bgColor: string; borderColor: string }> = {
  high: { label: '高危', color: 'text-red-400', bgColor: 'bg-red-500/20', borderColor: 'border-red-500/30' },
  medium: { label: '中危', color: 'text-amber-400', bgColor: 'bg-amber-500/20', borderColor: 'border-amber-500/30' },
  low: { label: '低危', color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', borderColor: 'border-emerald-500/30' },
};

/**
 * 可疑程序分类颜色映射
 * <p>为每个可疑程序分类分配独特的标识颜色，用于界面区分
 */
const PROGRAM_CATEGORY_COLORS: Record<string, string> = {
  suspicious_process: '#ef4444',
  unknown_startup: '#f59e0b',
  unsigned_exe: '#8b5cf6',
  temp_executable: '#ec4899',
  browser_hijacker: '#f97316',
  service_anomaly: '#6366f1',
  scheduled_task: '#14b8a6',
};

/**
 * 安全漏洞分类颜色映射
 * <p>为每个安全漏洞分类分配独特的标识颜色，用于界面区分
 */
const VULN_CATEGORY_COLORS: Record<string, string> = {
  os_vulnerability: '#ef4444',
  software_vulnerability: '#f59e0b',
  weak_password: '#8b5cf6',
  open_port: '#3b82f6',
  outdated_software: '#10b981',
  security_setting: '#06b6d4',
};

/**
 * 风险等级标签组件
 * <p>根据风险等级显示不同颜色的标签，用于快速识别问题严重程度
 * 
 * @param severity - 风险等级：high（高危）/medium（中危）/low（低危）
 */
function SeverityBadge({ severity }: { severity: string }) {
  const config = SEVERITY_CONFIG[severity] || SEVERITY_CONFIG.low;
  return (
    <span className={`px-2 py-0.5 text-xs rounded-full ${config.bgColor} ${config.color} font-medium`}>
      {config.label}
    </span>
  );
}

/**
 * 健康等级样式配置
 * <p>定义不同健康等级的显示样式，包括文字颜色、背景色和边框色
 * <p>优秀-翠绿色、良好-青色、一般-琥珀色、较差-橙色、危险-红色
 */
const HEALTH_LEVEL_CONFIG: Record<string, { color: string; bgColor: string; borderColor: string }> = {
  优秀: { color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', borderColor: 'border-emerald-500/30' },
  良好: { color: 'text-cyan-400', bgColor: 'bg-cyan-500/20', borderColor: 'border-cyan-500/30' },
  一般: { color: 'text-amber-400', bgColor: 'bg-amber-500/20', borderColor: 'border-amber-500/30' },
  较差: { color: 'text-orange-400', bgColor: 'bg-orange-500/20', borderColor: 'border-orange-500/30' },
  危险: { color: 'text-red-400', bgColor: 'bg-red-500/20', borderColor: 'border-red-500/30' },
};

/**
 * 健康评分显示组件
 * <p>使用环形进度条可视化展示系统健康评分，包含评分数值和健康等级
 * <p>支持动画效果，评分变化时有平滑过渡
 * 
 * @param score - 健康评分，0-100的字符串
 * @param level - 健康等级：优秀/良好/一般/较差/危险
 */
function HealthScoreDisplay({ score, level }: { score: string; level: string }) {
  const config = HEALTH_LEVEL_CONFIG[level] || HEALTH_LEVEL_CONFIG.一般;
  const scoreNum = parseInt(score);

  return (
    <div className="flex items-center gap-6">
      <div className="relative w-32 h-32 shrink-0">
        <svg className="w-full h-full transform -rotate-90">
          <circle
            cx="64"
            cy="64"
            r="56"
            stroke="currentColor"
            strokeWidth="8"
            fill="none"
            className="text-white/10"
          />
          <circle
            cx="64"
            cy="64"
            r="56"
            stroke="url(#securityGradient)"
            strokeWidth="8"
            fill="none"
            strokeLinecap="round"
            strokeDasharray={`${scoreNum * 3.52} 352`}
            className="transition-all duration-1000 ease-out"
          />
          <defs>
            <linearGradient id="securityGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" className={scoreNum >= 90 ? 'stop-color-emerald-500' : scoreNum >= 75 ? 'stop-color-cyan-500' : scoreNum >= 60 ? 'stop-color-amber-500' : scoreNum >= 40 ? 'stop-color-orange-500' : 'stop-color-red-500'} />
              <stop offset="100%" className={scoreNum >= 90 ? 'stop-color-teal-500' : scoreNum >= 75 ? 'stop-color-blue-500' : scoreNum >= 60 ? 'stop-color-yellow-500' : scoreNum >= 40 ? 'stop-color-red-500' : 'stop-color-rose-600'} />
            </linearGradient>
          </defs>
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-3xl font-bold text-white">{score}</span>
          <span className="text-xs text-gray-400">安全分</span>
        </div>
      </div>
      <div className="flex-1">
        <div className="flex items-center gap-2 mb-2">
          <Shield className={`w-5 h-5 ${config.color}`} />
          <span className={`text-lg font-bold ${config.color}`}>{level}</span>
        </div>
        <p className="text-gray-400 text-sm">
          {scoreNum >= 90
            ? '系统安全状况非常好，继续保持良好的安全习惯！'
            : scoreNum >= 75
            ? '系统安全状况良好，存在少量可优化项。'
            : scoreNum >= 60
            ? '系统存在一些安全问题，建议进行处理。'
            : scoreNum >= 40
            ? '系统安全问题较多，可能面临被攻击风险，建议尽快处理。'
            : '系统存在严重安全问题，强烈建议立即处理！'}
        </p>
      </div>
    </div>
  );
}

/**
 * 可疑程序列表项组件
 * <p>展示单个可疑程序的详细信息，支持展开/收起查看详细内容
 * <p>包含复选框用于选择要处理的程序，点击可展开查看详细信息
 * 
 * @param program - 可疑程序数据对象
 * @param onToggle - 复选框点击回调，参数为程序ID
 * @param isExpanded - 是否已展开显示详细信息
 * @param onToggleExpand - 展开/收起按钮点击回调
 */
function SuspiciousProgramItem({
  program,
  onToggle,
  isExpanded,
  onToggleExpand,
}: {
  program: SuspiciousProgram;
  onToggle: (id: string) => void;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const severity = SEVERITY_CONFIG[program.severity] || SEVERITY_CONFIG.low;
  const categoryColor = PROGRAM_CATEGORY_COLORS[program.category] || '#9ca3af';

  return (
    <div className={`bg-white/5 rounded-2xl overflow-hidden border ${severity.borderColor} hover:bg-white/10 transition-colors`}>
      <div className="p-4 flex items-center gap-3">
        <button
          onClick={(e) => {
            e.stopPropagation();
            onToggle(program.id);
          }}
          className="shrink-0 hover:scale-110 transition-transform"
        >
          {program.selected ? (
            <CheckSquare className="w-5 h-5 text-cyan-400" />
          ) : (
            <Square className="w-5 h-5 text-gray-500" />
          )}
        </button>
        <button
          onClick={onToggleExpand}
          className="flex-1 flex items-center gap-3 text-left min-w-0"
        >
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${severity.bgColor}`}>
            <Bug className={`w-5 h-5 ${severity.color}`} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-0.5 flex-wrap">
              <p className="text-white font-medium text-sm truncate">{program.programName}</p>
              <SeverityBadge severity={program.severity} />
              <span
                className="px-2 py-0.5 text-xs rounded-full shrink-0"
                style={{ backgroundColor: `${categoryColor}20`, color: categoryColor }}
              >
                {program.categoryLabel}
              </span>
            </div>
            <p className="text-gray-400 text-xs truncate">{program.description}</p>
          </div>
          {isExpanded ? (
            <ChevronUp className="w-4 h-4 text-gray-400 shrink-0" />
          ) : (
            <ChevronDown className="w-4 h-4 text-gray-400 shrink-0" />
          )}
        </button>
      </div>
      {isExpanded && (
        <div className="px-4 pb-4 space-y-3 animate-fadeIn">
          {program.programPath && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <FileText className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="text-cyan-400 font-medium mb-1">程序路径</p>
                <p className="text-gray-300 font-mono break-all">{program.programPath}</p>
              </div>
            </div>
          )}
          {program.processId && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <Activity className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="text-purple-400 font-medium mb-1">进程信息</p>
                <p className="text-gray-300">
                  PID: {program.processId}
                  {program.processName && ` · ${program.processName}`}
                </p>
              </div>
            </div>
          )}
          {program.suspiciousBehaviors && program.suspiciousBehaviors.length > 0 && (
            <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-red-500/10 to-rose-500/10 border border-red-500/20 rounded-xl">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="text-red-400 font-medium mb-1">可疑行为</p>
                <ul className="text-gray-300 space-y-1">
                  {program.suspiciousBehaviors.map((behavior, idx) => (
                    <li key={idx}>• {behavior}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/20 rounded-xl">
            <Info className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-cyan-400 font-medium mb-1">风险分析</p>
              <p className="text-gray-300">{program.riskReason}</p>
            </div>
          </div>
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-emerald-500/10 to-teal-500/10 border border-emerald-500/20 rounded-xl">
            <Zap className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-emerald-400 font-medium mb-1">处理建议</p>
              <p className="text-gray-300">{program.recommendation}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * 安全漏洞列表项组件
 * <p>展示单个安全漏洞的详细信息，支持展开/收起查看详细内容
 * <p>包含复选框用于选择要处理的漏洞，点击可展开查看CVE编号、修复步骤等详细信息
 * 
 * @param vuln - 安全漏洞数据对象
 * @param onToggle - 复选框点击回调，参数为漏洞ID
 * @param isExpanded - 是否已展开显示详细信息
 * @param onToggleExpand - 展开/收起按钮点击回调
 */
function VulnerabilityItem({
  vuln,
  onToggle,
  isExpanded,
  onToggleExpand,
}: {
  vuln: Vulnerability;
  onToggle: (id: string) => void;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const severity = SEVERITY_CONFIG[vuln.severity] || SEVERITY_CONFIG.low;
  const categoryColor = VULN_CATEGORY_COLORS[vuln.category] || '#9ca3af';

  return (
    <div className={`bg-white/5 rounded-2xl overflow-hidden border ${severity.borderColor} hover:bg-white/10 transition-colors`}>
      <div className="p-4 flex items-center gap-3">
        <button
          onClick={(e) => {
            e.stopPropagation();
            onToggle(vuln.id);
          }}
          className="shrink-0 hover:scale-110 transition-transform"
        >
          {vuln.selected ? (
            <CheckSquare className="w-5 h-5 text-cyan-400" />
          ) : (
            <Square className="w-5 h-5 text-gray-500" />
          )}
        </button>
        <button
          onClick={onToggleExpand}
          className="flex-1 flex items-center gap-3 text-left min-w-0"
        >
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${severity.bgColor}`}>
            <Unlock className={`w-5 h-5 ${severity.color}`} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-0.5 flex-wrap">
              <p className="text-white font-medium text-sm truncate">{vuln.title}</p>
              <SeverityBadge severity={vuln.severity} />
              <span
                className="px-2 py-0.5 text-xs rounded-full shrink-0"
                style={{ backgroundColor: `${categoryColor}20`, color: categoryColor }}
              >
                {vuln.categoryLabel}
              </span>
            </div>
            <p className="text-gray-400 text-xs truncate">{vuln.affectedComponent}</p>
          </div>
          {isExpanded ? (
            <ChevronUp className="w-4 h-4 text-gray-400 shrink-0" />
          ) : (
            <ChevronDown className="w-4 h-4 text-gray-400 shrink-0" />
          )}
        </button>
      </div>
      {isExpanded && (
        <div className="px-4 pb-4 space-y-3 animate-fadeIn">
          <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
            <Info className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-cyan-400 font-medium mb-1">漏洞描述</p>
              <p className="text-gray-300">{vuln.description}</p>
            </div>
          </div>
          {vuln.cveId && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <FileText className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="text-purple-400 font-medium mb-1">CVE编号</p>
                <p className="text-gray-300 font-mono">{vuln.cveId}</p>
                {vuln.cvssScore && <p className="text-gray-400 mt-1">CVSS评分: {vuln.cvssScore}</p>}
              </div>
            </div>
          )}
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-red-500/10 to-rose-500/10 border border-red-500/20 rounded-xl">
            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-red-400 font-medium mb-1">风险等级: {vuln.riskLevel}</p>
              <p className="text-gray-300">利用状态: {vuln.exploitStatus}</p>
            </div>
          </div>
          {vuln.remediationSteps && vuln.remediationSteps.length > 0 && (
            <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-emerald-500/10 to-teal-500/10 border border-emerald-500/20 rounded-xl">
              <Zap className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="text-emerald-400 font-medium mb-1">修复步骤</p>
                <ul className="text-gray-300 space-y-1">
                  {vuln.remediationSteps.map((step, idx) => (
                    <li key={idx}>{idx + 1}. {step}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * AI安全建议列表项组件
 * <p>展示AI给出的单条安全建议，支持展开/收起查看详细内容
 * <p>包含优先级编号、问题描述、风险分析、处理建议和修复步骤等
 * 
 * @param suggestion - AI安全建议数据对象
 * @param isExpanded - 是否已展开显示详细信息
 * @param onToggleExpand - 展开/收起按钮点击回调
 */
function AISuggestionItem({
  suggestion,
  isExpanded,
  onToggleExpand,
}: {
  suggestion: VirusAISuggestion;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const severity = SEVERITY_CONFIG[suggestion.riskLevel] || SEVERITY_CONFIG.low;
  const categoryColor = PROGRAM_CATEGORY_COLORS[suggestion.category] || VULN_CATEGORY_COLORS[suggestion.category] || '#9ca3af';

  return (
    <div className={`bg-white/5 rounded-2xl overflow-hidden border ${severity.borderColor} hover:bg-white/10 transition-colors`}>
      <button onClick={onToggleExpand} className="w-full p-4 flex items-center gap-3 text-left">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${severity.bgColor}`}>
          <span className="text-white font-bold text-sm">#{suggestion.priority}</span>
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1 flex-wrap">
            <p className="text-white font-medium text-sm truncate">{suggestion.title}</p>
            <SeverityBadge severity={suggestion.riskLevel} />
            <span
              className="px-2 py-0.5 text-xs rounded-full shrink-0"
              style={{ backgroundColor: `${categoryColor}20`, color: categoryColor }}
            >
              {suggestion.categoryLabel}
            </span>
          </div>
          <div className="flex items-center gap-3 text-xs text-gray-400">
            <span className="flex items-center gap-1">
              <AlertCircle className="w-3 h-3" />
              {suggestion.issueCount} 项问题
            </span>
            <span className="flex items-center gap-1">
              <Zap className="w-3 h-3" />
              {suggestion.action}
            </span>
          </div>
        </div>
        {isExpanded ? (
          <ChevronUp className="w-4 h-4 text-gray-400 shrink-0" />
        ) : (
          <ChevronDown className="w-4 h-4 text-gray-400 shrink-0" />
        )}
      </button>
      {isExpanded && (
        <div className="px-4 pb-4 space-y-3 animate-fadeIn">
          <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
            <Info className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-cyan-400 font-medium mb-1">问题描述</p>
              <p className="text-gray-300">{suggestion.description}</p>
            </div>
          </div>
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-purple-500/10 to-pink-500/10 border border-purple-500/20 rounded-xl">
            <Brain className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-purple-400 font-medium mb-1">AI分析 - 为什么需要处理</p>
              <p className="text-gray-300">{suggestion.reason}</p>
            </div>
          </div>
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-emerald-500/10 to-teal-500/10 border border-emerald-500/20 rounded-xl">
            <TrendingUp className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-emerald-400 font-medium mb-1">预期效果</p>
              <p className="text-gray-300">{suggestion.impact}</p>
            </div>
          </div>
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-amber-500/10 to-orange-500/10 border border-amber-500/20 rounded-xl">
            <Shield className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-amber-400 font-medium mb-1">注意事项</p>
              <p className="text-gray-300">{suggestion.precaution}</p>
            </div>
          </div>
          {suggestion.remediationSteps && suggestion.remediationSteps.length > 0 && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <Terminal className="w-4 h-4 text-gray-400 shrink-0 mt-0.5" />
              <div className="text-xs flex-1">
                <p className="text-gray-400 font-medium mb-1">具体修复步骤</p>
                <ul className="text-gray-300 space-y-1">
                  {suggestion.remediationSteps.map((step, idx) => (
                    <li key={idx}>{idx + 1}. {step}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}
          {suggestion.affectedPrograms && suggestion.affectedPrograms.length > 0 && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <FileText className="w-4 h-4 text-gray-400 shrink-0 mt-0.5" />
              <div className="text-xs flex-1">
                <p className="text-gray-400 font-medium mb-1">受影响的项目（{suggestion.affectedPrograms.length}个）</p>
                <div className="space-y-1">
                  {suggestion.affectedPrograms.slice(0, 5).map((item, idx) => (
                    <p key={idx} className="text-gray-300 text-xs">
                      • {item}
                    </p>
                  ))}
                  {suggestion.affectedPrograms.length > 5 && (
                    <p className="text-gray-500">...还有 {suggestion.affectedPrograms.length - 5} 个</p>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

/**
 * 修复脚本预览弹窗组件
 * <p>以模态框形式展示生成的修复脚本内容，包含脚本预览、
 * 警告信息、使用说明和下载按钮
 * 
 * @param script - 修复脚本数据对象
 * @param onClose - 关闭弹窗回调
 * @param onDownload - 下载脚本回调
 */
function ScriptPreviewModal({
  script,
  onClose,
  onDownload,
}: {
  script: RemediationScript;
  onClose: () => void;
  onDownload: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass-card rounded-3xl p-6 max-w-4xl w-full max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">{script.scriptName}</h2>
              <p className="text-gray-400 text-sm">包含 {script.issueCount} 项修复操作</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl hover:bg-white/10 transition-colors"
          >
            <ChevronUp className="w-5 h-5 text-gray-400" />
          </button>
        </div>

        <div className={`p-4 rounded-2xl mb-4 border ${SEVERITY_CONFIG.high.borderColor} ${SEVERITY_CONFIG.high.bgColor}`}>
          <div className="flex items-start gap-3">
            <AlertTriangle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-red-300 font-medium">{script.warning}</p>
              <p className="text-gray-400 text-sm mt-1 whitespace-pre-line">{script.usageInstructions}</p>
            </div>
          </div>
        </div>

        <div className="flex-1 overflow-auto bg-gray-900/50 rounded-2xl p-4 mb-4">
          <pre className="text-xs text-gray-300 font-mono whitespace-pre-wrap break-all">
            {script.scriptContent}
          </pre>
        </div>

        <div className="flex justify-end gap-3">
          <button onClick={onClose} className="btn-secondary">
            关闭
          </button>
          <button onClick={onDownload} className="btn-primary flex items-center gap-2">
            <Download className="w-5 h-5" />
            下载脚本
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * 病毒检测与安全分析主组件
 * <p>提供完整的系统安全检测功能，包括：
 * <ul>
 *   <li>系统安全扫描：扫描可疑进程、启动项、服务、漏洞等9个维度</li>
 *   <li>AI智能分析：调用AI对扫描结果进行深度分析，提供专业建议</li>
 *   <li>问题列表展示：可疑程序和安全漏洞分类展示，支持筛选和搜索</li>
 *   <li>修复脚本生成：根据选中的问题自动生成修复脚本</li>
 *   <li>脚本预览与下载：预览脚本内容，支持下载执行</li>
 * </ul>
 * <p>所有操作都有完整的加载状态和错误处理
 */
export default function VirusDetector() {
  /** 页面导航Hook */
  const navigate = useNavigate();
  /** 完整的扫描结果数据 */
  const [scanResult, setScanResult] = useState<VirusScanResult | null>(null);
  /** 可疑程序列表 */
  const [suspiciousPrograms, setSuspiciousPrograms] = useState<SuspiciousProgram[]>([]);
  /** 安全漏洞列表 */
  const [vulnerabilities, setVulnerabilities] = useState<Vulnerability[]>([]);
  /** 是否正在扫描中 */
  const [scanning, setScanning] = useState(false);
  /** 是否正在生成脚本中 */
  const [generating, setGenerating] = useState(false);
  /** 错误信息 */
  const [error, setError] = useState('');
  /** 当前展开的可疑程序ID */
  const [expandedProgramId, setExpandedProgramId] = useState<string | null>(null);
  /** 当前展开的安全漏洞ID */
  const [expandedVulnId, setExpandedVulnId] = useState<string | null>(null);
  /** 可疑程序分类筛选条件 */
  const [filterProgramCategory, setFilterProgramCategory] = useState<string>('all');
  /** 安全漏洞分类筛选条件 */
  const [filterVulnCategory, setFilterVulnCategory] = useState<string>('all');
  /** 风险等级筛选条件 */
  const [filterSeverity, setFilterSeverity] = useState<string>('all');
  /** 当前激活的标签页：programs-可疑程序 / vulnerabilities-安全漏洞 */
  const [activeTab, setActiveTab] = useState<'programs' | 'vulnerabilities'>('programs');
  /** 脚本预览数据 */
  const [scriptPreview, setScriptPreview] = useState<RemediationScript | null>(null);
  /** AI分析结果数据 */
  const [aiAnalysis, setAiAnalysis] = useState<VirusAIAnalysisResult | null>(null);
  /** 是否正在AI分析中 */
  const [aiAnalyzing, setAiAnalyzing] = useState(false);
  /** 是否显示AI分析结果 */
  const [showAIResult, setShowAIResult] = useState(false);
  /** 当前展开的AI建议ID */
  const [expandedSuggestionId, setExpandedSuggestionId] = useState<number | null>(null);

  /**
   * 筛选后的可疑程序列表
   * <p>根据分类筛选条件和风险等级筛选条件对可疑程序进行过滤
   */
  const filteredPrograms = useMemo(() => {
    return suspiciousPrograms.filter((program) => {
      if (filterProgramCategory !== 'all' && program.category !== filterProgramCategory) return false;
      if (filterSeverity !== 'all' && program.severity !== filterSeverity) return false;
      return true;
    });
  }, [suspiciousPrograms, filterProgramCategory, filterSeverity]);

  /**
   * 筛选后的安全漏洞列表
   * <p>根据分类筛选条件和风险等级筛选条件对安全漏洞进行过滤
   */
  const filteredVulns = useMemo(() => {
    return vulnerabilities.filter((vuln) => {
      if (filterVulnCategory !== 'all' && vuln.category !== filterVulnCategory) return false;
      if (filterSeverity !== 'all' && vuln.severity !== filterSeverity) return false;
      return true;
    });
  }, [vulnerabilities, filterVulnCategory, filterSeverity]);

  /** 所有问题的合并列表（可疑程序 + 安全漏洞） */
  const allItems = [...suspiciousPrograms, ...vulnerabilities];
  /** 已选中的问题数量 */
  const selectedCount = allItems.filter((i) => i.selected).length;
  /** 已选中的问题ID列表，用于生成修复脚本 */
  const selectedIds = useMemo(() => allItems.filter((i) => i.selected).map((i) => i.id), [allItems]);

  /**
   * 执行系统安全扫描
   * <p>调用后端API执行完整的系统安全扫描，包括9个检测维度
   * <p>扫描完成后更新状态并展示结果
   */
  const handleScan = async () => {
    setScanning(true);
    setError('');
    setScanResult(null);
    setSuspiciousPrograms([]);
    setVulnerabilities([]);
    setShowAIResult(false);
    try {
      const res = await toolsApi.scanVirus();
      if (res.code === 200 && res.data) {
        setScanResult(res.data);
        setSuspiciousPrograms(res.data.suspiciousPrograms);
        setVulnerabilities(res.data.vulnerabilities);
      } else {
        setError(res.message || '扫描失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '扫描失败，请稍后重试');
    } finally {
      setScanning(false);
    }
  };

  /**
   * 切换可疑程序的选中状态
   * <p>点击复选框时调用，用于选择要处理的程序
   * 
   * @param id - 可疑程序ID
   */
  const toggleProgram = (id: string) => {
    setSuspiciousPrograms((prev) =>
      prev.map((program) => (program.id === id ? { ...program, selected: !program.selected } : program))
    );
  };

  /**
   * 切换安全漏洞的选中状态
   * <p>点击复选框时调用，用于选择要处理的漏洞
   * 
   * @param id - 安全漏洞ID
   */
  const toggleVulnerability = (id: string) => {
    setVulnerabilities((prev) =>
      prev.map((vuln) => (vuln.id === id ? { ...vuln, selected: !vuln.selected } : vuln))
    );
  };

  /**
   * 全选/取消全选当前筛选结果
   * <p>根据当前激活的标签页，对筛选后的结果进行全选或取消全选操作
   */
  const toggleSelectAll = () => {
    if (activeTab === 'programs') {
      const allSelected = filteredPrograms.every((i) => i.selected);
      const filteredIds = new Set(filteredPrograms.map((i) => i.id));
      setSuspiciousPrograms((prev) =>
        prev.map((program) => (filteredIds.has(program.id) ? { ...program, selected: !allSelected } : program))
      );
    } else {
      const allSelected = filteredVulns.every((i) => i.selected);
      const filteredIds = new Set(filteredVulns.map((i) => i.id));
      setVulnerabilities((prev) =>
        prev.map((vuln) => (filteredIds.has(vuln.id) ? { ...vuln, selected: !allSelected } : vuln))
      );
    }
  };

  /**
   * 按风险等级全选
   * <p>选中所有指定风险等级的可疑程序和安全漏洞
   * 
   * @param severity - 风险等级：high（高危）/medium（中危）/low（低危）
   */
  const selectBySeverity = (severity: string) => {
    setSuspiciousPrograms((prev) =>
      prev.map((program) => (program.severity === severity ? { ...program, selected: true } : program))
    );
    setVulnerabilities((prev) =>
      prev.map((vuln) => (vuln.severity === severity ? { ...vuln, selected: true } : vuln))
    );
  };

  /**
   * 执行AI智能分析
   * <p>调用后端API执行AI深度分析，获取专业的安全评估和修复建议
   * <p>分析完成后自动切换到AI结果展示视图
   */
  const handleAIAnalyze = async () => {
    setAiAnalyzing(true);
    setError('');
    try {
      const res = await toolsApi.aiAnalyzeVirus();
      if (res.code === 200 && res.data) {
        setAiAnalysis(res.data);
        setShowAIResult(true);
      } else {
        setError(res.message || 'AI分析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI分析失败，请稍后重试');
    } finally {
      setAiAnalyzing(false);
    }
  };

  /**
   * 生成修复脚本
   * <p>根据用户选中的问题ID列表，调用后端API生成修复脚本
   * <p>生成成功后打开脚本预览弹窗
   */
  const handleGenerateScript = async () => {
    if (selectedIds.length === 0) {
      setError('请选择要处理的安全问题');
      return;
    }

    setGenerating(true);
    setError('');
    try {
      const request: GenerateRemediationScriptRequest = {
        selectedIssueIds: selectedIds,
      };
      const res = await toolsApi.generateRemediationScript(request);
      if (res.code === 200 && res.data) {
        setScriptPreview(res.data);
      } else {
        setError(res.message || '生成脚本失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '生成脚本失败，请稍后重试');
    } finally {
      setGenerating(false);
    }
  };

  /**
   * 下载修复脚本文件
   * <p>调用后端API下载修复脚本文件，自动触发浏览器下载
   * <p>下载完成后关闭预览弹窗
   */
  const handleDownloadScript = async () => {
    if (selectedIds.length === 0) return;

    try {
      const request: GenerateRemediationScriptRequest = {
        selectedIssueIds: selectedIds,
      };
      const blob = await toolsApi.downloadRemediationScript(request);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = scriptPreview?.scriptName || 'security_fix.bat';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      setScriptPreview(null);
    } catch (err: any) {
      setError(err.response?.data?.message || '下载失败，请稍后重试');
    }
  };

  const allProgramsSelected = filteredPrograms.length > 0 && filteredPrograms.every((i) => i.selected);
  const allVulnsSelected = filteredVulns.length > 0 && filteredVulns.every((i) => i.selected);

  return (
    <div className="space-y-8">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/tools')}
          className="p-3 rounded-xl hover:bg-white/5 transition-all"
        >
          <ArrowLeft className="w-5 h-5 text-gray-400" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-white mb-1">病毒检测与安全分析</h1>
          <p className="text-gray-400">检测系统可疑程序、高危漏洞，提供安全修复方案</p>
        </div>
      </div>

      {error && (
        <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
            <p className="text-red-300 text-sm">{error}</p>
          </div>
        </div>
      )}

      <div className="glass-card rounded-3xl p-6 border border-red-500/30 bg-gradient-to-br from-red-500/10 to-rose-500/10">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-red-500 to-rose-500 flex items-center justify-center shrink-0">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white mb-2">安全声明</h2>
            <p className="text-gray-300 text-sm leading-relaxed">
              本工具<strong className="text-red-400">仅提供安全检测分析功能，不会直接修改或删除您的系统文件</strong>。
              检测结果仅供参考，不能替代专业的杀毒软件。
              对于检测到的可疑程序，请谨慎处理，建议使用专业杀毒软件进行确认。
              请定期更新系统补丁，启用防火墙，使用正规杀毒软件保护系统安全。
            </p>
          </div>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
          <ShieldAlert className="w-5 h-5 text-red-400" />
          系统安全扫描
        </h2>

        <div className="flex flex-wrap items-center gap-3 mb-6">
          <button
            onClick={handleScan}
            disabled={scanning || aiAnalyzing}
            className="btn-primary flex items-center justify-center gap-2"
          >
            {scanning ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                扫描系统中...
              </>
            ) : (
              <>
                <Zap className="w-5 h-5" />
                {scanResult && !showAIResult ? '重新扫描' : '开始扫描'}
              </>
            )}
          </button>
          <button
            onClick={handleAIAnalyze}
            disabled={scanning || aiAnalyzing}
            className="btn-secondary flex items-center justify-center gap-2 bg-gradient-to-r from-purple-600/20 to-pink-600/20 border-purple-500/30 hover:from-purple-600/30 hover:to-pink-600/30"
          >
            {aiAnalyzing ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                AI分析中...
              </>
            ) : (
              <>
                <Sparkles className="w-5 h-5 text-purple-400" />
                AI智能分析
              </>
            )}
          </button>
          {scanResult && !showAIResult && (
            <button
              onClick={handleScan}
              disabled={scanning || aiAnalyzing}
              className="btn-secondary flex items-center gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              重新扫描
            </button>
          )}
          {showAIResult && (
            <button
              onClick={() => setShowAIResult(false)}
              className="btn-secondary flex items-center gap-2"
            >
              <Bug className="w-4 h-4" />
              查看详细问题
            </button>
          )}
        </div>

        {scanning && (
          <div className="text-center py-12">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-red-500/20 to-rose-600/20 flex items-center justify-center mx-auto mb-4">
              <Loader2 className="w-10 h-10 text-red-400 animate-spin" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">正在扫描系统安全</h3>
            <p className="text-gray-400 text-sm">正在检测可疑程序和安全漏洞，请耐心等待...</p>
            <div className="mt-4 max-w-xs mx-auto">
              <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full rounded-full bg-gradient-to-r from-red-500 to-rose-600 animate-shimmer" style={{ width: '60%' }} />
              </div>
            </div>
          </div>
        )}

        {aiAnalyzing && (
          <div className="text-center py-12">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-purple-500/20 to-pink-600/20 flex items-center justify-center mx-auto mb-4">
              <Brain className="w-10 h-10 text-purple-400 animate-pulse" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">AI正在深入分析</h3>
            <p className="text-gray-400 text-sm">AI正在扫描并分析系统安全问题，请耐心等待...</p>
            <div className="mt-4 max-w-xs mx-auto">
              <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full rounded-full bg-gradient-to-r from-purple-500 to-pink-600 animate-shimmer" style={{ width: '70%' }} />
              </div>
            </div>
          </div>
        )}

        {showAIResult && aiAnalysis && !aiAnalyzing && (
          <>
            <div className="glass-card rounded-2xl p-6 mb-6 bg-gradient-to-br from-purple-500/10 to-pink-500/10 border border-purple-500/30">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center shrink-0">
                  <Sparkles className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">AI智能安全分析报告</h3>
                  <div className="flex items-center gap-2 text-xs text-gray-400">
                    <Clock className="w-3 h-3" />
                    <span>分析耗时: {(aiAnalysis.analysisDurationMs / 1000).toFixed(1)}s</span>
                    {aiAnalysis.tokens && (
                      <>
                        <span className="text-gray-600">|</span>
                        <span>Token消耗: {aiAnalysis.tokens}</span>
                      </>
                    )}
                    {aiAnalysis.model && (
                      <>
                        <span className="text-gray-600">|</span>
                        <span className={aiAnalysis.model === 'fallback' ? 'text-amber-400' : 'text-purple-400'}>
                          模型: {aiAnalysis.model === 'fallback' ? '智能模式(离线)' : aiAnalysis.model}
                        </span>
                      </>
                    )}
                  </div>
                </div>
              </div>
              <HealthScoreDisplay score={aiAnalysis.systemHealthScore} level={aiAnalysis.systemHealthLevel} />
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">安全问题</p>
                <p className="text-white font-bold text-lg">{aiAnalysis.totalIssues}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">高危项</p>
                <p className="text-red-400 font-bold text-lg">{aiAnalysis.highRiskCount}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">中危项</p>
                <p className="text-amber-400 font-bold text-lg">{aiAnalysis.mediumRiskCount}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">低危项</p>
                <p className="text-emerald-400 font-bold text-lg">{aiAnalysis.lowRiskCount}</p>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6 bg-gradient-to-r from-purple-500/10 to-blue-500/10 border border-purple-500/20">
              <div className="flex items-start gap-3">
                <Brain className="w-5 h-5 text-purple-400 shrink-0 mt-0.5" />
                <div>
                  <p className="text-purple-400 font-medium mb-1">AI分析见解</p>
                  <p className="text-gray-300 text-sm">{aiAnalysis.analysisInsight}</p>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6 bg-gradient-to-r from-red-500/10 to-rose-500/10 border border-red-500/20">
              <div className="flex items-start gap-3">
                <Shield className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                <div>
                  <p className="text-red-400 font-medium mb-1">安全评估</p>
                  <p className="text-gray-300 text-sm">{aiAnalysis.securityAssessment}</p>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6 bg-gradient-to-r from-cyan-500/10 to-teal-500/10 border border-cyan-500/20">
              <div className="flex items-start gap-3">
                <Lightbulb className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />
                <div>
                  <p className="text-cyan-400 font-medium mb-1">系统安全优化建议</p>
                  <div className="text-gray-300 text-sm whitespace-pre-line">{aiAnalysis.optimizationAdvice}</div>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6">
              <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                <Gauge className="w-4 h-4 text-purple-400" />
                AI安全建议（按优先级排序）
              </h3>
              <div className="space-y-3">
                {aiAnalysis.suggestions.map((suggestion, index) => (
                  <AISuggestionItem
                    key={index}
                    suggestion={suggestion}
                    isExpanded={expandedSuggestionId === index}
                    onToggleExpand={() => setExpandedSuggestionId(expandedSuggestionId === index ? null : index)}
                  />
                ))}
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 bg-gradient-to-br from-amber-500/10 to-orange-500/10 border border-amber-500/30">
              <div className="flex items-start gap-3">
                <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
                <div className="text-sm">
                  <p className="text-amber-400 font-medium mb-2">AI分析说明</p>
                  <p className="text-gray-300">
                    {aiAnalysis.model === 'fallback'
                      ? '当前使用智能离线模式提供分析建议。配置有效的API Key后可获得更精准的AI分析结果。'
                      : 'AI分析结果仅供参考，处理安全问题前请务必备份重要数据并确认操作的安全性。'}
                  </p>
                </div>
              </div>
            </div>
          </>
        )}

        {scanResult && !scanning && !showAIResult && (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-4 mb-6">
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">可疑程序</p>
                <p className="text-white font-bold text-lg">{scanResult.totalSuspiciousPrograms}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">安全漏洞</p>
                <p className="text-white font-bold text-lg">{scanResult.totalVulnerabilities}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">高危项</p>
                <p className="text-red-400 font-bold text-lg">{scanResult.highRiskCount}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">中危项</p>
                <p className="text-amber-400 font-bold text-lg">{scanResult.mediumRiskCount}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">扫描耗时</p>
                <p className="text-emerald-400 font-bold text-lg">{(scanResult.scanDurationMs / 1000).toFixed(1)}s</p>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/20">
              <div className="flex items-center gap-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="text-white font-medium">系统安全评分</span>
                    <span className="text-2xl font-bold text-white">{scanResult.systemHealthScore}</span>
                    <span className="text-cyan-400">/ 100</span>
                    <span className={`px-2 py-0.5 text-xs rounded-full ${HEALTH_LEVEL_CONFIG[scanResult.systemHealthLevel]?.bgColor || 'bg-gray-500/20'} ${HEALTH_LEVEL_CONFIG[scanResult.systemHealthLevel]?.color || 'text-gray-400'}`}>
                      {scanResult.systemHealthLevel}
                    </span>
                  </div>
                  <p className="text-gray-300 text-sm">{scanResult.summary}</p>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2 mb-4">
              <button
                onClick={() => setActiveTab('programs')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-colors ${
                  activeTab === 'programs'
                    ? 'bg-cyan-500 text-white'
                    : 'bg-white/5 text-gray-400 hover:text-white hover:bg-white/10'
                }`}
              >
                <Bug className="w-4 h-4 inline mr-2" />
                可疑程序 ({suspiciousPrograms.length})
              </button>
              <button
                onClick={() => setActiveTab('vulnerabilities')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-colors ${
                  activeTab === 'vulnerabilities'
                    ? 'bg-cyan-500 text-white'
                    : 'bg-white/5 text-gray-400 hover:text-white hover:bg-white/10'
                }`}
              >
                <Unlock className="w-4 h-4 inline mr-2" />
                安全漏洞 ({vulnerabilities.length})
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-4 mb-4">
              <div className="flex items-center gap-2">
                <label className="text-gray-400 text-sm">分类筛选:</label>
                <select
                  value={activeTab === 'programs' ? filterProgramCategory : filterVulnCategory}
                  onChange={(e) => {
                    if (activeTab === 'programs') {
                      setFilterProgramCategory(e.target.value);
                    } else {
                      setFilterVulnCategory(e.target.value);
                    }
                  }}
                  className="bg-white/10 border border-white/20 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-cyan-500/50"
                >
                  <option value="all">全部分类</option>
                  {Object.entries(
                    activeTab === 'programs' ? PROGRAM_CATEGORY_LABELS : VULN_CATEGORY_LABELS
                  ).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex items-center gap-2">
                <label className="text-gray-400 text-sm">风险筛选:</label>
                <select
                  value={filterSeverity}
                  onChange={(e) => setFilterSeverity(e.target.value)}
                  className="bg-white/10 border border-white/20 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-cyan-500/50"
                >
                  <option value="all">全部风险</option>
                  <option value="high">高危</option>
                  <option value="medium">中危</option>
                  <option value="low">低危</option>
                </select>
              </div>
              <button
                onClick={toggleSelectAll}
                disabled={(activeTab === 'programs' ? filteredPrograms : filteredVulns).length === 0}
                className="text-sm text-cyan-400 hover:text-cyan-300 disabled:text-gray-600 disabled:cursor-not-allowed flex items-center gap-1"
              >
                {(activeTab === 'programs' ? allProgramsSelected : allVulnsSelected) ? (
                  <><CheckSquare className="w-4 h-4" /> 取消全选</>
                ) : (
                  <><Square className="w-4 h-4" /> 全选当前筛选</>
                )}
              </button>
            </div>

            <div className="flex items-center justify-between mb-4 p-4 bg-white/5 rounded-xl">
              <div className="flex items-center gap-2">
                <CheckSquare className="w-5 h-5 text-cyan-400" />
                <span className="text-white">
                  已选择 <span className="text-cyan-400 font-bold">{selectedCount}</span> 项
                </span>
              </div>
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                  {(['high', 'medium', 'low'] as const).map((sev) => (
                    <button
                      key={sev}
                      onClick={() => selectBySeverity(sev)}
                      className={`px-2 py-1 text-xs rounded-full transition-colors ${SEVERITY_CONFIG[sev].bgColor} ${SEVERITY_CONFIG[sev].color} hover:opacity-80`}
                    >
                      全选{SEVERITY_CONFIG[sev].label}
                    </button>
                  ))}
                </div>
                <button
                  onClick={handleGenerateScript}
                  disabled={selectedCount === 0 || generating}
                  className="btn-primary flex items-center gap-2"
                >
                  {generating ? (
                    <>
                      <Loader2 className="w-5 h-5 animate-spin" />
                      生成中...
                    </>
                  ) : (
                    <>
                      <FileCode className="w-5 h-5" />
                      生成修复脚本
                    </>
                  )}
                </button>
              </div>
            </div>

            {activeTab === 'programs' && (
              <div className="space-y-3">
                {filteredPrograms.length === 0 ? (
                  <div className="text-center py-8">
                    <Shield className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
                    <p className="text-white font-medium">未发现可疑程序</p>
                    <p className="text-gray-400 text-sm mt-1">当前筛选条件下没有可疑程序</p>
                  </div>
                ) : (
                  filteredPrograms.map((program) => (
                    <SuspiciousProgramItem
                      key={program.id}
                      program={program}
                      onToggle={toggleProgram}
                      isExpanded={expandedProgramId === program.id}
                      onToggleExpand={() => setExpandedProgramId(expandedProgramId === program.id ? null : program.id)}
                    />
                  ))
                )}
              </div>
            )}

            {activeTab === 'vulnerabilities' && (
              <div className="space-y-3">
                {filteredVulns.length === 0 ? (
                  <div className="text-center py-8">
                    <Lock className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
                    <p className="text-white font-medium">未发现安全漏洞</p>
                    <p className="text-gray-400 text-sm mt-1">当前筛选条件下没有安全漏洞</p>
                  </div>
                ) : (
                  filteredVulns.map((vuln) => (
                    <VulnerabilityItem
                      key={vuln.id}
                      vuln={vuln}
                      onToggle={toggleVulnerability}
                      isExpanded={expandedVulnId === vuln.id}
                      onToggleExpand={() => setExpandedVulnId(expandedVulnId === vuln.id ? null : vuln.id)}
                    />
                  ))
                )}
              </div>
            )}
          </>
        )}

        {!scanResult && !scanning && !error && (
          <div className="text-center py-12">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-red-500/10 to-rose-600/10 flex items-center justify-center mx-auto mb-4">
              <ShieldAlert className="w-10 h-10 text-red-400" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">开始系统安全扫描</h3>
            <p className="text-gray-400 text-sm max-w-md mx-auto">
              点击"开始扫描"按钮，系统将检测可疑程序、系统漏洞、开放端口等安全风险，提供全面的安全评估报告
            </p>
          </div>
        )}
      </div>

      {scriptPreview && (
        <ScriptPreviewModal
          script={scriptPreview}
          onClose={() => setScriptPreview(null)}
          onDownload={handleDownloadScript}
        />
      )}
    </div>
  );
}
