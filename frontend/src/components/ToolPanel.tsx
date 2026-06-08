import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Image, ArrowRight, FileText, Code2, HardDrive, Database, Globe, Camera, Shapes, ShieldAlert, Wrench, Clock, Search, Table2, MonitorPlay, Mic, Container, MonitorDown, Check, Loader2 } from 'lucide-react';
import { toolsApi } from '@/api';

interface ToolInfo {
  id: string;
  title: string;
  description: string;
  icon: any;
  gradient: string;
  path: string;
  badge?: string;
}

const tools: ToolInfo[] = [
  {
    id: 'app-icon-generator',
    title: 'APP ICON 生成器',
    description: '一键生成 APP、小程序图标和公司 Logo，支持多种风格、形状、尺寸和配色方案',
    icon: Shapes,
    gradient: 'from-fuchsia-500 to-pink-600',
    path: '/tools/app-icon-generator',
    badge: '新',
  },
  {
    id: 'photo-standardization',
    title: '证件照规范化',
    description: '调整照片尺寸、更换背景色，生成符合各类系统要求的证件照',
    icon: Camera,
    gradient: 'from-violet-500 to-purple-600',
    path: '/tools/photo-standardization',
    badge: '新',
  },
  {
    id: 'image-converter',
    title: '图片格式转换',
    description: '支持 JPG、PNG、GIF、WebP、TIFF 等多种主流格式的互相转换，可调节压缩质量和尺寸',
    icon: Image,
    gradient: 'from-cyan-500 to-blue-600',
    path: '/tools/image-converter',
  },
  {
    id: 'file-converter',
    title: '文件格式转换',
    description: '支持 Word(doc/docx)、PDF、TXT、HTML、Excel(xls/xlsx)、CSV 等格式的互相转换',
    icon: FileText,
    gradient: 'from-emerald-500 to-teal-600',
    path: '/tools/file-converter',
    badge: '新',
  },
  {
    id: 'json-formatter',
    title: 'JSON 格式化',
    description: 'JSON 格式化、压缩、校验，支持 AI 智能修复格式错误，详细错误定位与修复建议',
    icon: Code2,
    gradient: 'from-purple-500 to-pink-600',
    path: '/tools/json-formatter',
    badge: '新',
  },
  {
    id: 'disk-analyzer',
    title: '磁盘空间分析',
    description: '分析 Windows 盘符空间占用，按文件类型分类统计，识别大文件夹，提供清理建议',
    icon: HardDrive,
    gradient: 'from-amber-500 to-orange-600',
    path: '/tools/disk-analyzer',
    badge: '新',
  },
  {
    id: 'registry-cleaner',
    title: '注册表清理',
    description: '扫描 Windows 注册表冗余项，分析无效关联、启动项、卸载残留等，生成清理脚本',
    icon: Database,
    gradient: 'from-rose-500 to-red-600',
    path: '/tools/registry-cleaner',
    badge: '新',
  },
  {
    id: 'ip-analyzer',
    title: 'IP地址分析器',
    description: '查看当前IP信息、Ping测试、路由追踪、DNS解析、局域网扫描，一键分析连通性并定位卡点',
    icon: Globe,
    gradient: 'from-indigo-500 to-blue-600',
    path: '/tools/ip-analyzer',
    badge: '新',
  },
  {
    id: 'image-moderation',
    title: '图片内容检测',
    description: '智能检测图片中的涉黄、涉政、涉爆等违规内容，多维度安全分析，保障内容安全',
    icon: ShieldAlert,
    gradient: 'from-red-500 to-rose-600',
    path: '/tools/image-moderation',
    badge: '新',
  },
  {
    id: 'virus-detector',
    title: '病毒检测与安全分析',
    description: '检测系统可疑程序、高危漏洞，AI智能分析安全风险，提供修复方案和安全加固建议',
    icon: ShieldAlert,
    gradient: 'from-red-500 to-rose-600',
    path: '/tools/virus-detector',
    badge: '新',
  },
  {
    id: 'cron-generator',
    title: 'Cron 表达式生成器',
    description: '可视化生成、解析 Cron 表达式，支持预设模板和 AI 自然语言转换，计算下次执行时间',
    icon: Clock,
    gradient: 'from-sky-500 to-cyan-600',
    path: '/tools/cron-generator',
    badge: '新',
  },
  {
    id: 'regex-tester',
    title: '正则表达式校验工具',
    description: '正则表达式校验、匹配测试，AI 自动生成和修正，可视化展示匹配结果和捕获组',
    icon: Search,
    gradient: 'from-teal-500 to-emerald-600',
    path: '/tools/regex-tester',
    badge: '新',
  },
  {
    id: 'sql-formatter',
    title: 'SQL 格式化工具',
    description: 'SQL 格式化、压缩、语法校验，支持 AI 智能修复语法错误和性能优化建议',
    icon: Table2,
    gradient: 'from-cyan-500 to-blue-600',
    path: '/tools/sql-formatter',
    badge: '新',
  },
  {
    id: 'video-player',
    title: '在线视频播放器',
    description: '本地 MP4 视频播放，0.25x~4x 自由调速，对抗网盘倍速收费限制，完全免费',
    icon: MonitorPlay,
    gradient: 'from-orange-500 to-red-600',
    path: '/tools/video-player',
    badge: '热门',
  },
  {
    id: 'speech-to-text',
    title: '语音转文字',
    description: '上传手机录音文件，AI 智能识别语音内容转为可编辑文字，支持中文等 8 种语言',
    icon: Mic,
    gradient: 'from-green-500 to-emerald-600',
    path: '/tools/speech-to-text',
    badge: '新',
  },
  {
    id: 'docker-generator',
    title: 'Docker 容器化工具',
    description: '自动分析 Java 项目，生成 Dockerfile、.dockerignore、docker-compose.yml，一键构建部署',
    icon: Container,
    gradient: 'from-blue-500 to-indigo-600',
    path: '/tools/docker-generator',
    badge: '新',
  },
];

