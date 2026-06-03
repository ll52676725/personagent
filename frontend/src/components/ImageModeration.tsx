import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ShieldAlert,
  ArrowLeft,
  Upload,
  Image,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Download,
  Info,
  Trash2,
  Shield,
  AlertTriangle,
  Eye,
  Zap,
  Clock,
  Settings
} from 'lucide-react';
import { toolsApi } from '@/api';
import { ImageModerationRequest, ImageModerationResult, ModerationCategoryDetail } from '@/types';

const ACCEPTED_EXTENSIONS = '.jpg,.jpeg,.png,.gif,.bmp,.webp,.tiff';
const MAX_FILE_SIZE = 10 * 1024 * 1024;

const CATEGORY_CONFIG: Record<string, { label: string; color: string; bgColor: string; borderColor: string; icon: any }> = {
  pornography: {
    label: '涉黄',
    color: 'text-pink-400',
    bgColor: 'bg-pink-500/20',
    borderColor: 'border-pink-500/30',
    icon: AlertTriangle
  },
  political: {
    label: '涉政',
    color: 'text-red-400',
    bgColor: 'bg-red-500/20',
    borderColor: 'border-red-500/30',
    icon: ShieldAlert
  },
  violence: {
    label: '涉爆',
    color: 'text-orange-400',
    bgColor: 'bg-orange-500/20',
    borderColor: 'border-orange-500/30',
    icon: Zap
  },
  other: {
    label: '其他违规',
    color: 'text-amber-400',
    bgColor: 'bg-amber-500/20',
    borderColor: 'border-amber-500/30',
    icon: AlertCircle
  }
};

