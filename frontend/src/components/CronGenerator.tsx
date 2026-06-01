import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Clock,
  Play,
  Sparkles,
  Copy,
  Check,
  RotateCcw,
  Info,
  AlertCircle,
  Calendar,
  MessageSquare,
  ChevronDown
} from 'lucide-react';
import { toolsApi } from '@/api';
import { CronGenerateResult, CronParseResult, CronNextTimesResult, CronNLResult } from '@/types';

export default function CronGenerator() {
  const navigate = useNavigate();

  const [second, setSecond] = useState<string>('*');
  const [minute, setMinute] = useState<string>('*');
  const [hour, setHour] = useState<string>('*');
  const [day, setDay] = useState<string>('*');
  const [month, setMonth] = useState<string>('*');
  const [weekDay, setWeekDay] = useState<string>('?');
  const [cronExpression, setCronExpression] = useState<string>('');
  const [generateResult, setGenerateResult] = useState<CronGenerateResult | null>(null);
  const [parseResult, setParseResult] = useState<CronParseResult | null>(null);
  const [nextTimesResult, setNextTimesResult] = useState<CronNextTimesResult | null>(null);
  const [nlResult, setNlResult] = useState<CronNLResult | null>(null);
  const [naturalLanguage, setNaturalLanguage] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [aiLoading, setAiLoading] = useState<boolean>(false);
  const [copied, setCopied] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<'visual' | 'manual' | 'ai'>('visual');
  const [showNextTimes, setShowNextTimes] = useState<boolean>(false);
  const [excludeHoliday, setExcludeHoliday] = useState<boolean>(false);
  const [activePreset, setActivePreset] = useState<string | null>(null);

  const presets = [
    { name: '每秒', second: '*', minute: '*', hour: '*', day: '*', month: '*', weekDay: '?' },
    { name: '每分钟', second: '0', minute: '*', hour: '*', day: '*', month: '*', weekDay: '?' },
    { name: '每小时', second: '0', minute: '0', hour: '*', day: '*', month: '*', weekDay: '?' },
    { name: '每天零点', second: '0', minute: '0', hour: '0', day: '*', month: '*', weekDay: '?' },
    { name: '每天8点', second: '0', minute: '0', hour: '8', day: '*', month: '*', weekDay: '?' },
    { name: '每天12点', second: '0', minute: '0', hour: '12', day: '*', month: '*', weekDay: '?' },
    { name: '每周一零点', second: '0', minute: '0', hour: '0', day: '?', month: '*', weekDay: '1' },
    { name: '每月1号零点', second: '0', minute: '0', hour: '0', day: '1', month: '*', weekDay: '?' },
    { name: '工作日8点', second: '0', minute: '0', hour: '8', day: '?', month: '*', weekDay: '1-5' },
    { name: '每5分钟', second: '0', minute: '0/5', hour: '*', day: '*', month: '*', weekDay: '?' },
    { name: '每30分钟', second: '0', minute: '0/30', hour: '*', day: '*', month: '*', weekDay: '?' },
    { name: '每10秒', second: '0/10', minute: '*', hour: '*', day: '*', month: '*', weekDay: '?' },
  ];

  const cronFields = [
    { key: 'second', label: '秒', placeholder: '0-59', description: '0-59 整数' },
    { key: 'minute', label: '分', placeholder: '0-59', description: '0-59 整数' },
    { key: 'hour', label: '时', placeholder: '0-23', description: '0-23 整数' },
    { key: 'day', label: '日', placeholder: '1-31', description: '1-31 整数' },
    { key: 'month', label: '月', placeholder: '1-12', description: '1-12 整数' },
    { key: 'weekDay', label: '周', placeholder: '1-7', description: '1-7 (1=周一,7=周日)' },
  ];

  const fieldSetters: Record<string, (val: string) => void> = {
    second: setSecond,
    minute: setMinute,
    hour: setHour,
    day: setDay,
    month: setMonth,
    weekDay: setWeekDay,
  };

  const fieldValues: Record<string, string> = {
    second,
    minute,
    hour,
    day,
    month,
    weekDay,
  };

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    setGenerateResult(null);
    setParseResult(null);
    setNextTimesResult(null);
    setNlResult(null);
    setShowNextTimes(false);

    try {
      const res = await toolsApi.generateCron({
        second,
        minute,
        hour,
        day,
        month,
        weekDay
      });
      if (res.code === 200 && res.data) {
        setGenerateResult(res.data);
        setCronExpression(res.data.cronExpression);
      } else {
        setError(res.message || '生成失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '生成失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleParse = async () => {
    if (!cronExpression.trim()) {
      setError('请输入 Cron 表达式');
      return;
    }

    setLoading(true);
    setError('');
    setGenerateResult(null);
    setParseResult(null);
    setNextTimesResult(null);
    setNlResult(null);

    try {
      const res = await toolsApi.parseCron({ cronExpression: cronExpression.trim() });
      if (res.code === 200 && res.data) {
        setParseResult(res.data);
        if (res.data.fields) {
          setSecond(res.data.fields['second'] || second);
          setMinute(res.data.fields['minute'] || minute);
          setHour(res.data.fields['hour'] || hour);
          setDay(res.data.fields['day'] || day);
          setMonth(res.data.fields['month'] || month);
          setWeekDay(res.data.fields['weekDay'] || weekDay);
          setActivePreset(null);
        }
      } else {
        setError(res.message || '解析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '解析失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleNextTimes = async () => {
    if (!cronExpression.trim()) {
      setError('请先输入或生成 Cron 表达式');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const res = await toolsApi.getCronNextTimes({ 
        cronExpression: cronExpression.trim(), 
        count: 5,
        excludeHoliday: excludeHoliday,
        useChinaHoliday: true
      });
      if (res.code === 200 && res.data) {
        setNextTimesResult(res.data);
        setShowNextTimes(true);
      } else {
        setError(res.message || '获取下次执行时间失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '获取下次执行时间失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const handleNaturalLanguage = async () => {
    if (!naturalLanguage.trim()) {
      setError('请输入自然语言描述');
      return;
    }

    setAiLoading(true);
    setError('');
    setNlResult(null);
    setGenerateResult(null);
    setParseResult(null);
    setNextTimesResult(null);
    setShowNextTimes(false);
    setCronExpression('');
    setActivePreset(null);

    try {
      const res = await toolsApi.parseCronNaturalLanguage({ naturalLanguage: naturalLanguage.trim() });
      if (res.code === 200 && res.data) {
        setNlResult(res.data);
        setCronExpression(res.data.cronExpression || '');
        if (res.data.cronExpression) {
          try {
            const nextRes = await toolsApi.getCronNextTimes({
              cronExpression: res.data.cronExpression.trim(),
              count: 5,
              excludeHoliday: false,
            });
            if (nextRes.code === 200 && nextRes.data) {
              setNextTimesResult(nextRes.data);
              setShowNextTimes(true);
            }
          } catch {
            // ignore next times error
          }
        }
      } else {
        setError(res.message || 'AI 解析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI 解析失败，请稍后重试');
    } finally {
      setAiLoading(false);
    }
  };

  const handleCopy = async () => {
    const text = cronExpression || generateResult?.cronExpression || nlResult?.cronExpression || '';
    if (!text) return;

    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      setError('复制失败，请手动复制');
    }
  };

  const handlePreset = (preset: typeof presets[number]) => {
    setSecond(preset.second);
    setMinute(preset.minute);
    setHour(preset.hour);
    setDay(preset.day);
    setMonth(preset.month);
    setWeekDay(preset.weekDay);
    setActivePreset(preset.name);
  };

  const handleApplyNlResult = () => {
    if (!nlResult) return;
    setCronExpression(nlResult.cronExpression);
    setActiveTab('manual');
  };

  const displayCron = cronExpression || generateResult?.cronExpression || '';

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
          <h1 className="text-3xl font-bold text-white mb-1">Cron 表达式生成器</h1>
          <p className="text-gray-400">可视化生成、解析和AI自然语言转换 Cron 表达式</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <button
                onClick={() => setActiveTab('visual')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  activeTab === 'visual'
                    ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/20'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                可视化生成
              </button>
              <button
                onClick={() => setActiveTab('manual')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  activeTab === 'manual'
                    ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/20'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                手动输入
              </button>
              <button
                onClick={() => setActiveTab('ai')}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                  activeTab === 'ai'
                    ? 'bg-gradient-to-r from-purple-500 to-pink-500 text-white shadow-lg shadow-purple-500/20'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10'
                }`}
              >
                AI 自然语言
              </button>
            </div>

            {activeTab === 'visual' && (
              <div className="space-y-6 animate-fadeIn">
                <div>
                  <h3 className="text-sm font-medium text-gray-300 mb-3">快捷预设</h3>
                  <div className="grid grid-cols-3 gap-2">
                    {presets.map((preset) => (
                      <button
                        key={preset.name}
                        onClick={() => handlePreset(preset)}
                        className={`px-3 py-2 text-xs rounded-lg transition-all border ${
                          activePreset === preset.name
                            ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/30 border-cyan-500/50'
                            : 'bg-gradient-to-r from-cyan-500/20 to-blue-600/20 text-cyan-300 hover:from-cyan-500/30 hover:to-blue-600/30 border-cyan-500/10'
                        }`}
                      >
                        {preset.name}
                      </button>
                    ))}
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-medium text-gray-300 mb-3">字段设置</h3>
                  <div className="grid grid-cols-2 gap-4">
                    {cronFields.map((field) => (
                      <div key={field.key}>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                          {field.label}
                        </label>
                        <input
                          type="text"
                          value={fieldValues[field.key]}
                          onChange={(e) => {
                            fieldSetters[field.key](e.target.value);
                            setActivePreset(null);
                          }}
                          placeholder={field.placeholder}
                          className="w-full bg-black/30 border border-white/10 rounded-2xl px-4 py-3 text-gray-200 font-mono text-sm focus:outline-none focus:border-cyan-500/50 focus:ring-2 focus:ring-cyan-500/20 transition-all placeholder:text-gray-500"
                        />
                        <p className="text-xs text-gray-500 mt-1">{field.description}</p>
                      </div>
                    ))}
                  </div>
                </div>

                <button
                  onClick={handleGenerate}
                  disabled={loading}
                  className="btn-primary w-full flex items-center justify-center gap-2"
                >
                  {loading ? (
                    <>
                      <RotateCcw className="w-5 h-5 animate-spin" />
                      生成中...
                    </>
                  ) : (
                    <>
                      <Play className="w-5 h-5" />
                      生成表达式
                    </>
                  )}
                </button>
              </div>
            )}

            {activeTab === 'manual' && (
              <div className="space-y-6 animate-fadeIn">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    Cron 表达式
                  </label>
                  <textarea
                    value={cronExpression}
                    onChange={(e) => setCronExpression(e.target.value)}
                    placeholder="输入 Cron 表达式，如: 0 0 8 * * ?"
                    className="w-full h-40 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 font-mono text-sm resize-none focus:outline-none focus:border-cyan-500/50 focus:ring-2 focus:ring-cyan-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                  <p className="text-xs text-gray-500 mt-1">格式: 秒 分 时 日 月 周</p>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <button
                    onClick={handleParse}
                    disabled={loading || !cronExpression.trim()}
                    className="btn-primary flex items-center justify-center gap-2"
                  >
                    {loading ? (
                      <>
                        <RotateCcw className="w-5 h-5 animate-spin" />
                        解析中...
                      </>
                    ) : (
                      <>
                        <Play className="w-5 h-5" />
                        解析表达式
                      </>
                    )}
                  </button>
                  <button
                    onClick={handleNextTimes}
                    disabled={loading || !cronExpression.trim()}
                    className="px-4 py-3 bg-white/5 text-white rounded-xl font-medium hover:bg-white/10 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                  >
                    <Calendar className="w-5 h-5" />
                    下次执行时间
                  </button>
                </div>
              </div>
            )}

            {activeTab === 'ai' && (
              <div className="space-y-6 animate-fadeIn">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-1.5">
                    自然语言描述
                  </label>
                  <textarea
                    value={naturalLanguage}
                    onChange={(e) => setNaturalLanguage(e.target.value)}
                    placeholder="用自然语言描述定时规则，如：每天早上8点执行"
                    className="w-full h-40 bg-black/30 border border-white/10 rounded-2xl p-4 text-gray-200 text-sm resize-none focus:outline-none focus:border-purple-500/50 focus:ring-2 focus:ring-purple-500/20 transition-all placeholder:text-gray-500"
                    spellCheck={false}
                  />
                </div>

                <div>
                  <h3 className="text-sm font-medium text-gray-300 mb-2">示例提示</h3>
                  <div className="flex flex-wrap gap-2">
                    {['每天8点', '工作日9点半', '每周五下午3点', '每月15号', '国庆假期每天早上8点', '春节期间每天9点'].map((example) => (
                      <button
                        key={example}
                        onClick={() => setNaturalLanguage(example)}
                        className="px-3 py-1.5 text-xs bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 hover:text-gray-300 transition-colors"
                      >
                        {example}
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  onClick={handleNaturalLanguage}
                  disabled={aiLoading || !naturalLanguage.trim()}
                  className="w-full py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl font-medium hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-purple-500/20"
                >
                  {aiLoading ? (
                    <>
                      <RotateCcw className="w-5 h-5 animate-spin" />
                      AI 解析中...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-5 h-5" />
                      AI 解析
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
                <span className="px-2 py-1 bg-cyan-500/20 text-cyan-400 rounded-lg text-xs font-medium shrink-0">可视化生成</span>
                <p>通过选择预设或填写各字段值快速生成 Cron 表达式</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded-lg text-xs font-medium shrink-0">手动输入</span>
                <p>直接输入 Cron 表达式进行校验和解析</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-purple-500/20 text-purple-400 rounded-lg text-xs font-medium shrink-0">AI 自然语言</span>
                <p>用自然语言描述定时规则，AI 自动转换为 Cron 表达式</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-green-500/20 text-green-400 rounded-lg text-xs font-medium shrink-0">下次执行</span>
                <p>计算并展示 Cron 表达式未来多次执行时间，支持排除法定节假日</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-yellow-500/20 text-yellow-400 rounded-lg text-xs font-medium shrink-0">周字段说明</span>
                <p>1=周一,2=周二,...,6=周六,7=周日。指定周字段时日字段须为 ?，否则为 OR 关系</p>
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

          {displayCron && (
            <div className="glass-card rounded-3xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                  <Clock className="w-5 h-5 text-cyan-400" />
                  Cron 表达式
                </h2>
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
              </div>
              <div className="bg-black/30 border border-white/10 rounded-2xl p-6 text-center">
                <p className="text-3xl font-mono font-bold text-cyan-400 tracking-wider">
                  {displayCron}
                </p>
              </div>
              {(generateResult?.description || parseResult?.description) && (
                <p className="text-gray-300 text-sm mt-3 text-center">
                  {generateResult?.description || parseResult?.description}
                </p>
              )}
            </div>
          )}

          {generateResult?.warnings && generateResult.warnings.length > 0 && (
            <div className="glass-card rounded-3xl p-5 mt-4 animate-fadeIn border-yellow-500/30 bg-yellow-500/5">
              <div className="flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-yellow-400 flex-shrink-0 mt-0.5" />
                <div className="space-y-2">
                  <h3 className="text-yellow-400 font-semibold">自动修正提示</h3>
                  {generateResult.warnings.map((warning, idx) => (
                    <p key={idx} className="text-yellow-200/90 text-sm leading-relaxed">
                      {warning}
                    </p>
                  ))}
                </div>
              </div>
            </div>
          )}

          {generateResult && (
            <div className="glass-card rounded-3xl p-6 animate-fadeIn">
              <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                <Calendar className="w-5 h-5 text-cyan-400" />
                生成结果
              </h2>
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">状态</p>
                  <p className={`font-bold ${generateResult.valid ? 'text-green-400' : 'text-red-400'}`}>
                    {generateResult.valid ? '有效' : '无效'}
                  </p>
                </div>
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">预设</p>
                  <p className="text-white font-bold">{generateResult.presetName || '-'}</p>
                </div>
              </div>
              <div className="mt-4 bg-white/5 rounded-xl p-4">
                <label className="flex items-center justify-between cursor-pointer">
                  <div className="flex items-center gap-2">
                    <Calendar className="w-5 h-5 text-yellow-400" />
                    <div>
                      <p className="text-white font-medium">排除中国法定节假日</p>
                      <p className="text-gray-400 text-xs mt-0.5">自动跳过春节、国庆等法定节假日（含调休信息）</p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setExcludeHoliday(!excludeHoliday)}
                    className={`relative w-12 h-6 rounded-full transition-colors ${
                      excludeHoliday ? 'bg-gradient-to-r from-purple-500 to-pink-500' : 'bg-white/10'
                    }`}
                  >
                    <span
                      className={`absolute top-0.5 w-5 h-5 rounded-full bg-white transition-transform ${
                        excludeHoliday ? 'translate-x-6' : 'translate-x-0.5'
                      }`}
                    />
                  </button>
                </label>
              </div>

              <div className="mt-4">
                <button
                  onClick={handleNextTimes}
                  disabled={loading}
                  className="px-4 py-2.5 bg-white/5 text-white rounded-xl font-medium hover:bg-white/10 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 w-full justify-center"
                >
                  <Calendar className="w-5 h-5" />
                  查看下次执行时间
                </button>
              </div>
            </div>
          )}

          {parseResult && (
            <div className="glass-card rounded-3xl p-6 animate-fadeIn">
              <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                <AlertCircle className="w-5 h-5 text-cyan-400" />
                解析结果
              </h2>
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">状态</p>
                  <p className={`font-bold ${parseResult.valid ? 'text-green-400' : 'text-red-400'}`}>
                    {parseResult.valid ? '有效' : '无效'}
                  </p>
                </div>
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-1">描述</p>
                  <p className="text-white font-bold text-sm">{parseResult.description}</p>
                </div>
              </div>
              {parseResult.fields && Object.keys(parseResult.fields).length > 0 && (
                <div className="bg-white/5 rounded-xl p-4">
                  <p className="text-gray-400 text-sm mb-2">字段详情</p>
                  <div className="grid grid-cols-3 gap-2">
                    {Object.entries(parseResult.fields).map(([key, value]) => (
                      <div key={key} className="text-center">
                        <p className="text-xs text-gray-500">{key}</p>
                        <p className="text-sm font-mono text-gray-200">{value}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              {parseResult.errors && parseResult.errors.length > 0 && (
                <div className="mt-4 space-y-2">
                  {parseResult.errors.map((err, index) => (
                    <div key={index} className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl">
                      <p className="text-red-300 text-sm">{err}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {nlResult && (
            <div className="glass-card rounded-3xl p-6 animate-fadeIn">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <MessageSquare className="w-5 h-5 text-purple-400" />
                  AI 解析结果
                </h2>
                <span className={`px-2 py-1 rounded-lg text-xs font-medium ${
                  nlResult.confidence >= 0.8
                    ? 'bg-green-500/20 text-green-400'
                    : nlResult.confidence >= 0.5
                      ? 'bg-yellow-500/20 text-yellow-400'
                      : 'bg-red-500/20 text-red-400'
                }`}>
                  置信度 {Math.round(nlResult.confidence * 100)}%
                </span>
              </div>
              <div className="space-y-3">
                <div className="bg-black/30 border border-white/10 rounded-2xl p-4">
                  <p className="text-xs text-gray-500 mb-1">生成的表达式</p>
                  <p className="text-xl font-mono font-bold text-purple-400">{nlResult.cronExpression}</p>
                </div>
                <p className="text-gray-300 text-sm">{nlResult.description}</p>
                {nlResult.aiModel && (
                  <p className="text-xs text-gray-500">AI 模型: {nlResult.aiModel}</p>
                )}
                {nlResult.fallback && (
                  <div className="p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
                    <p className="text-yellow-200/80 text-sm">使用了规则匹配回退方案，AI 未直接生成</p>
                  </div>
                )}
                {nlResult.errors && nlResult.errors.length > 0 && (
                  <div className="space-y-2">
                    {nlResult.errors.map((err, index) => (
                      <p key={index} className="text-red-300 text-sm">{err}</p>
                    ))}
                  </div>
                )}
                <button
                  onClick={handleApplyNlResult}
                  className="w-full py-2.5 bg-gradient-to-r from-purple-500 to-pink-500 text-white rounded-xl font-medium hover:opacity-90 transition-all flex items-center justify-center gap-2 shadow-lg shadow-purple-500/20"
                >
                  <Sparkles className="w-5 h-5" />
                  应用到手动输入
                </button>
              </div>
            </div>
          )}

          {showNextTimes && nextTimesResult && (
            <div className="glass-card rounded-3xl p-6 animate-fadeIn">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <ChevronDown className="w-5 h-5 text-cyan-400" />
                  下次执行时间
                </h2>
                <button
                  onClick={() => setShowNextTimes(false)}
                  className="px-3 py-1.5 text-sm bg-white/5 text-gray-400 rounded-lg hover:bg-white/10 transition-colors"
                >
                  收起
                </button>
              </div>
              {nextTimesResult.nextTimes && nextTimesResult.nextTimes.length > 0 ? (
                <div className="space-y-2">
                  {nextTimesResult.nextTimes.map((time, index) => (
                    <div key={index} className="flex items-center gap-3 p-3 bg-white/5 rounded-xl">
                      <span className="w-6 h-6 rounded-full bg-cyan-500/20 text-cyan-400 text-xs flex items-center justify-center font-medium">
                        {index + 1}
                      </span>
                      <span className="text-gray-200 font-mono text-sm">{time}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-400 text-sm text-center">无法计算下次执行时间</p>
              )}
              {nextTimesResult.holidayInfo && nextTimesResult.holidayInfo.length > 0 && (
                <div className="mt-4 p-3 bg-yellow-500/10 border border-yellow-500/20 rounded-xl">
                  <div className="flex items-start gap-2 mb-2">
                    <AlertCircle className="w-5 h-5 text-yellow-400 flex-shrink-0 mt-0.5" />
                    <h4 className="text-yellow-400 font-semibold text-sm">节假日排除信息</h4>
                  </div>
                  <div className="space-y-1">
                    {nextTimesResult.holidayInfo.map((info, index) => (
                      <p key={index} className="text-yellow-200/80 text-xs pl-7">{info}</p>
                    ))}
                  </div>
                </div>
              )}
              {nextTimesResult.excludedTimes && nextTimesResult.excludedTimes.length > 0 && (
                <div className="mt-4">
                  <button
                    onClick={() => {}}
                    className="text-gray-400 text-xs hover:text-gray-300 transition-colors flex items-center gap-1"
                  >
                    已跳过 {nextTimesResult.excludedTimes.length} 个节假日执行时间
                  </button>
                </div>
              )}
              {nextTimesResult.errors && nextTimesResult.errors.length > 0 && (
                <div className="mt-3 space-y-2">
                  {nextTimesResult.errors.map((err, index) => (
                    <p key={index} className="text-red-300 text-sm">{err}</p>
                  ))}
                </div>
              )}
            </div>
          )}

          {!displayCron && !generateResult && !parseResult && !nlResult && !error && (
            <div className="glass-card rounded-3xl p-8 text-center">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/10 to-blue-500/10 flex items-center justify-center mx-auto mb-4">
                <Clock className="w-10 h-10 text-cyan-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待操作</h3>
              <p className="text-gray-400 text-sm">
                通过可视化生成、手动输入或 AI 自然语言方式创建 Cron 表达式
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
