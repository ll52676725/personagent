import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, 
  BookOpen, 
  TrendingUp, 
  Clock,
  ArrowRight,
  Zap,
  PenTool,
  Brain,
  Rocket,
  Plus,
  MessageSquare,
  Image as ImageIcon,
  Code
} from 'lucide-react';
import { agentApi, articleApi } from '@/api';
import { Agent, Article } from '@/types';

export default function Dashboard() {
  const navigate = useNavigate();
  const [agents, setAgents] = useState<Agent[]>([]);
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [agentsRes, articlesRes] = await Promise.all([
          agentApi.getAll(),
          articleApi.getAll(),
        ]);
        if (agentsRes.code === 200) setAgents(agentsRes.data);
        if (articlesRes.code === 200) {
          const data = articlesRes.data as any;
          setArticles(Array.isArray(data) ? data : data?.content || []);
        }
      } catch (err) {
        console.error('Failed to fetch data:', err);
        setLoading(false);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const stats = [
    { 
      label: '可用Agent', 
      value: agents.length, 
      icon: Sparkles, 
      gradient: 'from-violet-500 via-purple-500 to-fuchsia-500',
      glow: 'shadow-violet-500/30',
      description: '智能助手待命',
      size: 'md'
    },
    { 
      label: '创作文章', 
      value: articles.length, 
      icon: PenTool, 
      gradient: 'from-indigo-500 via-blue-500 to-cyan-400',
      glow: 'shadow-indigo-500/30',
      description: '内容资产累计',
      size: 'md'
    },
    { 
      label: '已发布', 
      value: articles.filter(a => a.status === 1).length, 
      icon: Rocket, 
      gradient: 'from-emerald-500 via-teal-500 to-cyan-400',
      glow: 'shadow-emerald-500/30',
      description: '对外展示内容',
      size: 'md'
    },
    { 
      label: '草稿箱', 
      value: articles.filter(a => a.status === 0).length, 
      icon: Clock, 
      gradient: 'from-amber-500 via-orange-500 to-pink-500',
      glow: 'shadow-amber-500/30',
      description: '待完善内容',
      size: 'md'
    },
  ];

  const quickActions = [
    {
      title: '创建文章',
      description: '使用 AI 快速生成高质量文章',
      icon: PenTool,
      gradient: 'from-indigo-500 to-cyan-400',
      action: () => navigate('/articles/new'),
      size: 'large'
    },
    {
      title: '探索 Agent',
      description: '发现更多智能 Agent 能力',
      icon: Brain,
      gradient: 'from-violet-500 to-purple-600',
      action: () => navigate('/agents'),
      size: 'small'
    },
    {
      title: '图像生成',
      description: 'AI 驱动的创意图像创作',
      icon: ImageIcon,
      gradient: 'from-pink-500 to-rose-500',
      action: () => navigate('/tools'),
      size: 'small'
    },
    {
      title: '代码助手',
      description: '智能编程辅助与代码审查',
      icon: Code,
      gradient: 'from-emerald-500 to-teal-400',
      action: () => navigate('/tools'),
      size: 'large'
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-indigo-500 to-cyan-400 rounded-3xl animate-pulse" />
            <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
              <Zap className="w-10 h-10 text-white" />
            </div>
          </div>
          <p className="text-gray-400 animate-pulse">加载中...</p>
        </div>
      </div>
    );
  }

  const publishedCount = articles.filter(a => a.status === 1).length;
  const publishRate = articles.length > 0 ? Math.round((publishedCount / articles.length) * 100) : 0;

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <span className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse" />
            <span className="text-sm text-emerald-400 font-medium">系统运行正常</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            欢迎回来，<span className="gradient-text-aurora">创作者</span>
          </h1>
          <p className="text-gray-400 text-lg">
            探索 AI 创作的无限可能，让灵感触手可及
          </p>
        </div>
        <button
          onClick={() => navigate('/articles/new')}
          className="btn-primary flex items-center gap-2 self-start md:self-auto text-base px-6 py-3.5 group"
        >
          <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
          创建新文章
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div 
              key={stat.label}
              className="stat-card animate-fadeIn opacity-0"
              style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
            >
              <div className="flex items-start justify-between mb-5">
                <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${stat.gradient} flex items-center justify-center shadow-xl ${stat.glow}`}>
                  <Icon className="w-7 h-7 text-white" />
                </div>
                <div className="text-right">
                  <span className="text-3xl font-bold text-white tracking-tight">{stat.value}</span>
                </div>
              </div>
              <p className="text-white font-semibold text-lg">{stat.label}</p>
              <p className="text-gray-400 text-sm mt-1">{stat.description}</p>
              
              <div className="mt-4 h-1 bg-white/5 rounded-full overflow-hidden">
                <div 
                  className={`h-full bg-gradient-to-r ${stat.gradient} rounded-full transition-all duration-1000`}
                  style={{ width: `${Math.min(stat.value * 10, 100)}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 space-y-6">
          <div className="relative overflow-hidden rounded-3xl glass-card-strong p-6">
            <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-br from-indigo-500/20 via-violet-500/10 to-transparent rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
            <div className="absolute bottom-0 left-0 w-64 h-64 bg-gradient-to-tr from-cyan-500/15 to-transparent rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />
            
            <div className="relative">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-2xl font-bold text-white tracking-tight">快捷操作</h2>
                  <p className="text-gray-400 mt-1">快速开始您的创作之旅</p>
                </div>
              </div>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {quickActions.map((action, index) => {
                  const Icon = action.icon;
                  const isLarge = action.size === 'large';
                  return (
                    <button
                      key={action.title}
                      onClick={action.action}
                      className={`group relative overflow-hidden rounded-2xl p-5 text-left transition-all duration-500 hover:scale-[1.02] glass-card-hover animate-fadeIn opacity-0 ${isLarge ? 'md:col-span-2' : ''}`}
                      style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
                    >
                      <div className={`absolute inset-0 bg-gradient-to-br ${action.gradient} opacity-0 group-hover:opacity-10 transition-opacity duration-500`} />
                      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                      
                      <div className="relative">
                        <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${action.gradient} flex items-center justify-center mb-4 shadow-xl group-hover:scale-110 transition-transform duration-300`}>
                          <Icon className="w-6 h-6 text-white" />
                        </div>
                        <h3 className="text-white font-semibold text-lg mb-1">{action.title}</h3>
                        <p className="text-gray-400 text-sm mb-4">{action.description}</p>
                        <div className="flex items-center text-cyan-400 group-hover:text-white transition-colors">
                          <span className="text-sm font-medium">立即开始</span>
                          <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                        </div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div className="relative overflow-hidden rounded-3xl glass-card p-6">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
            
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-xl font-bold text-white">最近文章</h2>
                <p className="text-gray-400 text-sm mt-1">您的创作记录</p>
              </div>
              <button 
                onClick={() => navigate('/articles')}
                className="group text-cyan-400 hover:text-cyan-300 text-sm font-medium flex items-center gap-1 transition-colors"
              >
                查看全部
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </button>
            </div>
            
            {articles.length > 0 ? (
              <div className="space-y-2">
                {articles.slice(0, 4).map((article, index) => (
                  <div 
                    key={article.id}
                    className="group flex items-center gap-4 p-4 rounded-2xl hover:bg-white/5 transition-all cursor-pointer animate-fadeIn opacity-0 border border-transparent hover:border-white/5"
                    style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}
                    onClick={() => navigate(`/articles/${article.id}`)}
                  >
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 transition-all group-hover:scale-110 ${
                      article.status === 1 
                        ? 'bg-gradient-to-br from-emerald-500/20 to-teal-500/20 text-emerald-400' 
                        : 'bg-gradient-to-br from-amber-500/20 to-orange-500/20 text-amber-400'
                    }`}>
                      <BookOpen className="w-6 h-6" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white font-medium truncate group-hover:text-cyan-300 transition-colors">
                        {article.title}
                      </p>
                      <div className="flex items-center gap-3 mt-1">
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
                          article.status === 1 
                            ? 'bg-emerald-500/10 text-emerald-400' 
                            : 'bg-amber-500/10 text-amber-400'
                        }`}>
                          {article.status === 1 ? '已发布' : '草稿'}
                        </span>
                        <span className="text-sm text-gray-500">
                          {new Date(article.createdAt).toLocaleDateString('zh-CN')}
                        </span>
                      </div>
                    </div>
                    <ArrowRight className="w-5 h-5 text-gray-600 group-hover:text-cyan-400 group-hover:translate-x-1 transition-all opacity-0 group-hover:opacity-100" />
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-16">
                <div className="relative w-24 h-24 mx-auto mb-6">
                  <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/20 to-cyan-500/20 rounded-full animate-pulse" />
                  <div className="absolute inset-2 bg-white/5 rounded-full flex items-center justify-center">
                    <BookOpen className="w-10 h-10 text-gray-500" />
                  </div>
                </div>
                <p className="text-gray-400 mb-4 text-lg">还没有文章</p>
                <button 
                  onClick={() => navigate('/articles/new')}
                  className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors inline-flex items-center gap-2"
                >
                  创建第一篇文章
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="lg:col-span-4 space-y-6">
          <div className="relative overflow-hidden rounded-3xl glass-card p-6">
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-violet-500/25 to-transparent rounded-full blur-2xl" />
            
            <div className="relative">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-xl font-bold text-white">智能 Agent</h2>
                  <p className="text-gray-400 text-sm mt-1">您的 AI 助手</p>
                </div>
              </div>
              
              <div className="space-y-2">
                {agents.slice(0, 4).map((agent, index) => (
                  <div 
                    key={agent.id}
                    className="group flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 transition-all cursor-pointer animate-fadeIn opacity-0 border border-transparent hover:border-white/5"
                    style={{ animationDelay: `${index * 50 + 100}ms`, animationFillMode: 'forwards' }}
                    onClick={() => navigate('/agents')}
                  >
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 flex items-center justify-center flex-shrink-0 shadow-lg shadow-violet-500/25 group-hover:scale-110 transition-transform">
                      <Sparkles className="w-5 h-5 text-white" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white font-medium truncate">{agent.name}</p>
                      <p className="text-xs text-gray-400 truncate">{agent.description}</p>
                    </div>
                  </div>
                ))}
              </div>

              <button 
                onClick={() => navigate('/agents')}
                className="w-full mt-4 py-3 rounded-xl border border-white/10 text-gray-400 hover:text-white hover:border-white/20 hover:bg-white/5 transition-all text-sm font-medium group"
              >
                查看全部 Agent
                <ArrowRight className="w-4 h-4 inline-block ml-2 group-hover:translate-x-1 transition-transform" />
              </button>
            </div>
          </div>

          <div className="relative overflow-hidden rounded-3xl glass-card-strong p-6">
            <div className="absolute top-0 right-0 w-40 h-40 bg-gradient-to-br from-indigo-500/30 via-violet-500/20 to-cyan-500/20 rounded-full blur-3xl" />
            <div className="absolute bottom-0 left-0 w-32 h-32 bg-gradient-to-tr from-emerald-500/20 to-transparent rounded-full blur-2xl" />
            
            <div className="relative">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 via-violet-500 to-cyan-400 flex items-center justify-center mb-4 shadow-xl shadow-indigo-500/30">
                <TrendingUp className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">创作统计</h3>
              <p className="text-gray-400 text-sm mb-6">本周创作活跃度</p>
              
              <div className="space-y-5">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-400 text-sm">文章数量</span>
                    <span className="text-white font-semibold">{articles.length} 篇</span>
                  </div>
                  <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-gradient-to-r from-indigo-500 to-cyan-400 rounded-full"
                      style={{ width: `${Math.min(articles.length * 5, 100)}%` }}
                    />
                  </div>
                </div>
                
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-400 text-sm">发布率</span>
                    <span className="text-emerald-400 font-semibold">{publishRate}%</span>
                  </div>
                  <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-gradient-to-r from-emerald-500 to-teal-400 rounded-full"
                      style={{ width: `${publishRate}%` }}
                    />
                  </div>
                </div>
                
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-400 text-sm">可用 Agent</span>
                    <span className="text-violet-400 font-semibold">{agents.length} 个</span>
                  </div>
                  <div className="h-2 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-gradient-to-r from-violet-500 to-purple-500 rounded-full"
                      style={{ width: `${Math.min(agents.length * 10, 100)}%` }}
                    />
                  </div>
                </div>
              </div>

              <div className="mt-6 pt-6 border-t border-white/5">
                <div className="flex items-center gap-3 p-3 rounded-xl bg-gradient-to-r from-indigo-500/10 to-transparent">
                  <MessageSquare className="w-5 h-5 text-cyan-400" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-white font-medium">AI 小贴士</p>
                    <p className="text-xs text-gray-400 truncate">尝试使用合集功能批量管理文章</p>
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