export default function ToolPanel() {
  const navigate = useNavigate();
  const [deploying, setDeploying] = useState<string | null>(null);
  const [deployed, setDeployed] = useState<Set<string>>(new Set());
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleDeploy = async (e: React.MouseEvent, tool: ToolInfo) => {
    e.stopPropagation();
    if (deploying) return;

    setDeploying(tool.id);
    try {
      const baseUrl = window.location.origin;
      const response = await toolsApi.createDesktopShortcut({
        toolId: tool.id,
        toolName: tool.title,
        toolPath: tool.path,
        baseUrl,
        openAsApp: true,
      });

      if (response.code === 200 && response.data?.success) {
        setDeployed(prev => new Set(prev).add(tool.id));
        showToast(response.data.message || `${tool.title} 已部署到桌面`);
      } else {
        showToast(response.data?.message || response.message || '部署失败', 'error');
      }
    } catch (error: any) {
      showToast(error?.response?.data?.message || '部署失败，请检查后端服务是否启动', 'error');
    } finally {
      setDeploying(null);
    }
  };

  const handleDeployAll = async () => {
    if (deploying) return;

    setDeploying('__all__');
    let successCount = 0;
    let failCount = 0;
    const baseUrl = window.location.origin;

    for (const tool of tools) {
      try {
        const response = await toolsApi.createDesktopShortcut({
          toolId: tool.id,
          toolName: tool.title,
          toolPath: tool.path,
          baseUrl,
          openAsApp: true,
        });
        if (response.code === 200 && response.data?.success) {
          successCount++;
          setDeployed(prev => new Set(prev).add(tool.id));
        } else {
          failCount++;
        }
      } catch {
        failCount++;
      }
    }

    setDeploying(null);
    if (failCount === 0) {
      showToast(`全部 ${successCount} 个工具已部署到桌面`);
    } else {
      showToast(`已部署 ${successCount} 个，失败 ${failCount} 个`, 'error');
    }
  };

  return (
    <div className="space-y-8">
      {toast && (
        <div className={`fixed top-6 right-6 z-50 px-5 py-3 rounded-xl shadow-2xl backdrop-blur-xl border animate-fadeIn ${
          toast.type === 'success'
            ? 'bg-emerald-500/20 border-emerald-500/30 text-emerald-300'
            : 'bg-red-500/20 border-red-500/30 text-red-300'
        }`}>
          <div className="flex items-center gap-2">
            {toast.type === 'success' ? (
              <Check className="w-5 h-5" />
            ) : (
              <ShieldAlert className="w-5 h-5" />
            )}
            <span className="text-sm font-medium">{toast.message}</span>
          </div>
        </div>
      )}

      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <Wrench className="w-4 h-4 text-cyan-400" />
            <span className="text-sm text-cyan-400 font-medium">多功能工具箱</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            工具<span className="gradient-text-aurora">集</span>
          </h1>
          <p className="text-gray-400 text-lg">
            实用工具箱，助您高效创作
          </p>
        </div>

        <button
          onClick={handleDeployAll}
          disabled={deploying !== null}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-violet-500 to-indigo-500 text-white font-medium text-sm shadow-lg shadow-violet-500/25 hover:shadow-violet-500/40 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {deploying === '__all__' ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <MonitorDown className="w-4 h-4" />
          )}
          {deploying === '__all__' ? '批量部署中...' : '全部部署到桌面'}
        </button>
      </div>

      <div className="relative">
        <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-cyan-500/10 to-transparent rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-violet-500/10 to-transparent rounded-full blur-2xl" />
        <div className="relative grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {tools.map((tool, index) => {
            const Icon = tool.icon;
            const isDeploying = deploying === tool.id;
            const isDeployed = deployed.has(tool.id);
            return (
              <div
                key={tool.id}
                className="group relative overflow-hidden glass-card rounded-3xl p-6 glass-card-hover cursor-pointer animate-fadeIn opacity-0"
                style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
                onClick={() => navigate(tool.path)}
              >
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
                <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-cyan-500/5 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />

                <div className="relative">
                  <div className="relative mb-4">
                    <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${tool.gradient} flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300`}>
                      <Icon className="w-7 h-7 text-white" />
                    </div>
                    {tool.badge && (
                      <span className="absolute top-0 right-0 px-2 py-0.5 text-xs bg-gradient-to-r from-pink-500 to-orange-400 text-white rounded-full font-medium">
                        {tool.badge}
                      </span>
                    )}
                  </div>

                  <h3 className="text-xl font-bold text-white mb-2">{tool.title}</h3>
                  <p className="text-gray-400 text-sm mb-4 line-clamp-2">{tool.description}</p>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center text-cyan-400 group-hover:text-white transition-colors">
                      <span className="text-sm font-medium">立即使用</span>
                      <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </div>

                    <button
                      onClick={(e) => handleDeploy(e, tool)}
                      disabled={deploying !== null}
                      className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-300 ${
                        isDeployed
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                          : isDeploying
                          ? 'bg-white/5 text-gray-400 border border-white/10'
                          : 'bg-violet-500/15 text-violet-400 border border-violet-500/25 hover:bg-violet-500/25 hover:text-violet-300'
                      }`}
                    >
                      {isDeploying ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : isDeployed ? (
                        <Check className="w-3.5 h-3.5" />
                      ) : (
                        <MonitorDown className="w-3.5 h-3.5" />
                      )}
                      {isDeploying ? '部署中' : isDeployed ? '已部署' : '部署'}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
