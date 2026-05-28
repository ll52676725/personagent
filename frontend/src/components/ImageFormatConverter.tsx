import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Image as ImageIcon, 
  ArrowLeft, 
  Upload, 
  Download, 
  Settings2, 
  FileImage,
  CheckCircle,
  AlertCircle,
  Loader2
} from 'lucide-react';
import { toolsApi } from '@/api';
import { ImageFormatInfo, ImageConvertResult } from '@/types';

export default function ImageFormatConverter() {
  const navigate = useNavigate();
  const [formats, setFormats] = useState<ImageFormatInfo[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>('');
  const [targetFormat, setTargetFormat] = useState<string>('png');
  const [quality, setQuality] = useState<number>(85);
  const [width, setWidth] = useState<number | ''>('');
  const [height, setHeight] = useState<number | ''>('');
  const [keepAspectRatio, setKeepAspectRatio] = useState<boolean>(true);
  const [originalDimensions, setOriginalDimensions] = useState<{ width: number; height: number } | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [converting, setConverting] = useState<boolean>(false);
  const [result, setResult] = useState<ImageConvertResult | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    fetchFormats();
  }, []);

  const fetchFormats = async () => {
    setLoading(true);
    try {
      const res = await toolsApi.getImageFormats();
      if (res.code === 200 && res.data) {
        setFormats(res.data.filter(f => f.writable));
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '加载格式列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('请选择图片文件');
      return;
    }

    setSelectedFile(file);
    setResult(null);
    setError('');

    const reader = new FileReader();
    reader.onload = (ev) => {
      const dataUrl = ev.target?.result as string;
      setPreviewUrl(dataUrl);

      const img = new window.Image();
      img.onload = () => {
        setOriginalDimensions({ width: img.width, height: img.height });
      };
      img.src = dataUrl;
    };
    reader.readAsDataURL(file);
  };

  const handleWidthChange = useCallback((value: number | '') => {
    setWidth(value);
    if (keepAspectRatio && value !== '' && originalDimensions) {
      const aspectRatio = originalDimensions.height / originalDimensions.width;
      setHeight(Math.round(value * aspectRatio));
    }
  }, [keepAspectRatio, originalDimensions]);

  const handleHeightChange = useCallback((value: number | '') => {
    setHeight(value);
    if (keepAspectRatio && value !== '' && originalDimensions) {
      const aspectRatio = originalDimensions.width / originalDimensions.height;
      setWidth(Math.round(value * aspectRatio));
    }
  }, [keepAspectRatio, originalDimensions]);

  const handleConvert = async () => {
    if (!selectedFile) {
      setError('请先选择图片文件');
      return;
    }

    setConverting(true);
    setError('');
    setResult(null);

    try {
      const res = await toolsApi.convertImage(
        selectedFile,
        targetFormat,
        quality / 100,
        width || undefined,
        height || undefined
      );
      if (res.code === 200 && res.data) {
        setResult(res.data);
      } else {
        setError(res.message || '转换失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '转换失败，请稍后重试');
    } finally {
      setConverting(false);
    }
  };

  const handleDownload = async () => {
    if (!selectedFile || !result) return;

    try {
      const blob = await toolsApi.downloadConvertedImage(
        selectedFile,
        targetFormat,
        quality / 100,
        width || undefined,
        height || undefined
      );
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = result.convertedFileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err: any) {
      setError('下载失败，请稍后重试');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const calculateSavings = () => {
    if (!result) return null;
    const savings = result.originalSize - result.convertedSize;
    const percentage = ((savings / result.originalSize) * 100).toFixed(1);
    return { savings, percentage };
  };

  const selectedFormatInfo = formats.find(f => f.formatName === targetFormat);
  const isLossyFormat = ['jpg', 'jpeg', 'webp'].includes(targetFormat.toLowerCase());
  const savings = calculateSavings();

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
          <h1 className="text-3xl font-bold text-white mb-1">图片格式转换</h1>
          <p className="text-gray-400">支持多种主流图片格式的互相转换</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Upload className="w-5 h-5 text-cyan-400" />
              上传图片
            </h2>
            <label className="block">
              <div className="border-2 border-dashed border-white/10 rounded-2xl p-8 text-center cursor-pointer hover:border-cyan-400/50 hover:bg-white/5 transition-all group">
                {previewUrl ? (
                  <div className="space-y-4">
                    <img
                      src={previewUrl}
                      alt="预览"
                      className="max-h-64 mx-auto rounded-xl"
                    />
                    <p className="text-white font-medium">{selectedFile?.name}</p>
                    <p className="text-gray-400 text-sm">
                      {selectedFile && formatFileSize(selectedFile.size)}
                      {originalDimensions && ` · ${originalDimensions.width} × ${originalDimensions.height}`}
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-600/20 flex items-center justify-center mx-auto group-hover:scale-110 transition-transform">
                      <FileImage className="w-8 h-8 text-cyan-400" />
                    </div>
                    <p className="text-white font-medium">点击或拖拽上传图片</p>
                    <p className="text-gray-400 text-sm">支持 JPG、PNG、GIF、BMP、WebP、TIFF 等格式</p>
                  </div>
                )}
              </div>
              <input
                type="file"
                accept="image/*"
                onChange={handleFileSelect}
                className="hidden"
              />
            </label>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Settings2 className="w-5 h-5 text-cyan-400" />
              转换设置
            </h2>
            <div className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  目标格式
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  {formats.filter(f => f.writable).map((format) => (
                    <button
                      key={format.formatName}
                      onClick={() => setTargetFormat(format.formatName)}
                      className={`px-3 py-2 rounded-xl text-sm font-medium transition-all ${
                        targetFormat === format.formatName
                          ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg'
                          : 'bg-white/5 text-gray-300 hover:bg-white/10'
                      }`}
                    >
                      {format.formatName.toUpperCase()}
                    </button>
                  ))}
                </div>
                {selectedFormatInfo && (
                  <p className="mt-2 text-sm text-gray-400">
                    {selectedFormatInfo.description}
                  </p>
                )}
              </div>

              {isLossyFormat && (
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    压缩质量: {quality}%
                  </label>
                  <input
                    type="range"
                    min="10"
                    max="100"
                    value={quality}
                    onChange={(e) => setQuality(Number(e.target.value))}
                    className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-cyan-400"
                  />
                  <div className="flex justify-between text-xs text-gray-500 mt-1">
                    <span>小文件</span>
                    <span>高质量</span>
                  </div>
                </div>
              )}

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-sm font-medium text-gray-300">
                    调整尺寸
                  </label>
                  <label className="flex items-center gap-2 text-sm text-gray-400 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={keepAspectRatio}
                      onChange={(e) => setKeepAspectRatio(e.target.checked)}
                      className="rounded accent-cyan-400"
                    />
                    保持宽高比
                  </label>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">宽度 (px)</label>
                    <input
                      type="number"
                      placeholder={originalDimensions?.width.toString() || '原始宽度'}
                      value={width}
                      onChange={(e) => handleWidthChange(e.target.value ? Number(e.target.value) : '')}
                      className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">高度 (px)</label>
                    <input
                      type="number"
                      placeholder={originalDimensions?.height.toString() || '原始高度'}
                      value={height}
                      onChange={(e) => handleHeightChange(e.target.value ? Number(e.target.value) : '')}
                      className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
                    />
                  </div>
                </div>
              </div>

              <button
                onClick={handleConvert}
                disabled={!selectedFile || converting || loading}
                className="btn-primary w-full flex items-center justify-center gap-2"
              >
                {converting ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    转换中...
                  </>
                ) : (
                  <>
                    <ImageIcon className="w-5 h-5" />
                    开始转换
                  </>
                )}
              </button>
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

          {result && (
            <div className="space-y-6">
              <div className="glass-card rounded-3xl p-6">
                <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                  <CheckCircle className="w-5 h-5 text-emerald-400" />
                  转换完成
                </h2>
                <div className="space-y-4">
                  <img
                    src={result.imageDataBase64}
                    alt="转换结果"
                    className="max-h-64 mx-auto rounded-xl bg-white/5 p-2"
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">原始大小</p>
                      <p className="text-white font-bold text-lg">
                        {formatFileSize(result.originalSize)}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">转换后</p>
                      <p className="text-white font-bold text-lg">
                        {formatFileSize(result.convertedSize)}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">尺寸</p>
                      <p className="text-white font-bold">
                        {result.width} × {result.height}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">格式</p>
                      <p className="text-white font-bold uppercase">
                        {result.targetFormat}
                      </p>
                    </div>
                  </div>
                  {savings && savings.savings > 0 && (
                    <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-4">
                      <div className="flex items-center gap-2">
                        <CheckCircle className="w-5 h-5 text-emerald-400" />
                        <span className="text-emerald-300 font-medium">
                          文件大小减少 {savings.percentage}% ({formatFileSize(savings.savings)})
                        </span>
                      </div>
                    </div>
                  )}
                  <button
                    onClick={handleDownload}
                    className="btn-primary w-full flex items-center justify-center gap-2"
                  >
                    <Download className="w-5 h-5" />
                    下载 {result.convertedFileName}
                  </button>
                </div>
              </div>
            </div>
          )}

          {!result && !error && (
            <div className="glass-card rounded-3xl p-8 text-center">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/10 to-blue-600/10 flex items-center justify-center mx-auto mb-4">
                <ImageIcon className="w-10 h-10 text-cyan-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待转换</h3>
              <p className="text-gray-400 text-sm">
                上传图片并设置转换参数后，点击"开始转换"
              </p>
            </div>
          )}

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4">支持的格式</h2>
            <div className="space-y-2 max-h-80 overflow-y-auto scrollbar-thin">
              {formats.map((format) => (
                <div
                  key={format.formatName}
                  className="flex items-center justify-between p-3 rounded-xl hover:bg-white/5 transition-colors"
                >
                  <div>
                    <span className="text-white font-medium uppercase">
                      {format.formatName}
                    </span>
                    <p className="text-gray-400 text-xs mt-0.5">
                      {format.description}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {format.readable && (
                      <span className="px-2 py-0.5 text-xs bg-emerald-500/20 text-emerald-400 rounded-full">
                        可读
                      </span>
                    )}
                    {format.writable && (
                      <span className="px-2 py-0.5 text-xs bg-cyan-500/20 text-cyan-400 rounded-full">
                        可写
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
