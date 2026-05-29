import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Camera, 
  ArrowLeft, 
  Upload, 
  Download, 
  Settings2, 
  FileImage,
  CheckCircle,
  AlertCircle,
  Loader2,
  Palette
} from 'lucide-react';
import { toolsApi } from '@/api';
import { PhotoSize, PhotoStandardizationResult } from '@/types';

export default function PhotoStandardization() {
  const navigate = useNavigate();
  const [photoSizes, setPhotoSizes] = useState<PhotoSize[]>([]);
  const [backgrounds, setBackgrounds] = useState<string[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string>('');
  const [selectedSize, setSelectedSize] = useState<string>('');
  const [selectedBackground, setSelectedBackground] = useState<string>('白色');
  const [jpegOutput, setJpegOutput] = useState<boolean>(true);
  const [quality, setQuality] = useState<number>(90);
  const [loading, setLoading] = useState<boolean>(false);
  const [processing, setProcessing] = useState<boolean>(false);
  const [result, setResult] = useState<PhotoStandardizationResult | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    setLoading(true);
    try {
      const [sizesRes, backgroundsRes] = await Promise.all([
        toolsApi.getPhotoSizes(),
        toolsApi.getPhotoBackgrounds()
      ]);
      
      if (sizesRes.code === 200 && sizesRes.data) {
        setPhotoSizes(sizesRes.data);
        if (sizesRes.data.length > 0) {
          setSelectedSize(sizesRes.data[0].code);
        }
      }
      
      if (backgroundsRes.code === 200 && backgroundsRes.data) {
        setBackgrounds(backgroundsRes.data);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '加载配置失败');
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
    };
    reader.readAsDataURL(file);
  };

  const handleProcess = async () => {
    if (!selectedFile) {
      setError('请先选择图片文件');
      return;
    }

    if (!selectedSize) {
      setError('请选择证件照尺寸');
      return;
    }

    setProcessing(true);
    setError('');
    setResult(null);

    try {
      const res = await toolsApi.standardizePhoto({
        file: selectedFile,
        photoSize: selectedSize,
        backgroundColor: selectedBackground,
        jpegOutput,
        quality: jpegOutput ? quality : 100,
        autoDetectFace: false
      });
      
      if (res.code === 200 && res.data) {
        setResult(res.data);
      } else {
        setError(res.message || '处理失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '处理失败，请稍后重试');
    } finally {
      setProcessing(false);
    }
  };

  const handleDownload = async () => {
    if (!selectedFile || !result) return;

    try {
      const blob = await toolsApi.downloadStandardizedPhoto({
        file: selectedFile,
        photoSize: selectedSize,
        backgroundColor: selectedBackground,
        jpegOutput,
        quality: jpegOutput ? quality : 100,
        autoDetectFace: false
      });
      
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

  const getBackgroundColor = (name: string) => {
    switch (name) {
      case '白色': return 'bg-white border-gray-300';
      case '红色': return 'bg-red-200 border-red-400';
      case '蓝色': return 'bg-blue-200 border-blue-400';
      default: return 'bg-white border-gray-300';
    }
  };

  const selectedSizeInfo = photoSizes.find(s => s.code === selectedSize);

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
          <h1 className="text-3xl font-bold text-white mb-1">证件照规范化</h1>
          <p className="text-gray-400">调整尺寸、更换背景色，生成符合规范的证件照</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Upload className="w-5 h-5 text-cyan-400" />
              上传照片
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
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-600/20 flex items-center justify-center mx-auto group-hover:scale-110 transition-transform">
                      <FileImage className="w-8 h-8 text-cyan-400" />
                    </div>
                    <p className="text-white font-medium">点击或拖拽上传照片</p>
                    <p className="text-gray-400 text-sm">支持 JPG、PNG 等常见格式</p>
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
              处理设置
            </h2>
            <div className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-3">
                  证件照尺寸
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {photoSizes.map((size) => (
                    <button
                      key={size.code}
                      onClick={() => setSelectedSize(size.code)}
                      className={`p-3 rounded-xl text-left transition-all ${
                        selectedSize === size.code
                          ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg'
                          : 'bg-white/5 text-gray-300 hover:bg-white/10'
                      }`}
                    >
                      <div className="font-bold text-sm">{size.name}</div>
                      <div className="text-xs opacity-75 mt-0.5">
                        {size.widthMm}×{size.heightMm}mm
                      </div>
                      <div className="text-xs opacity-60 mt-0.5">
                        {size.usage}
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-3">
                  背景颜色
                </label>
                <div className="grid grid-cols-3 gap-3">
                  {backgrounds.map((bg) => (
                    <button
                      key={bg}
                      onClick={() => setSelectedBackground(bg)}
                      className={`p-4 rounded-xl transition-all ${
                        selectedBackground === bg
                          ? 'ring-2 ring-cyan-400 shadow-lg'
                          : 'hover:ring-2 hover:ring-white/20'
                      } ${getBackgroundColor(bg)}`}
                    >
                      <div className="text-sm font-medium text-gray-700 text-center">
                        {bg}
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  输出格式
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={() => setJpegOutput(true)}
                    className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                      jpegOutput
                        ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg'
                        : 'bg-white/5 text-gray-300 hover:bg-white/10'
                    }`}
                  >
                    JPEG
                  </button>
                  <button
                    onClick={() => setJpegOutput(false)}
                    className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                      !jpegOutput
                        ? 'bg-gradient-to-br from-cyan-500 to-blue-600 text-white shadow-lg'
                        : 'bg-white/5 text-gray-300 hover:bg-white/10'
                    }`}
                  >
                    PNG
                  </button>
                </div>
              </div>

              {jpegOutput && (
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    输出质量: {quality}%
                  </label>
                  <input
                    type="range"
                    min="50"
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

              {selectedSizeInfo && (
                <div className="bg-white/5 rounded-xl p-4">
                  <div className="flex items-center gap-2 text-cyan-400 mb-2">
                    <Camera className="w-4 h-4" />
                    <span className="text-sm font-medium">尺寸信息</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <div>
                      <span className="text-gray-400">像素尺寸：</span>
                      <span className="text-white">{selectedSizeInfo.widthPx} × {selectedSizeInfo.heightPx} px</span>
                    </div>
                    <div>
                      <span className="text-gray-400">打印尺寸：</span>
                      <span className="text-white">{selectedSizeInfo.widthMm} × {selectedSizeInfo.heightMm} mm</span>
                    </div>
                    <div>
                      <span className="text-gray-400">分辨率：</span>
                      <span className="text-white">300 DPI</span>
                    </div>
                    <div>
                      <span className="text-gray-400">用途：</span>
                      <span className="text-white">{selectedSizeInfo.usage}</span>
                    </div>
                  </div>
                </div>
              )}

              <button
                onClick={handleProcess}
                disabled={!selectedFile || !selectedSize || processing || loading}
                className="btn-primary w-full flex items-center justify-center gap-2"
              >
                {processing ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    处理中...
                  </>
                ) : (
                  <>
                    <Camera className="w-5 h-5" />
                    开始处理
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
                  处理完成
                </h2>
                <div className="space-y-4">
                  <img
                    src={result.imageDataBase64}
                    alt="处理结果"
                    className="max-h-72 mx-auto rounded-xl bg-white/5 p-2"
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">原始大小</p>
                      <p className="text-white font-bold text-lg">
                        {formatFileSize(result.originalSize)}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">处理后</p>
                      <p className="text-white font-bold text-lg">
                        {formatFileSize(result.convertedSize)}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">尺寸</p>
                      <p className="text-white font-bold">
                        {result.width} × {result.height} px
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">规格</p>
                      <p className="text-white font-bold">
                        {result.photoSize} · {result.dpi}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">背景色</p>
                      <p className="text-white font-bold">
                        {result.backgroundColor}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">格式</p>
                      <p className="text-white font-bold uppercase">
                        {result.mimeType.split('/')[1]}
                      </p>
                    </div>
                  </div>
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
                <Camera className="w-10 h-10 text-cyan-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待处理</h3>
              <p className="text-gray-400 text-sm">
                上传照片并设置参数后，点击"开始处理"
              </p>
            </div>
          )}

          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Palette className="w-5 h-5 text-cyan-400" />
              背景颜色说明
            </h2>
            <div className="space-y-3">
              <div className="bg-white/5 rounded-xl p-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-white border-2 border-gray-300"></div>
                  <div>
                    <p className="text-white font-medium">白色背景</p>
                    <p className="text-gray-400 text-xs mt-0.5">
                      身份证、签证、驾驶证、社保卡等
                    </p>
                  </div>
                </div>
              </div>
              <div className="bg-white/5 rounded-xl p-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-red-200 border-2 border-red-400"></div>
                  <div>
                    <p className="text-white font-medium">红色背景</p>
                    <p className="text-gray-400 text-xs mt-0.5">
                      结婚证、部分护照照片、部分证件照等
                    </p>
                  </div>
                </div>
              </div>
              <div className="bg-white/5 rounded-xl p-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-blue-200 border-2 border-blue-400"></div>
                  <div>
                    <p className="text-white font-medium">蓝色背景</p>
                    <p className="text-gray-400 text-xs mt-0.5">
                      护照、签证、毕业证、学历证书、社保卡等
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
