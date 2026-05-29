import { useNavigate } from 'react-router-dom';
import { Image, ArrowRight, FileText, Code2, HardDrive, Database } from 'lucide-react';

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
    id: 'image-converter',
    title: '图片格式转换',
    description: '支持 JPG、PNG、GIF、WebP、TIFF 等多种主流格式的互相转换，可调节压缩质量和尺寸',
    icon: Image,
    gradient: 'from-cyan-500 to-blue-600',
    path: '/tools/image-converter',
    badge: '新',
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
];

export default function ToolPanel() {
  const navigate = useNavigate();

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white mb-2">
            工具集
          </h1>
          <p className="text-gray-400">
            实用工具箱，助您高效创作
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {tools.map((tool, index) => {
          const Icon = tool.icon;
          return (
            <div
              key={tool.id}
              className="glass-card rounded-3xl p-6 glass-card-hover cursor-pointer group animate-fadeIn opacity-0"
              style={{ animationDelay: `${index * 100}ms`, animationFillMode: 'forwards' }}
              onClick={() => navigate(tool.path)}
            >
              <div className="relative mb-4">
                <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${tool.gradient} flex items-center justify-center shadow-lg`}>
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
              <div className="flex items-center text-cyan-400 group-hover:text-white transition-colors">
                <span className="text-sm font-medium">立即使用</span>
                <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
