import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Mic,
  ArrowLeft,
  Upload,
  FileAudio,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Copy,
  Check,
  Download,
  Info,
  Volume2,
  Trash2,
  Languages
} from 'lucide-react';
import { toolsApi } from '@/api';
import { AudioFormatInfo, SpeechToTextResult } from '@/types';

const LANGUAGE_OPTIONS = [
  { code: 'zh', label: '中文（普通话）', icon: '🇨🇳' },
  { code: 'en', label: 'English', icon: '🇺🇸' },
  { code: 'ja', label: '日本語', icon: '🇯🇵' },
  { code: 'ko', label: '한국어', icon: '🇰🇷' },
  { code: 'fr', label: 'Français', icon: '🇫🇷' },
  { code: 'de', label: 'Deutsch', icon: '🇩🇪' },
  { code: 'es', label: 'Español', icon: '🇪🇸' },
  { code: 'ru', label: 'Русский', icon: '🇷🇺' },
];

const ACCEPTED_EXTENSIONS = '.mp3,.wav,.m4a,.aac,.ogg,.flac,.amr,.wma,.webm';
const MAX_FILE_SIZE = 25 * 1024 * 1024;

export default function SpeechToText() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [formats, setFormats] = useState<AudioFormatInfo[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [language, setLanguage] = useState<string>('zh');
  const [transcribing, setTranscribing] = useState<boolean>(false);
  const [result, setResult] = useState<SpeechToTextResult | null>(null);
  const [error, setError] = useState<string>('');
  const [copied, setCopied] = useState<boolean>(false);
  const [isDragOver, setIsDragOver] = useState<boolean>(false);

  useEffect(() => {
    fetchFormats();
  }, []);

  const fetchFormats = async () => {
    try {
      const res = await toolsApi.getAudioFormats();
      if (res.code === 200 && res.data) {
        setFormats(res.data);
      }
    } catch (err: any) {
      console.error('[SpeechToText] 加载格式列表失败:', err);
    }
  };

  const validateAndSetFile = (file: File) => {
    setError('');
    setResult(null);

    if (file.size > MAX_FILE_SIZE) {
      setError(`文件大小 ${(file.size / 1024 / 1024).toFixed(1)}MB 超过限制（最大 25MB）`);
      return;
    }

    const ext = file.name.split('.').pop()?.toLowerCase();
    const allowedExts = ['mp3', 'wav', 'm4a', 'aac', 'ogg', 'flac', 'amr', 'wma', 'webm'];
    if (!ext || !allowedExts.includes(ext)) {
      setError(`不支持的音频格式 .${ext}，支持: ${allowedExts.join(', ')}`);
      return;
    }

    setSelectedFile(file);
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

  const handleTranscribe = async () => {
    if (!selectedFile) {
      setError('请先选择音频文件');
      return;
    }

    setTranscribing(true);
    setError('');
    setResult(null);

    try {
      console.log('[SpeechToText] 开始转写:', {
        fileName: selectedFile.name,
        fileSize: selectedFile.size,
        language
      });

      const res = await toolsApi.speechToText(selectedFile, language);

      if (res.code === 200 && res.data) {
        setResult(res.data);
        console.log('[SpeechToText] 转写完成:', {
          textLength: res.data.text?.length,
          detectedLanguage: res.data.detectedLanguage,
          duration: res.data.duration
        });
      } else {
        setError(res.message || '语音转文字失败');
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || '语音转文字失败，请检查后端服务是否启动';
      setError(msg);
      console.error('[SpeechToText] 转写失败:', err);
    } finally {
      setTranscribing(false);
    }
  };

  const handleCopy = async () => {
    if (!result?.text) return;
    try {
      await navigator.clipboard.writeText(result.text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (e) {
      console.error('[SpeechToText] 复制失败:', e);
    }
  };

  const handleDownload = () => {
    if (!result?.text) return;
    const blob = new Blob([result.text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const baseName = result.originalFileName?.replace(/\.[^/.]+$/, '') || 'transcription';
    a.download = `${baseName}_转写结果.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleReset = () => {
    setSelectedFile(null);
    setResult(null);
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const formatDuration = (seconds: number): string => {
    if (seconds < 60) return `${seconds.toFixed(1)} 秒`;
    const min = Math.floor(seconds / 60);
    const sec = Math.floor(seconds % 60);
    return `${min} 分 ${sec} 秒`;
  };

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
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center">
              <Mic className="w-5 h-5 text-white" />
            </div>
            <h1 className="text-2xl font-bold text-white">语音转文字</h1>
          </div>
          <p className="text-gray-400 text-sm mt-1 ml-13">
            上传手机录音文件，AI 智能识别语音内容，转为可编辑文字
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-5">
          <div
            className={`glass-card rounded-2xl p-6 transition-all duration-200 ${
              isDragOver ? 'border-green-400/50 bg-green-500/5' : ''
            }`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <div className="flex items-center gap-2 mb-4">
              <Upload className="w-4 h-4 text-green-400" />
              <span className="text-sm font-medium text-gray-300">上传音频文件</span>
            </div>

            {!selectedFile ? (
              <div
                className="border-2 border-dashed border-gray-600 rounded-xl p-8 text-center cursor-pointer hover:border-green-400/50 hover:bg-green-500/5 transition-all"
                onClick={() => fileInputRef.current?.click()}
              >
                <FileAudio className="w-12 h-12 text-gray-500 mx-auto mb-3" />
                <p className="text-gray-300 mb-1">点击选择或拖拽音频文件到此处</p>
                <p className="text-gray-500 text-sm">
                  支持 MP3、WAV、M4A、AAC、OGG、FLAC、AMR 等格式
                </p>
                <p className="text-gray-500 text-xs mt-1">最大 25MB</p>
              </div>
            ) : (
              <div className="bg-white/5 rounded-xl p-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-3 min-w-0">
                    <div className="w-10 h-10 rounded-lg bg-green-500/20 flex items-center justify-center flex-shrink-0">
                      <Volume2 className="w-5 h-5 text-green-400" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-white text-sm font-medium truncate">{selectedFile.name}</p>
                      <p className="text-gray-400 text-xs mt-0.5">
                        {formatFileSize(selectedFile.size)} · .{selectedFile.name.split('.').pop()}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={handleReset}
                    className="p-1.5 rounded-lg hover:bg-white/10 transition-colors flex-shrink-0"
                    title="移除文件"
                  >
                    <Trash2 className="w-4 h-4 text-gray-400" />
                  </button>
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
              <Languages className="w-4 h-4 text-green-400" />
              <span className="text-sm font-medium text-gray-300">识别语言</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              {LANGUAGE_OPTIONS.map(lang => (
                <button
                  key={lang.code}
                  onClick={() => setLanguage(lang.code)}
                  className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-all ${
                    language === lang.code
                      ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                      : 'bg-white/5 text-gray-400 border border-transparent hover:bg-white/10'
                  }`}
                >
                  <span>{lang.icon}</span>
                  <span>{lang.label}</span>
                </button>
              ))}
            </div>
          </div>

          {formats.length > 0 && (
            <div className="glass-card rounded-2xl p-6">
              <div className="flex items-center gap-2 mb-3">
                <Info className="w-4 h-4 text-green-400" />
                <span className="text-sm font-medium text-gray-300">支持的音频格式</span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {formats.map(fmt => (
                  <div
                    key={fmt.extension}
                    className="bg-white/5 rounded-lg px-2 py-1.5 text-center"
                  >
                    <span className="text-xs text-gray-400">.{fmt.extension}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <button
            onClick={handleTranscribe}
            disabled={!selectedFile || transcribing}
            className={`w-full py-3 rounded-xl font-medium transition-all flex items-center justify-center gap-2 ${
              !selectedFile || transcribing
                ? 'bg-gray-600/30 text-gray-500 cursor-not-allowed'
                : 'bg-gradient-to-r from-green-500 to-emerald-600 text-white hover:shadow-lg hover:shadow-green-500/25'
            }`}
          >
            {transcribing ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                正在转写中...
              </>
            ) : (
              <>
                <Mic className="w-5 h-5" />
                开始转写
              </>
            )}
          </button>
        </div>

        <div className="space-y-5">
          {error && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-xl p-4 flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-red-400 text-sm font-medium">转写失败</p>
                <p className="text-red-300/80 text-sm mt-1">{error}</p>
              </div>
            </div>
          )}

          {result && (
            <div className="glass-card rounded-2xl p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-5 h-5 text-green-400" />
                  <span className="text-sm font-medium text-gray-300">转写结果</span>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleCopy}
                    className="p-2 rounded-lg hover:bg-white/10 transition-colors"
                    title="复制文字"
                  >
                    {copied ? (
                      <Check className="w-4 h-4 text-green-400" />
                    ) : (
                      <Copy className="w-4 h-4 text-gray-400" />
                    )}
                  </button>
                  <button
                    onClick={handleDownload}
                    className="p-2 rounded-lg hover:bg-white/10 transition-colors"
                    title="下载为文本文件"
                  >
                    <Download className="w-4 h-4 text-gray-400" />
                  </button>
                </div>
              </div>

              <div className="bg-white/5 rounded-xl p-4 mb-4">
                <pre className="text-gray-200 text-sm whitespace-pre-wrap break-words leading-relaxed font-sans">
                  {result.text}
                </pre>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {result.detectedLanguage && (
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">识别语言</p>
                    <p className="text-gray-300 text-sm">{result.detectedLanguage}</p>
                  </div>
                )}
                {result.duration != null && result.duration > 0 && (
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">音频时长</p>
                    <p className="text-gray-300 text-sm">{formatDuration(result.duration)}</p>
                  </div>
                )}
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-gray-500 text-xs mb-1">文件大小</p>
                  <p className="text-gray-300 text-sm">{formatFileSize(result.originalSize)}</p>
                </div>
                {result.text && (
                  <div className="bg-white/5 rounded-lg p-3">
                    <p className="text-gray-500 text-xs mb-1">字数</p>
                    <p className="text-gray-300 text-sm">{result.text.length} 字</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {!result && !error && (
            <div className="glass-card rounded-2xl p-8 text-center">
              <Mic className="w-12 h-12 text-gray-600 mx-auto mb-3" />
              <p className="text-gray-400 mb-2">上传音频文件开始转写</p>
              <p className="text-gray-500 text-sm">
                支持手机录音、会议录音等常见音频格式，AI 自动识别语音内容
              </p>
              <div className="mt-4 grid grid-cols-2 gap-3 text-left">
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-green-400 text-xs font-medium mb-1">📱 手机录音</p>
                  <p className="text-gray-500 text-xs">iPhone M4A、Android AAC</p>
                </div>
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-green-400 text-xs font-medium mb-1">🎙️ 会议录音</p>
                  <p className="text-gray-500 text-xs">MP3、WAV 等常见格式</p>
                </div>
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-green-400 text-xs font-medium mb-1">💬 通话录音</p>
                  <p className="text-gray-500 text-xs">AMR 等通话录音格式</p>
                </div>
                <div className="bg-white/5 rounded-lg p-3">
                  <p className="text-green-400 text-xs font-medium mb-1">🌐 多语言</p>
                  <p className="text-gray-500 text-xs">中文、英文、日文等 8 种语言</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
