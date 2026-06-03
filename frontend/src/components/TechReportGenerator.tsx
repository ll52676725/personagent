import { useState, useEffect, useRef } from 'react';
import { 
  Sparkles, 
  AlertCircle, 
  StopCircle, 
  Loader2, 
  Download, 
  FileText, 
  Presentation,
  ChevronLeft,
  ChevronRight,
  Users,
  Target,
  Clock,
  Lightbulb,
  MessageSquare,
  Eye,
  Edit3,
  BookOpen,
  Copy,
  Check,
  PlayCircle
} from 'lucide-react';
import { techReportApi } from '@/api';
import type { TechReportRequest, TechReportResult, ReportSlide, SceneTemplate, AudienceType } from '@/types';

type ViewMode = 'form' | 'result' | 'preview';
type GeneratingType = 'outline' | 'full' | null;

export default function TechReportGenerator() {
  const [viewMode, setViewMode] = useState<ViewMode>('form');
  const [generatingType, setGeneratingType] = useState<GeneratingType>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [currentSlideIndex, setCurrentSlideIndex] = useState(0);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'slides' | 'qa' | 'tips'>('slides');

  const [scenes, setScenes] = useState<SceneTemplate[]>([]);
  const [audiences, setAudienceTypes] = useState<AudienceType[]>([]);

  const [formData, setFormData] = useState<TechReportRequest>({
    scene: '',
    description: '',
    audience: '',
    reportType: 'ppt',
    slideCount: 15,
    keyPoints: [],
    industry: '',
    companySize: '',
    additionalInfo: ''
  });

  const [keyPointInput, setKeyPointInput] = useState('');
  const [result, setResult] = useState<TechReportResult | null>(null);
  const [streamContent, setStreamContent] = useState('');

  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const loadMetadata = async () => {
      try {
        const [scenesRes, audiencesRes] = await Promise.all([
          techReportApi.getScenes(),
          techReportApi.getAudiences()
        ]);
        if (scenesRes.code === 200) {
          setScenes(scenesRes.data);
        }
        if (audiencesRes.code === 200) {
          setAudienceTypes(audiencesRes.data);
        }
      } catch (err) {
        console.error('Failed to load metadata:', err);
      }
    };
    loadMetadata();
  }, []);

  const handleSceneSelect = (scene: SceneTemplate) => {
    setFormData(prev => ({
      ...prev,
      scene: scene.name,
      audience: scene.defaultAudience
    }));
  };

  const handleAddKeyPoint = () => {
    if (keyPointInput.trim()) {
      setFormData(prev => ({
        ...prev,
        keyPoints: [...(prev.keyPoints || []), keyPointInput.trim()]
      }));
      setKeyPointInput('');
    }
  };

  const handleRemoveKeyPoint = (index: number) => {
    setFormData(prev => ({
      ...prev,
      keyPoints: (prev.keyPoints || []).filter((_, i) => i !== index)
    }));
  };

  const handleKeyPointKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddKeyPoint();
    }
  };

  const handleStopGenerate = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setGeneratingType(null);
    setLoading(false);
  };

  const handleGenerateOutline = async () => {
    if (!formData.scene.trim()) {
      setError('请选择或输入汇报场景');
      return;
    }
    if (!formData.audience.trim()) {
      setError('请选择汇报受众');
      return;
    }

    setLoading(true);
    setError('');
    setGeneratingType('outline');
    setStreamContent('');

    try {
      const response = await techReportApi.generateOutline(formData);
      if (response.code === 200) {
        setResult(response.data);
        setViewMode('result');
      }
    } catch (err: any) {
      setError(err.message || '生成失败，请稍后重试');
    } finally {
      setGeneratingType(null);
      setLoading(false);
    }
  };

  const handleGenerateFull = async () => {
    if (!formData.scene.trim()) {
      setError('请选择或输入汇报场景');
      return;
    }
    if (!formData.audience.trim()) {
      setError('请选择汇报受众');
      return;
    }

    setLoading(true);
    setError('');
    setGeneratingType('full');
    setStreamContent('');
    setResult(null);

    try {
      const response = await techReportApi.generate(formData);
      if (response.code === 200) {
        setResult(response.data);
        setViewMode('result');
        setCurrentSlideIndex(0);
      }
    } catch (err: any) {
      setError(err.message || '生成失败，请稍后重试');
    } finally {
      setGeneratingType(null);
      setLoading(false);
    }
  };

  const handleCopyToClipboard = async (text: string, field: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    } catch (err) {
      console.error('Copy failed:', err);
    }
  };

  const handleDownloadMarkdown = () => {
    if (result) {
      const safeTitle = result.reportTitle.replace(/[^\w\u4e00-\u9fa5]/g, '_');
      techReportApi.downloadMarkdown(result, `${safeTitle}.md`);
    }
  };

  const handleDownloadPptOutline = () => {
    if (result) {
      const safeTitle = result.reportTitle.replace(/[^\w\u4e00-\u9fa5]/g, '_');
      techReportApi.downloadPptOutline(result, `${safeTitle}_PPT大纲.txt`);
    }
  };

  const handlePrevSlide = () => {
    if (result && currentSlideIndex > 0) {
      setCurrentSlideIndex(currentSlideIndex - 1);
    }
  };

  const handleNextSlide = () => {
    if (result && currentSlideIndex < result.slides.length - 1) {
      setCurrentSlideIndex(currentSlideIndex + 1);
    }
  };

  const getSlideTypeIcon = (type: string) => {
    const icons: Record<string, any> = {
      cover: Presentation,
      toc: BookOpen,
      summary: FileText,
      content: Edit3,
      architecture: Target,
      roadmap: PlayCircle,
      data: FileText,
      comparison: Target,
      risk: AlertCircle,
      resource: Users,
      qa: MessageSquare
    };
    return icons[type] || FileText;
  };

  const getSlideTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      cover: '封面',
      toc: '目录',
      summary: '摘要',
      content: '内容',
      architecture: '架构',
      roadmap: '路线图',
      data: '数据',
      comparison: '对比',
      risk: '风险',
      resource: '资源',
      qa: '问答'
    };
    return labels[type] || type;
  };

  const getSlideTypeColor = (type: string) => {
    const colors: Record<string, string> = {
      cover: 'from-purple-500 to-pink-500',
      toc: 'from-blue-500 to-cyan-500',
      summary: 'from-emerald-500 to-teal-500',
      content: 'from-indigo-500 to-purple-500',
      architecture: 'from-orange-500 to-red-500',
      roadmap: 'from-pink-500 to-rose-500',
      data: 'from-cyan-500 to-blue-500',
      comparison: 'from-amber-500 to-orange-500',
      risk: 'from-red-500 to-orange-500',
      resource: 'from-green-500 to-emerald-500',
      qa: 'from-violet-500 to-purple-500'
    };
    return colors[type] || 'from-gray-500 to-slate-500';
  };

  const renderSlidePreview = (slide: ReportSlide) => {
    const SlideIcon = getSlideTypeIcon(slide.type);
    const gradientClass = getSlideTypeColor(slide.type);

    return (
      <div className="relative w-full max-w-4xl mx-auto">
        <div className={`relative overflow-hidden rounded-3xl glass-card-strong p-8 animate-fadeIn`}>
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-transparent via-white/20 to-transparent" />
          <div className={`absolute top-0 right-0 w-64 h-64 bg-gradient-to-br ${gradientClass} opacity-10 rounded-full blur-3xl`} />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/10 to-transparent rounded-full blur-2xl" />

          <div className="relative">
            <div className="flex items-center gap-3 mb-6">
              <div className={`p-3 rounded-2xl bg-gradient-to-br ${gradientClass} shadow-lg`}>
                <SlideIcon className="w-6 h-6 text-white" />
              </div>
              <div>
                <span className="text-xs text-gray-500 font-medium">
                  第 {slide.slideNumber} 页 · {getSlideTypeLabel(slide.type)} · {slide.durationMinutes}分钟
                </span>
                <h2 className="text-3xl font-bold text-white mt-1">{slide.title}</h2>
              </div>
            </div>

            {slide.content && (
              <div className="mb-6">
                <p className="text-gray-300 text-lg leading-relaxed">{slide.content}</p>
              </div>
            )}

            {slide.keyPoints && slide.keyPoints.length > 0 && (
              <div className="mb-6">
                <h4 className="text-sm font-semibold text-cyan-400 mb-3 flex items-center gap-2">
                  <Lightbulb className="w-4 h-4" />
                  核心要点
                </h4>
                <ul className="space-y-3">
                  {slide.keyPoints.map((point, idx) => (
                    <li key={idx} className="flex items-start gap-3 group">
                      <span className="flex-shrink-0 w-6 h-6 rounded-full bg-gradient-to-br from-cyan-500 to-blue-500 flex items-center justify-center text-xs font-bold text-white mt-0.5">
                        {idx + 1}
                      </span>
                      <span className="text-gray-200 leading-relaxed">{point}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {slide.speakerNotes && (
              <div className="bg-gradient-to-r from-amber-500/10 to-orange-500/10 border border-amber-500/20 rounded-2xl p-5">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-semibold text-amber-400 flex items-center gap-2">
                    <MessageSquare className="w-4 h-4" />
                    演讲者备注
                  </h4>
                  <button
                    onClick={() => handleCopyToClipboard(slide.speakerNotes!, `speaker-${slide.slideNumber}`)}
                    className="p-1.5 rounded-lg hover:bg-white/5 text-gray-400 hover:text-amber-400 transition-all"
                  >
                    {copiedField === `speaker-${slide.slideNumber}` ? (
                      <Check className="w-4 h-4 text-emerald-400" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>
                <p className="text-amber-200/80 text-sm leading-relaxed">{slide.speakerNotes}</p>
              </div>
            )}

            {slide.visualSuggestion && (
              <div className="mt-4 flex items-center gap-2 text-sm text-gray-400">
                <Eye className="w-4 h-4" />
                <span>视觉建议：{slide.visualSuggestion}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  if (viewMode === 'form') {
    return (
      <div className="space-y-8 max-w-5xl mx-auto">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
              <Presentation className="w-4 h-4 text-cyan-400" />
              <span className="text-sm text-cyan-400 font-medium">架构师专属工具</span>
            </div>
            <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
              技术汇报<span className="gradient-text-aurora">生成器</span>
            </h1>
            <p className="text-gray-400 text-lg">
              输入简单场景，AI自动生成架构师级别的专业技术汇报方案
            </p>
          </div>
        </div>

        {error && (
          <div className="relative overflow-hidden glass-card rounded-2xl p-5 flex items-center gap-4 animate-fadeIn border border-red-500/30">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-red-500/30 to-transparent" />
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-red-500/10 to-transparent rounded-full blur-2xl" />
            <div className="relative flex items-center gap-4 w-full">
              <div className="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center flex-shrink-0">
                <AlertCircle className="w-5 h-5 text-red-400" />
              </div>
              <span className="text-red-300 flex-1">{error}</span>
              <button 
                onClick={() => setError('')}
                className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-red-400 hover:text-red-300 transition-all text-sm font-medium"
              >
                关闭
              </button>
            </div>
          </div>
        )}

        {generatingType && (
          <div className="relative overflow-hidden glass-card rounded-2xl p-5 flex items-center gap-4 animate-fadeIn border border-cyan-500/30">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-cyan-500/30 to-transparent" />
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-cyan-500/10 to-transparent rounded-full blur-2xl" />
            <div className="relative flex items-center gap-4 w-full">
              <div className="w-10 h-10 rounded-xl bg-cyan-500/20 flex items-center justify-center flex-shrink-0">
                <Loader2 className="w-5 h-5 text-cyan-400 animate-spin" />
              </div>
              <span className="text-cyan-300 flex-1">
                正在{generatingType === 'outline' ? '生成汇报大纲' : '生成完整汇报方案'}...
                {streamContent.length > 0 && 
                  ` (${streamContent.length} 字)`
                }
              </span>
              <button 
                onClick={handleStopGenerate}
                className="px-4 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-400 hover:text-red-300 flex items-center gap-2 transition-all text-sm font-medium"
              >
                <StopCircle className="w-4 h-4" />
                停止
              </button>
            </div>
          </div>
        )}

        <div className="relative overflow-hidden rounded-3xl glass-card-strong p-8 animate-fadeIn">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-violet-500/15 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/10 to-transparent rounded-full blur-2xl" />

          <div className="relative space-y-8">
            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '50ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-3 flex items-center gap-2">
                <Target className="w-4 h-4 text-cyan-400" />
                快速选择场景
              </label>
              {scenes.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                  {scenes.map((scene, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSceneSelect(scene)}
                      className={`p-4 rounded-2xl text-left transition-all ${
                        formData.scene === scene.name
                          ? 'bg-gradient-to-br from-indigo-500/30 to-cyan-500/30 border-2 border-cyan-500/50 shadow-lg shadow-cyan-500/20'
                          : 'bg-white/5 border-2 border-transparent hover:bg-white/10 hover:border-white/10'
                      }`}
                    >
                      <h4 className="font-semibold text-white mb-1">{scene.name}</h4>
                      <p className="text-xs text-gray-400 line-clamp-2">{scene.description}</p>
                      <div className="mt-2 flex items-center gap-2 text-xs text-gray-500">
                        <Users className="w-3 h-3" />
                        <span>{scene.defaultAudience}</span>
                        <span>·</span>
                        <span>{scene.slideCount}页</span>
                      </div>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-sm">
                  <div className="flex items-start gap-2">
                    <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="font-medium">后端服务未连接</p>
                      <p className="text-xs text-amber-400/70 mt-1">场景模板加载失败，请手动输入场景和受众。启动后端服务可获得完整体验。</p>
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '100ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                汇报场景 *
              </label>
              <input
                type="text"
                value={formData.scene}
                onChange={(e) => setFormData(prev => ({ ...prev, scene: e.target.value }))}
                className="input-field w-full text-lg"
                placeholder="例如：Q3季度技术汇报、微服务架构升级方案、新项目立项汇报等"
                disabled={loading}
              />
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '150ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                场景补充描述
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                className="input-field w-full h-24 resize-none"
                placeholder="补充说明汇报的背景、目标、特殊要求等..."
                disabled={loading}
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 animate-fadeIn opacity-0" style={{ animationDelay: '200ms', animationFillMode: 'forwards' }}>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-3 flex items-center gap-2">
                  <Users className="w-4 h-4 text-cyan-400" />
                  汇报受众 *
                </label>
                {audiences.length > 0 ? (
                  <div className="space-y-2">
                    {audiences.map((aud, idx) => (
                      <button
                        key={idx}
                        onClick={() => setFormData(prev => ({ ...prev, audience: aud.label }))}
                        className={`w-full p-3 rounded-xl text-left transition-all flex items-start gap-3 ${
                          formData.audience === aud.label
                            ? 'bg-gradient-to-r from-cyan-500/20 to-indigo-500/20 border-2 border-cyan-500/40'
                            : 'bg-white/5 border-2 border-transparent hover:bg-white/10'
                        }`}
                      >
                        <div className={`w-2 h-2 rounded-full mt-2 flex-shrink-0 ${
                          formData.audience === aud.label ? 'bg-cyan-400' : 'bg-gray-600'
                        }`} />
                        <div>
                          <h4 className="font-medium text-white text-sm">{aud.label}</h4>
                          <p className="text-xs text-gray-400">{aud.description}</p>
                        </div>
                      </button>
                    ))}
                  </div>
                ) : (
                  <input
                    type="text"
                    value={formData.audience}
                    onChange={(e) => setFormData(prev => ({ ...prev, audience: e.target.value }))}
                    className="input-field w-full"
                    placeholder="请输入汇报受众，如：公司管理层、技术团队、客户等"
                    disabled={loading}
                  />
                )}
              </div>

              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2 flex items-center gap-2">
                    <Clock className="w-4 h-4 text-cyan-400" />
                    PPT页数
                  </label>
                  <input
                    type="range"
                    min="8"
                    max="30"
                    value={formData.slideCount}
                    onChange={(e) => setFormData(prev => ({ ...prev, slideCount: parseInt(e.target.value) }))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-cyan-500"
                    disabled={loading}
                  />
                  <div className="text-center text-sm text-gray-400 mt-1">
                    约 <span className="text-cyan-400 font-semibold">{formData.slideCount}</span> 页
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    所属行业
                  </label>
                  <input
                    type="text"
                    value={formData.industry}
                    onChange={(e) => setFormData(prev => ({ ...prev, industry: e.target.value }))}
                    className="input-field w-full"
                    placeholder="如：互联网、金融、制造业等"
                    disabled={loading}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    公司规模
                  </label>
                  <select
                    value={formData.companySize}
                    onChange={(e) => setFormData(prev => ({ ...prev, companySize: e.target.value }))}
                    className="input-field w-full"
                    disabled={loading}
                  >
                    <option value="">请选择</option>
                    <option value="初创公司（<50人）">初创公司（{'<'}50人）</option>
                    <option value="中小企业（50-500人）">中小企业（50-500人）</option>
                    <option value="大型企业（500-5000人）">大型企业（500-5000人）</option>
                    <option value="超大型企业（>5000人）">超大型企业（{'>'}5000人）</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '250ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-2 flex items-center gap-2">
                <Lightbulb className="w-4 h-4 text-cyan-400" />
                必须包含的关键点（可选）
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={keyPointInput}
                  onChange={(e) => setKeyPointInput(e.target.value)}
                  onKeyDown={handleKeyPointKeyDown}
                  className="input-field flex-1"
                  placeholder="输入关键点后按回车添加"
                  disabled={loading}
                />
                <button
                  type="button"
                  onClick={handleAddKeyPoint}
                  disabled={loading || !keyPointInput.trim()}
                  className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-500 text-white font-medium hover:opacity-90 transition-all disabled:opacity-50"
                >
                  添加
                </button>
              </div>
              {formData.keyPoints && formData.keyPoints.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-3">
                  {formData.keyPoints.map((point, idx) => (
                    <span
                      key={idx}
                      className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-cyan-500/10 border border-cyan-500/30 text-cyan-300 text-sm"
                    >
                      {point}
                      <button
                        onClick={() => handleRemoveKeyPoint(idx)}
                        className="w-4 h-4 rounded-full bg-cyan-500/20 hover:bg-cyan-500/40 flex items-center justify-center transition-colors"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '300ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                其他补充信息
              </label>
              <textarea
                value={formData.additionalInfo}
                onChange={(e) => setFormData(prev => ({ ...prev, additionalInfo: e.target.value }))}
                className="input-field w-full h-20 resize-none"
                placeholder="其他需要说明的信息..."
                disabled={loading}
              />
            </div>

            <div className="flex gap-4 pt-4 animate-fadeIn opacity-0" style={{ animationDelay: '350ms', animationFillMode: 'forwards' }}>
              <button
                type="button"
                onClick={handleGenerateOutline}
                disabled={loading || !formData.scene.trim() || !formData.audience.trim()}
                className="flex-1 btn-secondary py-4 text-base disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                <FileText className="w-5 h-5" />
                生成大纲
              </button>
              <button
                type="button"
                onClick={handleGenerateFull}
                disabled={loading || !formData.scene.trim() || !formData.audience.trim()}
                className="flex-1 btn-primary disabled:opacity-50 disabled:cursor-not-allowed py-4 text-base flex items-center justify-center gap-2"
              >
                <Sparkles className="w-5 h-5" />
                生成完整方案
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (viewMode === 'result' && result) {
    const currentSlide = result.slides[currentSlideIndex];

    return (
      <div className="space-y-8">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
              <Sparkles className="w-4 h-4 text-emerald-400" />
              <span className="text-sm text-emerald-400 font-medium">生成完成</span>
            </div>
            <h1 className="text-3xl font-bold text-white mb-2 tracking-tight">
              {result.reportTitle}
            </h1>
            <div className="flex flex-wrap items-center gap-4 text-gray-400">
              <span className="flex items-center gap-1.5">
                <FileText className="w-4 h-4" />
                {result.totalSlides} 页
              </span>
              <span className="flex items-center gap-1.5">
                <Clock className="w-4 h-4" />
                约 {result.estimatedDurationMinutes} 分钟
              </span>
              <span className="flex items-center gap-1.5">
                <Sparkles className="w-4 h-4" />
                {result.model}
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setViewMode('form')}
              className="btn-secondary py-2.5 px-5 flex items-center gap-2"
            >
              <Edit3 className="w-4 h-4" />
              重新生成
            </button>
            <button
              onClick={handleDownloadMarkdown}
              className="btn-secondary py-2.5 px-5 flex items-center gap-2"
            >
              <Download className="w-4 h-4" />
              导出Markdown
            </button>
            <button
              onClick={handleDownloadPptOutline}
              className="btn-primary py-2.5 px-5 flex items-center gap-2"
            >
              <Presentation className="w-4 h-4" />
              导出PPT大纲
            </button>
          </div>
        </div>

        <div className="relative overflow-hidden rounded-2xl glass-card-strong p-6">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          <h3 className="text-lg font-semibold text-white mb-3">执行摘要</h3>
          <p className="text-gray-300 leading-relaxed">{result.executiveSummary}</p>
        </div>

        <div className="flex gap-2 border-b border-white/10 pb-2">
          <button
            onClick={() => setActiveTab('slides')}
            className={`px-4 py-2 rounded-t-xl font-medium transition-all ${
              activeTab === 'slides'
                ? 'bg-white/10 text-cyan-400'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <span className="flex items-center gap-2">
              <Presentation className="w-4 h-4" />
              幻灯片详情
            </span>
          </button>
          <button
            onClick={() => setActiveTab('qa')}
            className={`px-4 py-2 rounded-t-xl font-medium transition-all ${
              activeTab === 'qa'
                ? 'bg-white/10 text-cyan-400'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <span className="flex items-center gap-2">
              <MessageSquare className="w-4 h-4" />
              Q&A 准备
              {result.qaPreparation.length > 0 && (
                <span className="px-2 py-0.5 text-xs bg-cyan-500/20 rounded-full">
                  {result.qaPreparation.length}
                </span>
              )}
            </span>
          </button>
          <button
            onClick={() => setActiveTab('tips')}
            className={`px-4 py-2 rounded-t-xl font-medium transition-all ${
              activeTab === 'tips'
                ? 'bg-white/10 text-cyan-400'
                : 'text-gray-400 hover:text-white'
            }`}
          >
            <span className="flex items-center gap-2">
              <Lightbulb className="w-4 h-4" />
              演讲技巧
            </span>
          </button>
        </div>

        {activeTab === 'slides' && (
          <>
            <div className="flex items-center gap-2 mb-6 overflow-x-auto pb-2 scrollbar-thin">
              {result.slides.map((slide, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentSlideIndex(idx)}
                  className={`flex-shrink-0 p-3 rounded-xl transition-all min-w-[100px] ${
                    currentSlideIndex === idx
                      ? 'bg-gradient-to-br from-cyan-500/30 to-indigo-500/30 border-2 border-cyan-500/50'
                      : 'bg-white/5 border-2 border-transparent hover:bg-white/10'
                  }`}
                >
                  <div className="text-xs text-gray-400 mb-1">第{slide.slideNumber}页</div>
                  <div className="text-sm font-medium text-white truncate max-w-[120px]">
                    {slide.title}
                  </div>
                </button>
              ))}
            </div>

            <div className="flex items-center gap-4 mb-6">
              <button
                onClick={handlePrevSlide}
                disabled={currentSlideIndex === 0}
                className="p-3 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-all disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <div className="flex-1 text-center text-sm text-gray-400">
                {currentSlideIndex + 1} / {result.slides.length}
              </div>
              <button
                onClick={handleNextSlide}
                disabled={currentSlideIndex === result.slides.length - 1}
                className="p-3 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-all disabled:opacity-30 disabled:cursor-not-allowed"
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>

            {renderSlidePreview(currentSlide)}
          </>
        )}

        {activeTab === 'qa' && (
          <div className="space-y-4">
            {result.qaPreparation.map((qa, idx) => (
              <div
                key={idx}
                className="relative overflow-hidden rounded-2xl glass-card p-5"
              >
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500/30 to-purple-500/30 flex items-center justify-center flex-shrink-0">
                    <span className="font-bold text-violet-300">Q{idx + 1}</span>
                  </div>
                  <div className="flex-1">
                    <p className="text-gray-200 leading-relaxed whitespace-pre-wrap">{qa}</p>
                  </div>
                  <button
                    onClick={() => handleCopyToClipboard(qa, `qa-${idx}`)}
                    className="p-2 rounded-lg hover:bg-white/5 text-gray-400 hover:text-white transition-all flex-shrink-0"
                  >
                    {copiedField === `qa-${idx}` ? (
                      <Check className="w-4 h-4 text-emerald-400" />
                    ) : (
                      <Copy className="w-4 h-4" />
                    )}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 'tips' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {result.presentationTips.map((tip, idx) => (
              <div
                key={idx}
                className="relative overflow-hidden rounded-2xl glass-card p-5"
              >
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-500/30 to-orange-500/30 flex items-center justify-center flex-shrink-0">
                    <Lightbulb className="w-5 h-5 text-amber-400" />
                  </div>
                  <div className="flex-1">
                    <p className="text-gray-200 leading-relaxed">{tip}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  return null;
}
