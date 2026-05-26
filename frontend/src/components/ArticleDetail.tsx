import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Edit, Calendar, Tag, Send, Check, X } from 'lucide-react';
import { articleApi } from '@/api';
import { Article } from '@/types';

export default function ArticleDetail() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const [article, setArticle] = useState<Article | null>(null);
  const [loading, setLoading] = useState(true);
  const [publishModal, setPublishModal] = useState(false);
  const [platforms, setPlatforms] = useState<string[]>([]);

  useEffect(() => {
    const fetchArticle = async () => {
      if (!id) return;
      try {
        const response = await articleApi.getById(parseInt(id));
        if (response.code === 200) {
          setArticle(response.data);
        }
      } catch (err) {
        console.error('Failed to fetch article:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchArticle();
  }, [id]);

  const handlePublish = async () => {
    if (!id) return;
    try {
      const response = await articleApi.publish(parseInt(id), platforms);
      if (response.code === 200) {
        setPublishModal(false);
        setPlatforms([]);
        window.location.reload();
      }
    } catch (err) {
      console.error('Failed to publish:', err);
    }
  };

  const getStatusLabel = (status: number) => {
    switch (status) {
      case 0: return { label: '草稿', color: 'bg-amber-500/20 text-amber-400 border-amber-500/30' };
      case 1: return { label: '已发布', color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30' };
      case 2: return { label: '已归档', color: 'bg-gray-500/20 text-gray-400 border-gray-500/30' };
      default: return { label: '未知', color: 'bg-gray-500/20 text-gray-400 border-gray-500/30' };
    }
  };

  const togglePlatform = (platform: string) => {
    setPlatforms(prev => 
      prev.includes(platform) 
        ? prev.filter(p => p !== platform)
        : [...prev, platform]
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center mx-auto mb-4 animate-pulse">
            <Tag className="w-8 h-8 text-white" />
          </div>
          <p className="text-gray-400">加载中...</p>
        </div>
      </div>
    );
  }

  if (!article) {
    return (
      <div className="text-center py-12 glass-card rounded-3xl">
        <p className="text-gray-400">文章不存在</p>
        <button
          onClick={() => navigate('/articles')}
          className="text-cyan-400 hover:text-cyan-300 font-medium mt-4 transition-colors"
        >
          返回文章列表
        </button>
      </div>
    );
  }

  const status = getStatusLabel(article.status);
  const tagList = article.tags ? article.tags.split(',').filter(t => t.trim()) : [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/articles')}
          className="p-3 rounded-xl hover:bg-white/5 text-gray-400 hover:text-white transition-all"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-white">{article.title}</h1>
          <div className="flex items-center gap-4 mt-2">
            <span className={`px-3 py-1 rounded-full text-sm font-medium border ${status.color}`}>
              {status.label}
            </span>
            <span className="flex items-center gap-1 text-gray-400 text-sm">
              <Calendar className="w-4 h-4" />
              {new Date(article.createdAt).toLocaleDateString('zh-CN')}
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-end gap-3">
        <button
          onClick={() => navigate(`/articles/${article.id}/edit`)}
          className="btn-secondary flex items-center gap-2"
        >
          <Edit className="w-4 h-4" />
          编辑
        </button>
        {article.status === 0 && (
          <button
            onClick={() => setPublishModal(true)}
            className="btn-primary flex items-center gap-2"
          >
            <Send className="w-4 h-4" />
            发布文章
          </button>
        )}
      </div>

      <div className="glass-card rounded-3xl p-8">
        {article.summary && (
          <div className="mb-6 pb-6 border-b border-white/10">
            <h2 className="text-lg font-semibold text-white mb-3">概要</h2>
            <p className="text-gray-300 leading-relaxed">{article.summary}</p>
          </div>
        )}

        {tagList.length > 0 && (
          <div className="mb-6">
            <div className="flex items-center gap-2 mb-3">
              <Tag className="w-4 h-4 text-gray-400" />
              <span className="text-sm font-medium text-gray-300">标签</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {tagList.map((tag) => (
                <span key={tag} className="px-3 py-1 bg-indigo-500/20 text-cyan-300 rounded-full text-sm border border-indigo-500/30">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}

        <div>
          <h2 className="text-lg font-semibold text-white mb-4">正文</h2>
          <div className="text-gray-300 leading-relaxed">
            {article.content ? (
              <pre className="whitespace-pre-wrap font-sans bg-white/5 p-6 rounded-2xl border border-white/10">{article.content}</pre>
            ) : (
              <p className="text-gray-500 italic">暂无正文内容</p>
            )}
          </div>
        </div>
      </div>

      {publishModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setPublishModal(false)} />
          <div className="relative glass-card rounded-3xl p-6 w-full max-w-md animate-fadeIn">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-bold text-white">发布文章</h3>
              <button
                onClick={() => { setPublishModal(false); setPlatforms([]); }}
                className="p-2 rounded-xl hover:bg-white/10 text-gray-400 hover:text-white transition-all"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-gray-400 text-sm mb-6">选择要发布的平台</p>
            
            <div className="space-y-3">
              {['csdn', 'juejin', 'zhihu'].map((platform) => (
                <button
                  key={platform}
                  onClick={() => togglePlatform(platform)}
                  className={`w-full px-4 py-4 rounded-2xl border transition-all flex items-center justify-between ${
                    platforms.includes(platform)
                      ? 'border-cyan-400/50 bg-cyan-500/10 shadow-lg shadow-cyan-500/20'
                      : 'border-white/10 hover:border-white/20 bg-white/5'
                  }`}
                >
                  <span className="font-medium text-white">
                    {platform === 'csdn' ? 'CSDN' : platform === 'juejin' ? '掘金' : '知乎'}
                  </span>
                  {platforms.includes(platform) && (
                    <div className="w-6 h-6 rounded-full bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center">
                      <Check className="w-4 h-4 text-white" />
                    </div>
                  )}
                </button>
              ))}
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => {
                  setPublishModal(false);
                  setPlatforms([]);
                }}
                className="flex-1 btn-secondary"
              >
                取消
              </button>
              <button
                onClick={handlePublish}
                disabled={platforms.length === 0}
                className="flex-1 btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
              >
                发布到 {platforms.length} 个平台
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
