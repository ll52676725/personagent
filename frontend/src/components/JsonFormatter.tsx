import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Code2,
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
  Lightbulb
} from 'lucide-react';
import { toolsApi } from '@/api';
import { JsonFormatResult, JsonErrorDetail } from '@/types';

/**
 * JSON格式化工具组件
 * 提供JSON格式化、压缩、校验和AI修复功能
 * 参考在线JSON格式化工具的交互体验
 */
export default function JsonFormatter() {
  const navigate = useNavigate();

  // 输入的JSON字符串
  const [inputJson, setInputJson] = useState<string>('');
  // 输出的JSON字符串
  const [outputJson, setOutputJson] = useState<string>('');
  // 格式化结果
  const [result, setResult] = useState<JsonFormatResult | null>(null);
  // 错误信息
  const [error, setError] = useState<string>('');
  // 加载状态
  const [loading, setLoading] = useState<boolean>(false);
  // AI修复加载状态
  const [aiLoading, setAiLoading] = useState<boolean>(false);
  // 复制成功状态
  const [copied, setCopied] = useState<boolean>(false);
  // 缩进空格数
  const [indentSize, setIndentSize] = useState<number>(2);
  // 是否对键名排序
  const [sortKeys, setSortKeys] = useState<boolean>(false);
  // 是否显示设置面板
  const [showSettings, setShowSettings] = useState<boolean>(false);
  // 当前操作类型
  const [currentAction, setCurrentAction] = useState<string>('');

  // 示例JSON，用于演示
  const sampleJson = `{
    "name": "张三",
    "age": 25,
    "isStudent": true,
    "hobbies": ["阅读", "编程", "旅行"],
    "address": {
      "city": "北京",
      "district": "朝阳区",
      "street": "某某街道123号"
    }
  }`;

  /**
   * 执行JSON格式化
   * 调用后端API对输入的JSON进行美化格式化
   */
  const handleFormat = async () => {
    if (!inputJson.trim()) {
      setError('请输入要格式化的JSON内容');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setOutputJson('');
    setCurrentAction('format');

    try {
      const res = await toolsApi.formatJson({
        content: inputJson,
        indentSize,
        sortKeys
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputJson(res.data.formattedJson || '');
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

  /**
   * 执行JSON压缩
   * 调用后端API将格式化的JSON压缩为紧凑格式
   */
  const handleCompact = async () => {
    if (!inputJson.trim()) {
      setError('请输入要压缩的JSON内容');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setOutputJson('');
    setCurrentAction('compact');

    try {
      const res = await toolsApi.compactJson({
        content: inputJson
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputJson(res.data.compactJson || '');
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

  /**
   * 执行JSON校验
   * 调用后端API检查JSON语法是否正确
   */
  const handleValidate = async () => {
    if (!inputJson.trim()) {
      setError('请输入要校验的JSON内容');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setOutputJson('');
    setCurrentAction('validate');

    try {
      const res = await toolsApi.validateJson({
        content: inputJson
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

  /**
   * 使用AI修复JSON格式错误
   * 当JSON存在语法错误时，调用LLM尝试自动修复
   */
  const handleFixWithAI = async () => {
    if (!inputJson.trim()) {
      setError('请输入要修复的JSON内容');
      return;
    }

    setAiLoading(true);
    setError('');
    setCurrentAction('fix');

    try {
      const res = await toolsApi.fixJsonWithAI({
        content: inputJson,
        indentSize,
        sortKeys
      });
      if (res.code === 200 && res.data) {
        setResult(res.data);
        setOutputJson(res.data.formattedJson || res.data.aiFixedJson || '');
        // 将修复后的内容同步到输入框
        setInputJson(res.data.formattedJson || res.data.aiFixedJson || inputJson);
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

  /**
   * 复制输出结果到剪贴板
   */
  const handleCopy = async () => {
    if (!outputJson) return;

    try {
      await navigator.clipboard.writeText(outputJson);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      setError('复制失败，请手动复制');
    }
  };

  /**
   * 清空输入和输出
   */
  const handleClear = () => {
    setInputJson('');
    setOutputJson('');
    setResult(null);
    setError('');
  };

  /**
   * 加载示例JSON
   */
  const handleLoadSample = () => {
    setInputJson(sampleJson);
    setOutputJson('');
    setResult(null);
    setError('');
  };

  /**
   * 下载JSON文件
   */
  const handleDownload = () => {
    if (!outputJson) return;

    const blob = new Blob([outputJson], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `formatted-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  /**
   * 将输出内容同步到输入框
   * 方便用户基于格式化后的结果继续编辑
   */
  const handleSyncToInput = () => {
    if (outputJson) {
      setInputJson(outputJson);
    }
  };

  /**
   * 获取错误类型对应的颜色
   */
  const getErrorTypeColor = (errorType: string) => {
    const colors: Record<string, string> = {
      '语法错误': 'text-red-400',
      '无效转义字符': 'text-orange-400',
      '字符串未闭合': 'text-yellow-400',
      'JSON未完整': 'text-purple-400',
      '重复键名': 'text-pink-400',
      '多余逗号': 'text-cyan-400',
      '缺少冒号': 'text-blue-400',
      '编码错误': 'text-indigo-400'
    };
    return colors[errorType] || 'text-gray-400';
  };

  /**
   * 获取错误类型对应的背景色
   */
  const getErrorTypeBgColor = (errorType: string) => {
    const colors: Record<string, string> = {
      '语法错误': 'bg-red-500/20',
      '无效转义字符': 'bg-orange-500/20',
      '字符串未闭合': 'bg-yellow-500/20',
      'JSON未完整': 'bg-purple-500/20',
      '重复键名': 'bg-pink-500/20',
      '多余逗号': 'bg-cyan-500/20',
      '缺少冒号': 'bg-blue-500/20',
      '编码错误': 'bg-indigo-500/20'
    };
    return colors[errorType] || 'bg-gray-500/20';
  };

  return (
    <div className="space-y-8">
      {/* 页面标题 */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/tools')}
          className="p-3 rounded-xl hover:bg-white/5 transition-all"
        >
          <ArrowLeft className="w-5 h-5 text-gray-400" />
        </button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-white mb-1">JSON 格式化工具</h1>
          <p className="text-gray-400">格式化、压缩、校验 JSON，支持 AI 智能修复格式错误</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* 左侧：输入区域 */}
        <div className="space-y-6">
          {/* 输入框 */}
          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Code2 className="w-5 h-5 text-purple-400" />
                输入 JSON
              </h2>
              <div className="flex items-center gap-2">
                <button
                  onClick={handleLoadSample}
                  className="px-3 py-1.5 text-sm bg-purple-500/20 text-purple-400 rounded-lg hover:bg-purple-500/30 transition-colors"
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
              value={inputJson}
              onChange={(e) => setInputJson(e.target.value)}
              placeholder="在此粘贴或输入 JSON 内容..."
              className="w-full h-80 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none focus:border-purple-500/50 focus:ring-2 focus:ring-purple-500/20 transition-all placeholder:text-gray-500"
              spellCheck={false}
            />
            <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
              <span>字符数: {inputJson.length}</span>
              <span>行数: {inputJson.split('\n').length}</span>
            </div>
          </div>

          {/* 操作按钮区域 */}
          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                <Play className="w-5 h-5 text-purple-400" />
                操作
              </h2>
              <button
                onClick={() => setShowSettings(!showSettings)}
                className={`p-2 rounded-xl transition-all ${
                  showSettings ? 'bg-purple-500/20 text-purple-400' : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                <Settings2 className="w-5 h-5" />
              </button>
            </div>

            {/* 设置面板 */}
            {showSettings && (
              <div className="mb-6 p-4 bg-white/5 rounded-2xl space-y-4 animate-fadeIn">
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
                            ? 'bg-gradient-to-br from-purple-500 to-pink-500 text-white'
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
                    对键名进行排序
                  </label>
                  <button
                    onClick={() => setSortKeys(!sortKeys)}
                    className={`w-12 h-6 rounded-full transition-all ${
                      sortKeys ? 'bg-purple-500' : 'bg-white/20'
                    }`}
                  >
                    <div
                      className={`w-5 h-5 bg-white rounded-full shadow transition-transform ${
                        sortKeys ? 'translate-x-6' : 'translate-x-0.5'
                      }`}
                    />
                  </button>
                </div>
              </div>
            )}

            {/* 操作按钮 */}
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={handleFormat}
                disabled={loading || aiLoading || !inputJson.trim()}
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
                disabled={loading || aiLoading || !inputJson.trim()}
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
                disabled={loading || aiLoading || !inputJson.trim()}
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
                disabled={loading || aiLoading || !inputJson.trim()}
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
            </div>
          </div>

          {/* 使用说明 */}
          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Info className="w-5 h-5 text-purple-400" />
              使用说明
            </h2>
            <div className="space-y-3 text-sm text-gray-400">
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-purple-500/20 text-purple-400 rounded-lg text-xs font-medium shrink-0">格式化</span>
                <p>将压缩或混乱的 JSON 美化成易读的格式，可自定义缩进大小和键排序</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded-lg text-xs font-medium shrink-0">压缩</span>
                <p>移除 JSON 中的所有空格和换行，减小体积便于传输</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-green-500/20 text-green-400 rounded-lg text-xs font-medium shrink-0">校验</span>
                <p>检查 JSON 语法是否正确，显示详细的错误位置和修复建议</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-pink-500/20 text-pink-400 rounded-lg text-xs font-medium shrink-0">AI修复</span>
                <p>当 JSON 存在语法错误时，使用 AI 大模型尝试自动修复格式问题</p>
              </div>
            </div>
          </div>
        </div>

        {/* 右侧：结果区域 */}
        <div className="space-y-6">
          {/* 错误提示 */}
          {error && (
            <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
              <div className="flex items-center gap-3">
                <XCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
                <p className="text-red-300">{error}</p>
              </div>
            </div>
          )}

          {/* AI 修复结果提示 */}
          {result?.aiFixDescription && (
            <div className={`glass-card rounded-2xl p-4 border ${result.aiFixSuccess ? 'border-purple-500/30 bg-purple-500/10' : 'border-orange-500/30 bg-orange-500/10'}`}>
              <div className="flex items-start gap-3">
                <Sparkles className={`w-5 h-5 flex-shrink-0 mt-0.5 ${result.aiFixSuccess ? 'text-purple-400' : 'text-orange-400'}`} />
                <div>
                  <p className={`font-medium ${result.aiFixSuccess ? 'text-purple-300' : 'text-orange-300'}`}>
                    {result.aiFixSuccess ? 'AI 修复成功' : 'AI 修复结果'}
                  </p>
                  <p className={`text-sm mt-1 ${result.aiFixSuccess ? 'text-purple-200/80' : 'text-orange-200/80'}`}>
                    {result.aiFixDescription}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* 校验成功提示 */}
          {currentAction === 'validate' && result?.success && (
            <div className="glass-card rounded-2xl p-4 border border-green-500/30 bg-green-500/10">
              <div className="flex items-center gap-3">
                <CheckCircle2 className="w-5 h-5 text-green-400 flex-shrink-0" />
                <p className="text-green-300">JSON 格式正确，语法校验通过！</p>
              </div>
            </div>
          )}

          {/* 输出结果 */}
          {outputJson && (
            <div className="glass-card rounded-3xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <Code2 className="w-5 h-5 text-green-400" />
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
                value={outputJson}
                readOnly
                className="w-full h-80 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none"
                spellCheck={false}
              />
              <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
                <span>字符数: {outputJson.length}</span>
                <span>行数: {outputJson.split('\n').length}</span>
              </div>
            </div>
          )}

          {/* 错误详情列表 */}
          {result?.errors && result.errors.length > 0 && (
            <div className="glass-card rounded-3xl p-6">
              <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                <AlertCircle className="w-5 h-5 text-red-400" />
                错误详情
              </h2>
              <div className="space-y-4 max-h-96 overflow-y-auto scrollbar-thin">
                {result.errors.map((err: JsonErrorDetail, index: number) => (
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

          {/* 统计信息 */}
          {result?.statistics && (result?.success || outputJson) && (
            <div className="glass-card rounded-3xl p-6">
              <h2 className="text-lg font-bold text-white mb-4">数据统计</h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">数据类型</p>
                  <p className="text-white font-bold">{result.jsonType || '-'}</p>
                </div>
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">详细信息</p>
                  <p className="text-white font-bold text-sm">{result.statistics || '-'}</p>
                </div>
              </div>
            </div>
          )}

          {/* 等待操作提示 */}
          {!outputJson && !result?.errors?.length && !error && (
            <div className="glass-card rounded-3xl p-8 text-center">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-purple-500/10 to-pink-500/10 flex items-center justify-center mx-auto mb-4">
                <Code2 className="w-10 h-10 text-purple-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待操作</h3>
              <p className="text-gray-400 text-sm">
                输入 JSON 内容后，点击格式化、压缩、校验或 AI 修复按钮
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
