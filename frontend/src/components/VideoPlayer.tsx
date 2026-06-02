import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Play,
  Pause,
  Volume2,
  VolumeX,
  Maximize,
  Minimize,
  SkipBack,
  SkipForward,
  Upload,
  MonitorPlay,
  Zap,
  Keyboard,
  ChevronUp,
  ChevronDown,
  Gauge,
  X,
  Ear,
  EarOff,
  FileVideo,
  Download,
  AlertCircle,
  CheckCircle,
  Loader2,
  RefreshCw,
  FileText
} from 'lucide-react';
import {
  getVideoConverter,
  SUPPORTED_FORMATS,
  VideoFormat,
  ConvertProgress,
  ConvertResult,
  LogEntry,
  formatFileSize,
  formatDuration,
  isWasmSupported,
  isSharedArrayBufferSupported,
  isCrossOriginIsolated
} from '@/utils/videoConverter';
import type { VideoConverter } from '@/utils/videoConverter';

const PRESET_SPEEDS = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 2, 2.5, 3, 4];

const SPEED_STEP = 0.25;

function formatTime(seconds: number): string {
  if (!isFinite(seconds) || isNaN(seconds)) return '0:00';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function VideoPlayer() {
  const navigate = useNavigate();
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const progressRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [videoSrc, setVideoSrc] = useState<string>('');
  const [videoName, setVideoName] = useState<string>('');
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [buffered, setBuffered] = useState(0);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [showShortcuts, setShowShortcuts] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [dragTime, setDragTime] = useState(0);
  const [isDragOver, setIsDragOver] = useState(false);
  const [denoiseEnabled, setDenoiseEnabled] = useState(false);
  const [denoiseStrength, setDenoiseStrength] = useState<'light' | 'medium' | 'heavy'>('medium');

  // 视频转换相关状态
  const [showConvertPanel, setShowConvertPanel] = useState(false);
  const [targetFormat, setTargetFormat] = useState<VideoFormat>('mp4');
  const [convertQuality, setConvertQuality] = useState<number>(23);
  const [convertVideoBitrate, setConvertVideoBitrate] = useState<string>('');
  const [convertAudioBitrate, setConvertAudioBitrate] = useState<string>('');
  const [convertResolution, setConvertResolution] = useState<string>('');
  const [convertFps, setConvertFps] = useState<number | ''>('');
  const [isConverting, setIsConverting] = useState(false);
  const [convertProgress, setConvertProgress] = useState<ConvertProgress | null>(null);
  const [convertResult, setConvertResult] = useState<ConvertResult | null>(null);
  const [convertError, setConvertError] = useState<string>('');
  const [converterLogs, setConverterLogs] = useState<LogEntry[]>([]);
  const [showLogs, setShowLogs] = useState(false);
  const [wasmSupported, setWasmSupported] = useState<boolean>(true);
  const [ffmpegLoading, setFfmpegLoading] = useState(false);
  const [ffmpegLoaded, setFfmpegLoaded] = useState(false);

  const controlsTimeoutRef = useRef<ReturnType<typeof setTimeout>>();
  const converterRef = useRef<VideoConverter | null>(null);
  const convertFileRef = useRef<File | null>(null);
  const audioCtxRef = useRef<AudioContext | null>(null);
  const sourceNodeRef = useRef<MediaElementAudioSourceNode | null>(null);
  const lowpassRef = useRef<BiquadFilterNode | null>(null);
  const highpassRef = useRef<BiquadFilterNode | null>(null);
  const compressorRef = useRef<DynamicsCompressorNode | null>(null);
  const gainRef = useRef<GainNode | null>(null);
  const audioConnectedRef = useRef(false);

  const hideControlsTimer = useCallback(() => {
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    setShowControls(true);
    if (isPlaying && videoSrc) {
      controlsTimeoutRef.current = setTimeout(() => setShowControls(false), 3000);
    }
  }, [isPlaying, videoSrc]);

  useEffect(() => {
    return () => {
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    };
  }, []);

  useEffect(() => {
    hideControlsTimer();
  }, [isPlaying, hideControlsTimer]);

  useEffect(() => {
    return () => {
      if (videoSrc) URL.revokeObjectURL(videoSrc);
    };
  }, [videoSrc]);

  const getDenoiseParams = useCallback((rate: number, strength: 'light' | 'medium' | 'heavy') => {
    const ratio = Math.max(0.25, rate);
    const strengthMultipliers = { light: 0.6, medium: 1, heavy: 1.4 };
    const m = strengthMultipliers[strength];
    const lowpassFreq = ratio >= 1
      ? 20000
      : Math.min(20000, Math.max(2000, 8000 * ratio * m + 2000));
    const highpassFreq = ratio >= 1
      ? 0
      : Math.min(300, Math.max(20, 120 / ratio * m));
    const compressorThreshold = ratio >= 1
      ? -24
      : Math.max(-50, -24 - (1 - ratio) * 30 * m);
    const compressorRatio = ratio >= 1
      ? 12
      : Math.min(20, 12 + (1 - ratio) * 15 * m);
    const gainValue = ratio >= 1
      ? 1
      : Math.min(2, 1 + (1 - ratio) * 0.8 * m);
    return { lowpassFreq, highpassFreq, compressorThreshold, compressorRatio, gainValue };
  }, []);

  const initAudioPipeline = useCallback(() => {
    const video = videoRef.current;
    if (!video || audioConnectedRef.current) return;

    try {
      const ctx = new AudioContext();
      const source = ctx.createMediaElementSource(video);
      const lowpass = ctx.createBiquadFilter();
      lowpass.type = 'lowpass';
      lowpass.frequency.value = 20000;
      lowpass.Q.value = 0.7;

      const highpass = ctx.createBiquadFilter();
      highpass.type = 'highpass';
      highpass.frequency.value = 0;
      highpass.Q.value = 0.7;

      const compressor = ctx.createDynamicsCompressor();
      compressor.threshold.value = -24;
      compressor.knee.value = 30;
      compressor.ratio.value = 12;
      compressor.attack.value = 0.003;
      compressor.release.value = 0.25;

      const gain = ctx.createGain();
      gain.gain.value = 1;

      source.connect(lowpass);
      lowpass.connect(highpass);
      highpass.connect(compressor);
      compressor.connect(gain);
      gain.connect(ctx.destination);

      audioCtxRef.current = ctx;
      sourceNodeRef.current = source;
      lowpassRef.current = lowpass;
      highpassRef.current = highpass;
      compressorRef.current = compressor;
      gainRef.current = gain;
      audioConnectedRef.current = true;
    } catch (e) {
      console.error('音频管线初始化失败:', e);
    }
  }, []);

  const updateAudioPipeline = useCallback((rate: number, enabled: boolean, strength: 'light' | 'medium' | 'heavy') => {
    if (!audioConnectedRef.current) return;
    const params = getDenoiseParams(rate, strength);

    if (lowpassRef.current) {
      lowpassRef.current.frequency.value = enabled ? params.lowpassFreq : 20000;
    }
    if (highpassRef.current) {
      highpassRef.current.frequency.value = enabled ? params.highpassFreq : 0;
    }
    if (compressorRef.current) {
      compressorRef.current.threshold.value = enabled ? params.compressorThreshold : -24;
      compressorRef.current.ratio.value = enabled ? params.compressorRatio : 12;
    }
    if (gainRef.current) {
      gainRef.current.gain.value = enabled ? params.gainValue : 1;
    }
  }, [getDenoiseParams]);

  const toggleDenoise = useCallback(() => {
    const next = !denoiseEnabled;
    setDenoiseEnabled(next);
    if (next && !audioConnectedRef.current) {
      initAudioPipeline();
    }
    updateAudioPipeline(playbackRate, next, denoiseStrength);
  }, [denoiseEnabled, playbackRate, denoiseStrength, initAudioPipeline, updateAudioPipeline]);

  const changeDenoiseStrength = useCallback((strength: 'light' | 'medium' | 'heavy') => {
    setDenoiseStrength(strength);
    updateAudioPipeline(playbackRate, denoiseEnabled, strength);
  }, [playbackRate, denoiseEnabled, updateAudioPipeline]);

  useEffect(() => {
    return () => {
      if (audioCtxRef.current) {
        audioCtxRef.current.close();
        audioCtxRef.current = null;
      }
      audioConnectedRef.current = false;
      sourceNodeRef.current = null;
      lowpassRef.current = null;
      highpassRef.current = null;
      compressorRef.current = null;
      gainRef.current = null;
    };
  }, []);

  // 检查 WebAssembly 支持并初始化转换器
  useEffect(() => {
    const wasmOk = isWasmSupported();
    const sabOk = isSharedArrayBufferSupported();
    const coiOk = isCrossOriginIsolated();
    setWasmSupported(wasmOk);

    console.log('[VideoPlayer] 环境检查:', {
      WebAssembly: wasmOk,
      SharedArrayBuffer: sabOk,
      CrossOriginIsolated: coiOk
    });

    if (wasmOk) {
      try {
        const converter = getVideoConverter();
        converterRef.current = converter;

        // 设置日志回调
        converter.setOnLogCallback((log) => {
          setConverterLogs(prev => [...prev, log]);
        });

        setFfmpegLoaded(converter.isLoaded());

        // 如果 SharedArrayBuffer 不可用，提前给出警告
        if (!sabOk) {
          console.warn('[VideoPlayer] SharedArrayBuffer 不可用，FFmpeg 加载可能失败');
          console.warn('[VideoPlayer] 请确保 Vite 配置了 COOP/COEP 响应头');
        }
      } catch (error) {
        console.error('[VideoPlayer] 转换器初始化失败:', error);
      }
    }

    // 注意：不在组件卸载时销毁单例
    // 单例会在页面刷新时自动重置
    // 如果销毁，会导致后续使用异常
  }, []);

  // 预加载 FFmpeg 模块
  const preloadFfmpeg = useCallback(async () => {
    if (!converterRef.current) {
      console.error('[VideoPlayer] 转换器未初始化，尝试重新获取');
      const converter = getVideoConverter();
      converterRef.current = converter;
      converter.setOnLogCallback((log) => {
        setConverterLogs(prev => [...prev, log]);
      });
    }

    if (ffmpegLoaded) {
      console.log('[VideoPlayer] FFmpeg 已加载，无需重复加载');
      return;
    }

    if (ffmpegLoading) {
      console.log('[VideoPlayer] FFmpeg 正在加载中，请勿重复点击');
      return;
    }

    console.log('[VideoPlayer] 开始预加载 FFmpeg 模块');
    setFfmpegLoading(true);
    setConvertError('');

    try {
      const success = await converterRef.current.init();
      setFfmpegLoaded(success);
      console.log('[VideoPlayer] FFmpeg 模块预加载完成:', success);
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : '未知错误';
      console.error('[VideoPlayer] FFmpeg 模块预加载失败:', error);

      // 检查是否是 SharedArrayBuffer 相关错误
      const isSabError = errorMsg.includes('SharedArrayBuffer') ||
                         errorMsg.includes('shared') ||
                         errorMsg.includes('cross-origin');
      if (isSabError) {
        setConvertError(
          `FFmpeg 加载失败：浏览器缺少 Cross-Origin Isolation 配置。` +
          `请确保 Vite 服务器配置了 COOP/COEP 响应头后重新启动开发服务器。` +
          `（错误：${errorMsg}）`
        );
      } else {
        setConvertError(`FFmpeg 模块加载失败: ${errorMsg}`);
      }
    } finally {
      setFfmpegLoading(false);
    }
  }, [ffmpegLoaded, ffmpegLoading]);

  // 处理转换进度
  const handleConvertProgress = useCallback((progress: ConvertProgress) => {
    setConvertProgress(progress);
    console.log('[VideoPlayer] 转换进度更新:', progress);
  }, []);

  // 开始转换
  const startConvert = useCallback(async () => {
    if (!converterRef.current) {
      setConvertError('转换器未初始化');
      return;
    }

    const file = convertFileRef.current;
    if (!file) {
      setConvertError('请先选择视频文件');
      return;
    }

    console.log('[VideoPlayer] 开始视频转换', {
      fileName: file.name,
      targetFormat,
      quality: convertQuality,
      videoBitrate: convertVideoBitrate,
      audioBitrate: convertAudioBitrate,
      resolution: convertResolution,
      fps: convertFps
    });

    setIsConverting(true);
    setConvertError('');
    setConvertResult(null);
    setConverterLogs([]);

    try {
      const result = await converterRef.current.convert(
        file,
        {
          targetFormat,
          quality: convertQuality,
          videoBitrate: convertVideoBitrate || undefined,
          audioBitrate: convertAudioBitrate || undefined,
          resolution: convertResolution || undefined,
          fps: convertFps || undefined
        },
        handleConvertProgress
      );

      setConvertResult(result);

      if (!result.success && result.error) {
        setConvertError(result.error);
      }

      console.log('[VideoPlayer] 转换完成:', result);
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : '未知错误';
      console.error('[VideoPlayer] 转换异常:', error);
      setConvertError(`转换失败: ${errorMsg}`);
    } finally {
      setIsConverting(false);
    }
  }, [targetFormat, convertQuality, convertVideoBitrate, convertAudioBitrate, convertResolution, convertFps, handleConvertProgress]);

  // 下载转换结果
  const downloadConvertedFile = useCallback(() => {
    if (!convertResult || !convertResult.blob || !convertResult.fileName) {
      console.warn('[VideoPlayer] 没有可下载的转换结果');
      return;
    }

    console.log('[VideoPlayer] 开始下载转换结果:', convertResult.fileName);

    try {
      const url = URL.createObjectURL(convertResult.blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = convertResult.fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      console.log('[VideoPlayer] 下载完成');
    } catch (error) {
      console.error('[VideoPlayer] 下载失败:', error);
      setConvertError('下载失败，请稍后重试');
    }
  }, [convertResult]);

  // 重置转换状态
  const resetConvertState = useCallback(() => {
    console.log('[VideoPlayer] 重置转换状态');
    setConvertProgress(null);
    setConvertResult(null);
    setConvertError('');
    setConverterLogs([]);
    if (converterRef.current) {
      converterRef.current.clearLogs();
    }
  }, []);

  // 计算节省空间
  const convertSavings = useMemo(() => {
    if (!convertResult || !convertResult.convertedSize) return null;
    const savings = convertResult.originalSize - convertResult.convertedSize;
    const percentage = ((savings / convertResult.originalSize) * 100).toFixed(1);
    return { savings, percentage };
  }, [convertResult]);

  // 获取当前目标格式信息
  const targetFormatInfo = useMemo(() => {
    return SUPPORTED_FORMATS.find(f => f.format === targetFormat);
  }, [targetFormat]);

  const handleFileSelect = useCallback((file: File) => {
    // 保存文件引用用于转换
    convertFileRef.current = file;
    console.log('[VideoPlayer] 文件已选择，保存用于转换:', file.name, file.size);

    if (!file.type.includes('mp4') && !file.type.includes('video') && !file.name.endsWith('.mp4')) {
      return;
    }
    if (videoSrc) URL.revokeObjectURL(videoSrc);
    if (audioCtxRef.current) {
      audioCtxRef.current.close();
      audioCtxRef.current = null;
      audioConnectedRef.current = false;
      sourceNodeRef.current = null;
      lowpassRef.current = null;
      highpassRef.current = null;
      compressorRef.current = null;
      gainRef.current = null;
    }
    setDenoiseEnabled(false);
    const url = URL.createObjectURL(file);
    setVideoSrc(url);
    setVideoName(file.name);
    setIsPlaying(false);
    setCurrentTime(0);
    setDuration(0);
    setPlaybackRate(1);
  }, [videoSrc]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const togglePlay = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      video.play();
      setIsPlaying(true);
    } else {
      video.pause();
      setIsPlaying(false);
    }
  }, []);

  const seek = useCallback((time: number) => {
    const video = videoRef.current;
    if (!video) return;
    video.currentTime = Math.max(0, Math.min(time, video.duration || 0));
  }, []);

  const changeVolume = useCallback((v: number) => {
    const video = videoRef.current;
    if (!video) return;
    const newVol = Math.max(0, Math.min(1, v));
    video.volume = newVol;
    setVolume(newVol);
    if (newVol > 0 && video.muted) {
      video.muted = false;
      setIsMuted(false);
    }
  }, []);

  const toggleMute = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = !video.muted;
    setIsMuted(video.muted);
  }, []);

  const changePlaybackRate = useCallback((rate: number) => {
    const video = videoRef.current;
    if (!video) return;
    const clamped = Math.max(0.25, Math.min(4, rate));
    video.playbackRate = clamped;
    setPlaybackRate(clamped);
    if (denoiseEnabled) {
      updateAudioPipeline(clamped, true, denoiseStrength);
    }
  }, [denoiseEnabled, denoiseStrength, updateAudioPipeline]);

  const toggleFullscreen = useCallback(() => {
    const container = containerRef.current;
    if (!container) return;
    if (!document.fullscreenElement) {
      container.requestFullscreen();
      setIsFullscreen(true);
    } else {
      document.exitFullscreen();
      setIsFullscreen(false);
    }
  }, []);

  useEffect(() => {
    const handler = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', handler);
    return () => document.removeEventListener('fullscreenchange', handler);
  }, []);

  useEffect(() => {
    if (!videoSrc) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      switch (e.key) {
        case ' ':
          e.preventDefault();
          togglePlay();
          break;
        case 'ArrowLeft':
          e.preventDefault();
          seek((videoRef.current?.currentTime || 0) - 5);
          break;
        case 'ArrowRight':
          e.preventDefault();
          seek((videoRef.current?.currentTime || 0) + 5);
          break;
        case 'ArrowUp':
          e.preventDefault();
          changeVolume(volume + 0.1);
          break;
        case 'ArrowDown':
          e.preventDefault();
          changeVolume(volume - 0.1);
          break;
        case 'f':
        case 'F':
          toggleFullscreen();
          break;
        case 'm':
        case 'M':
          toggleMute();
          break;
        case ',':
        case '<':
          changePlaybackRate(playbackRate - SPEED_STEP);
          break;
        case '.':
        case '>':
          changePlaybackRate(playbackRate + SPEED_STEP);
          break;
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [videoSrc, togglePlay, seek, changeVolume, toggleFullscreen, toggleMute, changePlaybackRate, volume, playbackRate, denoiseEnabled, denoiseStrength]);

  const handleTimeUpdate = () => {
    const video = videoRef.current;
    if (!video) return;
    setCurrentTime(video.currentTime);
    if (video.buffered.length > 0) {
      setBuffered(video.buffered.end(video.buffered.length - 1));
    }
  };

  const handleLoadedMetadata = () => {
    const video = videoRef.current;
    if (!video) return;
    setDuration(video.duration);
    video.playbackRate = playbackRate;
  };

  const handleVideoEnd = () => {
    setIsPlaying(false);
  };

  const handleProgressClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const bar = progressRef.current;
    if (!bar || !duration) return;
    const rect = bar.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    seek(ratio * duration);
  };

  const handleProgressMouseDown = (e: React.MouseEvent<HTMLDivElement>) => {
    const bar = progressRef.current;
    if (!bar || !duration) return;
    setIsDragging(true);
    const rect = bar.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    setDragTime(ratio * duration);

    const handleMouseMove = (ev: MouseEvent) => {
      const r = Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width));
      setDragTime(r * duration);
    };
    const handleMouseUp = (ev: MouseEvent) => {
      const r = Math.max(0, Math.min(1, (ev.clientX - rect.left) / rect.width));
      seek(r * duration);
      setIsDragging(false);
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  const displayTime = isDragging ? dragTime : currentTime;
  const progress = duration > 0 ? (displayTime / duration) * 100 : 0;
  const bufferedPercent = duration > 0 ? (buffered / duration) * 100 : 0;
  const showDenoiseHint = videoSrc && playbackRate <= 0.75 && !denoiseEnabled;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/tools')}
          className="p-3 rounded-xl hover:bg-white/5 transition-all"
        >
          <ArrowLeft className="w-5 h-5 text-gray-400" />
        </button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-white mb-1">
            在线视频<span className="gradient-text-aurora">播放器</span>
          </h1>
          <p className="text-gray-400">自由调速，不受限——本地 MP4 播放，倍速完全免费</p>
        </div>
        <button
          onClick={() => setShowShortcuts(!showShortcuts)}
          className={`px-4 py-2.5 rounded-xl font-medium flex items-center gap-2 transition-all ${
            showShortcuts
              ? 'bg-orange-500/20 text-orange-400 border border-orange-500/30'
              : 'bg-white/5 text-gray-400 hover:bg-white/10'
          }`}
        >
          <Keyboard className="w-4 h-4" />
          快捷键
        </button>
      </div>

      {showShortcuts && (
        <div className="glass-card rounded-2xl p-5 animate-fadeIn">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            {[
              ['空格', '播放/暂停'],
              ['← →', '快退/快进 5 秒'],
              ['↑ ↓', '音量增减'],
              ['F', '全屏切换'],
              ['M', '静音切换'],
              ['< >', '减速/加速'],
              ['拖拽', '拖入 MP4 文件'],
              ['点击', '点击进度条跳转'],
            ].map(([key, desc]) => (
              <div key={key} className="flex items-center gap-2">
                <kbd className="px-2 py-1 bg-white/10 border border-white/20 rounded-lg text-xs font-mono text-gray-300 min-w-[40px] text-center">
                  {key}
                </kbd>
                <span className="text-gray-400">{desc}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div
        ref={containerRef}
        className="relative glass-card rounded-3xl overflow-hidden group"
        onMouseMove={hideControlsTimer}
        onMouseLeave={() => { if (isPlaying && videoSrc) setShowControls(false); }}
      >
        {!videoSrc ? (
          <div
            className={`relative flex flex-col items-center justify-center py-32 transition-all ${
              isDragOver ? 'bg-orange-500/10' : ''
            }`}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
          >
            <div className="absolute inset-0 bg-grid-dot opacity-20 pointer-events-none" />
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-96 bg-gradient-to-br from-orange-500/15 to-red-500/10 rounded-full blur-3xl pointer-events-none" />
            <div className={`relative border-2 border-dashed rounded-3xl p-16 transition-all ${
              isDragOver ? 'border-orange-500 bg-orange-500/5' : 'border-white/20'
            }`}>
              <div className="w-24 h-24 rounded-3xl bg-gradient-to-br from-orange-500/20 to-red-500/20 flex items-center justify-center mx-auto mb-6">
                <MonitorPlay className="w-12 h-12 text-orange-400" />
              </div>
              <h2 className="text-2xl font-bold text-white mb-3 text-center">选择视频文件</h2>
              <p className="text-gray-400 mb-6 text-center">
                点击下方按钮选择本地 MP4 文件，或直接拖拽文件到此处
              </p>
              <button
                onClick={() => fileInputRef.current?.click()}
                className="btn-primary flex items-center gap-2 mx-auto"
                style={{
                  background: 'linear-gradient(135deg, #f97316 0%, #dc2626 100%)',
                  boxShadow: '0 4px 12px rgba(249, 115, 22, 0.4), 0 0 0 1px rgba(255,255,255,0.1) inset'
                }}
              >
                <Upload className="w-5 h-5" />
                选择 MP4 文件
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="video/mp4,.mp4"
                className="hidden"
                onChange={handleFileChange}
              />
            </div>
          </div>
        ) : (
          <>
            <div
              className="relative bg-black cursor-pointer"
              onClick={togglePlay}
              onDoubleClick={toggleFullscreen}
              onDrop={handleDrop}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
            >
              <video
                ref={videoRef}
                src={videoSrc}
                className="w-full max-h-[70vh] mx-auto"
                onTimeUpdate={handleTimeUpdate}
                onLoadedMetadata={handleLoadedMetadata}
                onEnded={handleVideoEnd}
                onPlay={() => setIsPlaying(true)}
                onPause={() => setIsPlaying(false)}
              />
              {!isPlaying && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/30 transition-opacity">
                  <div className="w-20 h-20 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center hover:bg-white/30 transition-all hover:scale-110">
                    <Play className="w-10 h-10 text-white ml-1" />
                  </div>
                </div>
              )}
            </div>

            <div
              className={`transition-all duration-300 ${
                showControls ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4 pointer-events-none'
              }`}
            >
              <div className="px-4 pt-3">
                <div
                  ref={progressRef}
                  className="relative h-2 bg-white/10 rounded-full cursor-pointer group/progress hover:h-3 transition-all"
                  onClick={handleProgressClick}
                  onMouseDown={handleProgressMouseDown}
                >
                  <div
                    className="absolute top-0 left-0 h-full bg-white/20 rounded-full"
                    style={{ width: `${bufferedPercent}%` }}
                  />
                  <div
                    className="absolute top-0 left-0 h-full rounded-full"
                    style={{
                      width: `${progress}%`,
                      background: 'linear-gradient(90deg, #f97316, #dc2626)',
                    }}
                  />
                  <div
                    className="absolute top-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-white shadow-lg opacity-0 group-hover/progress:opacity-100 transition-opacity"
                    style={{
                      left: `calc(${progress}% - 8px)`,
                      boxShadow: '0 0 8px rgba(249, 115, 22, 0.5)',
                    }}
                  />
                </div>
              </div>

              <div className="flex items-center gap-3 px-4 py-3">
                <button onClick={() => seek(currentTime - 10)} className="p-2 rounded-lg hover:bg-white/10 transition-all text-gray-400 hover:text-white">
                  <SkipBack className="w-4 h-4" />
                </button>
                <button
                  onClick={togglePlay}
                  className="p-3 rounded-xl transition-all text-white"
                  style={{
                    background: 'linear-gradient(135deg, #f97316, #dc2626)',
                    boxShadow: '0 2px 8px rgba(249, 115, 22, 0.3)',
                  }}
                >
                  {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5 ml-0.5" />}
                </button>
                <button onClick={() => seek(currentTime + 10)} className="p-2 rounded-lg hover:bg-white/10 transition-all text-gray-400 hover:text-white">
                  <SkipForward className="w-4 h-4" />
                </button>

                <span className="text-sm font-mono text-gray-300 min-w-[120px]">
                  {formatTime(displayTime)} / {formatTime(duration)}
                </span>

                <div className="flex items-center gap-2 ml-2">
                  <button onClick={toggleMute} className="p-2 rounded-lg hover:bg-white/10 transition-all text-gray-400 hover:text-white">
                    {isMuted || volume === 0 ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
                  </button>
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.01"
                    value={isMuted ? 0 : volume}
                    onChange={(e) => changeVolume(parseFloat(e.target.value))}
                    className="w-20 accent-orange-500"
                  />
                </div>

                <div className="flex-1" />

                {videoName && (
                  <span className="hidden md:block text-sm text-gray-500 truncate max-w-[200px]" title={videoName}>
                    {videoName}
                  </span>
                )}

                <button onClick={toggleFullscreen} className="p-2 rounded-lg hover:bg-white/10 transition-all text-gray-400 hover:text-white">
                  {isFullscreen ? <Minimize className="w-4 h-4" /> : <Maximize className="w-4 h-4" />}
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {videoSrc && (
        <div className="glass-card rounded-3xl p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Gauge className="w-5 h-5 text-orange-400" />
              播放速率控制
            </h2>
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-400">当前速率</span>
              <span
                className="text-2xl font-bold font-mono px-4 py-1 rounded-xl"
                style={{
                  background: 'linear-gradient(135deg, rgba(249, 115, 22, 0.2), rgba(220, 38, 38, 0.2))',
                  color: '#fb923c',
                  border: '1px solid rgba(249, 115, 22, 0.3)',
                }}
              >
                {playbackRate}x
              </span>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 mb-6">
            {PRESET_SPEEDS.map((speed) => (
              <button
                key={speed}
                onClick={() => changePlaybackRate(speed)}
                className={`px-4 py-2.5 rounded-xl font-medium text-sm transition-all ${
                  playbackRate === speed
                    ? 'text-white shadow-lg scale-105'
                    : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white border border-white/10'
                }`}
                style={
                  playbackRate === speed
                    ? {
                        background: 'linear-gradient(135deg, #f97316, #dc2626)',
                        boxShadow: '0 4px 12px rgba(249, 115, 22, 0.3)',
                      }
                    : undefined
                }
              >
                {speed}x
              </button>
            ))}
          </div>

          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-400 min-w-[40px]">0.25x</span>
            <div className="flex-1 relative">
              <input
                type="range"
                min="0.25"
                max="4"
                step="0.05"
                value={playbackRate}
                onChange={(e) => changePlaybackRate(parseFloat(e.target.value))}
                className="w-full accent-orange-500 h-2"
              />
              <div
                className="absolute -top-8 text-xs font-mono text-orange-400 bg-orange-500/20 px-2 py-0.5 rounded-lg pointer-events-none"
                style={{
                  left: `${((playbackRate - 0.25) / 3.75) * 100}%`,
                  transform: 'translateX(-50%)',
                }}
              >
                {playbackRate.toFixed(2)}x
              </div>
            </div>
            <span className="text-sm text-gray-400 min-w-[30px]">4x</span>
          </div>

          <div className="flex items-center gap-3 mt-6 pt-5 border-t border-white/10">
            <Zap className="w-4 h-4 text-orange-400" />
            <p className="text-sm text-gray-400">
              提示：使用键盘 <kbd className="px-1.5 py-0.5 bg-white/10 rounded text-xs font-mono text-gray-300">&lt;</kbd> <kbd className="px-1.5 py-0.5 bg-white/10 rounded text-xs font-mono text-gray-300">&gt;</kbd> 可快速调整速率，每次 ±{SPEED_STEP}x
            </p>
          </div>
        </div>
      )}

      {videoSrc && (
        <div className="glass-card rounded-3xl p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              {denoiseEnabled ? <Ear className="w-5 h-5 text-emerald-400" /> : <EarOff className="w-5 h-5 text-gray-400" />}
              音频降噪
            </h2>
            <button
              onClick={toggleDenoise}
              className={`px-5 py-2.5 rounded-xl font-medium flex items-center gap-2 transition-all ${
                denoiseEnabled
                  ? 'text-white shadow-lg'
                  : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white border border-white/10'
              }`}
              style={
                denoiseEnabled
                  ? {
                      background: 'linear-gradient(135deg, #10b981, #059669)',
                      boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
                    }
                  : undefined
              }
            >
              {denoiseEnabled ? <Ear className="w-4 h-4" /> : <EarOff className="w-4 h-4" />}
              {denoiseEnabled ? '降噪已开启' : '开启降噪'}
            </button>
          </div>

          {showDenoiseHint && (
            <div className="mb-5 p-4 rounded-2xl border border-amber-500/30 bg-amber-500/10 animate-fadeIn">
              <div className="flex items-center gap-3">
                <Ear className="w-5 h-5 text-amber-400 flex-shrink-0" />
                <div className="flex-1">
                  <p className="text-amber-300 font-medium text-sm">检测到低速播放，音频可能存在噪音</p>
                  <p className="text-amber-200/70 text-xs mt-1">
                    当前速率 {playbackRate}x，建议开启音频降噪改善听觉体验
                  </p>
                </div>
                <button
                  onClick={toggleDenoise}
                  className="px-4 py-2 rounded-xl text-sm font-medium text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}
                >
                  立即开启
                </button>
              </div>
            </div>
          )}

          {denoiseEnabled && (
            <div className="animate-fadeIn">
              <p className="text-sm text-gray-400 mb-4">
                降噪通过低通滤波器滤除高频噪音、高通滤波器去除低频嗡声、动态压缩器归一化音量，三重处理让低速播放时人声更清晰
              </p>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">降噪强度</label>
                  <div className="flex gap-3">
                    {([
                      { key: 'light', label: '轻度', desc: '轻微滤波，保留更多音质细节' },
                      { key: 'medium', label: '中度', desc: '平衡降噪与音质，推荐使用' },
                      { key: 'heavy', label: '重度', desc: '强力降噪，适合极低速播放' },
                    ] as const).map(({ key, label, desc }) => (
                      <button
                        key={key}
                        onClick={() => changeDenoiseStrength(key)}
                        className={`flex-1 p-3 rounded-xl text-left transition-all border ${
                          denoiseStrength === key
                            ? 'bg-emerald-500/15 border-emerald-500/40'
                            : 'bg-white/5 border-white/10 hover:bg-white/10'
                        }`}
                      >
                        <div className={`text-sm font-medium ${denoiseStrength === key ? 'text-emerald-400' : 'text-gray-300'}`}>
                          {label}
                        </div>
                        <div className="text-xs text-gray-500 mt-0.5">{desc}</div>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-3 pt-2">
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-gray-500 text-xs mb-1">低通截止</p>
                    <p className="text-white font-mono text-sm">
                      {getDenoiseParams(playbackRate, denoiseStrength).lowpassFreq >= 20000
                        ? '全频段'
                        : `${Math.round(getDenoiseParams(playbackRate, denoiseStrength).lowpassFreq)} Hz`}
                    </p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-gray-500 text-xs mb-1">高通截止</p>
                    <p className="text-white font-mono text-sm">
                      {getDenoiseParams(playbackRate, denoiseStrength).highpassFreq <= 0
                        ? '关闭'
                        : `${Math.round(getDenoiseParams(playbackRate, denoiseStrength).highpassFreq)} Hz`}
                    </p>
                  </div>
                  <div className="bg-white/5 rounded-xl p-3">
                    <p className="text-gray-500 text-xs mb-1">增益补偿</p>
                    <p className="text-white font-mono text-sm">
                      {getDenoiseParams(playbackRate, denoiseStrength).gainValue.toFixed(2)}x
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {!denoiseEnabled && !showDenoiseHint && (
            <p className="text-sm text-gray-500">
              低速播放（≤0.75x）时音频可能产生噪音和失真，开启降噪可通过滤波和动态压缩显著改善音质
            </p>
          )}
        </div>
      )}

      {/* 视频格式转换面板 */}
      {videoSrc && (
        <div className="glass-card rounded-3xl p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <FileVideo className="w-5 h-5 text-orange-400" />
              视频格式转换
            </h2>
            <div className="flex items-center gap-3">
              {!wasmSupported && (
                <div className="flex items-center gap-2 px-3 py-1 rounded-lg bg-red-500/10 border border-red-500/30">
                  <AlertCircle className="w-4 h-4 text-red-400" />
                  <span className="text-xs text-red-400">浏览器不支持 WebAssembly</span>
                </div>
              )}
              {wasmSupported && !ffmpegLoaded && !ffmpegLoading && (
                <button
                  onClick={preloadFfmpeg}
                  className="px-3 py-1.5 rounded-lg text-xs bg-white/5 text-gray-400 hover:bg-white/10 transition-all border border-white/10 flex items-center gap-1.5"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  预加载转换引擎
                </button>
              )}
              {ffmpegLoading && (
                <div className="flex items-center gap-2 px-3 py-1 rounded-lg bg-orange-500/10 border border-orange-500/30">
                  <Loader2 className="w-4 h-4 text-orange-400 animate-spin" />
                  <span className="text-xs text-orange-400">正在加载转换引擎...</span>
                </div>
              )}
              {ffmpegLoaded && (
                <div className="flex items-center gap-2 px-3 py-1 rounded-lg bg-emerald-500/10 border border-emerald-500/30">
                  <CheckCircle className="w-4 h-4 text-emerald-400" />
                  <span className="text-xs text-emerald-400">转换引擎已就绪</span>
                </div>
              )}
              <button
                onClick={() => setShowConvertPanel(!showConvertPanel)}
                className="p-2 rounded-lg hover:bg-white/10 transition-all text-gray-400 hover:text-white"
              >
                {showConvertPanel ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {showConvertPanel && (
            <div className="animate-fadeIn space-y-6">
              {convertError && (
                <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
                  <div className="flex items-center gap-3">
                    <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
                    <p className="text-red-300 text-sm">{convertError}</p>
                  </div>
                </div>
              )}

              {convertProgress && (
                <div className="bg-white/5 rounded-2xl p-5">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium text-gray-300">{convertProgress.message}</span>
                    <span className="text-sm font-mono text-orange-400">{convertProgress.progress}%</span>
                  </div>
                  <div className="w-full h-3 bg-white/10 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-300"
                      style={{
                        width: `${convertProgress.progress}%`,
                        background: convertProgress.phase === 'error'
                          ? 'linear-gradient(90deg, #ef4444, #dc2626)'
                          : convertProgress.phase === 'complete'
                          ? 'linear-gradient(90deg, #10b981, #059669)'
                          : 'linear-gradient(90deg, #f97316, #dc2626)'
                      }}
                    />
                  </div>
                  {convertProgress.time && (
                    <p className="text-xs text-gray-500 mt-2">已用时: {formatDuration(convertProgress.time)}</p>
                  )}
                </div>
              )}

              {convertResult && convertResult.success && (
                <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-2xl p-5">
                  <div className="flex items-center gap-2 mb-4">
                    <CheckCircle className="w-5 h-5 text-emerald-400" />
                    <span className="text-emerald-300 font-medium">转换完成！</span>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-500 text-xs mb-1">原始大小</p>
                      <p className="text-white font-mono text-sm">{formatFileSize(convertResult.originalSize)}</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-500 text-xs mb-1">转换后大小</p>
                      <p className="text-white font-mono text-sm">{formatFileSize(convertResult.convertedSize || 0)}</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-500 text-xs mb-1">目标格式</p>
                      <p className="text-white font-mono text-sm uppercase">{targetFormat}</p>
                    </div>
                    <div className="bg-white/5 rounded-xl p-3">
                      <p className="text-gray-500 text-xs mb-1">耗时</p>
                      <p className="text-white font-mono text-sm">{formatDuration(convertResult.duration || 0)}</p>
                    </div>
                  </div>
                  {convertSavings && convertSavings.savings > 0 && (
                    <div className="bg-emerald-500/10 border border-emerald-500/30 rounded-xl p-3 mb-4">
                      <div className="flex items-center gap-2">
                        <CheckCircle className="w-4 h-4 text-emerald-400" />
                        <span className="text-emerald-300 text-sm">
                          文件大小减少 {convertSavings.percentage}% ({formatFileSize(convertSavings.savings)})
                        </span>
                      </div>
                    </div>
                  )}
                  {convertSavings && convertSavings.savings < 0 && (
                    <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-3 mb-4">
                      <div className="flex items-center gap-2">
                        <AlertCircle className="w-4 h-4 text-amber-400" />
                        <span className="text-amber-300 text-sm">
                          文件大小增加 {Math.abs(Number(convertSavings.percentage))}% ({formatFileSize(Math.abs(convertSavings.savings))})
                        </span>
                      </div>
                    </div>
                  )}
                  <div className="flex gap-3">
                    <button
                      onClick={downloadConvertedFile}
                      className="flex-1 py-2.5 rounded-xl font-medium text-white flex items-center justify-center gap-2"
                      style={{ background: 'linear-gradient(135deg, #10b981, #059669)' }}
                    >
                      <Download className="w-4 h-4" />
                      下载 {convertResult.fileName}
                    </button>
                    <button
                      onClick={resetConvertState}
                      className="px-5 py-2.5 rounded-xl font-medium bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white transition-all border border-white/10 flex items-center gap-2"
                    >
                      <RefreshCw className="w-4 h-4" />
                      重新转换
                    </button>
                  </div>
                </div>
              )}

              {!convertResult && (
                <div className="space-y-5">
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-3">
                      目标格式
                    </label>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                      {SUPPORTED_FORMATS.map((format) => (
                        <button
                          key={format.format}
                          onClick={() => setTargetFormat(format.format)}
                          disabled={isConverting || !wasmSupported}
                          className={`px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                            targetFormat === format.format
                              ? 'text-white shadow-lg'
                              : 'bg-white/5 text-gray-300 hover:bg-white/10 border border-white/10'
                          } ${(isConverting || !wasmSupported) ? 'opacity-50 cursor-not-allowed' : ''}`}
                          style={
                            targetFormat === format.format
                              ? {
                                  background: 'linear-gradient(135deg, #f97316, #dc2626)',
                                  boxShadow: '0 4px 12px rgba(249, 115, 22, 0.3)'
                                }
                              : undefined
                          }
                        >
                          {format.label}
                        </button>
                      ))}
                    </div>
                    {targetFormatInfo && (
                      <p className="mt-2 text-sm text-gray-400">{targetFormatInfo.description}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">
                      视频质量 (CRF): {convertQuality}
                    </label>
                    <input
                      type="range"
                      min="0"
                      max="51"
                      value={convertQuality}
                      onChange={(e) => setConvertQuality(Number(e.target.value))}
                      disabled={isConverting}
                      className="w-full h-2 bg-white/10 rounded-lg appearance-none cursor-pointer accent-orange-500"
                    />
                    <div className="flex justify-between text-xs text-gray-500 mt-1">
                      <span>无损 (0)</span>
                      <span>推荐 (23)</span>
                      <span>最大压缩 (51)</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        视频比特率 (可选)
                      </label>
                      <input
                        type="text"
                        placeholder="例如: 2M, 1000k"
                        value={convertVideoBitrate}
                        onChange={(e) => setConvertVideoBitrate(e.target.value)}
                        disabled={isConverting}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-orange-500/50 transition-colors disabled:opacity-50"
                      />
                      <p className="text-xs text-gray-500 mt-1">留空则使用原始比特率</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        音频比特率 (可选)
                      </label>
                      <input
                        type="text"
                        placeholder="例如: 128k, 192k"
                        value={convertAudioBitrate}
                        onChange={(e) => setConvertAudioBitrate(e.target.value)}
                        disabled={isConverting}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-orange-500/50 transition-colors disabled:opacity-50"
                      />
                      <p className="text-xs text-gray-500 mt-1">留空则使用原始比特率</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        分辨率 (可选)
                      </label>
                      <input
                        type="text"
                        placeholder="例如: 1920x1080, 1280x720"
                        value={convertResolution}
                        onChange={(e) => setConvertResolution(e.target.value)}
                        disabled={isConverting}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-orange-500/50 transition-colors disabled:opacity-50"
                      />
                      <p className="text-xs text-gray-500 mt-1">留空则保持原始分辨率</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">
                        帧率 FPS (可选)
                      </label>
                      <input
                        type="number"
                        placeholder="例如: 30, 24, 60"
                        value={convertFps}
                        onChange={(e) => setConvertFps(e.target.value ? Number(e.target.value) : '')}
                        disabled={isConverting}
                        className="w-full px-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-white placeholder-gray-500 focus:outline-none focus:border-orange-500/50 transition-colors disabled:opacity-50"
                      />
                      <p className="text-xs text-gray-500 mt-1">留空则保持原始帧率</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <button
                      onClick={() => setShowLogs(!showLogs)}
                      className="px-4 py-2 rounded-xl text-sm font-medium bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white transition-all border border-white/10 flex items-center gap-2"
                    >
                      <FileText className="w-4 h-4" />
                      {showLogs ? '隐藏日志' : '查看日志'}
                    </button>
                    <button
                      onClick={startConvert}
                      disabled={isConverting || !wasmSupported || !convertFileRef.current}
                      className={`px-8 py-2.5 rounded-xl font-medium flex items-center gap-2 transition-all ${
                        isConverting || !wasmSupported || !convertFileRef.current
                          ? 'bg-white/10 text-gray-500 cursor-not-allowed'
                          : 'text-white shadow-lg'
                      }`}
                      style={
                        !isConverting && wasmSupported && convertFileRef.current
                          ? {
                              background: 'linear-gradient(135deg, #f97316, #dc2626)',
                              boxShadow: '0 4px 12px rgba(249, 115, 22, 0.3)'
                            }
                          : undefined
                      }
                    >
                      {isConverting ? (
                        <>
                          <Loader2 className="w-5 h-5 animate-spin" />
                          转换中...
                        </>
                      ) : (
                        <>
                          <RefreshCw className="w-5 h-5" />
                          开始转换
                        </>
                      )}
                    </button>
                  </div>

                  {showLogs && (
                    <div className="bg-black/30 border border-white/10 rounded-xl p-4 max-h-60 overflow-y-auto">
                      <h3 className="text-sm font-medium text-gray-400 mb-3 flex items-center gap-2">
                        <FileText className="w-4 h-4" />
                        转换日志
                      </h3>
                      {converterLogs.length === 0 ? (
                        <p className="text-gray-500 text-sm">暂无日志，开始转换后会显示详细信息</p>
                      ) : (
                        <div className="space-y-1.5 font-mono text-xs">
                          {converterLogs.map((log, index) => (
                            <div
                              key={index}
                              className={`flex items-start gap-2 ${
                                log.level === 'error' ? 'text-red-400' :
                                log.level === 'warn' ? 'text-amber-400' :
                                log.level === 'info' ? 'text-cyan-400' :
                                'text-gray-500'
                              }`}
                            >
                              <span className="text-gray-600 flex-shrink-0">
                                [{new Date(log.timestamp).toLocaleTimeString()}]
                              </span>
                              <span className="uppercase font-bold flex-shrink-0 w-12">[{log.level}]</span>
                              <span className="flex-1 break-all">{log.message}</span>
                              {log.data && (
                                <span className="text-gray-500 ml-1">
                                  {typeof log.data === 'object' ? JSON.stringify(log.data) : String(log.data)}
                                </span>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              <div className="bg-amber-500/5 border border-amber-500/20 rounded-xl p-4">
                <div className="flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="text-amber-300 text-sm font-medium mb-1">温馨提示</p>
                    <ul className="text-amber-200/70 text-xs space-y-1">
                      <li>• 视频转换完全在浏览器本地进行，不会上传到服务器，保护您的隐私</li>
                      <li>• 首次使用需要下载约 25MB 的 FFmpeg WASM 模块，请耐心等待</li>
                      <li>• 大文件转换可能需要较长时间，建议关闭其他占用资源的程序</li>
                      <li>• 支持格式: MP4, WebM, MOV, AVI, MKV, FLV, WMV, M4V 互相转换</li>
                      <li>• CRF 值越小质量越高，0 为无损，推荐值 18-28</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          )}

          {!showConvertPanel && (
            <p className="text-sm text-gray-500">
              支持多种视频格式互相转换，点击展开设置转换参数
            </p>
          )}
        </div>
      )}

      {videoSrc && (
        <div className="flex items-center justify-center gap-4">
          <button
            onClick={() => fileInputRef.current?.click()}
            className="px-5 py-2.5 bg-white/5 text-gray-300 rounded-xl font-medium hover:bg-white/10 transition-all flex items-center gap-2 border border-white/10"
          >
            <Upload className="w-4 h-4" />
            更换视频
          </button>
          <button
            onClick={() => {
              if (videoSrc) URL.revokeObjectURL(videoSrc);
              if (audioCtxRef.current) {
                audioCtxRef.current.close();
                audioCtxRef.current = null;
                audioConnectedRef.current = false;
                sourceNodeRef.current = null;
                lowpassRef.current = null;
                highpassRef.current = null;
                compressorRef.current = null;
                gainRef.current = null;
              }
              setVideoSrc('');
              setVideoName('');
              setIsPlaying(false);
              setCurrentTime(0);
              setDuration(0);
              setPlaybackRate(1);
              setDenoiseEnabled(false);
            }}
            className="px-5 py-2.5 bg-white/5 text-gray-300 rounded-xl font-medium hover:bg-white/10 transition-all flex items-center gap-2 border border-white/10"
          >
            <X className="w-4 h-4" />
            关闭视频
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="video/mp4,.mp4"
            className="hidden"
            onChange={handleFileChange}
          />
        </div>
      )}
    </div>
  );
}
