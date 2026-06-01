import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Play,
  Sparkles,
  Copy,
  Check,
  RotateCcw,
  AlertCircle,
  Info,
  ChevronDown,
  Wrench,
  CheckCircle2,
  XCircle,
  Zap,
} from 'lucide-react';
import { toolsApi } from '@/api';
import { RegexValidateResult, RegexGenerateResult, RegexFixResult, RegexAlternative } from '@/types';

export default function RegexTester() {
  const navigate = useNavigate();

  const [pattern, setPattern] = useState<string>('');
  const [testString, setTestString] = useState<string>('');
  const [flags, setFlags] = useState<string>('g');
  const [activeTab, setActiveTab] = useState<'validate' | 'generate' | 'fix'>('validate');

  const [validateResult, setValidateResult] = useState<RegexValidateResult | null>(null);
  const [generateResult, setGenerateResult] = useState<RegexGenerateResult | null>(null);
  const [fixResult, setFixResult] = useState<RegexFixResult | null>(null);

  const [generateDescription, setGenerateDescription] = useState<string>('');
  const [generateCategory, setGenerateCategory] = useState<string>('');
  const [fixIntent, setFixIntent] = useState<string>('');

  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);
  const [copiedAlt, setCopiedAlt] = useState<number | null>(null);

  const flagOptions = [
    { key: 'g', label: '全局 (g)', desc: '匹配所有' },
    { key: 'i', label: '忽略大小写 (i)', desc: '不区分大小写' },
    { key: 'm', label: '多行 (m)', desc: '^$匹配行首行尾' },
    { key: 's', label: '点号匹配换行 (s)', desc: '.匹配\\n' },
  ];

  const categories = ['邮箱', '手机号', 'URL', 'IP地址', '日期', '数字', '中文', '密码', '身份证', '自定义'];

  const generateExamples = [
    '匹配中国大陆手机号',
    '匹配邮箱地址',
    '匹配URL链接',
    '提取HTML标签中的内容',
    '匹配IPv4地址',
    '验证6-20位密码（含大小写字母和数字）',
    '匹配YYYY-MM-DD日期格式',
    '匹配中文字符',
  ];

  const convertFlagsToNumber = (flagStr: string): number => {
    let value = 0;
    if (flagStr.includes('i')) value |= 2; // Pattern.CASE_INSENSITIVE
    if (flagStr.includes('m')) value |= 8; // Pattern.MULTILINE
    if (flagStr.includes('s')) value |= 32; // Pattern.DOTALL
    return value;
  };

  const handleValidate = async () => {
    if (!pattern.trim()) {
      setError('请输入正则表达式');
      return;
    }

    setLoading(true);
    setError('');
    setValidateResult(null);
    setGenerateResult(null);
    setFixResult(null);

    try {
      const res = await toolsApi.validateRegex({
        pattern: pattern.trim(),
        testString: testString,
        flags: convertFlagsToNumber(flags),
      });
      if (res.code === 200 && res.data) {
        setValidateResult(res.data);
      } else {
        setError(res.message || '校验失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '校验失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async () => {
    if (!generateDescription.trim()) {
      setError('请输入正则表达式描述');
      return;
    }

    setLoading(true);
    setError('');
    setValidateResult(null);
    setGenerateResult(null);
    setFixResult(null);

    try {
      const res = await toolsApi.generateRegex({
        description: generateDescription.trim(),
        testString: testString || undefined,
        category: generateCategory || undefined,
      });
      if (res.code === 200 && res.data) {
        setGenerateResult(res.data);
        if (res.data.pattern) {
          setPattern(res.data.pattern);
        }
      } else {
        setError(res.message || 'AI生成失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI生成失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleFix = async () => {
    if (!pattern.trim()) {
      setError('请输入需要修正的正则表达式');
      return;
    }

    setLoading(true);
    setError('');
    setValidateResult(null);
    setGenerateResult(null);
    setFixResult(null);

    try {
      const res = await toolsApi.fixRegex({
        pattern: pattern.trim(),
        testString: testString || undefined,
        intent: fixIntent || undefined,
        errorMessage: validateResult?.errors?.[0] || undefined,
      });
      if (res.code === 200 && res.data) {
        setFixResult(res.data);
      } else {
        setError(res.message || 'AI修正失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI修正失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleApplyFix = () => {
    if (fixResult?.fixedPattern) {
      setPattern(fixResult.fixedPattern);
      setFixResult(null);
      setActiveTab('validate');
    }
  };

  const handleApplyAlternative = (alt: RegexAlternative) => {
    setPattern(alt.pattern);
    setGenerateResult(null);
    setActiveTab('validate');
  };

  const handleCopy = async (text: string, altIndex?: number) => {
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      if (altIndex !== undefined) {
        setCopiedAlt(altIndex);
        setTimeout(() => setCopiedAlt(null), 2000);
      } else {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }
    } catch {
      setError('复制失败，请手动复制');
    }
  };

  const highlightMatches = () => {
    if (!validateResult?.matches?.length || !testString) return testString;

    const parts: { text: string; isMatch: boolean; index: number }[] = [];
    let lastIndex = 0;

    const sortedMatches = [...validateResult.matches].sort((a, b) => a.startIndex - b.startIndex);

    for (const match of sortedMatches) {
      if (match.startIndex > lastIndex) {
        parts.push({ text: testString.substring(lastIndex, match.startIndex), isMatch: false, index: lastIndex });
      }
      parts.push({ text: match.matchedText, isMatch: true, index: match.startIndex });
      lastIndex = match.endIndex;
    }

    if (lastIndex < testString.length) {
      parts.push({ text: testString.substring(lastIndex), isMatch: false, index: lastIndex });
    }

    return parts;
  };

  const matchParts = highlightMatches();

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
          <h1 className="text-3xl font-bold text-white mb-1">正则表达式校验工具</h1>
          <p className="text-gray-400">校验、生成、AI修正正则表达式，一键调试匹配结果</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <button
                onClick={() => setActiveTab('validate')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  activeTab === 'validate'
                    ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/20'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                校验测试
              </button>
              <button
                onClick={() => setActiveTab('generate')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  activeTab === 'generate'
                    ? 'bg-gradient-to-r from-purple-500 to-pink-500 text-white shadow-lg shadow-purple-500/20'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                AI 生成
              </button>
              <button
                onClick={() => setActiveTab('fix')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  activeTab === 'fix'
                    ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-lg shadow-amber-500/20'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                AI 修正
              </button>
            </div>

            {activeTab === 'validate' && (
              <div className="space-y-6 animate-fadeIn">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    正则表达式
                  </label>
                  <div className="flex gap-2">
                    <span className="flex items-center px-3 bg-cyan-500/20 text-cyan-400 rounded-l-2xl font-mono text-sm">/</span>
                    <input
                      type="text"
                      value={pattern}
                      onChange={(e) => setPattern(e.target.value)}
                      placeholder="输入正则表达式"
                      className="flex-1 bg-black/30 border border-white/10 border-l-0 px-0 py-3 text-gray-200 font-mono text-sm focus:outline-none focus:border-cyan-500/50 placeholder:text-gray-500"
                      spellCheck={false}
                    />
                    <span className="flex items-center px-3 bg-cyan-500/20 text-cyan-400 border border-white/10 border-l-0 font-mono text-sm">/{flags}</span>
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    修饰符（Flags）
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {flagOptions.map((flag) => (
                      <button
                        key={flag.key}
                        onClick={() => setFlags(prev => prev.includes(flag.key) ? prev.replace(flag.key, '') : prev + flag.key)}
                        className={`px-3 py-1.5 text-xs rounded-lg transition-all border ${
                          flags.includes(flag.key)
                            ? 'bg-gradient-to-r from-cyan-500/30 to-blue-600/30 text-cyan-300 border-cyan-500/30'
                            : 'bg-white/5 text-gray-400 hover:bg-white/10 border-white/5'
                        }`}
                        title={flag.desc}
                      >
                        {flag.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    测试字符串
                  </label>
                  <textarea
                    value={testString}
                    onChange={(e) => setTestString(e.target.value)}
                    placeholder="输入需要测试的字符串"
                    className="w-full h-32 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none focus:border-cyan-500/50 focus:ring-2 focus:ring-cyan-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                </div>

                <button
                  onClick={handleValidate}
                  disabled={loading || !pattern.trim()}
                  className="btn-primary w-full flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <>
                      <RotateCcw className="w-5 h-5 animate-spin" />
                      校验中...
                    </>
                  ) : (
                    <>
                      <Play className="w-5 h-5" />
                      校验并测试
                    </>
                  )}
                </button>
              </div>
            )}

            {activeTab === 'generate' && (
              <div className="space-y-6 animate-fadeIn">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    用自然语言描述
                  </label>
                  <textarea
                    value={generateDescription}
                    onChange={(e) => setGenerateDescription(e.target.value)}
                    placeholder="描述你需要的正则表达式，如：匹配中国大陆手机号"
                    className="w-full h-32 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 text-sm resize-none focus:outline-none focus:border-purple-500/50 focus:ring-2 focus:ring-purple-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    分类（可选）
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {categories.map((cat) => (
                      <button
                        key={cat}
                        onClick={() => setGenerateCategory(generateCategory === cat ? '' : cat)}
                        className={`px-3 py-1.5 text-xs rounded-lg transition-all border ${
                          generateCategory === cat
                            ? 'bg-gradient-to-r from-purple-500/30 to-pink-500/30 text-purple-300 border-purple-500/30'
                            : 'bg-white/5 text-gray-400 hover:bg-white/10 border-white/5'
                        }`}
                      >
                        {cat}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-medium text-gray-300 mb-2">示例提示</h3>
                  <div className="flex flex-wrap gap-2">
                    {generateExamples.map((example) => (
                      <button
                        key={example}
                        onClick={() => setGenerateDescription(example)}
                        className="px-3 py-1.5 text-xs bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 hover:text-gray-300 transition-colors"
                      >
                        {example}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  onClick={handleGenerate}
                  disabled={loading || !generateDescription.trim()}
                  className="w-full py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-purple-500/20"
                >
                  {loading ? (
                    <>
                      <RotateCcw className="w-5 h-5 animate-spin" />
                      AI 生成中...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-5 h-5" />
                      AI 生成正则
                    </>
                  )}
                </button>
              </div>
            )}

            {activeTab === 'fix' && (
              <div className="space-y-6 animate-fadeIn">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    需要修正的正则表达式
                  </label>
                  <input
                    type="text"
                    value={pattern}
                    onChange={(e) => setPattern(e.target.value)}
                    placeholder="输入有问题的正则表达式"
                    className="w-full bg-black/30 border border-white/10 rounded-2xl px-4 py-3 text-gray-200 font-mono text-sm focus:outline-none focus:border-amber-500/50 focus:ring-2 focus:ring-amber-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    修正意图（可选）
                  </label>
                  <input
                    type="text"
                    value={fixIntent}
                    onChange={(e) => setFixIntent(e.target.value)}
                    placeholder="描述你希望正则达到什么效果"
                    className="w-full bg-black/30 border border-white/10 rounded-2xl px-4 py-3 text-gray-200 text-sm focus:outline-none focus:border-amber-500/50 focus:ring-2 focus:ring-amber-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    测试字符串（可选）
                  </label>
                  <textarea
                    value={testString}
                    onChange={(e) => setTestString(e.target.value)}
                    placeholder="提供期望匹配的字符串，帮助AI更准确地修正"
                    className="w-full h-24 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none focus:border-amber-500/50 focus:ring-2 focus:ring-amber-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                </div>

                <button
                  onClick={handleFix}
                  disabled={loading || !pattern.trim()}
                  className="w-full py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-amber-500/20"
                >
                  {loading ? (
                    <>
                      <RotateCcw className="w-5 h-5 animate-spin" />
                      AI 修正中...
                    </>
                  ) : (
                    <>
                      <Wrench className="w-5 h-5" />
                      AI 修正正则
                    </>
                  )}
                </button>
              </div>
            )}
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Info className="w-5 h-5 text-purple-400" />
              使用说明
            </h2>
            <div className="space-y-3 text-sm text-gray-400">
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-cyan-500/20 text-cyan-400 rounded-lg text-xs font-medium shrink-0">校验测试</span>
                <p>输入正则和测试字符串，实时查看匹配结果、捕获组和高亮</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-purple-500/20 text-purple-400 rounded-lg text-xs font-medium shrink-0">AI 生成</span>
                <p>用自然语言描述匹配规则，AI自动生成正则。常见模式（手机号、邮箱等）优先使用经过验证的标准知识库</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-amber-500/20 text-amber-400 rounded-lg text-xs font-medium shrink-0">AI 修正</span>
                <p>输入有问题的正则，AI分析错误原因并自动修正，提供修复说明</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-emerald-500/20 text-emerald-400 rounded-lg text-xs font-medium shrink-0">标准模式</span>
                <p>支持台湾手机号(09开头10位)、香港手机号(5/6/9开头8位)、大陆手机号、邮箱、URL等13种常用模式</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-green-500/20 text-green-400 rounded-lg text-xs font-medium shrink-0">修饰符</span>
                <p>g=全局匹配 i=忽略大小写 m=多行模式 s=点号匹配换行</p>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          {error && (
            <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
              <div className="flex items-center gap-3">
                <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
                <p className="text-red-300">{error}</p>
              </div>
            </div>
          )}

          {activeTab === 'validate' && validateResult && (
            <div className="space-y-6 animate-fadeIn">
              <div className="glass-card rounded-3xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    {validateResult.valid ? (
                      <CheckCircle2 className="w-5 h-5 text-green-400" />
                    ) : (
                      <XCircle className="w-5 h-5 text-red-400" />
                    )}
                    校验结果
                  </h2>
                  <button
                    onClick={() => handleCopy(pattern)}
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
                </div>

                <div className="bg-black/30 border border-white/10 rounded-2xl p-4 mb-4">
                  <p className="text-xs text-gray-500 mb-1">正则表达式</p>
                  <p className="text-lg font-mono font-bold text-cyan-400 break-all">/{pattern}/{flags}</p>
                </div>

                {validateResult.description && (
                  <p className="text-gray-300 text-sm mb-4">{validateResult.description}</p>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div className="bg-white/5 rounded-xl p-4">
                    <p className="text-gray-400 text-sm mb-1">语法状态</p>
                    <p className={`font-bold ${validateResult.valid ? 'text-green-400' : 'text-red-400'}`}>
                      {validateResult.valid ? '有效' : '无效'}
                    </p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-4">
                    <p className="text-gray-400 text-sm mb-1">匹配数量</p>
                    <p className="text-white font-bold">{validateResult.matchCount}</p>
                  </div>
                </div>
              </div>

              {validateResult.errors && validateResult.errors.length > 0 && (
                <div className="glass-card rounded-3xl p-5 border-red-500/30 bg-red-500/5">
                  <div className="flex items-start gap-3">
                    <XCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                    <div className="space-y-2">
                      <h3 className="text-red-400 font-semibold">错误信息</h3>
                      {validateResult.errors.map((err, idx) => (
                        <p key={idx} className="text-red-200/90 text-sm leading-relaxed break-all">{err}</p>
                      ))}
                      <button
                        onClick={() => { setActiveTab('fix'); }}
                        className="mt-2 px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-lg text-sm font-medium hover:opacity-90 transition-all flex items-center gap-1"
                      >
                        <Zap className="w-4 h-4" />
                        使用AI修正
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {testString && validateResult.valid && (
                <div className="glass-card rounded-3xl p-6">
                  <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                    <ChevronDown className="w-5 h-5 text-cyan-400" />
                    匹配结果高亮
                  </h2>
                  <div className="bg-black/30 border border-white/10 rounded-2xl p-4 font-mono text-sm leading-relaxed break-all whitespace-pre-wrap">
                    {typeof matchParts === 'string' ? (
                      <span className="text-gray-300">{matchParts}</span>
                    ) : (
                      matchParts.map((part, idx) =>
                        part.isMatch ? (
                          <span key={idx} className="bg-cyan-500/30 text-cyan-300 rounded px-0.5">
                            {part.text}
                          </span>
                        ) : (
                          <span key={idx} className="text-gray-300">{part.text}</span>
                        )
                      )
                    )}
                  </div>
                </div>
              )}

              {validateResult.matches && validateResult.matches.length > 0 && (
                <div className="glass-card rounded-3xl p-6">
                  <h2 className="text-lg font-bold text-white mb-4">匹配详情</h2>
                  <div className="space-y-3">
                    {validateResult.matches.map((match, idx) => (
                      <div key={idx} className="bg-white/5 rounded-xl p-4">
                        <div className="flex items-center gap-3 mb-2">
                          <span className="w-6 h-6 rounded-full bg-cyan-500/20 text-cyan-400 text-xs flex items-center justify-center font-medium">
                            {idx + 1}
                          </span>
                          <span className="text-cyan-300 font-mono text-sm break-all">{match.matchedText}</span>
                        </div>
                        <div className="flex gap-4 text-xs text-gray-500">
                          <span>位置: {match.startIndex}-{match.endIndex}</span>
                          <span>长度: {match.matchedText.length}</span>
                        </div>
                        {match.groups && match.groups.length > 1 && (
                          <div className="mt-2 flex flex-wrap gap-2">
                            {match.groups.slice(1).map((group, gIdx) => (
                              <span key={gIdx} className="px-2 py-1 bg-purple-500/10 text-purple-300 rounded text-xs font-mono break-all">
                                ${gIdx + 1}: {group || '(空)'}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {validateResult.groups && validateResult.groups.length > 0 && (
                <div className="glass-card rounded-3xl p-6">
                  <h2 className="text-lg font-bold text-white mb-4">命名捕获组</h2>
                  <div className="flex flex-wrap gap-2">
                    {validateResult.groups.map((group, idx) => (
                      <span key={idx} className="px-3 py-1.5 bg-green-500/10 text-green-300 rounded-lg text-xs font-mono">
                        &lt;{group}&gt;
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'generate' && generateResult && (
            <div className="space-y-6 animate-fadeIn">
              <div className="glass-card rounded-3xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Sparkles className="w-5 h-5 text-purple-400" />
                    AI 生成结果
                  </h2>
                  <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                    generateResult.confidence >= 0.8
                      ? 'bg-green-500/20 text-green-400'
                      : generateResult.confidence >= 0.5
                        ? 'bg-yellow-500/20 text-yellow-400'
                        : 'bg-red-500/20 text-red-400'
                  }`}>
                    置信度 {Math.round((generateResult.confidence || 0) * 100)}%
                  </span>
                </div>

                <div className="bg-black/30 border border-white/10 rounded-2xl p-4 mb-4">
                  <p className="text-xs text-gray-500 mb-1">生成的正则表达式</p>
                  <div className="flex items-center gap-2">
                    <p className="text-xl font-mono font-bold text-purple-400 break-all flex-1">{generateResult.pattern}</p>
                    <button
                      onClick={() => handleCopy(generateResult.pattern)}
                      className="p-2 bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors shrink-0"
                    >
                      {copied ? <Check className="w-4 h-4 text-green-400" /> : <Copy className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {generateResult.description && (
                  <p className="text-gray-300 text-sm mb-3">{generateResult.description}</p>
                )}

                {generateResult.explanation && (
                  <div className="bg-white/5 rounded-xl p-4 mb-4">
                    <p className="text-xs text-gray-500 mb-1">解释</p>
                    <p className="text-gray-300 text-sm leading-relaxed">{generateResult.explanation}</p>
                  </div>
                )}

                {generateResult.fallback && (
                  <div className="p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-xl mb-4">
                    <p className="text-yellow-200/80 text-sm">使用了规则匹配回退方案，AI 未直接生成</p>
                  </div>
                )}

                {generateResult.aiModel && (
                  <p className="text-xs text-gray-500 mb-4 flex items-center gap-1">
                    来源: {generateResult.aiModel === 'knowledge-base' ? (
                      <span className="px-1.5 py-0.5 bg-emerald-500/20 text-emerald-400 rounded text-xs font-medium">标准知识库 ✓</span>
                    ) : generateResult.aiModel === 'fallback-rules' ? (
                      <span className="px-1.5 py-0.5 bg-amber-500/20 text-amber-400 rounded text-xs font-medium">本地规则库</span>
                    ) : (
                      <span className="text-purple-400">{generateResult.aiModel}</span>
                    )}
                  </p>
                )}

                {generateResult.testCases && generateResult.testCases.length > 0 && (
                  <div className="mb-4">
                    <p className="text-sm font-medium text-gray-300 mb-2">测试用例</p>
                    <div className="flex flex-wrap gap-2">
                      {generateResult.testCases.map((tc, idx) => (
                        <span key={idx} className="px-3 py-1.5 bg-white/5 text-gray-300 rounded-lg text-xs font-mono">
                          {tc}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <button
                  onClick={() => { setActiveTab('validate'); setGenerateResult(null); }}
                  className="w-full py-2.5 bg-gradient-to-r from-cyan-500 to-blue-600 text-white rounded-xl font-medium hover:opacity-90 transition-all flex items-center justify-center gap-2 shadow-lg shadow-cyan-500/20"
                >
                  <Play className="w-5 h-5" />
                  应用到校验测试
                </button>
              </div>

              {generateResult.alternatives && generateResult.alternatives.length > 0 && (
                <div className="glass-card rounded-3xl p-6">
                  <h2 className="text-lg font-bold text-white mb-4">替代方案</h2>
                  <div className="space-y-3">
                    {generateResult.alternatives.map((alt, idx) => (
                      <div key={idx} className="bg-white/5 rounded-xl p-4">
                        <div className="flex items-center justify-between mb-2">
                          <p className="text-purple-300 font-mono text-sm break-all flex-1">{alt.pattern}</p>
                          <div className="flex items-center gap-1 ml-2 shrink-0">
                            <button
                              onClick={() => handleCopy(alt.pattern, idx)}
                              className="p-1.5 bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors"
                            >
                              {copiedAlt === idx ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
                            </button>
                            <button
                              onClick={() => handleApplyAlternative(alt)}
                              className="px-2 py-1 bg-cyan-500/20 text-cyan-400 rounded-lg text-xs hover:bg-cyan-500/30 transition-colors"
                            >
                              应用
                            </button>
                          </div>
                        </div>
                        <p className="text-gray-400 text-xs">{alt.description}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {generateResult.errors && generateResult.errors.length > 0 && (
                <div className="glass-card rounded-3xl p-5 border-red-500/30 bg-red-500/5">
                  <div className="flex items-start gap-3">
                    <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                    <div className="space-y-2">
                      {generateResult.errors.map((err, idx) => (
                        <p key={idx} className="text-red-300 text-sm">{err}</p>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'fix' && fixResult && (
            <div className="space-y-6 animate-fadeIn">
              <div className="glass-card rounded-3xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Wrench className="w-5 h-5 text-amber-400" />
                    AI 修正结果
                  </h2>
                  {fixResult.valid !== undefined && (
                    <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                      fixResult.valid
                        ? 'bg-green-500/20 text-green-400'
                        : 'bg-red-500/20 text-red-400'
                    }`}>
                      {fixResult.valid ? '修正成功' : '修正可能不完整'}
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-1 gap-4 mb-4">
                  <div className="bg-red-500/5 border border-red-500/20 rounded-xl p-4">
                    <p className="text-xs text-red-400 mb-1">原始正则</p>
                    <p className="text-gray-300 font-mono text-sm break-all">{fixResult.originalPattern}</p>
                  </div>
                  <div className="bg-green-500/5 border border-green-500/20 rounded-xl p-4">
                    <p className="text-xs text-green-400 mb-1">修正后正则</p>
                    <div className="flex items-center gap-2">
                      <p className="text-green-300 font-mono text-sm break-all flex-1">{fixResult.fixedPattern}</p>
                      <button
                        onClick={() => handleCopy(fixResult.fixedPattern)}
                        className="p-2 bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors shrink-0"
                      >
                        {copied ? <Check className="w-4 h-4 text-green-400" /> : <Copy className="w-4 h-4" />}
                      </button>
                    </div>
                  </div>
                </div>

                {fixResult.fixDescription && (
                  <div className="bg-amber-500/10 border border-amber-500/20 rounded-xl p-4 mb-4">
                    <p className="text-xs text-amber-400 mb-1">修正说明</p>
                    <p className="text-amber-200/90 text-sm leading-relaxed">{fixResult.fixDescription}</p>
                  </div>
                )}

                {fixResult.explanation && (
                  <div className="bg-white/5 rounded-xl p-4 mb-4">
                    <p className="text-xs text-gray-500 mb-1">正则解释</p>
                    <p className="text-gray-300 text-sm leading-relaxed">{fixResult.explanation}</p>
                  </div>
                )}

                {fixResult.fallback && (
                  <div className="p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-xl mb-4">
                    <p className="text-yellow-200/80 text-sm">使用了本地规则回退方案，建议配置AI服务获得更精确的修复</p>
                  </div>
                )}

                {fixResult.suggestions && fixResult.suggestions.length > 0 && (
                  <div className="mb-4">
                    <p className="text-sm font-medium text-gray-300 mb-2">使用建议</p>
                    <div className="space-y-2">
                      {fixResult.suggestions.map((s, idx) => (
                        <div key={idx} className="flex items-start gap-2">
                          <span className="w-5 h-5 rounded-full bg-blue-500/20 text-blue-400 text-xs flex items-center justify-center shrink-0 mt-0.5">
                            {idx + 1}
                          </span>
                          <p className="text-gray-300 text-sm">{s}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {fixResult.testMatches && fixResult.testMatches.length > 0 && (
                  <div className="mb-4">
                    <p className="text-sm font-medium text-gray-300 mb-2">测试匹配结果</p>
                    <div className="space-y-2">
                      {fixResult.testMatches.map((match, idx) => (
                        <div key={idx} className="p-2 bg-white/5 rounded-lg flex items-center gap-2">
                          <span className="text-cyan-400 font-mono text-xs">{match.matchedText}</span>
                          <span className="text-gray-500 text-xs">位置: {match.startIndex}-{match.endIndex}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <button
                  onClick={handleApplyFix}
                  disabled={!fixResult.valid}
                  className="w-full py-2.5 bg-gradient-to-r from-cyan-500 to-blue-600 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-cyan-500/20"
                >
                  <Play className="w-5 h-5" />
                  应用修正结果到校验测试
                </button>
              </div>

              {fixResult.errors && fixResult.errors.length > 0 && (
                <div className="glass-card rounded-3xl p-5 border-red-500/30 bg-red-500/5">
                  <div className="flex items-start gap-3">
                    <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                    <div className="space-y-2">
                      {fixResult.errors.map((err, idx) => (
                        <p key={idx} className="text-red-300 text-sm">{err}</p>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {!validateResult && !generateResult && !fixResult && !error && (
            <div className="glass-card rounded-3xl p-8 text-center">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/10 to-blue-500/10 flex items-center justify-center mx-auto mb-4">
                <Zap className="w-10 h-10 text-cyan-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待操作</h3>
              <p className="text-gray-400 text-sm">
                通过校验测试、AI 生成或 AI 修正来使用正则表达式工具
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
