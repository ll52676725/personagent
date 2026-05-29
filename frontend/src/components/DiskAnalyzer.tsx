import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  HardDrive,
  ArrowLeft,
  Loader2,
  AlertCircle,
  Folder,
  Trash2,
  FileText,
  BarChart3,
  Shield,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Info,
  Sparkles,
  Zap,
  Brain,
  Lightbulb,
  Star,
} from 'lucide-react';
import { toolsApi } from '@/api';
import { DriveInfo, DriveAnalysisResult, FileTypeStat, FolderStat, CleanupSuggestion, AIAnalysisResult } from '@/types';

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  if (bytes < 1024 * 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
  return (bytes / (1024 * 1024 * 1024 * 1024)).toFixed(2) + ' TB';
}

function DonutChart({ stats }: { stats: FileTypeStat[] }) {
  const total = stats.reduce((sum, s) => sum + s.size, 0);
  if (total === 0) return null;

  const radius = 80;
  const circumference = 2 * Math.PI * radius;
  let accumulated = 0;

  const segments = stats.map((stat) => {
    const percentage = stat.size / total;
    const offset = circumference * (1 - accumulated);
    const dashLength = circumference * percentage;
    accumulated += percentage;
    return { ...stat, offset, dashLength, percentage };
  });

  return (
    <div className="relative flex items-center justify-center">
      <svg width="220" height="220" viewBox="0 0 220 220" className="-rotate-90">
        <circle cx="110" cy="110" r={radius} fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="28" />
        {segments.map((seg, i) => (
          <circle
            key={i}
            cx="110"
            cy="110"
            r={radius}
            fill="none"
            stroke={seg.color}
            strokeWidth="28"
            strokeDasharray={`${seg.dashLength} ${circumference - seg.dashLength}`}
            strokeDashoffset={seg.offset}
            strokeLinecap="butt"
            className="transition-all duration-700"
          />
        ))}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-2xl font-bold text-white">{formatSize(total)}</span>
        <span className="text-xs text-gray-400 mt-1">已扫描总量</span>
      </div>
    </div>
  );
}

