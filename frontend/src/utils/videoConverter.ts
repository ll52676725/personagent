import { FFmpeg } from '@ffmpeg/ffmpeg';
import { fetchFile } from '@ffmpeg/util';

/**
 * 视频格式类型定义
 * 支持的视频格式：MP4, WebM, MOV, AVI, MKV, FLV, WMV, M4V
 */
export type VideoFormat = 'mp4' | 'webm' | 'mov' | 'avi' | 'mkv' | 'flv' | 'wmv' | 'm4v';

/**
 * 视频格式信息接口
 */
export interface VideoFormatInfo {
  format: VideoFormat;
  label: string;
  description: string;
  mimeType: string;
  extension: string;
  codec: string;
}

/**
 * 转换进度回调接口
 */
export interface ConvertProgress {
  phase: 'loading' | 'processing' | 'complete' | 'error';
  progress: number;
  message: string;
  /** 已用时间，单位：毫秒 */
  time?: number;
}

/**
 * 转换结果接口
 */
export interface ConvertResult {
  success: boolean;
  blob?: Blob;
  fileName?: string;
  originalSize: number;
  convertedSize?: number;
  error?: string;
  duration?: number;
}

/**
 * 转换配置接口
 */
export interface ConvertOptions {
  targetFormat: VideoFormat;
  videoBitrate?: string;
  audioBitrate?: string;
  resolution?: string;
  fps?: number;
  quality?: number;
}

/**
 * 日志级别
 */
type LogLevel = 'debug' | 'info' | 'warn' | 'error';

/**
 * 日志条目接口
 */
export interface LogEntry {
  timestamp: number;
  level: LogLevel;
  message: string;
  data?: any;
}

/**
 * 支持的视频格式列表
 */
export const SUPPORTED_FORMATS: VideoFormatInfo[] = [
  {
    format: 'mp4',
    label: 'MP4',
    description: 'MPEG-4 格式，兼容性最好，适合大多数设备',
    mimeType: 'video/mp4',
    extension: '.mp4',
    codec: 'libx264'
  },
  {
    format: 'webm',
    label: 'WebM',
    description: 'WebM 格式，开源免费，适合网页播放',
    mimeType: 'video/webm',
    extension: '.webm',
    codec: 'libvpx-vp9'
  },
  {
    format: 'mov',
    label: 'MOV',
    description: 'QuickTime 格式，适合 Apple 设备',
    mimeType: 'video/quicktime',
    extension: '.mov',
    codec: 'libx264'
  },
  {
    format: 'avi',
    label: 'AVI',
    description: 'AVI 格式，兼容性好，文件较大',
    mimeType: 'video/x-msvideo',
    extension: '.avi',
    codec: 'libx264'
  },
  {
    format: 'mkv',
    label: 'MKV',
    description: 'Matroska 格式，支持多音轨多字幕',
    mimeType: 'video/x-matroska',
    extension: '.mkv',
    codec: 'libx264'
  },
  {
    format: 'flv',
    label: 'FLV',
    description: 'Flash 视频格式，适合网络直播',
    mimeType: 'video/x-flv',
    extension: '.flv',
    codec: 'flv'
  },
  {
    format: 'wmv',
    label: 'WMV',
    description: 'Windows Media 格式，适合 Windows 设备',
    mimeType: 'video/x-ms-wmv',
    extension: '.wmv',
    codec: 'wmv2'
  },
  {
    format: 'm4v',
    label: 'M4V',
    description: 'iTunes 视频格式，支持 DRM 保护',
    mimeType: 'video/x-m4v',
    extension: '.m4v',
    codec: 'libx264'
  }
];

/**
 * FFmpeg 核心文件 URL
 * 使用 Vite 的 ?url 导入方式，直接从本地 node_modules 加载
 * 这样避免了从 CDN 下载的不稳定性，也避免了 CORS 问题
 * 注意：Vite 项目必须使用 esm 版本，不能用 umd 版本
 */
