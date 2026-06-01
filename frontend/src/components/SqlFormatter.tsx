import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Database,
  ArrowLeft,
  Play,
  Minimize2,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  Copy,
  Check,
  RotateCcw,
  Settings2,
  Download,
  Upload,
  Info,
  XCircle,
  Lightbulb,
  Zap
} from 'lucide-react';
import { toolsApi } from '@/api';
import { SqlFormatResult, SqlErrorDetail } from '@/types';

export default function SqlFormatter() {
  const navigate = useNavigate();

  const [inputSql, setInputSql] = useState<string>('');
  const [outputSql, setOutputSql] = useState<string>('');
  const [result, setResult] = useState<SqlFormatResult | null>(null);
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [aiLoading, setAiLoading] = useState<boolean>(false);
  const [optimizeLoading, setOptimizeLoading] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);
  const [indentSize, setIndentSize] = useState<number>(4);
  const [uppercase, setUppercase] = useState<boolean>(true);
  const [dbType, setDbType] = useState<string>('MYSQL');
  const [showSettings, setShowSettings] = useState<boolean>(false);
  const [currentAction, setCurrentAction] = useState<string>('');

  const sampleSql = `SELECT users.id, users.name, users.email, 
COUNT(orders.id) as order_count, SUM(orders.total) as total_spent
FROM users
LEFT JOIN orders ON users.id = orders.user_id
WHERE users.created_at >= '2024-01-01' 
AND orders.status = 'completed'
GROUP BY users.id, users.name, users.email
HAVING COUNT(orders.id) > 5
ORDER BY total_spent DESC
LIMIT 10`;

  const handleFormat = async () => {
    if (!inputSql.trim()) {
      setError('请输入要格式化的SQL内容');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setOutputSql('');
    setCurrentAction('format');

    try {
      const res = await toolsApi.formatSql({
        content: inputSql,
        indentSize,
        uppercase,
        dbType
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputSql(res.data.formattedSql || '');
      } else {
        setResult(res.data);
        setError(res.message || '格式化失败');
      }
    } catch (err: any) {
      const errorData = err.response?.data?.data;
      if (errorData) {
        setResult(errorData);
      }
      setError(err.response?.data?.message || '格式化失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleCompact = async () => {
    if (!inputSql.trim()) {
      setError('请输入要压缩的SQL内容');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setOutputSql('');
    setCurrentAction('compact');

    try {
      const res = await toolsApi.compactSql({
        content: inputSql,
        dbType
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputSql(res.data.compactSql || '');
      } else {
        setResult(res.data);
        setError(res.message || '压缩失败');
      }
    } catch (err: any) {
      const errorData = err.response?.data?.data;
      if (errorData) {
        setResult(errorData);
      }
      setError(err.response?.data?.message || '压缩失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleValidate = async () => {
    if (!inputSql.trim()) {
      setError('请输入要校验的SQL内容');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setOutputSql('');
    setCurrentAction('validate');

    try {
      const res = await toolsApi.validateSql({
        content: inputSql,
        dbType
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
      } else {
        setResult(res.data);
        setError(res.message || '校验失败');
      }
    } catch (err: any) {
      const errorData = err.response?.data?.data;
      if (errorData) {
        setResult(errorData);
      }
      setError(err.response?.data?.message || '校验失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleFixWithAI = async () => {
    if (!inputSql.trim()) {
      setError('请输入要修复的SQL内容');
      return;
    }

    setAiLoading(true);
    setError('');
    setCurrentAction('fix');

    try {
      const res = await toolsApi.fixSqlWithAI({
        content: inputSql,
        dbType
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputSql(res.data.formattedSql || res.data.aiFixedSql || '');
        setInputSql(res.data.formattedSql || res.data.aiFixedSql || inputSql);
      } else {
        setResult(res.data);
        setError(res.message || 'AI修复失败');
      }
    } catch (err: any) {
      const errorData = err.response?.data?.data;
      if (errorData) {
        setResult(errorData);
      }
      setError(err.response?.data?.message || 'AI修复失败，请稍后重试');
    } finally {
      setAiLoading(false);
    }
  };

  const handleOptimizeWithAI = async () => {
    if (!inputSql.trim()) {
      setError('请输入要优化的SQL内容');
      return;
    }

    setOptimizeLoading(true);
    setError('');
    setCurrentAction('optimize');

    try {
      const res = await toolsApi.optimizeSqlWithAI({
        content: inputSql,
        dbType
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputSql(res.data.formattedSql || '');
      } else {
        setResult(res.data);
        setError(res.message || 'AI优化失败');
      }
    } catch (err: any) {
      const errorData = err.response?.data?.data;
      if (errorData) {
        setResult(errorData);
      }
      setError(err.response?.data?.message || 'AI优化失败，请稍后重试');
    } finally {
      setOptimizeLoading(false);
    }
  };

  const handleCopy = async () => {
    if (!outputSql) return;

    try {
      await navigator.clipboard.writeText(outputSql);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      setError('复制失败，请手动复制');
    }
  };

  const handleClear = () => {
    setInputSql('');
    setOutputSql('');
    setResult(null);
    setError('');
  };

  const handleLoadSample = () => {
    setInputSql(sampleSql);
    setOutputSql('');
    setResult(null);
    setError('');
  };

  const handleDownload = () => {
    if (!outputSql) return;

    const blob = new Blob([outputSql], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `formatted-sql-${Date.now()}.sql`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleSyncToInput = () => {
    if (outputSql) {
      setInputSql(outputSql);
    }
  };

  const getErrorTypeColor = (errorType: string) => {
    const colors: Record<string, string> = {
      '语法错误': 'text-red-400',
      '括号不匹配': 'text-orange-400',
      '引号不匹配': 'text-yellow-400',
      '缺少子句': 'text-purple-400',
      'JOIN缺少条件': 'text-pink-400'
    };
    return colors[errorType] || 'text-gray-400';
  };

  const getErrorTypeBgColor = (errorType: string) => {
    const colors: Record<string, string> = {
      '语法错误': 'bg-red-500/20',
      '括号不匹配': 'bg-orange-500/20',
      '引号不匹配': 'bg-yellow-500/20',
      '缺少子句': 'bg-purple-500/20',
      'JOIN缺少条件': 'bg-pink-500/20'
    };
    return colors[errorType] || 'bg-gray-500/20';
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
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-white mb-1">SQL 格式化工具</h1>
          <p className="text-gray-400">格式化、压缩、校验 SQL，支持 AI 智能修复语法错误和性能优化</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Database className="w-5 h-5 text-cyan-400" />
                输入 SQL
              </h2>
              <div className="flex items-center gap-2">
                <button
                  onClick={handleLoadSample}
                  className="px-3 py-1.5 text-sm bg-cyan-500/20 text-cyan-400 rounded-lg hover:bg-cyan-500/30 transition-colors"
                >
                  加载示例
                </button>
                <button
                  onClick={handleClear}
                  className="px-3 py-1.5 text-sm bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors"
                >
                  清空
                </button>
              </div>
            </div>
            <textarea
              value={inputSql}
              onChange={(e) => setInputSql(e.target.value)}
              placeholder="在此粘贴或输入 SQL 语句..."
              className="w-full h-64 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none focus:border-cyan-500/50 focus:ring-2 focus:ring-cyan-500/20 transition-all placeholder:text-gray-500"
              spellCheck={false}
            />
            <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
              <span>字符数: {inputSql.length}</span>
              <span>行数: {inputSql.split('\n').length}</span>
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Play className="w-5 h-5 text-cyan-400" />
                操作
              </h2>
              <button
                onClick={() => setShowSettings(!showSettings)}
                className={`p-2 rounded-xl transition-all ${
                  showSettings ? 'bg-cyan-500/20 text-cyan-400' : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                <Settings2 className="w-5 h-5" />
              </button>
            </div>

            {showSettings && (
              <div className="mb-6 p-4 bg-white/5 rounded-2xl space-y-4 animate-fadeIn">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    数据库类型
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {['MYSQL', 'POSTGRESQL', 'ORACLE', 'SQLSERVER', 'SQLITE'].map((type) => (
                      <button
                        key={type}
                        onClick={() => setDbType(type)}
                        className={`px-3 py-2 rounded-xl text-sm font-medium transition-all ${
                          dbType === type
                            ? 'bg-gradient-to-br from-cyan-500 to-blue-500 text-white'
                            : 'bg-white/5 text-gray-300 hover:bg-white/10'
                        }`}
                      >
                        {type}
                      </button>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    缩进空格数
                  </label>
                  <div className="flex gap-2">
                    {[2, 4, 8].map((size) => (
                      <button
                        key={size}
                        onClick={() => setIndentSize(size)}
                        className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                          indentSize === size
                            ? 'bg-gradient-to-br from-cyan-500 to-blue-500 text-white'
                            : 'bg-white/5 text-gray-300 hover:bg-white/10'
                        }`}
                      >
                        {size} 空格
                      </button>
                    ))}
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium text-gray-300">
                    关键字大写
                  </label>
                  <button
                    onClick={() => setUppercase(!uppercase)}
                    className={`w-12 h-6 rounded-full transition-all ${
                      uppercase ? 'bg-cyan-500' : 'bg-white/20'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 bg-white rounded-full shadow transition-transform ${
                        uppercase ? 'translate-x-6' : 'translate-x-0.5'
                      }`}
                    />
                  </button>
                </div>
              </div>
            )}

            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={handleFormat}
                disabled={loading || aiLoading || optimizeLoading || !inputSql.trim()}
                className="btn-primary flex items-center justify-center gap-2"
              >
                {loading && currentAction === 'format' ? (
                  <>
                    <RotateCcw className="w-5 h-5 animate-spin" />
                    格式化中...
                  </>
                ) : (
                  <>
                    <Play className="w-5 h-5" />
                    格式化
                  </>
                )}
              </button>
              <button
                onClick={handleCompact}
                disabled={loading || aiLoading || optimizeLoading || !inputSql.trim()}
                className="px-4 py-3 bg-white/5 text-white rounded-xl font-medium hover:bg-white/10 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {loading && currentAction === 'compact' ? (
                  <>
                    <RotateCcw className="w-5 h-5 animate-spin" />
                    压缩中...
                  </>
                ) : (
                  <>
                    <Minimize2 className="w-5 h-5" />
                    压缩
                  </>
                )}
              </button>
              <button
                onClick={handleValidate}
                disabled={loading || aiLoading || optimizeLoading || !inputSql.trim()}
                className="px-4 py-3 bg-white/5 text-white rounded-xl font-medium hover:bg-white/10 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {loading && currentAction === 'validate' ? (
                  <>
                    <RotateCcw className="w-5 h-5 animate-spin" />
                    校验中...
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-5 h-5" />
                    校验语法
                  </>
                )}
              </button>
              <button
                onClick={handleFixWithAI}
                disabled={loading || aiLoading || optimizeLoading || !inputSql.trim()}
                className="px-4 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-purple-500/20"
              >
                {aiLoading ? (
                  <>
                    <RotateCcw className="w-5 h-5 animate-spin" />
                    AI 修复中...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    AI 修复
                  </>
                )}
              </button>
              <button
                onClick={handleOptimizeWithAI}
                disabled={loading || aiLoading || optimizeLoading || !inputSql.trim()}
                className="col-span-2 px-4 py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-amber-500/20"
              >
                {optimizeLoading ? (
                  <>
                    <RotateCcw className="w-5 h-5 animate-spin" />
                    AI 性能优化中...
                  </>
                ) : (
                  <>
                    <Zap className="w-5 h-5" />
                    AI 性能优化（含索引建议）
                  </>
                )}
              </button>
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Info className="w-5 h-5 text-cyan-400" />
              使用说明
            </h2>
            <div className="space-y-3 text-sm text-gray-400">
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-cyan-500/20 text-cyan-400 rounded-lg text-xs font-medium shrink-0">格式化</span>
                <p>将 SQL 语句美化成易读的格式，支持自定义缩进和关键字大小写设置</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded-lg text-xs font-medium shrink-0">压缩</span>
                <p>移除 SQL 中的所有空格和换行，减小体积便于传输</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-green-500/20 text-green-400 rounded-lg text-xs font-medium shrink-0">校验</span>
                <p>检查 SQL 语法是否正确，显示详细的错误位置和修复建议</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-pink-500/20 text-pink-400 rounded-lg text-xs font-medium shrink-0">AI修复</span>
                <p>当 SQL 存在语法错误时，使用 AI 大模型尝试自动修复格式问题</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-amber-500/20 text-amber-400 rounded-lg text-xs font-medium shrink-0">AI优化</span>
                <p>使用 AI 分析 SQL 性能瓶颈，提供索引建议和查询优化方案</p>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {error && (
            <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
              <div className="flex items-center gap-3">
                <XCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
                <p className="text-red-300">{error}</p>
              </div>
            </div>
          )}

          {result?.aiFixDescription && (
            <div className={`glass-card rounded-2xl p-4 border ${result.aiFixSuccess !== false ? 'border-purple-500/30 bg-purple-500/10' : 'border-orange-500/30 bg-orange-500/10'}`}>
              <div className="flex items-start gap-3">
                <Sparkles className={`w-5 h-5 flex-shrink-0 mt-0.5 ${result.aiFixSuccess !== false ? 'text-purple-400' : 'text-orange-400'}`} />
                <div>
                  <p className={`font-medium ${result.aiFixSuccess !== false ? 'text-purple-300' : 'text-orange-300'}`}>
                    {result.aiFixSuccess !== false ? 'AI 修复成功' : 'AI 修复结果'}
                  </p>
                  <p className={`text-sm mt-1 ${result.aiFixSuccess !== false ? 'text-purple-200/80' : 'text-orange-200/80'}`}>
                    {result.aiFixDescription}
                  </p>
                </div>
              </div>
            </div>
          )}

          {currentAction === 'validate' && result?.success && (
            <div className="glass-card rounded-2xl p-4 border border-green-500/30 bg-green-500/10">
              <div className="flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-green-400 flex-shrink-0" />
                <p className="text-green-300">SQL 语法校验通过！</p>
              </div>
            </div>
          )}

          {outputSql && (
            <div className="glass-card rounded-3xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <Database className="w-5 h-5 text-green-400" />
                  输出结果
                </h2>
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleSyncToInput}
                    className="px-3 py-1.5 text-sm bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors flex items-center gap-1"
                    title="同步到输入框"
                  >
                    <Upload className="w-4 h-4" />
                    同步
                  </button>
                  <button
                    onClick={handleCopy}
                    className="px-3 py-1.5 text-sm bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors flex items-center gap-1"
                  >
                    {copied ? (
                      <>
                        <Check className="w-4 h-4 text-green-400" />
                        已复制
                      </>
                    ) : (
                      <>
                        <Copy className="w-4 h-4" />
                        复制
                      </>
                    )}
                  </button>
                  <button
                    onClick={handleDownload}
                    className="px-3 py-1.5 text-sm bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors flex items-center gap-1"
                  >
                    <Download className="w-4 h-4" />
                    下载
                  </button>
                </div>
              </div>
              <textarea
                value={outputSql}
                readOnly
                className="w-full h-64 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none"
                spellCheck={false}
              />
              <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
                <span>字符数: {outputSql.length}</span>
                <span>行数: {outputSql.split('\n').length}</span>
              </div>
            </div>
          )}

          {result?.errors && result.errors.length > 0 && (
            <div className="glass-card rounded-3xl p-6">
              <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                <AlertCircle className="w-5 h-5 text-red-400" />
                错误详情
              </h2>
              <div className="space-y-4 max-h-64 overflow-y-auto scrollbar-thin">
                {result.errors.map((err: SqlErrorDetail, index: number) => (
                  <div
                    key={index}
                    className="p-4 bg-white/5 rounded-2xl border border-white/10 hover:border-white/20 transition-colors"
                  >
                    <div className="flex items-center gap-3 mb-3">
                      <span className={`px-2 py-1 rounded-lg text-xs font-medium ${getErrorTypeBgColor(err.errorType)} ${getErrorTypeColor(err.errorType)}`}>
                        {err.errorType}
                      </span>
                      {err.lineNumber && (
                        <span className="text-xs text-gray-500">
                          第 {err.lineNumber} 行，第 {err.columnNumber} 列
                        </span>
                      )}
                    </div>
                    <p className="text-gray-300 text-sm mb-3">{err.message}</p>
                    {err.errorContext && (
                      <div className="p-3 bg-black/30 rounded-xl font-mono text-xs text-gray-400 mb-3 overflow-x-auto">
                        <code>{err.errorContext}</code>
                      </div>
                    )}
                    {err.suggestion && (
                      <div className="flex items-start gap-2 p-3 bg-yellow-500/10 rounded-xl border border-yellow-500/20">
                        <Lightbulb className="w-4 h-4 text-yellow-400 flex-shrink-0 mt-0.5" />
                        <p className="text-yellow-200/80 text-sm">{err.suggestion}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
              <div className="mt-4 pt-4 border-t border-white/10">
                <button
                  onClick={handleFixWithAI}
                  disabled={aiLoading}
                  className="w-full py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-purple-500/20"
                >
                  {aiLoading ? (
                    <>
                      <RotateCcw className="w-5 h-5 animate-spin" />
                      AI 正在修复...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-5 h-5" />
                      使用 AI 修复以上错误
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

          {result?.suggestions && result.suggestions.length > 0 && (currentAction === 'optimize' || currentAction === 'validate') && (
            <div className="glass-card rounded-3xl p-6">
              <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                <Lightbulb className="w-5 h-5 text-amber-400" />
                {currentAction === 'optimize' ? '优化建议' : '性能建议'}
              </h2>
              <div className="space-y-3 max-h-72 overflow-y-auto scrollbar-thin">
                {result.suggestions.map((suggestion: string, index: number) => (
                  <div key={index} className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
                    <span className="flex-shrink-0 w-6 h-6 rounded-full bg-amber-500/20 flex items-center justify-center text-amber-400 text-sm font-medium">
                      {index + 1}
                    </span>
                    <p className="text-gray-300 text-sm">{suggestion}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {(result?.statistics || result?.sqlType) && (result?.success || outputSql) && (
            <div className="glass-card rounded-3xl p-6">
              <h2 className="text-lg font-bold text-white mb-4">数据统计</h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">SQL 类型</p>
                  <p className="text-white font-bold">{result.sqlType || '-'}</p>
                </div>
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">数据库类型</p>
                  <p className="text-white font-bold">{result.dbType || dbType}</p>
                </div>
                <div className="col-span-2 bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">详细信息</p>
                  <p className="text-white font-bold text-sm">{result.statistics || '-'}</p>
                </div>
              </div>
            </div>
          )}

          {!outputSql && !result?.errors?.length && !error && !result?.suggestions?.length && (
            <div className="glass-card rounded-3xl p-8 text-center">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/10 to-blue-500/10 flex items-center justify-center mx-auto mb-4">
                <Database className="w-10 h-10 text-cyan-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待操作</h3>
              <p className="text-gray-400 text-sm">
                输入 SQL 内容后，点击格式化、压缩、校验、AI 修复或 AI 优化按钮
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
