import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, 
  BookOpen, 
  TrendingUp, 
  Clock,
  ArrowRight
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
        if (articlesRes.code === 200) setArticles(articlesRes.data);
      } catch (err) {
        console.error('Failed to fetch data:', err);
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
      color: 'bg-indigo-500',
      bgColor: 'bg-indigo-50'
    },
    { 
      label: '我的文章', 
      value: articles.length, 
      icon: BookOpen, 
      color: 'bg-green-500',
      bgColor: 'bg-green-50'
    },
    { 
      label: '已发布', 
      value: articles.filter(a => a.status === 1).length, 
      icon: TrendingUp, 
      color: 'bg-orange-500',
      bgColor: 'bg-orange-50'
    },
    { 
      label: '草稿', 
      value: articles.filter(a => a.status === 0).length, 
      icon: Clock, 
      color: 'bg-gray-500',
      bgColor: 'bg-gray-50'
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">欢迎回来</h1>
        <p className="text-gray-500 mt-1">这是您的个人Agent平台控制台</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div 
              key={stat.label}
              className="bg-white rounded-xl shadow-sm p-6 hover:shadow-md transition"
            >
              <div className={`w-12 h-12 ${stat.bgColor} rounded-lg flex items-center justify-center mb-4`}>
                <Icon className={`w-6 h-6 ${stat.color.replace('bg-', 'text-')}`} />
              </div>
              <p className="text-3xl font-bold text-gray-800">{stat.value}</p>
              <p className="text-gray-500 mt-1">{stat.label}</p>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800">推荐Agent</h2>
            <button 
              onClick={() => navigate('/agents')}
              className="text-indigo-600 hover:text-indigo-700 text-sm font-medium flex items-center gap-1"
            >
              查看全部 <ArrowRight className="w-4 h-4" />
            </button>
          </div>
          <div className="space-y-3">
            {agents.slice(0, 3).map((agent) => (
              <div 
                key={agent.id}
                className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 transition cursor-pointer"
                onClick={() => navigate('/agents')}
              >
                <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center">
                  <Sparkles className="w-5 h-5 text-indigo-600" />
                </div>
                <div className="flex-1">
                  <p className="font-medium text-gray-800">{agent.name}</p>
                  <p className="text-sm text-gray-500">{agent.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800">最近文章</h2>
            <button 
              onClick={() => navigate('/articles')}
              className="text-indigo-600 hover:text-indigo-700 text-sm font-medium flex items-center gap-1"
            >
              查看全部 <ArrowRight className="w-4 h-4" />
            </button>
          </div>
          <div className="space-y-3">
            {articles.length > 0 ? (
              articles.slice(0, 3).map((article) => (
                <div 
                  key={article.id}
                  className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 transition cursor-pointer"
                  onClick={() => navigate(`/articles/${article.id}`)}
                >
                  <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                    <BookOpen className="w-5 h-5 text-green-600" />
                  </div>
                  <div className="flex-1">
                    <p className="font-medium text-gray-800 truncate">{article.title}</p>
                    <p className="text-sm text-gray-500">
                      {article.status === 1 ? '已发布' : '草稿'} · {new Date(article.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-gray-500">
                <BookOpen className="w-12 h-12 mx-auto mb-2 text-gray-300" />
                <p>暂无文章</p>
                <button 
                  onClick={() => navigate('/articles')}
                  className="text-indigo-600 hover:text-indigo-700 text-sm font-medium mt-2"
                >
                  创建第一篇文章
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}