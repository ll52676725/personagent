import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Database,
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
  HeartPulse,
  Lightbulb,
  Gauge,
  Clock,
  Sparkles,
} from 'lucide-react';
import { toolsApi } from '@/api';
import { RegistryAnalysisResult, RegistryIssue, CleanupScript, GenerateScriptRequest, RegistryAIAnalysisResult, RegistryAISuggestion } from '@/types';

const CATEGORY_LABELS: Record<string, string> = {
  file_association: '文件关联',
  startup_entry: '启动项',
  uninstall_info: '卸载残留',
  com_component: 'COM组件',
  service_entry: '服务项',
  help_file: '帮助文件',
  dll_reference: 'DLL引用',
  menu_extension: '菜单扩展',
  recent_docs: '最近文档',
  software_reg: '软件注册',
};

const SEVERITY_CONFIG: Record<string, { label: string; color: string; bgColor: string; borderColor: string }> = {
  high: { label: '高危', color: 'text-red-400', bgColor: 'bg-red-500/20', borderColor: 'border-red-500/30' },
  medium: { label: '中危', color: 'text-amber-400', bgColor: 'bg-amber-500/20', borderColor: 'border-amber-500/30' },
  low: { label: '低危', color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', borderColor: 'border-emerald-500/30' },
};

const CATEGORY_COLORS: Record<string, string> = {
  file_association: '#06b6d4',
  startup_entry: '#f59e0b',
  uninstall_info: '#8b5cf6',
  com_component: '#ef4444',
  service_entry: '#ec4899',
  help_file: '#14b8a6',
  dll_reference: '#3b82f6',
  menu_extension: '#10b981',
  recent_docs: '#6366f1',
  software_reg: '#f97316',
};

function SeverityBadge({ severity }: { severity: string }) {
  const config = SEVERITY_CONFIG[severity] || SEVERITY_CONFIG.low;
  return (
    <span className={`px-2 py-0.5 text-xs rounded-full ${config.bgColor} ${config.color} font-medium`}>
      {config.label}
    </span>
  );
}

function CategoryBarChart({ stats }: { stats: Record<string, number> }) {
  const entries = Object.entries(stats).filter(([, count]) => count > 0);
  const maxCount = Math.max(...entries.map(([, count]) => count), 1);

  if (entries.length === 0) return null;

  return (
    <div className="space-y-3">
      {entries.map(([category, count]) => (
        <div key={category} className="group">
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2">
              <span
                className="w-3 h-3 rounded-full shrink-0"
                style={{ backgroundColor: CATEGORY_COLORS[category] || '#9ca3af' }}
              />
              <span className="text-white text-sm font-medium">{CATEGORY_LABELS[category] || category}</span>
            </div>
            <span className="text-white font-bold">{count}</span>
          </div>
          <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden">
            <div
              className="h-full rounded-full transition-all duration-700 group-hover:opacity-80"
              style={{
                width: `${(count / maxCount) * 100}%`,
                backgroundColor: CATEGORY_COLORS[category] || '#9ca3af',
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

function IssueItem({
  issue,
  onToggle,
  isExpanded,
  onToggleExpand,
}: {
  issue: RegistryIssue;
  onToggle: (id: string) => void;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const severity = SEVERITY_CONFIG[issue.severity] || SEVERITY_CONFIG.low;

  return (
    <div className={`bg-white/5 rounded-2xl overflow-hidden border ${severity.borderColor} hover:bg-white/10 transition-colors`}>
      <div className="p-4 flex items-center gap-3">
        <button
          onClick={(e) => {
            e.stopPropagation();
            onToggle(issue.id);
          }}
          className="shrink-0 hover:scale-110 transition-transform"
        >
          {issue.selected ? (
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
            <AlertCircle className={`w-5 h-5 ${severity.color}`} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-0.5">
              <p className="text-white font-medium text-sm truncate">{issue.categoryLabel}</p>
              <SeverityBadge severity={issue.severity} />
            </div>
            <p className="text-gray-400 text-xs truncate">{issue.description}</p>
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
              <p className="text-cyan-400 font-medium mb-1">注册表路径</p>
              <p className="text-gray-300 font-mono break-all">{issue.registryPath}</p>
            </div>
          </div>
          {issue.valueName && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <FileText className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
              <div className="text-xs">
                <p className="text-purple-400 font-medium mb-1">值名称</p>
                <p className="text-gray-300 font-mono">{issue.valueName}</p>
              </div>
            </div>
          )}
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/20 rounded-xl">
            <Zap className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-cyan-400 font-medium mb-1">清理理由</p>
              <p className="text-gray-300">{issue.reason}</p>
            </div>
          </div>
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-amber-500/10 to-orange-500/10 border border-amber-500/20 rounded-xl">
            <Terminal className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-amber-400 font-medium mb-1">清理命令</p>
              <p className="text-gray-300 font-mono text-xs break-all">{issue.regCommand}</p>
            </div>
          </div>
          <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-red-500/10 to-rose-500/10 border border-red-500/20 rounded-xl">
            <Shield className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
            <div className="text-xs">
              <p className="text-red-400 font-medium mb-1">风险提示</p>
              <p className="text-gray-300">
                {issue.severity === 'high'
                  ? '高危操作！清理此项目可能影响系统或程序正常运行，请确保了解操作后果，建议先备份注册表。'
                  : issue.severity === 'medium'
                  ? '中风险操作。清理前建议先备份注册表，确认了解操作后果后再执行。'
                  : '低风险操作。一般不会影响系统正常运行，但仍建议备份后执行。'}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ScriptPreviewModal({
  script,
  onClose,
  onDownload,
}: {
  script: CleanupScript;
  onClose: () => void;
  onDownload: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="glass-card rounded-3xl p-6 max-w-4xl w-full max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center">
              <FileCode className="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">{script.scriptName}</h2>
              <p className="text-gray-400 text-sm">包含 {script.issueCount} 项清理操作</p>
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

const HEALTH_LEVEL_CONFIG: Record<string, { color: string; bgColor: string; borderColor: string }> = {
  优秀: { color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', borderColor: 'border-emerald-500/30' },
  良好: { color: 'text-cyan-400', bgColor: 'bg-cyan-500/20', borderColor: 'border-cyan-500/30' },
  一般: { color: 'text-amber-400', bgColor: 'bg-amber-500/20', borderColor: 'border-amber-500/30' },
  较差: { color: 'text-orange-400', bgColor: 'bg-orange-500/20', borderColor: 'border-orange-500/30' },
  危险: { color: 'text-red-400', bgColor: 'bg-red-500/20', borderColor: 'border-red-500/30' },
};

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
            stroke="url(#healthGradient)"
            strokeWidth="8"
            fill="none"
            strokeLinecap="round"
            strokeDasharray={`${scoreNum * 3.52} 352`}
            className="transition-all duration-1000 ease-out"
          />
          <defs>
            <linearGradient id="healthGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" className={scoreNum >= 90 ? 'stop-color-emerald-500' : scoreNum >= 75 ? 'stop-color-cyan-500' : scoreNum >= 60 ? 'stop-color-amber-500' : scoreNum >= 40 ? 'stop-color-orange-500' : 'stop-color-red-500'} />
              <stop offset="100%" className={scoreNum >= 90 ? 'stop-color-teal-500' : scoreNum >= 75 ? 'stop-color-blue-500' : scoreNum >= 60 ? 'stop-color-yellow-500' : scoreNum >= 40 ? 'stop-color-red-500' : 'stop-color-rose-600'} />
            </linearGradient>
          </defs>
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-3xl font-bold text-white">{score}</span>
          <span className="text-xs text-gray-400">健康分</span>
        </div>
      </div>
      <div className="flex-1">
        <div className="flex items-center gap-2 mb-2">
          <HeartPulse className={`w-5 h-5 ${config.color}`} />
          <span className={`text-lg font-bold ${config.color}`}>{level}</span>
        </div>
        <p className="text-gray-400 text-sm">
          {scoreNum >= 90
            ? '注册表状态非常好，继续保持良好的使用习惯！'
            : scoreNum >= 75
            ? '注册表状态良好，存在少量可优化项。'
            : scoreNum >= 60
            ? '注册表存在一些问题，建议进行清理优化。'
            : scoreNum >= 40
            ? '注册表问题较多，可能影响系统性能，建议尽快清理。'
            : '注册表存在严重问题，强烈建议立即清理并备份系统！'}
        </p>
      </div>
    </div>
  );
}

function AISuggestionItem({
  suggestion,
  isExpanded,
  onToggleExpand,
}: {
  suggestion: RegistryAISuggestion;
  isExpanded: boolean;
  onToggleExpand: () => void;
}) {
  const severity = SEVERITY_CONFIG[suggestion.riskLevel] || SEVERITY_CONFIG.low;
  const categoryColor = CATEGORY_COLORS[suggestion.category] || '#9ca3af';

  return (
    <div className={`bg-white/5 rounded-2xl overflow-hidden border ${severity.borderColor} hover:bg-white/10 transition-colors`}>
      <button onClick={onToggleExpand} className="w-full p-4 flex items-center gap-3 text-left">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${severity.bgColor}`}>
          <span className="text-white font-bold text-sm">#{suggestion.priority}</span>
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
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
              <p className="text-purple-400 font-medium mb-1">AI分析 - 为什么可以清理</p>
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
          {suggestion.registryPaths && suggestion.registryPaths.length > 0 && (
            <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
              <FileText className="w-4 h-4 text-gray-400 shrink-0 mt-0.5" />
              <div className="text-xs flex-1">
                <p className="text-gray-400 font-medium mb-1">相关注册表路径（{suggestion.registryPaths.length}条）</p>
                <div className="space-y-1">
                  {suggestion.registryPaths.slice(0, 3).map((path, idx) => (
                    <p key={idx} className="text-gray-300 font-mono text-xs break-all">
                      {path}
                    </p>
                  ))}
                  {suggestion.registryPaths.length > 3 && (
                    <p className="text-gray-500">...还有 {suggestion.registryPaths.length - 3} 条</p>
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

export default function RegistryCleaner() {
  const navigate = useNavigate();
  const [analysis, setAnalysis] = useState<RegistryAnalysisResult | null>(null);
  const [issues, setIssues] = useState<RegistryIssue[]>([]);
  const [analyzing, setAnalyzing] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [filterSeverity, setFilterSeverity] = useState<string>('all');
  const [scriptPreview, setScriptPreview] = useState<CleanupScript | null>(null);
  const [scriptType, setScriptType] = useState<'reg' | 'bat'>('reg');
  const [includeBackup, setIncludeBackup] = useState(true);
  const [aiAnalysis, setAiAnalysis] = useState<RegistryAIAnalysisResult | null>(null);
  const [aiAnalyzing, setAiAnalyzing] = useState(false);
  const [showAIResult, setShowAIResult] = useState(false);
  const [expandedSuggestionId, setExpandedSuggestionId] = useState<number | null>(null);

  const filteredIssues = useMemo(() => {
    return issues.filter((issue) => {
      if (filterCategory !== 'all' && issue.category !== filterCategory) return false;
      if (filterSeverity !== 'all' && issue.severity !== filterSeverity) return false;
      return true;
    });
  }, [issues, filterCategory, filterSeverity]);

  const selectedCount = issues.filter((i) => i.selected).length;
  const selectedIds = useMemo(() => issues.filter((i) => i.selected).map((i) => i.id), [issues]);

  const handleAnalyze = async () => {
    setAnalyzing(true);
    setError('');
    setAnalysis(null);
    setIssues([]);
    try {
      const res = await toolsApi.analyzeRegistry();
      if (res.code === 200 && res.data) {
        setAnalysis(res.data);
        setIssues(res.data.issues);
      } else {
        setError(res.message || '分析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '分析失败，请稍后重试');
    } finally {
      setAnalyzing(false);
    }
  };

  const toggleIssue = (id: string) => {
    setIssues((prev) =>
      prev.map((issue) => (issue.id === id ? { ...issue, selected: !issue.selected } : issue))
    );
  };

  const toggleSelectAll = () => {
    const allSelected = filteredIssues.every((i) => i.selected);
    const filteredIds = new Set(filteredIssues.map((i) => i.id));
    setIssues((prev) =>
      prev.map((issue) => (filteredIds.has(issue.id) ? { ...issue, selected: !allSelected } : issue))
    );
  };

  const selectBySeverity = (severity: string) => {
    setIssues((prev) =>
      prev.map((issue) => (issue.severity === severity ? { ...issue, selected: true } : issue))
    );
  };

  const handleAIAnalyze = async () => {
    setAiAnalyzing(true);
    setError('');
    try {
      const res = await toolsApi.aiAnalyzeRegistry();
      if (res.code === 200 && res.data) {
        setAiAnalysis(res.data);
        setShowAIResult(true);
        if (res.data.model !== 'fallback') {
          setAnalysis({
            summary: res.data.summary,
            totalIssues: res.data.totalIssues,
            analysisDurationMs: res.data.analysisDurationMs,
            categoryStats: {},
            severityStats: {
              high: res.data.highRiskCount,
              medium: res.data.mediumRiskCount,
              low: res.data.lowRiskCount,
            },
            issues: [],
            disclaimer: '',
          });
        }
      } else {
        setError(res.message || 'AI分析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI分析失败，请稍后重试');
    } finally {
      setAiAnalyzing(false);
    }
  };

  const handleGenerateScript = async () => {
    if (selectedIds.length === 0) {
      setError('请选择要清理的注册表项');
      return;
    }

    setGenerating(true);
    setError('');
    try {
      const request: GenerateScriptRequest = {
        selectedIssueIds: selectedIds,
        scriptType,
        includeBackup,
      };
      const res = await toolsApi.generateCleanupScript(request);
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

  const handleDownloadScript = async () => {
    if (selectedIds.length === 0) return;

    try {
      const request: GenerateScriptRequest = {
        selectedIssueIds: selectedIds,
        scriptType,
        includeBackup,
      };
      const blob = await toolsApi.downloadCleanupScript(request);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = scriptPreview?.scriptName || `registry_cleanup.${scriptType}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      setScriptPreview(null);
    } catch (err: any) {
      setError(err.response?.data?.message || '下载失败，请稍后重试');
    }
  };

  const allFilteredSelected = filteredIssues.length > 0 && filteredIssues.every((i) => i.selected);

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
          <h1 className="text-3xl font-bold text-white mb-1">注册表清理</h1>
          <p className="text-gray-400">扫描注册表冗余项，生成清理脚本，安全优化系统</p>
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

      <div className="glass-card rounded-3xl p-6 border border-amber-500/30 bg-gradient-to-br from-amber-500/10 to-orange-500/10">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-500 flex items-center justify-center shrink-0">
            <Shield className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-white mb-2">安全声明</h2>
            <p className="text-gray-300 text-sm leading-relaxed">
              本工具<strong className="text-amber-400">不会直接修改您的系统注册表</strong>。
              分析完成后，您可以选择需要清理的项目，工具将生成一个可执行的清理脚本（.reg 或 .bat 格式）。
              请在执行脚本前<strong className="text-amber-400">务必备份注册表</strong>并创建系统还原点。
              对于因使用本工具生成的脚本导致的任何问题，本工具不承担任何责任。
            </p>
          </div>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
          <Database className="w-5 h-5 text-rose-400" />
          注册表分析
        </h2>

        <div className="flex flex-wrap items-center gap-3 mb-6">
          <button
            onClick={handleAnalyze}
            disabled={analyzing || aiAnalyzing}
            className="btn-primary flex items-center justify-center gap-2"
          >
            {analyzing ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                扫描注册表中...
              </>
            ) : (
              <>
                <Zap className="w-5 h-5" />
                {analysis && !showAIResult ? '重新扫描' : '开始扫描'}
              </>
            )}
          </button>
          <button
            onClick={handleAIAnalyze}
            disabled={analyzing || aiAnalyzing}
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
          {analysis && !showAIResult && (
            <button
              onClick={handleAnalyze}
              disabled={analyzing || aiAnalyzing}
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
              <Database className="w-4 h-4" />
              查看详细问题
            </button>
          )}
        </div>

        {analyzing && (
          <div className="text-center py-12">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-rose-500/20 to-red-600/20 flex items-center justify-center mx-auto mb-4">
              <Loader2 className="w-10 h-10 text-rose-400 animate-spin" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">正在扫描注册表</h3>
            <p className="text-gray-400 text-sm">正在分析系统注册表，请耐心等待...</p>
            <div className="mt-4 max-w-xs mx-auto">
              <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                <div className="h-full rounded-full bg-gradient-to-r from-rose-500 to-red-600 animate-shimmer" style={{ width: '60%' }} />
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
            <p className="text-gray-400 text-sm">AI正在扫描并分析注册表问题，请耐心等待...</p>
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
                  <h3 className="text-lg font-bold text-white">AI智能分析报告</h3>
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
                <p className="text-gray-400 text-xs mb-1">发现问题</p>
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

            <div className="glass-card rounded-2xl p-5 mb-6 bg-gradient-to-r from-cyan-500/10 to-teal-500/10 border border-cyan-500/20">
              <div className="flex items-start gap-3">
                <Lightbulb className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />
                <div>
                  <p className="text-cyan-400 font-medium mb-1">系统优化建议</p>
                  <div className="text-gray-300 text-sm whitespace-pre-line">{aiAnalysis.optimizationAdvice}</div>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6">
              <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                <Gauge className="w-4 h-4 text-purple-400" />
                AI清理建议（按优先级排序）
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
                      : 'AI分析结果仅供参考，清理注册表前请务必备份重要数据并创建系统还原点。'}
                  </p>
                </div>
              </div>
            </div>
          </>
        )}

        {analysis && !analyzing && !showAIResult && (
          <>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">发现问题</p>
                <p className="text-white font-bold text-lg">{analysis.totalIssues}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">高危项</p>
                <p className="text-red-400 font-bold text-lg">{analysis.severityStats.high || 0}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">中危项</p>
                <p className="text-amber-400 font-bold text-lg">{analysis.severityStats.medium || 0}</p>
              </div>
              <div className="stat-card">
                <p className="text-gray-400 text-xs mb-1">扫描耗时</p>
                <p className="text-emerald-400 font-bold text-lg">{(analysis.analysisDurationMs / 1000).toFixed(1)}s</p>
              </div>
            </div>

            <div className="glass-card rounded-2xl p-5 mb-6 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/20">
              <div className="flex items-start gap-3">
                <Info className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />
                <p className="text-gray-300 text-sm">{analysis.summary}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
              <div className="lg:col-span-2">
                <div className="glass-card rounded-2xl p-5 h-full">
                  <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                    <Database className="w-4 h-4 text-rose-400" />
                    问题分类统计
                  </h3>
                  <CategoryBarChart stats={analysis.categoryStats} />
                </div>
              </div>
              <div>
                <div className="glass-card rounded-2xl p-5 h-full">
                  <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-amber-400" />
                    风险等级分布
                  </h3>
                  <div className="space-y-4">
                    {(['high', 'medium', 'low'] as const).map((sev) => {
                      const count = analysis.severityStats[sev] || 0;
                      return (
                        <div key={sev} className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <SeverityBadge severity={sev} />
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="text-white font-bold">{count}</span>
                            <button
                              onClick={() => selectBySeverity(sev)}
                              disabled={count === 0}
                              className="text-xs text-cyan-400 hover:text-cyan-300 disabled:text-gray-600 disabled:cursor-not-allowed"
                            >
                              全选
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-4 mb-4">
              <div className="flex items-center gap-2">
                <label className="text-gray-400 text-sm">分类筛选:</label>
                <select
                  value={filterCategory}
                  onChange={(e) => setFilterCategory(e.target.value)}
                  className="bg-white/10 border border-white/20 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-cyan-500/50"
                >
                  <option value="all">全部分类</option>
                  {Object.entries(CATEGORY_LABELS).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label} ({analysis.categoryStats[key] || 0})
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
                disabled={filteredIssues.length === 0}
                className="text-sm text-cyan-400 hover:text-cyan-300 disabled:text-gray-600 disabled:cursor-not-allowed flex items-center gap-1"
              >
                {allFilteredSelected ? (
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
                  已选择 <span className="text-cyan-400 font-bold">{selectedCount}</span> / {issues.length} 项
                </span>
              </div>
              <div className="flex items-center gap-4">
                <div className="flex items-center gap-2">
                  <label className="text-gray-400 text-sm">脚本类型:</label>
                  <div className="flex bg-white/10 rounded-lg p-1">
                    <button
                      onClick={() => setScriptType('reg')}
                      className={`px-3 py-1 rounded-md text-sm transition-colors ${
                        scriptType === 'reg'
                          ? 'bg-cyan-500 text-white'
                          : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      .reg
                    </button>
                    <button
                      onClick={() => setScriptType('bat')}
                      className={`px-3 py-1 rounded-md text-sm transition-colors ${
                        scriptType === 'bat'
                          ? 'bg-cyan-500 text-white'
                          : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      .bat
                    </button>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <label className="text-gray-400 text-sm">包含备份:</label>
                  <button
                    onClick={() => setIncludeBackup(!includeBackup)}
                    className={`w-10 h-6 rounded-full transition-colors ${
                      includeBackup ? 'bg-cyan-500' : 'bg-gray-600'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full bg-white transition-transform ${
                        includeBackup ? 'translate-x-5' : 'translate-x-1'
                      }`}
                    />
                  </button>
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
                      生成清理脚本
                    </>
                  )}
                </button>
              </div>
            </div>

            <div className="space-y-3">
              {filteredIssues.length === 0 ? (
                <div className="text-center py-8">
                  <Shield className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
                  <p className="text-white font-medium">未发现问题</p>
                  <p className="text-gray-400 text-sm mt-1">当前筛选条件下没有问题项</p>
                </div>
              ) : (
                filteredIssues.map((issue) => (
                  <IssueItem
                    key={issue.id}
                    issue={issue}
                    onToggle={toggleIssue}
                    isExpanded={expandedId === issue.id}
                    onToggleExpand={() => setExpandedId(expandedId === issue.id ? null : issue.id)}
                  />
                ))
              )}
            </div>
          </>
        )}

        {!analysis && !analyzing && !error && (
          <div className="text-center py-12">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-rose-500/10 to-red-600/10 flex items-center justify-center mx-auto mb-4">
              <Database className="w-10 h-10 text-rose-400" />
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">开始扫描注册表</h3>
            <p className="text-gray-400 text-sm max-w-md mx-auto">
              点击"开始扫描"按钮，系统将分析您的Windows注册表，识别无效的文件关联、启动项、卸载残留等冗余内容
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