import coreURL from '@ffmpeg/core?url';
import wasmURL from '@ffmpeg/core/wasm?url';

/**
 * 视频转换器类
 * 封装 FFmpeg.wasm 的使用，提供视频格式转换能力
 *
 * 关键说明：
 * - FFmpeg.wasm 需要浏览器支持 SharedArrayBuffer
 * - SharedArrayBuffer 需要 Cross-Origin Isolation 环境
 * - 因此 Vite 开发服务器必须配置 COOP/COEP 响应头
 * - 已在 vite.config.ts 中配置了相关响应头
 */
export class VideoConverter {
  private ffmpeg: FFmpeg;
  private loaded: boolean = false;
  private loading: boolean = false;
  private logs: LogEntry[] = [];
  private onLogCallback?: (log: LogEntry) => void;
  private logHandlersSet: boolean = false;

  constructor() {
    this.ffmpeg = new FFmpeg();
  }

  /**
   * 设置日志回调函数
   * @param callback 日志回调
   */
  setOnLogCallback(callback: (log: LogEntry) => void) {
    this.onLogCallback = callback;
  }

  /**
   * 记录日志
   * @param level 日志级别
   * @param message 日志消息
   * @param data 附加数据
   */
  private log(level: LogLevel, message: string, data?: any) {
    const entry: LogEntry = {
      timestamp: Date.now(),
      level,
      message,
      data
    };
    this.logs.push(entry);
    this.onLogCallback?.(entry);
    console[level](`[VideoConverter][${level.toUpperCase()}] ${message}`, data || '');
  }

  /**
   * 设置 FFmpeg 日志和进度处理器
   * 注意：必须在 ffmpeg.load() 之前设置，否则会丢失早期日志
   */
  private setupLogHandlers() {
    if (this.logHandlersSet) return;

    this.ffmpeg.on('log', ({ message }) => {
      this.log('debug', `FFmpeg: ${message}`);
    });

    // 注意：FFmpeg progress 事件中的 time 字段是"已处理的视频时长"，单位是微秒
    // 不是"已用时间"。已用时间需要用 Date.now() 自行计算
    this.ffmpeg.on('progress', ({ progress, time }) => {
      const processedMs = time / 1000;
      this.log('debug', `转换进度: ${(progress * 100).toFixed(1)}%, 已处理视频时长: ${processedMs}ms (${(processedMs / 1000).toFixed(2)}秒)`);
    });

    this.logHandlersSet = true;
  }

  /**
   * 获取所有日志
   */
  getLogs(): LogEntry[] {
    return [...this.logs];
  }

  /**
   * 清除日志
   */
  clearLogs() {
    this.logs = [];
  }