const RISK_LEVEL_CONFIG: Record<string, { label: string; color: string; bgColor: string; borderColor: string }> = {
  safe: { label: '安全', color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', borderColor: 'border-emerald-500/30' },
  low: { label: '低危', color: 'text-cyan-400', bgColor: 'bg-cyan-500/20', borderColor: 'border-cyan-500/30' },
  medium: { label: '中危', color: 'text-amber-400', bgColor: 'bg-amber-500/20', borderColor: 'border-amber-500/30' },
  high: { label: '高危', color: 'text-red-400', bgColor: 'bg-red-500/20', borderColor: 'border-red-500/30' }
};

const CONCLUSION_CONFIG: Record<string, { label: string; color: string; bgColor: string; icon: any }> = {
  pass: { label: '通过', color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', icon: CheckCircle2 },
  review: { label: '待复审', color: 'text-amber-400', bgColor: 'bg-amber-500/20', icon: Eye },
  block: { label: '拦截', color: 'text-red-400', bgColor: 'bg-red-500/20', icon: ShieldAlert }
};

interface DetectionOption {
  key: string;
  stateKey: 'detectPornography' | 'detectPolitical' | 'detectViolence' | 'detectOther';
  label: string;
  description: string;
}

const DETECTION_OPTIONS: DetectionOption[] = [
  { key: 'pornography', stateKey: 'detectPornography', label: '涉黄检测', description: '检测色情、低俗、性感暴露等内容' },
  { key: 'political', stateKey: 'detectPolitical', label: '涉政检测', description: '检测政治敏感人物、敏感事件、违禁标志等' },
  { key: 'violence', stateKey: 'detectViolence', label: '涉爆检测', description: '检测暴力、血腥、恐怖、爆炸物、武器等' },
  { key: 'other', stateKey: 'detectOther', label: '其他违规', description: '检测赌博、毒品、烟酒广告等内容' }
];

/**
 * 风险等级标签组件
 * <p>根据风险等级显示不同颜色的标签
 * 
 * @param riskLevel - 风险等级：safe/low/medium/high
 */
function RiskLevelBadge({ riskLevel }: { riskLevel: string }) {
  const config = RISK_LEVEL_CONFIG[riskLevel] || RISK_LEVEL_CONFIG.safe;
  return (
    <span className={`px-2 py-0.5 text-xs rounded-full ${config.bgColor} ${config.color} font-medium border ${config.borderColor}`}>
      {config.label}
    </span>
  );
}

/**
 * 置信度进度条组件
 * <p>可视化展示检测置信度
 * 
 * @param confidence - 置信度，0-100
 */
function ConfidenceBar({ confidence }: { confidence: number }) {
  const getColor = (val: number) => {
    if (val >= 80) return 'from-red-500 to-rose-500';
    if (val >= 50) return 'from-amber-500 to-orange-500';
    return 'from-emerald-500 to-teal-500';
  };

  return (
    <div className="flex items-center gap-3">
      <div className="flex-1 h-2 bg-white/10 rounded-full overflow-hidden">
        <div
          className={`h-full bg-gradient-to-r ${getColor(confidence)} transition-all duration-500`}
          style={{ width: `${confidence}%` }}
        />
      </div>
      <span className="text-sm font-mono text-gray-400 w-12 text-right">{confidence}%</span>
    </div>
  );
}

/**
 * 分类检测结果卡片组件
 * <p>展示单个检测分类的详细结果
 * 
 * @param detail - 分类检测详情
 */
function CategoryResultCard({ detail }: { detail: ModerationCategoryDetail }) {
  const config = CATEGORY_CONFIG[detail.category] || CATEGORY_CONFIG.other;
  const Icon = config.icon;

  return (
    <div className={`glass-card rounded-2xl p-5 border ${detail.violated ? config.borderColor : 'border-white/10'}`}>
      <div className="flex items-center gap-3 mb-4">
        <div className={`w-10 h-10 rounded-xl ${detail.violated ? config.bgColor : 'bg-white/5'} flex items-center justify-center`}>
          <Icon className={`w-5 h-5 ${detail.violated ? config.color : 'text-gray-500'}`} />
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h3 className="text-white font-medium">{detail.categoryLabel}</h3>
            {detail.violated ? (
              <span className="text-xs text-red-400">⚠️ 检测到违规</span>
            ) : (
              <span className="text-xs text-emerald-400">✅ 正常</span>
            )}
          </div>
          <p className="text-gray-500 text-xs mt-0.5">{detail.description}</p>
        </div>
        <RiskLevelBadge riskLevel={detail.riskLevel} />
      </div>

      <div className="mb-4">
        <p className="text-xs text-gray-400 mb-2">置信度</p>
        <ConfidenceBar confidence={detail.confidence} />
      </div>

      {detail.labels && detail.labels.length > 0 && (
        <div>
          <p className="text-xs text-gray-400 mb-2">检测标签</p>
          <div className="flex flex-wrap gap-2">
            {detail.labels.map((label, idx) => (
              <span
                key={idx}
                className={`px-2 py-1 text-xs rounded-lg ${detail.violated ? 'bg-red-500/10 text-red-400' : 'bg-emerald-500/10 text-emerald-400'}`}
              >
                {label}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * 图片内容检测主组件
 * <p>提供完整的图片内容安全检测功能，包括：
 * <ul>
 *   <li>图片上传 - 支持拖拽和点击上传</li>
 *   <li>图片预览 - 上传后预览图片内容</li>
 *   <li>检测配置 - 可选择检测维度和敏感度</li>
 *   <li>AI检测 - 调用多模态AI进行内容分析</li>
 *   <li>结果展示 - 分分类展示检测详情</li>
 *   <li>报告下载 - 支持下载检测报告</li>
 * </ul>
 */
export default function ImageModeration() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>('');
  const [isDragOver, setIsDragOver] = useState<boolean>(false);
  const [detecting, setDetecting] = useState<boolean>(false);
  const [result, setResult] = useState<ImageModerationResult | null>(null);
  const [error, setError] = useState<string>('');

  const [detectPornography, setDetectPornography] = useState<boolean>(true);
  const [detectPolitical, setDetectPolitical] = useState<boolean>(true);
  const [detectViolence, setDetectViolence] = useState<boolean>(true);
  const [detectOther, setDetectOther] = useState<boolean>(true);
  const [sensitivityThreshold, setSensitivityThreshold] = useState<number>(80);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const fileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        const result = reader.result as string;
        const base64 = result.split(',')[1];
        resolve(base64);
      };
      reader.onerror = reject;
    });
  };

  const validateAndSetFile = (file: File) => {
    setError('');
    setResult(null);

    if (file.size > MAX_FILE_SIZE) {
      setError(`文件大小 ${formatFileSize(file.size)} 超过限制（最大 10MB）`);
      return;
    }

    const ext = file.name.split('.').pop()?.toLowerCase();
    const allowedExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'tiff'];
    if (!ext || !allowedExts.includes(ext)) {
      setError(`不支持的图片格式 .${ext}，支持: ${allowedExts.join(', ')}`);
      return;
    }

    setSelectedFile(file);

    const url = URL.createObjectURL(file);
    setPreviewUrl(url);

    console.log('[ImageModeration] 选择图片:', {
      fileName: file.name,
      fileSize: file.size,
      mimeType: file.type
    });
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) validateAndSetFile(file);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) validateAndSetFile(file);
  };

  const handleReset = () => {
    setSelectedFile(null);
    setResult(null);
    setError('');
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl('');
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleDetect = async () => {
    if (!selectedFile) {
      setError('请先选择图片文件');
      return;
    }

    setDetecting(true);
    setError('');
    setResult(null);

    try {
      console.log('[ImageModeration] 开始检测图片内容...');

      const imageBase64 = await fileToBase64(selectedFile);

      const request: ImageModerationRequest = {
        imageBase64,
        fileName: selectedFile.name,
        fileSize: selectedFile.size,
        mimeType: selectedFile.type || 'image/jpeg',
        detectPornography,
        detectPolitical,
        detectViolence,
        detectOther,
        sensitivityThreshold
      };

      const res = await toolsApi.moderateImage(request);

      if (res.code === 200 && res.data) {
        setResult(res.data);
        console.log('[ImageModeration] 检测完成:', {
          taskId: res.data.taskId,
          conclusion: res.data.conclusionLabel,
          hasViolation: res.data.hasViolation,
          overallRiskLevel: res.data.overallRiskLevel,
          maxConfidence: res.data.maxConfidence,
          model: res.data.model,
          tokens: res.data.tokens,
          fallback: res.data.fallback
        });
      } else {
        setError(res.message || '图片内容检测失败');
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || '图片内容检测失败，请检查后端服务是否启动';
      setError(msg);
      console.error('[ImageModeration] 检测失败:', err);
    } finally {
      setDetecting(false);
    }
  };

  const handleDownloadReport = async () => {
    if (!result) return;

    try {
      console.log('[ImageModeration] 下载检测报告...');
      const blob = await toolsApi.downloadModerationReport(result);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const timestamp = new Date().toISOString().slice(0, 10);
      a.download = `图片内容检测报告_${result.taskId.slice(0, 8)}_${timestamp}.txt`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('[ImageModeration] 下载报告失败:', err);
      setError(err.response?.data?.message || err.message || '下载报告失败');
    }
  };

  const getConclusionConfig = () => {
    if (!result) return CONCLUSION_CONFIG.review;
    return CONCLUSION_CONFIG[result.conclusion] || CONCLUSION_CONFIG.review;
  };

  const conclusionConfig = getConclusionConfig();
  const ConclusionIcon = conclusionConfig.icon;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/tools')}
          className="p-2 rounded-xl glass-card hover:bg-white/10 transition-colors"
        >
          <ArrowLeft className="w-5 h-5 text-gray-400" />
        </button>
        <div>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center">
              <ShieldAlert className="w-5 h-5 text-white" />
            </div>
            <h1 className="text-2xl font-bold text-white">图片内容检测</h1>
          </div>
          <p className="text-gray-400 text-sm mt-1 ml-13">
            智能检测图片中的涉黄、涉政、涉爆等违规内容，保障内容安全
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-5">
          <div
            className={`glass-card rounded-2xl p-6 transition-all duration-200 ${isDragOver ? 'border-red-400/50 bg-red-500/5' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <div className="flex items-center gap-2 mb-4">
              <Upload className="w-4 h-4 text-red-400" />
              <span className="text-sm font-medium text-gray-300">上传图片</span>
            </div>

            {!selectedFile ? (
              <div
                className="border-2 border-dashed border-gray-600 rounded-xl p-8 text-center cursor-pointer hover:border-red-400/50 hover:bg-red-500/5 transition-all"
                onClick={() => fileInputRef.current?.click()}
              >
                <Image className="w-12 h-12 text-gray-500 mx-auto mb-3" />
                <p className="text-gray-300 mb-1">点击选择或拖拽图片到此处</p>
                <p className="text-gray-500 text-sm">
                  支持 JPG、PNG、GIF、WebP、BMP、TIFF 等格式
                </p>
                <p className="text-gray-500 text-xs mt-1">最大 10MB</p>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="relative group">
                  <img
                    src={previewUrl}
                    alt="预览"
                    className="w-full h-64 object-contain bg-white/5 rounded-xl"
                  />
                  <button
                    onClick={handleReset}
                    className="absolute top-2 right-2 p-1.5 rounded-lg bg-black/60 hover:bg-red-500/80 transition-colors opacity-0 group-hover:opacity-100"
                    title="移除图片"
                  >
                    <Trash2 className="w-4 h-4 text-white" />
                  </button>
                </div>

                <div className="bg-white/5 rounded-xl p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-lg bg-red-500/20 flex items-center justify-center flex-shrink-0">
                        <Image className="w-5 h-5 text-red-400" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-white text-sm font-medium truncate">{selectedFile.name}</p>
                        <p className="text-gray-400 text-xs mt-0.5">
                          {formatFileSize(selectedFile.size)} · .{selectedFile.name.split('.').pop()}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            <input
              ref={fileInputRef}
              type="file"
              accept={ACCEPTED_EXTENSIONS}
              onChange={handleFileSelect}
              className="hidden"
            />
          </div>

          <div className="glass-card rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <Settings className="w-4 h-4 text-red-400" />
              <span className="text-sm font-medium text-gray-300">检测配置</span>
            </div>

            <div className="space-y-3 mb-6">
              {DETECTION_OPTIONS.map((option) => {
                const stateMap: Record<string, { value: boolean; setter: (val: boolean) => void }> = {
                  detectPornography: { value: detectPornography, setter: setDetectPornography },
                  detectPolitical: { value: detectPolitical, setter: setDetectPolitical },
                  detectViolence: { value: detectViolence, setter: setDetectViolence },
                  detectOther: { value: detectOther, setter: setDetectOther }
                };
                const state = stateMap[option.stateKey];
                return (
                  <label key={option.key} className="flex items-start gap-3 p-3 rounded-xl bg-white/5 hover:bg-white/10 transition-colors cursor-pointer">
                    <input
                      type="checkbox"
                      checked={state.value}
                      onChange={(e) => state.setter(e.target.checked)}
                      className="mt-0.5 w-4 h-4 rounded border-gray-600 bg-white/5 text-red-500 focus:ring-red-500"
                    />
                    <div>
                      <p className="text-white text-sm font-medium">{option.label}</p>
                      <p className="text-gray-500 text-xs">{option.description}</p>
                    </div>
                  </label>
                );
              })}
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-sm text-gray-300">检测敏感度</label>
                <span className="text-sm font-mono text-red-400">{sensitivityThreshold}</span>
              </div>
              <input
                type="range"
                min="50"
                max="100"
                value={sensitivityThreshold}
                onChange={(e) => setSensitivityThreshold(parseInt(e.target.value))}
                className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-red-500"
              />
              <div className="flex justify-between text-xs text-gray-500 mt-1">
                <span>宽松</span>
                <span>严格</span>
              </div>
            </div>
          </div>

          <button
            onClick={handleDetect}
            disabled={!selectedFile || detecting}
            className={`w-full py-3 rounded-xl font-medium transition-all flex items-center justify-center gap-2 ${
              !selectedFile || detecting
                ? 'bg-gray-600/30 text-gray-500 cursor-not-allowed'
                : 'bg-gradient-to-r from-red-500 to-rose-600 text-white hover:shadow-lg hover:shadow-red-500/25'
            }`}
          >
            {detecting ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                正在检测中...
              </>
            ) : (
              <>
                <ShieldAlert className="w-5 h-5" />
                开始检测
              </>
            )}
          </button>
        </div>

        <div className="space-y-5">
          {error && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-4 flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-red-400 text-sm font-medium">检测失败</p>
                <p className="text-red-300/80 text-sm mt-1">{error}</p>
              </div>
            </div>
          )}

          {result && (
            <div className="space-y-5">
              <div className={`glass-card rounded-2xl p-6 border ${result.hasViolation ? 'border-red-500/30 bg-gradient-to-br from-red-500/10 to-rose-500/10' : 'border-emerald-500/30 bg-gradient-to-br from-emerald-500/10 to-teal-500/10'}`}>
                <div className="flex items-center gap-3 mb-4">
                  <div className={`w-10 h-10 rounded-xl ${conclusionConfig.bgColor} flex items-center justify-center`}>
                    <ConclusionIcon className={`w-5 h-5 ${conclusionConfig.color}`} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-white font-bold text-lg">检测完成</h3>
                      <span className={`px-2 py-0.5 text-xs rounded-full ${conclusionConfig.bgColor} ${conclusionConfig.color} font-medium`}>
                        {conclusionConfig.label}
                      </span>
                    </div>
                    <p className="text-gray-400 text-sm">{result.suggestion}</p>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3 mb-4">
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">总体风险</p>
                    <RiskLevelBadge riskLevel={result.overallRiskLevel} />
                  </div>
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">最高置信度</p>
                    <p className="text-white font-bold">{result.maxConfidence}%</p>
                  </div>
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">检测耗时</p>
                    <p className="text-white font-bold">{(result.detectionDurationMs / 1000).toFixed(1)}s</p>
                  </div>
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">图片尺寸</p>
                    <p className="text-white font-bold">{result.width} × {result.height}</p>
                  </div>
                </div>

                {result.allViolationLabels && result.allViolationLabels.length > 0 && (
                  <div className="mb-4">
                    <p className="text-gray-400 text-xs mb-2">违规标签</p>
                    <div className="flex flex-wrap gap-2">
                      {result.allViolationLabels.map((label, idx) => (
                        <span
                          key={idx}
                          className="px-2 py-1 text-xs rounded-lg bg-red-500/10 text-red-400 border border-red-500/20"
                        >
                          {label}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex items-center gap-3 text-xs text-gray-400">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {result.detectionTime}
                  </span>
                  {result.model && (
                    <>
                      <span className="text-gray-600">|</span>
                      <span className={result.fallback ? 'text-amber-400' : 'text-purple-400'}>
                        模型: {result.fallback ? '智能模式(离线)' : result.model}
                      </span>
                    </>
                  )}
                  {result.tokens && (
                    <>
                      <span className="text-gray-600">|</span>
                      <span>Token: {result.tokens}</span>
                    </>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <CategoryResultCard detail={result.pornographyResult} />
                <CategoryResultCard detail={result.politicalResult} />
                <CategoryResultCard detail={result.violenceResult} />
                <CategoryResultCard detail={result.otherResult} />
              </div>

              {result.auditNote && (
                <div className="glass-card rounded-2xl p-5 bg-gradient-to-r from-cyan-500/10 to-blue-500/10 border border-cyan-500/20">
                  <div className="flex items-start gap-3">
                    <Info className="w-5 h-5 text-cyan-400 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="text-cyan-400 font-medium text-sm mb-1">备注说明</p>
                      <p className="text-gray-300 text-sm">{result.auditNote}</p>
                    </div>
                  </div>
                </div>
              )}

              <div className="glass-card rounded-2xl p-5 border border-amber-500/30 bg-gradient-to-br from-amber-500/10 to-orange-500/10">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <p className="text-amber-400 font-medium text-sm mb-1">免责声明</p>
                    <p className="text-gray-400 text-sm whitespace-pre-line">{result.disclaimer}</p>
                  </div>
                </div>
              </div>

              <button
                onClick={handleDownloadReport}
                className="w-full py-3 rounded-xl font-medium transition-all flex items-center justify-center gap-2 btn-secondary bg-gradient-to-r from-purple-600/20 to-pink-600/20 border-purple-500/30 hover:from-purple-600/30 hover:to-pink-600/30"
              >
                <Download className="w-5 h-5" />
                下载检测报告
              </button>
            </div>
          )}

          {!result && !error && !detecting && (
            <div className="glass-card rounded-2xl p-8 text-center">
              <Shield className="w-12 h-12 text-gray-600 mx-auto mb-3" />
              <p className="text-gray-400 mb-2">上传图片开始内容检测</p>
              <p className="text-gray-500 text-sm">
                AI 智能识别图片中的违规内容，支持涉黄、涉政、涉爆等多维度检测
              </p>
              <div className="mt-4 grid grid-cols-2 gap-3 text-left">
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-pink-400 text-xs font-medium mb-1">🔞 涉黄检测</p>
                  <p className="text-gray-500 text-xs">色情、低俗、性感暴露</p>
                </div>
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-red-400 text-xs font-medium mb-1">🏛️ 涉政检测</p>
                  <p className="text-gray-500 text-xs">敏感人物、敏感事件</p>
                </div>
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-orange-400 text-xs font-medium mb-1">💥 涉爆检测</p>
                  <p className="text-gray-500 text-xs">暴力、血腥、武器弹药</p>
                </div>
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-amber-400 text-xs font-medium mb-1">⚠️ 其他违规</p>
                  <p className="text-gray-500 text-xs">赌博、毒品、烟酒广告</p>
                </div>
              </div>
            </div>
          )}

          {detecting && (
            <div className="text-center py-12">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-red-500/20 to-rose-600/20 flex items-center justify-center mx-auto mb-4">
                <Loader2 className="w-10 h-10 text-red-400 animate-spin" />
              </div>
              <h3 className="text-xl font-semibold text-white mb-2">正在检测图片内容</h3>
              <p className="text-gray-400 text-sm">AI 正在分析图片，请耐心等待...</p>
              <div className="mt-4 max-w-xs mx-auto">
                <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                  <div className="h-full rounded-full bg-gradient-to-r from-red-500 to-rose-600 animate-shimmer" style={{ width: '60%' }} />
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
