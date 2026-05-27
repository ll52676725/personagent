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
  Rocket
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
      gradient: 'from-violet-500 to-purple-600',
      description: '智能助手待命'
    },
    { 
      label: '创作文章', 
      value: articles.length, 
      icon: PenTool, 
      gradient: 'from-indigo-500 to-cyan-400',
      description: '内容资产累计'
    },
    { 
      label: '已发布', 
      value: articles.filter(a => a.status === 1).length, 
      icon: Rocket, 
      gradient: 'from-emerald-500 to-teal-400',
      description: '对外展示内容'
    },
    { 
      label: '草稿箱', 
      value: articles.filter(a => a.status === 0).length, 
      icon: Clock, 
      gradient: 'from-amber-500 to-orange-400',
      description: '待完善内容'
    },
  ];

  const quickActions = [
    {
      title: '创建文章',
      description: '使用 AI 快速生成高质量文章',
      icon: PenTool,
      gradient: 'from-indigo-500 to-cyan-400',
      action: () => navigate('/articles/new')
    },
    {
      title: '探索 Agent',
      description: '发现更多智能 Agent 能力',
      icon: Brain,
      gradient: 'from-violet-500 to-purple-600',
      action: () => navigate('/agents')
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center mx-auto mb-4 animate-pulse">
            <Zap className="w-8 h-8 text-white" />
          </div>
          <p className="text-gray-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white mb-2">
            控制台
          </h1>
          <p className="text-gray-400">
            探索 AI 创作的无限可能
          </p>
        </div>
        <button
          onClick={() => navigate('/articles/new')}
          className="btn-primary flex items-center gap-2 self-start md:self-auto"
        >
          <PenTool className="w-5 h-5" />
          创建新文章
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div 
              key={stat.label}
              className="stat-card animate-fadeIn opacity-0"
              style={{ animationDelay: `${index * 100}ms`, animationFillMode: 'forwards' }}
            >
              <div className="flex items-start justify-between mb-4">
                <div className={`w-14 h-14 rounded-2xl bg-gradient-to-br ${stat.gradient} flex items-center justify-center shadow-lg`}>
                  <Icon className="w-7 h-7 text-white" />
                </div>
                <span className="text-3xl font-bold text-white">{stat.value}</span>
              </div>
              <p className="text-white font-medium text-lg">{stat.label}</p>
              <p className="text-gray-400 text-sm mt-1">{stat.description}</p>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="glass-card rounded-3xl p-6 glass-card-hover">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-xl font-bold text-white">快捷操作</h2>
                <p className="text-gray-400 text-sm mt-1">快速开始您的创作之旅</p>
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {quickActions.map((action) => {
                const Icon = action.icon;
                return (
                  <button
                    key={action.title}
                    onClick={action.action}
                    className="group relative overflow-hidden rounded-2xl p-5 text-left transition-all hover:scale-[1.02]"
                    style={{
                      background: `linear-gradient(135deg, var(--tw-gradient-stops))`,
                    }}
                  >
                    <div className={`absolute inset-0 bg-gradient-to-br ${action.gradient} opacity-10 group-hover:opacity-20 transition-opacity`} />
                    <div className="relative">
                      <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${action.gradient} flex items-center justify-center mb-4 shadow-lg`}>
                        <Icon className="w-6 h-6 text-white" />
                      </div>
                      <h3 className="text-white font-semibold text-lg mb-1">{action.title}</h3>
                      <p className="text-gray-400 text-sm">{action.description}</p>
                      <div className="mt-4 flex items-center text-cyan-400 group-hover:text-white transition-colors">
                        <span className="text-sm font-medium">立即开始</span>
                        <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-xl font-bold text-white">最近文章</h2>
                <p className="text-gray-400 text-sm mt-1">您的创作记录</p>
              </div>
              <button 
                onClick={() => navigate('/articles')}
                className="text-cyan-400 hover:text-cyan-300 text-sm font-medium flex items-center gap-1 transition-colors"
              >
                查看全部
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
            
            {articles.length > 0 ? (
              <div className="space-y-3">
                {articles.slice(0, 4).map((article, index) => (
                  <div 
                    key={article.id}
                    className="group flex items-center gap-4 p-4 rounded-2xl hover:bg-white/5 transition-all cursor-pointer animate-fadeIn opacity-0"
                    style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}
                    onClick={() => navigate(`/articles/${article.id}`)}
                  >
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 transition-all group-hover:scale-110 ${
                      article.status === 1 
                        ? 'bg-emerald-500/20 text-emerald-400' 
                        : 'bg-amber-500/20 text-amber-400'
                    }`}>
                      <BookOpen className="w-6 h-6" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-white font-medium truncate group-hover:text-cyan-300 transition-colors">
                        {article.title}
                      </p>
                      <p className="text-sm text-gray-400 mt-0.5">
                        {article.status === 1 ? '已发布' : '草稿'} · {new Date(article.createdAt).toLocaleDateString('zh-CN')}
                      </p>
                    </div>
                    <ArrowRight className="w-5 h-5 text-gray-500 group-hover:text-cyan-400 group-hover:translate-x-1 transition-all opacity-0 group-hover:opacity-100" />
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-12">
                <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
                  <BookOpen className="w-10 h-10 text-gray-500" />
                </div>
                <p className="text-gray-400 mb-4">还没有文章</p>
                <button 
                  onClick={() => navigate('/articles/new')}
                  className="text-cyan-400 hover:text-cyan-300 font-medium transition-colors"
                >
                  创建第一篇文章 →
                </button>
              </div>
            )}
          </div>
        </div>

        <div className="space-y-6">
          <div className="glass-card rounded-3xl p-6">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-xl font-bold text-white">智能 Agent</h2>
                <p className="text-gray-400 text-sm mt-1">您的 AI 助手</p>
              </div>
            </div>
            
            <div className="space-y-3">
              {agents.slice(0, 4).map((agent, index) => (
                <div 
                  key={agent.id}
                  className="group flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 transition-all cursor-pointer animate-fadeIn opacity-0"
                  style={{ animationDelay: `${index * 50 + 100}ms`, animationFillMode: 'forwards' }}
                  onClick={() => navigate('/agents')}
                >
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center flex-shrink-0 shadow-lg">
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
              className="w-full mt-4 py-3 rounded-xl border border-white/10 text-gray-400 hover:text-white hover:border-white/20 transition-all text-sm font-medium"
            >
              查看全部 Agent
            </button>
          </div>

          <div className="glass-card rounded-3xl p-6 overflow-hidden relative">
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-indigo-500/30 to-cyan-500/30 rounded-full blur-2xl" />
            <div className="relative">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center mb-4 shadow-lg">
                <TrendingUp className="w-6 h-6 text-white" />
              </div>
              <h3 className="text-lg font-bold text-white mb-2">创作统计</h3>
              <p className="text-gray-400 text-sm mb-4">本周创作活跃度</p>
              
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-gray-400 text-sm">文章数量</span>
                  <span className="text-white font-semibold">{articles.length} 篇</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400 text-sm">发布率</span>
                  <span className="text-emerald-400 font-semibold">
                    {articles.length > 0 
                      ? Math.round((articles.filter(a => a.status === 1).length / articles.length) * 100) 
                      : 0}%
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-gray-400 text-sm">可用 Agent</span>
                  <span className="text-cyan-400 font-semibold">{agents.length} 个</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