  /**
   * 初始化 FFmpeg，加载 WASM 模块
   * 这是一个耗时操作，需要在第一次使用前调用
   *
   * 使用方式：
   * - 核心文件通过 Vite ?url 导入，从本地 node_modules 加载
   * - 不再从 CDN 下载，避免网络问题和 CORS 限制
   * - 需要 Cross-Origin Isolation 环境才能使用 SharedArrayBuffer
   */
  async init(): Promise<boolean> {
    if (this.loaded) {
      this.log('info', 'FFmpeg 已初始化，跳过加载');
      return true;
    }

    // 如果正在加载，等待加载完成
    if (this.loading) {
      this.log('info', 'FFmpeg 正在加载中，等待完成...');
      // 等待 loading 状态变化
      return new Promise((resolve) => {
        const checkInterval = setInterval(() => {
          if (this.loaded) {
            clearInterval(checkInterval);
            resolve(true);
          } else if (!this.loading) {
            clearInterval(checkInterval);
            resolve(false);
          }
        }, 200);
      });
    }

    try {
      this.loading = true;
      this.log('info', '开始加载 FFmpeg WASM 模块...');

      // 检查浏览器是否支持 SharedArrayBuffer（FFmpeg.wasm 需要）
      if (typeof SharedArrayBuffer === 'undefined') {
        this.log('warn', 'SharedArrayBuffer 不可用，这通常是因为缺少 Cross-Origin Isolation 配置');
        this.log('warn', '请确保 Vite 服务器配置了 COOP/COEP 响应头');
        this.log('warn', 'Cross-Origin-Opener-Policy: same-origin');
        this.log('warn', 'Cross-Origin-Embedder-Policy: require-corp');
      }

      this.log('debug', '核心文件 URL', { coreURL, wasmURL });

      // 在加载前设置日志处理器
      this.setupLogHandlers();

      // 使用 Vite ?url 导入的本地文件路径加载
      // 这比从 CDN 下载更可靠，不受网络影响
      await this.ffmpeg.load({
        coreURL,
        wasmURL
      });

      this.loaded = true;
      this.loading = false;
      this.log('info', 'FFmpeg WASM 模块加载成功');
      return true;
    } catch (error) {
      this.loading = false;
      this.log('error', 'FFmpeg WASM 模块加载失败', error);

      // 输出详细的错误诊断信息
      if (error instanceof Error) {
        if (error.message.includes('SharedArrayBuffer')) {
          this.log('error', '诊断：SharedArrayBuffer 不可用。请在 vite.config.ts 中添加 COOP/COEP 响应头');
        }
        if (error.message.includes('CORS') || error.message.includes('cross-origin')) {
          this.log('error', '诊断：CORS 错误。请检查服务器是否正确配置了跨域策略');
        }
        if (error.message.includes('fetch') || error.message.includes('network')) {
          this.log('error', '诊断：网络错误。请检查网络连接或尝试使用本地文件加载');
        }
      }

      throw error;
    }
  }

  /**
   * 检查 FFmpeg 是否已初始化
   */
  isLoaded(): boolean {
    return this.loaded;
  }

  /**
   * 检查是否正在加载
   */
  isLoading(): boolean {
    return this.loading;
  }

  /**
   * 获取格式信息
   * @param format 视频格式
   */
  getFormatInfo(format: VideoFormat): VideoFormatInfo | undefined {
    return SUPPORTED_FORMATS.find(f => f.format === format);
  }

  /**
   * 从文件名推断格式
   * @param fileName 文件名
   */
  inferFormatFromFileName(fileName: string): VideoFormat | null {
    const ext = fileName.split('.').pop()?.toLowerCase();
    if (!ext) return null;
    const format = SUPPORTED_FORMATS.find(f => f.format === ext);
    return format ? format.format : null;
  }

  /**
   * 生成输出文件名
   * @param originalName 原始文件名
   * @param targetFormat 目标格式
   */
  generateOutputFileName(originalName: string, targetFormat: VideoFormat): string {
    const baseName = originalName.replace(/\.[^/.]+$/, '');
    const info = this.getFormatInfo(targetFormat);
    const ext = info?.extension || `.${targetFormat}`;
    return `${baseName}_converted${ext}`;
  }

  /**
   * 构建 FFmpeg 命令参数
   * @param inputFile 输入文件名
   * @param outputFile 输出文件名
   * @param options 转换选项
   * @param targetFormat 目标格式
   */
  private buildFFmpegArgs(
    inputFile: string,
    outputFile: string,
    options: ConvertOptions,
    targetFormat: VideoFormat
  ): string[] {
    const args: string[] = ['-i', inputFile];
    const formatInfo = this.getFormatInfo(targetFormat);

    this.log('debug', '构建转换参数', { options, targetFormat, codec: formatInfo?.codec });

    // 视频编码器
    if (formatInfo?.codec) {
      args.push('-c:v', formatInfo.codec);
    }

    // 视频比特率
    if (options.videoBitrate) {
      args.push('-b:v', options.videoBitrate);
    }

    // 音频编码器
    if (targetFormat === 'webm') {
      args.push('-c:a', 'libopus');
    } else {
      args.push('-c:a', 'aac');
    }

    // 音频比特率
    if (options.audioBitrate) {
      args.push('-b:a', options.audioBitrate);
    }

    // 分辨率
    if (options.resolution) {
      args.push('-s', options.resolution);
    }

    // 帧率
    if (options.fps) {
      args.push('-r', options.fps.toString());
    }

    // 质量参数（CRF - Constant Rate Factor）
    if (options.quality !== undefined) {
      const crf = Math.max(0, Math.min(51, options.quality));
      args.push('-crf', crf.toString());
    }

    // 预设参数，平衡速度和质量
    args.push('-preset', 'medium');

    // 输出文件
    args.push('-y', outputFile);

    this.log('debug', 'FFmpeg 命令参数', args.join(' '));
    return args;
  }

