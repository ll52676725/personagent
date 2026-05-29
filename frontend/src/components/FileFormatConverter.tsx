import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FileText,
  ArrowLeft,
  Upload,
  Download,
  Settings2,
  File,
  CheckCircle,
  AlertCircle,
  Loader2
} from 'lucide-react';
import { toolsApi } from '@/api';
import { FileFormatInfo, FileConvertResult } from '@/types';

export default function FileFormatConverter() {
  const navigate = useNavigate();
  const [formats, setFormats] = useState<FileFormatInfo[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [targetFormat, setTargetFormat] = useState<string>('pdf');
  const [loading, setLoading] = useState<boolean>(false);
  const [converting, setConverting] = useState<boolean>(false);
  const [result, setResult] = useState<FileConvertResult | null>(null);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    fetchFormats();
  }, []);

  /**
   * 获取支持的文件格式列表
   */
  const fetchFormats = async () => {
    setLoading(true);
    try {
      const res = await toolsApi.getFileFormats();
      if (res.code === 200 && res.data) {
        setFormats(res.data.filter(f => f.writable));
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '加载格式列表失败');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 处理文件选择
   */
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setSelectedFile(file);
    setResult(null);
    setError('');
  };

  /**
   * 执行文件转换
   */
  const handleConvert = async () => {
    if (!selectedFile) {
      setError('请先选择要转换的文件');
      return;
    }

    setConverting(true);
    setError('');
    setResult(null);

    try {
      const res = await toolsApi.convertFile(selectedFile, targetFormat);
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

  /**
   * 下载转换后的文件
   */
  const handleDownload = async () => {
    if (!selectedFile || !result) return;

    try {
      const blob = await toolsApi.downloadConvertedFile(selectedFile, targetFormat);
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

  /**
   * 格式化文件大小显示
   */
  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  /**
   * 获取文件图标和颜色
   */
  const getFileIcon = (fileName: string) => {
    const ext = fileName.split('.').pop()?.toLowerCase();
    const colors: Record<string, string> = {
      pdf: 'text-red-400',
      doc: 'text-blue-400',
      docx: 'text-blue-400',
      xls: 'text-green-400',
      xlsx: 'text-green-400',
      csv: 'text-yellow-400',
      txt: 'text-gray-400',
      html: 'text-orange-400',
    };
    return colors[ext || ''] || 'text-gray-400';
  };

  const selectedFormatInfo = formats.find(f => f.formatName === targetFormat);

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
        <div>
          <h1 className="text-3xl font-bold text-white mb-1">文件格式转换</h1>
          <p className="text-gray-400">支持多种文档格式的互相转换</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* 左侧：上传和设置区域 */}
        <div className="space-y-6">
          {/* 文件上传区域 */}
          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Upload className="w-5 h-5 text-emerald-400" />
              上传文件
            </h2>
            <label className="block">
              <div className="border-2 border-dashed border-white/10 rounded-2xl p-8 text-center cursor-pointer hover:border-emerald-400/50 hover:bg-white/5 transition-all group">
                {selectedFile ? (
                  <div className="space-y-4">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500/20 to-teal-600/20 flex items-center justify-center mx-auto">
                      <FileText className={`w-8 h-8 ${getFileIcon(selectedFile.name)}`} />
                    </div>
                    <p className="text-white font-medium">{selectedFile.name}</p>
                    <p className="text-gray-400 text-sm">
                      {formatFileSize(selectedFile.size)}
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500/20 to-teal-600/20 flex items-center justify-center mx-auto group-hover:scale-110 transition-transform">
                      <File className="w-8 h-8 text-emerald-400" />
                    </div>
                    <p className="text-white font-medium">点击或拖拽上传文件</p>
                    <p className="text-gray-400 text-sm">支持 Word(doc/docx)、PDF、TXT、Excel(xls/xlsx) 等格式</p>
                  </div>
                )}
              </div>
              <input
                type="file"
                accept=".pdf,.doc,.docx,.txt,.html,.xls,.xlsx,.csv"
                onChange={handleFileSelect}
                className="hidden"
              />
            </label>
          </div>

          {/* 转换设置区域 */}
          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Settings2 className="w-5 h-5 text-emerald-400" />
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
                          ? 'bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg'
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
                    <FileText className="w-5 h-5" />
                    开始转换
                  </>
                )}
              </button>
            </div>
          </div>

          {/* 支持的转换类型说明 */}
          <div className="glass-card rounded-3xl p-6">
            <h2 className="text-lg font-bold text-white mb-4">支持的转换类型</h2>
            <div className="space-y-3 text-sm">
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded-lg text-xs font-medium shrink-0">Word</span>
                <p className="text-gray-400">doc/docx → PDF、TXT、HTML</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-red-500/20 text-red-400 rounded-lg text-xs font-medium shrink-0">PDF</span>
                <p className="text-gray-400">PDF → TXT</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-gray-500/20 text-gray-400 rounded-lg text-xs font-medium shrink-0">文本</span>
                <p className="text-gray-400">TXT → PDF</p>
              </div>
              <div className="flex items-start gap-3">
                <span className="px-2 py-1 bg-green-500/20 text-green-400 rounded-lg text-xs font-medium shrink-0">Excel</span>
                <p className="text-gray-400">xls/xlsx → CSV</p>
              </div>
            </div>
          </div>
        </div>

        {/* 右侧：结果显示区域 */}
        <div className="space-y-6">
          {/* 错误提示 */}
          {error && (
            <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
              <div className="flex items-center gap-3">
                <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
                <p className="text-red-300">{error}</p>
              </div>
            </div>
          )}

          {/* 转换成功结果 */}
          {result && (
            <div className="space-y-6">
              <div className="glass-card rounded-3xl p-6">
                <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
                  <CheckCircle className="w-5 h-5 text-emerald-400" />
                  转换完成
                </h2>
                <div className="space-y-4">
                  {/* 文件图标 */}
                  <div className="flex items-center justify-center py-8">
                    <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-emerald-500/20 to-teal-600/20 flex items-center justify-center">
                      <FileText className={`w-10 h-10 ${getFileIcon(result.convertedFileName)}`} />
                    </div>
                  </div>

                  {/* 转换信息 */}
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
                      <p className="text-gray-400 text-sm mb-1">原始格式</p>
                      <p className="text-white font-bold uppercase">
                        {result.originalFormat}
                      </p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-4">
                      <p className="text-gray-400 text-sm mb-1">目标格式</p>
                      <p className="text-white font-bold uppercase">
                        {result.targetFormat}
                      </p>
                    </div>
                  </div>

                  {/* 下载按钮 */}
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

          {/* 等待转换提示 */}
          {!result && !error && (
            <div className="glass-card rounded-3xl p-8 text-center">
              <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-emerald-500/10 to-teal-600/10 flex items-center justify-center mx-auto mb-4">
                <FileText className="w-10 h-10 text-emerald-400" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">等待转换</h3>
              <p className="text-gray-400 text-sm">
                上传文件并选择目标格式后，点击"开始转换"
              </p>
            </div>
          )}

          {/* 支持的格式列表 */}
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