function DriveCard({ drive, selected, onClick }: { drive: DriveInfo; selected: boolean; onClick: () => void }) {
  const usedPercent = drive.totalSpace > 0 ? (drive.usedSpace / drive.totalSpace) * 100 : 0;
  const barColor = usedPercent > 90 ? 'from-red-500 to-red-600' : usedPercent > 70 ? 'from-amber-500 to-orange-500' : 'from-cyan-500 to-blue-600';

  return (
    <button
      onClick={onClick}
      className={`glass-card rounded-2xl p-5 text-left transition-all w-full ${
        selected
          ? 'ring-2 ring-cyan-400/60 border-cyan-400/40 shadow-lg shadow-cyan-500/10'
          : 'glass-card-hover'
      }`}
    >
      <div className="flex items-center gap-3 mb-3">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
          selected ? 'bg-gradient-to-br from-cyan-500 to-blue-600' : 'bg-white/10'
        }`}>
          <HardDrive className={`w-5 h-5 ${selected ? 'text-white' : 'text-gray-400'}`} />
        </div>
        <div>
          <p className="text-white font-bold text-lg">{drive.displayName}</p>
          <p className="text-gray-400 text-xs">{drive.fileSystem || '未知文件系统'}</p>
        </div>
      </div>
      <div className="w-full h-2.5 bg-white/10 rounded-full overflow-hidden mb-2">
        <div
          className={`h-full rounded-full bg-gradient-to-r ${barColor} transition-all duration-500`}
          style={{ width: `${Math.min(usedPercent, 100)}%` }}
        />
      </div>
      <div className="flex justify-between text-xs">
        <span className="text-gray-400">已用 {formatSize(drive.usedSpace)}</span>
        <span className="text-gray-400">共 {formatSize(drive.totalSpace)}</span>
      </div>
      {drive.freeSpace < drive.totalSpace * 0.1 && (
        <p className="text-red-400 text-xs mt-1.5 flex items-center gap-1">
          <AlertCircle className="w-3 h-3" />
          剩余空间不足 10%
        </p>
      )}
    </button>
  );
}

function FileTypeStatsPanel({ stats }: { stats: FileTypeStat[] }) {
  const maxSize = Math.max(...stats.map(s => s.size), 1);

  return (
    <div className="glass-card rounded-3xl p-6">
      <h2 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
        <BarChart3 className="w-5 h-5 text-cyan-400" />
        文件类型分析
      </h2>
      <div className="flex flex-col lg:flex-row gap-6 items-center">
        <div className="shrink-0">
          <DonutChart stats={stats} />
        </div>
        <div className="flex-1 w-full space-y-3">
          {stats.map((stat) => (
            <div key={stat.category} className="group">
              <div className="flex items-center justify-between mb-1">
                <div className="flex items-center gap-2">
                  <span
                    className="w-3 h-3 rounded-full shrink-0"
                    style={{ backgroundColor: stat.color }}
                  />
                  <span className="text-white text-sm font-medium">{stat.label}</span>
                </div>
                <div className="flex items-center gap-3 text-xs text-gray-400">
                  <span>{stat.fileCount.toLocaleString()} 个文件</span>
                  <span className="text-white font-medium">{stat.percentage.toFixed(1)}%</span>
                </div>
              </div>
              <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-700 group-hover:opacity-80"
                  style={{
                    width: `${(stat.size / maxSize) * 100}%`,
                    backgroundColor: stat.color,
                  }}
                />
              </div>
              <p className="text-gray-400 text-xs mt-0.5">{formatSize(stat.size)}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function TopFoldersPanel({ folders }: { folders: FolderStat[] }) {
  const maxSize = Math.max(...folders.map(f => f.size), 1);

  return (
    <div className="glass-card rounded-3xl p-6">
      <h2 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
        <Folder className="w-5 h-5 text-amber-400" />
        占用空间最大的文件夹 TOP {folders.length}
      </h2>
      <div className="space-y-3">
        {folders.map((folder, index) => (
          <div key={folder.path} className="group hover:bg-white/5 rounded-xl p-3 transition-colors">
            <div className="flex items-center gap-3 mb-1.5">
              <span className="text-xs font-bold text-gray-500 w-5 text-right">{index + 1}</span>
              <div className="flex-1 min-w-0">
                <p className="text-white text-sm font-medium truncate">{folder.name}</p>
                <p className="text-gray-500 text-xs truncate">{folder.path}</p>
              </div>
              <div className="text-right shrink-0">
                <p className="text-white text-sm font-bold">{formatSize(folder.size)}</p>
                <p className="text-gray-500 text-xs">{folder.percentage.toFixed(1)}%</p>
              </div>
            </div>
            <div className="ml-8 w-full h-1.5 bg-white/5 rounded-full overflow-hidden">
              <div
                className="h-full rounded-full bg-gradient-to-r from-amber-500 to-orange-500 transition-all duration-500"
                style={{ width: `${(folder.size / maxSize) * 100}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function CleanupSuggestionsPanel({ suggestions }: { suggestions: CleanupSuggestion[] }) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  const riskConfig: Record<string, { label: string; color: string; bgColor: string }> = {
    low: { label: '低风险', color: 'text-emerald-400', bgColor: 'bg-emerald-500/20' },
    medium: { label: '中风险', color: 'text-amber-400', bgColor: 'bg-amber-500/20' },
    high: { label: '高风险', color: 'text-red-400', bgColor: 'bg-red-500/20' },
  };

  const totalCleanable = suggestions.reduce((sum, s) => sum + s.size, 0);

  return (
    <div className="glass-card rounded-3xl p-6">
      <h2 className="text-xl font-bold text-white mb-2 flex items-center gap-2">
        <Trash2 className="w-5 h-5 text-pink-400" />
        清理建议
      </h2>
      {suggestions.length > 0 && (
        <p className="text-gray-400 text-sm mb-5">
          共发现 <span className="text-pink-400 font-medium">{suggestions.length}</span> 项可清理内容，
          预计可释放 <span className="text-pink-400 font-medium">{formatSize(totalCleanable)}</span> 空间
        </p>
      )}

      {suggestions.length === 0 ? (
        <div className="text-center py-8">
          <Shield className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
          <p className="text-white font-medium">磁盘状态良好</p>
          <p className="text-gray-400 text-sm mt-1">未发现需要清理的内容</p>
        </div>
      ) : (
        <div className="space-y-3">
          {suggestions.map((suggestion, index) => {
            const risk = riskConfig[suggestion.riskLevel] || riskConfig.low;
            const isExpanded = expandedIndex === index;

            return (
              <div key={index} className="bg-white/5 rounded-2xl overflow-hidden">
                <button
                  onClick={() => setExpandedIndex(isExpanded ? null : index)}
                  className="w-full p-4 flex items-center gap-3 text-left hover:bg-white/5 transition-colors"
                >
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${risk.bgColor}`}>
                    <Trash2 className={`w-5 h-5 ${risk.color}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                      <p className="text-white font-medium text-sm truncate">{suggestion.title}</p>
                      <span className={`px-2 py-0.5 text-xs rounded-full ${risk.bgColor} ${risk.color} shrink-0`}>
                        {risk.label}
                      </span>
                    </div>
                    <p className="text-gray-400 text-xs">{formatSize(suggestion.size)}</p>
                  </div>
                  {isExpanded ? (
                    <ChevronUp className="w-4 h-4 text-gray-400 shrink-0" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-gray-400 shrink-0" />
                  )}
                </button>
                {isExpanded && (
                  <div className="px-4 pb-4 space-y-3 animate-fadeIn">
                    <p className="text-gray-300 text-sm">{suggestion.description}</p>
                    <div className="flex items-center gap-2 text-xs text-gray-400">
                      <Folder className="w-3.5 h-3.5" />
                      <span className="truncate">{suggestion.path}</span>
                    </div>
                    <div className="flex items-center gap-2 p-3 bg-white/5 rounded-xl">
                      <Info className="w-4 h-4 text-cyan-400 shrink-0" />
                      <p className="text-xs text-gray-300">
                        建议操作：{suggestion.action}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl">
                      <Shield className="w-4 h-4 text-amber-400 shrink-0" />
                      <p className="text-xs text-amber-300">
                        {suggestion.riskLevel === 'low'
                          ? '清理此项目风险较低，一般不会影响系统正常运行'
                          : suggestion.riskLevel === 'medium'
                          ? '清理前请确认无需保留相关文件，建议先备份重要数据'
                          : '清理此项目可能影响系统或程序运行，请谨慎操作'}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function AIAnalysisPanel({ aiResult }: { aiResult: AIAnalysisResult }) {
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  const riskConfig: Record<string, { label: string; color: string; bgColor: string }> = {
    low: { label: '低风险', color: 'text-emerald-400', bgColor: 'bg-emerald-500/20' },
    medium: { label: '中风险', color: 'text-amber-400', bgColor: 'bg-amber-500/20' },
    high: { label: '高风险', color: 'text-red-400', bgColor: 'bg-red-500/20' },
  };

  const priorityColor = (priority: number) => {
    if (priority >= 9) return 'from-red-500 to-orange-500';
    if (priority >= 7) return 'from-amber-500 to-yellow-500';
    if (priority >= 5) return 'from-cyan-500 to-blue-500';
    return 'from-gray-500 to-gray-600';
  };

  const priorityLabel = (priority: number) => {
    if (priority >= 9) return '紧急';
    if (priority >= 7) return '高优先级';
    if (priority >= 5) return '中优先级';
    return '低优先级';
  };

  return (
    <div className="space-y-6">
      <div className="glass-card rounded-3xl p-6 border border-purple-500/30 bg-gradient-to-br from-purple-500/10 to-pink-500/10">
        <div className="flex items-start gap-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center shrink-0">
            <Brain className="w-7 h-7 text-white" />
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <h2 className="text-xl font-bold text-white">AI 智能分析报告</h2>
              {aiResult.model && (
                <span className="px-2 py-0.5 text-xs rounded-full bg-purple-500/20 text-purple-300">
                  {aiResult.model === 'fallback' ? '模拟模式' : aiResult.model}
                </span>
              )}
            </div>
            <p className="text-gray-300 text-sm leading-relaxed">{aiResult.summary}</p>
            {aiResult.tokens && (
              <p className="text-gray-500 text-xs mt-2">Token 消耗: {aiResult.tokens} | 分析耗时: {(aiResult.analysisDurationMs / 1000).toFixed(1)}s</p>
            )}
          </div>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-500/20 flex items-center justify-center">
            <Zap className="w-5 h-5 text-cyan-400" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-white">可释放空间</h3>
            <p className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
              {formatSize(aiResult.totalReclaimableSpace)}
            </p>
          </div>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <Lightbulb className="w-5 h-5 text-amber-400" />
          AI 分析见解
        </h2>
        <div className="bg-gradient-to-r from-amber-500/10 to-orange-500/10 border border-amber-500/20 rounded-2xl p-4">
          <p className="text-gray-300 text-sm leading-relaxed">{aiResult.analysisInsight}</p>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-purple-400" />
            AI 清理建议
          </h2>
          <span className="text-sm text-gray-400">共 {aiResult.suggestions.length} 项</span>
        </div>

        <div className="space-y-3">
          {aiResult.suggestions.map((suggestion, index) => {
            const risk = riskConfig[suggestion.riskLevel] || riskConfig.low;
            const isExpanded = expandedIndex === index;

            return (
              <div key={index} className="bg-white/5 rounded-2xl overflow-hidden group hover:bg-white/10 transition-colors">
                <button
                  onClick={() => setExpandedIndex(isExpanded ? null : index)}
                  className="w-full p-4 flex items-center gap-3 text-left"
                >
                  <div className={`w-8 h-8 rounded-lg bg-gradient-to-r ${priorityColor(suggestion.priority)} flex items-center justify-center shrink-0`}>
                    <Star className="w-4 h-4 text-white" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                      <p className="text-white font-medium text-sm truncate">{suggestion.title}</p>
                      <span className={`px-1.5 py-0.5 text-xs rounded ${risk.bgColor} ${risk.color} shrink-0`}>
                        {risk.label}
                      </span>
                      <span className={`px-1.5 py-0.5 text-xs rounded bg-white/10 text-gray-400 shrink-0`}>
                        {priorityLabel(suggestion.priority)}
                      </span>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-gray-400">
                      <span className="text-purple-400 font-medium">{formatSize(suggestion.estimatedSize)}</span>
                      <span>·</span>
                      <span>优先级 {suggestion.priority}</span>
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
                    <p className="text-gray-300 text-sm">{suggestion.description}</p>
                    <div className="flex items-center gap-2 text-xs text-gray-400">
                      <Folder className="w-3.5 h-3.5" />
                      <span className="truncate font-mono">{suggestion.path}</span>
                    </div>
                    <div className="flex items-start gap-2 p-3 bg-white/5 rounded-xl">
                      <Info className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
                      <p className="text-xs text-gray-300">
                        <span className="text-cyan-400 font-medium">建议操作：</span>{suggestion.action}
                      </p>
                    </div>
                    <div className="flex items-start gap-2 p-3 bg-gradient-to-r from-purple-500/10 to-pink-500/10 border border-purple-500/20 rounded-xl">
                      <Brain className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
                      <p className="text-xs text-gray-300">
                        <span className="text-purple-400 font-medium">AI 分析：</span>{suggestion.reason}
                      </p>
                    </div>
                    <div className="flex items-start gap-2 p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl">
                      <Shield className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                      <p className="text-xs text-amber-300">
                        {suggestion.riskLevel === 'low'
                          ? '清理此项目风险较低，一般不会影响系统正常运行'
                          : suggestion.riskLevel === 'medium'
                          ? '清理前请确认无需保留相关文件，建议先备份重要数据'
                          : '清理此项目可能影响系统或程序运行，请谨慎操作'}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default function DiskAnalyzer() {
  const navigate = useNavigate();
  const [drives, setDrives] = useState<DriveInfo[]>([]);
  const [selectedDrive, setSelectedDrive] = useState<string>('');
  const [analysis, setAnalysis] = useState<DriveAnalysisResult | null>(null);
  const [aiAnalysis, setAiAnalysis] = useState<AIAnalysisResult | null>(null);
  const [loadingDrives, setLoadingDrives] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [aiAnalyzing, setAiAnalyzing] = useState(false);
  const [showAiAnalysis, setShowAiAnalysis] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchDrives();
  }, []);

  const fetchDrives = async () => {
    setLoadingDrives(true);
    setError('');
    try {
      const res = await toolsApi.getAvailableDrives();
      if (res.code === 200 && res.data) {
        setDrives(res.data);
        if (res.data.length > 0 && !selectedDrive) {
          setSelectedDrive(res.data[0].driveLetter);
        }
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '获取盘符列表失败');
    } finally {
      setLoadingDrives(false);
    }
  };

  const handleAnalyze = async () => {
    if (!selectedDrive) return;
    setAnalyzing(true);
    setError('');
    setAnalysis(null);
    setAiAnalysis(null);
    setShowAiAnalysis(false);
    try {
      const res = await toolsApi.analyzeDrive(selectedDrive, 5);
      if (res.code === 200 && res.data) {
        setAnalysis(res.data);
      } else {
        setError(res.message || '分析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '分析失败，请稍后重试');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleAIAnalyze = async () => {
    if (!selectedDrive) return;
    setAiAnalyzing(true);
    setError('');
    try {
      const res = await toolsApi.aiAnalyzeDrive(selectedDrive, 5);
      if (res.code === 200 && res.data) {
        setAiAnalysis(res.data);
        setShowAiAnalysis(true);
      } else {
        setError(res.message || 'AI分析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI分析失败，请稍后重试');
    } finally {
      setAiAnalyzing(false);
    }
  };

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
          <h1 className="text-3xl font-bold text-white mb-1">磁盘空间分析</h1>
          <p className="text-gray-400">分析盘符空间占用，按文件类型分类统计，提供清理建议</p>
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

      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <HardDrive className="w-5 h-5 text-cyan-400" />
          选择盘符
        </h2>

        {loadingDrives ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="w-6 h-6 text-cyan-400 animate-spin" />
            <span className="text-gray-400 ml-2">加载盘符信息...</span>
          </div>
        ) : drives.length === 0 ? (
          <div className="text-center py-8">
            <HardDrive className="w-12 h-12 text-gray-600 mx-auto mb-3" />
            <p className="text-gray-400">未检测到可用盘符</p>
          </div>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-5">
              {drives.map(drive => (
                <DriveCard
                  key={drive.driveLetter}
                  drive={drive}
                  selected={selectedDrive === drive.driveLetter}
                  onClick={() => setSelectedDrive(drive.driveLetter)}
                />
              ))}
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <button
                onClick={handleAnalyze}
                disabled={!selectedDrive || analyzing || aiAnalyzing}
                className="btn-primary flex items-center justify-center gap-2"
              >
                {analyzing ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    分析中...
                  </>
                ) : (
                  <>
                    <BarChart3 className="w-5 h-5" />
                    开始分析 {selectedDrive}: 盘
                  </>
                )}
              </button>
              <button
                onClick={handleAIAnalyze}
                disabled={!selectedDrive || !analysis || analyzing || aiAnalyzing}
                className="flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-semibold transition-all duration-300 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white disabled:opacity-40 disabled:cursor-not-allowed shadow-lg shadow-purple-500/20 hover:shadow-purple-500/40 disabled:shadow-none"
              >
                {aiAnalyzing ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    AI分析中...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    AI智能分析
                  </>
                )}
              </button>
              {analysis && (
                <button
                  onClick={handleAnalyze}
                  disabled={analyzing || aiAnalyzing}
                  className="btn-secondary flex items-center gap-2"
                >
                  <RefreshCw className="w-4 h-4" />
                  重新分析
                </button>
              )}
            </div>
          </>
        )}
      </div>

      {analyzing && (
        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-600/20 flex items-center justify-center mx-auto mb-4">
            <Loader2 className="w-10 h-10 text-cyan-400 animate-spin" />
          </div>
          <h3 className="text-xl font-semibold text-white mb-2">正在分析 {selectedDrive}: 盘</h3>
          <p className="text-gray-400 text-sm">扫描文件并统计占用情况，请耐心等待...</p>
          <div className="mt-4 max-w-xs mx-auto">
            <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full rounded-full bg-gradient-to-r from-cyan-500 to-blue-600 animate-shimmer" style={{ width: '60%' }} />
            </div>
          </div>
        </div>
      )}

      {analysis && !analyzing && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">总空间</p>
              <p className="text-white font-bold text-lg">{formatSize(analysis.driveInfo.totalSpace)}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">已使用</p>
              <p className="text-cyan-400 font-bold text-lg">{formatSize(analysis.driveInfo.usedSpace)}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">扫描文件数</p>
              <p className="text-amber-400 font-bold text-lg">{analysis.totalScannedFiles.toLocaleString()}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">扫描耗时</p>
              <p className="text-purple-400 font-bold text-lg">{(analysis.scanDurationMs / 1000).toFixed(1)}s</p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2">
              <FileTypeStatsPanel stats={analysis.fileTypeStats} />
            </div>
            <div>
              <div className="glass-card rounded-3xl p-6 h-full">
                <h2 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
                  <HardDrive className="w-5 h-5 text-cyan-400" />
                  空间概览
                </h2>
                <div className="space-y-4">
                  <div>
                    <div className="flex justify-between text-sm mb-1.5">
                      <span className="text-gray-400">使用率</span>
                      <span className="text-white font-bold">
                        {analysis.driveInfo.totalSpace > 0
                          ? ((analysis.driveInfo.usedSpace / analysis.driveInfo.totalSpace) * 100).toFixed(1)
                          : 0}%
                      </span>
                    </div>
                    <div className="w-full h-4 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all duration-700 ${
                          analysis.driveInfo.usedSpace / analysis.driveInfo.totalSpace > 0.9
                            ? 'bg-gradient-to-r from-red-500 to-red-600'
                            : 'bg-gradient-to-r from-cyan-500 to-blue-600'
                        }`}
                        style={{
                          width: `${Math.min(
                            (analysis.driveInfo.usedSpace / analysis.driveInfo.totalSpace) * 100,
                            100
                          )}%`,
                        }}
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-400 text-xs">已用</p>
                      <p className="text-white font-bold">{formatSize(analysis.driveInfo.usedSpace)}</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-400 text-xs">可用</p>
                      <p className="text-emerald-400 font-bold">{formatSize(analysis.driveInfo.freeSpace)}</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-400 text-xs">已扫描</p>
                      <p className="text-cyan-400 font-bold">{formatSize(analysis.totalScannedSize)}</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-400 text-xs">文件数</p>
                      <p className="text-amber-400 font-bold">{analysis.totalScannedFiles.toLocaleString()}</p>
                    </div>
                  </div>
                  <div className="pt-2 border-t border-white/10">
                    <div className="flex items-center gap-2 text-xs text-gray-400">
                      <FileText className="w-3.5 h-3.5" />
                      <span>扫描深度: 5 层 | 类型分类: {analysis.fileTypeStats.length} 种</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <TopFoldersPanel folders={analysis.topFolders} />

          <CleanupSuggestionsPanel suggestions={analysis.cleanupSuggestions} />
        </>
      )}

      {aiAnalyzing && (
        <div className="glass-card rounded-3xl p-12 text-center border border-purple-500/30 bg-gradient-to-br from-purple-500/10 to-pink-500/10">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center mx-auto mb-4">
            <Brain className="w-10 h-10 text-purple-400 animate-pulse" />
          </div>
          <h3 className="text-xl font-semibold text-white mb-2">AI 正在智能分析 {selectedDrive}: 盘</h3>
          <p className="text-gray-400 text-sm">正在将磁盘结构数据发送给 AI 进行智能分析，请稍候...</p>
          <div className="mt-4 max-w-xs mx-auto">
            <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full rounded-full bg-gradient-to-r from-purple-500 to-pink-500 animate-shimmer" style={{ width: '70%' }} />
            </div>
          </div>
        </div>
      )}

      {aiAnalysis && !aiAnalyzing && showAiAnalysis && (
        <AIAnalysisPanel aiResult={aiAnalysis} />
      )}

      {!analysis && !analyzing && !aiAnalyzing && !error && (
        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/10 to-blue-600/10 flex items-center justify-center mx-auto mb-4">
            <HardDrive className="w-10 h-10 text-cyan-400" />
          </div>
          <h3 className="text-xl font-semibold text-white mb-2">选择盘符开始分析</h3>
          <p className="text-gray-400 text-sm max-w-md mx-auto">
            选择要分析的盘符，点击"开始分析"按钮，系统将扫描磁盘文件并按类型统计占用空间
          </p>
        </div>
      )}
    </div>
  );
}