  /**
   * 转换视频格式
   * @param file 输入视频文件
   * @param options 转换选项
   * @param onProgress 进度回调
   */
  async convert(
    file: File,
    options: ConvertOptions,
    onProgress?: (progress: ConvertProgress) => void
  ): Promise<ConvertResult> {
    const startTime = Date.now();
    const originalFormat = this.inferFormatFromFileName(file.name) || 'unknown';

    this.log('info', '开始视频转换', {
      fileName: file.name,
      fileSize: file.size,
      fileType: file.type,
      originalFormat,
      targetFormat: options.targetFormat
    });

    // 报告进度
    onProgress?.({
      phase: 'loading',
      progress: 0,
      message: '正在准备转换...',
      time: 0
    });

    // 确保 FFmpeg 已初始化
    if (!this.loaded) {
      this.log('info', 'FFmpeg 未初始化，开始初始化...');
      try {
        await this.init();
      } catch (error) {
        const errorMsg = error instanceof Error ? error.message : '未知错误';
        this.log('error', 'FFmpeg 初始化失败', error);
        onProgress?.({
          phase: 'error',
          progress: 100,
          message: `FFmpeg 初始化失败: ${errorMsg}`,
          time: Date.now() - startTime
        });
        return {
          success: false,
          originalSize: file.size,
          error: `FFmpeg 初始化失败: ${errorMsg}`
        };
      }
    }

    try {
      const inputFileName = `input_${Date.now()}`;
      const outputFileName = `output_${Date.now()}.${options.targetFormat}`;

      this.log('info', '写入文件到 FFmpeg 虚拟文件系统', { inputFileName, outputFileName });

      onProgress?.({
        phase: 'loading',
        progress: 10,
        message: '正在加载视频文件...',
        time: Date.now() - startTime
      });

      // 写入输入文件到虚拟文件系统
      const data = await fetchFile(file);
      await this.ffmpeg.writeFile(inputFileName, data as any);

      this.log('info', '文件写入完成，开始转换');

      onProgress?.({
        phase: 'processing',
        progress: 20,
        message: '正在转换视频格式...',
        time: Date.now() - startTime
      });

      // 设置进度监听
      // 注意：FFmpeg progress 事件中的 time 字段是"已处理的视频时长"，单位是微秒
      // 不是"已用时间"，所以我们需要自己用 Date.now() - startTime 计算已用时间
      const progressHandler = ({ progress, time }: { progress: number; time: number }) => {
        const adjustedProgress = 20 + progress * 70;
        const elapsedMs = Date.now() - startTime;
        // time 是已处理的视频时长（微秒），转换为毫秒用于日志
        const processedVideoDurationMs = time / 1000;
        this.log('debug', `转换进度: ${(progress * 100).toFixed(1)}%, 已用时: ${elapsedMs}ms, 已处理视频时长: ${processedVideoDurationMs}ms`);
        onProgress?.({
          phase: 'processing',
          progress: Math.round(adjustedProgress),
          message: `正在转换... ${adjustedProgress.toFixed(0)}%`,
          time: elapsedMs
        });
      };

      this.ffmpeg.on('progress', progressHandler);

      // 执行转换
      const ffmpegArgs = this.buildFFmpegArgs(
        inputFileName,
        outputFileName,
        options,
        options.targetFormat
      );

      this.log('info', '执行 FFmpeg 命令', { args: ffmpegArgs.join(' ') });

      await this.ffmpeg.exec(ffmpegArgs);

      this.log('info', 'FFmpeg 命令执行完成');

      onProgress?.({
        phase: 'processing',
        progress: 90,
        message: '正在生成输出文件...',
        time: Date.now() - startTime
      });

      // 读取输出文件
      const outputData = await this.ffmpeg.readFile(outputFileName);
      const outputBlob = new Blob([new Uint8Array(outputData as Uint8Array)], {
        type: this.getFormatInfo(options.targetFormat)?.mimeType || 'application/octet-stream'
      });

      const outputFileNameFinal = this.generateOutputFileName(file.name, options.targetFormat);
      const duration = Date.now() - startTime;

      this.log('info', '视频转换完成', {
        outputFileName: outputFileNameFinal,
        originalSize: file.size,
        convertedSize: outputBlob.size,
        duration: `${duration}ms`
      });

      onProgress?.({
        phase: 'complete',
        progress: 100,
        message: '转换完成！',
        time: duration
      });

      // 清理临时文件
      try {
        await this.ffmpeg.deleteFile(inputFileName);
        await this.ffmpeg.deleteFile(outputFileName);
        this.log('debug', '临时文件已清理');
      } catch (cleanupError) {
        this.log('warn', '临时文件清理失败', cleanupError);
      }

      return {
        success: true,
        blob: outputBlob,
        fileName: outputFileNameFinal,
        originalSize: file.size,
        convertedSize: outputBlob.size,
        duration
      };
    } catch (error) {
      const duration = Date.now() - startTime;
      const errorMsg = error instanceof Error ? error.message : '未知错误';

      this.log('error', '视频转换失败', { error, duration: `${duration}ms` });

      onProgress?.({
        phase: 'error',
        progress: 100,
        message: `转换失败: ${errorMsg}`,
        time: duration
      });

      return {
        success: false,
        originalSize: file.size,
        error: errorMsg,
        duration
      };
    }
  }
}

/**
 * 单例模式获取转换器实例
 * 注意：不要在组件卸载时销毁单例，否则后续使用会异常
 * 单例会在页面刷新时自动重置
 */
let converterInstance: VideoConverter | null = null;

export function getVideoConverter(): VideoConverter {
  if (!converterInstance) {
    converterInstance = new VideoConverter();
  }
  return converterInstance;
}

/**
 * 重置转换器单例
 * 仅在需要完全重新初始化时调用
 */
export function resetVideoConverter(): void {
  converterInstance = null;
}

/**
 * 格式化文件大小
 * @param bytes 字节数
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 0) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

/**
 * 格式化时间
 * @param ms 毫秒
 */
export function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms} ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(1)} 秒`;
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  return `${minutes} 分 ${seconds} 秒`;
}

/**
 * 检查浏览器是否支持 WebAssembly
 */
export function isWasmSupported(): boolean {
  try {
    if (typeof WebAssembly === 'object' &&
        typeof WebAssembly.instantiate === 'function') {
      const module = new WebAssembly.Module(
        Uint8Array.of(0x0, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00)
      );
      return module instanceof WebAssembly.Module;
    }
    return false;
  } catch (e) {
    return false;
  }
}

/**
 * 检查浏览器是否支持 SharedArrayBuffer（FFmpeg.wasm 多线程需要）
 */
export function isSharedArrayBufferSupported(): boolean {
  return typeof SharedArrayBuffer !== 'undefined';
}

/**
 * 检查当前页面是否处于 Cross-Origin Isolation 环境
 */
export function isCrossOriginIsolated(): boolean {
  return window.crossOriginIsolated === true;
}
