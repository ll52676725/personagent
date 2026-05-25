import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Edit, Calendar, Tag, Send } from 'lucide-react';
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
      case 0: return { label: '草稿', color: 'bg-gray-100 text-gray-600' };
      case 1: return { label: '已发布', color: 'bg-green-100 text-green-600' };
      case 2: return { label: '已归档', color: 'bg-orange-100 text-orange-600' };
      default: return { label: '未知', color: 'bg-gray-100 text-gray-500' };
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
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (!article) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">文章不存在</p>
        <button
          onClick={() => navigate('/articles')}
          className="text-indigo-600 hover:text-indigo-700 font-medium mt-2"
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
          className="p-2 text-gray-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-800">{article.title}</h1>
          <div className="flex items-center gap-4 mt-2">
            <span className={`px-3 py-1 rounded-full text-sm font-medium ${status.color}`}>
              {status.label}
            </span>
            <span className="flex items-center gap-1 text-gray-500 text-sm">
              <Calendar className="w-4 h-4" />
              {new Date(article.createdAt).toLocaleDateString()}
            </span>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-end gap-3">
        <button
          onClick={() => navigate(`/articles/${article.id}/edit`)}
          className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition flex items-center gap-2"
        >
          <Edit className="w-4 h-4" />
          编辑
        </button>
        {article.status === 0 && (
          <button
            onClick={() => setPublishModal(true)}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition flex items-center gap-2"
          >
            <Send className="w-4 h-4" />
            发布文章
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm p-8">
        {article.summary && (
          <div className="mb-6 pb-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-800 mb-2">概要</h2>
            <p className="text-gray-600">{article.summary}</p>
          </div>
        )}

        {tagList.length > 0 && (
          <div className="mb-6">
            <div className="flex items-center gap-2 mb-3">
              <Tag className="w-4 h-4 text-gray-500" />
              <span className="text-sm font-medium text-gray-700">标签</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {tagList.map((tag) => (
                <span key={tag} className="px-3 py-1 bg-indigo-100 text-indigo-700 rounded-full text-sm">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}

        <div>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">正文</h2>
          <div className="prose prose-indigo max-w-none">
            {article.content ? (
              <pre className="whitespace-pre-wrap text-gray-700 bg-gray-50 p-4 rounded-lg">{article.content}</pre>
            ) : (
              <p className="text-gray-400 italic">暂无正文内容</p>
            )}
          </div>
        </div>
      </div>

      {publishModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-2">发布文章</h3>
            <p className="text-gray-500 text-sm mb-4">选择要发布的平台</p>
            
            <div className="space-y-2">
              {['csdn', 'juejin', 'zhihu'].map((platform) => (
                <button
                  key={platform}
                  onClick={() => togglePlatform(platform)}
                  className={`w-full px-4 py-3 rounded-lg border-2 transition flex items-center justify-between ${
                    platforms.includes(platform)
                      ? 'border-indigo-500 bg-indigo-50'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <span className="font-medium text-gray-800">
                    {platform === 'csdn' ? 'CSDN' : platform === 'juejin' ? '掘金' : '知乎'}
                  </span>
                  {platforms.includes(platform) && (
                    <svg className="w-5 h-5 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              ))}
            </div>

            <div className="flex gap-3 mt-4">
              <button
                onClick={() => {
                  setPublishModal(false);
                  setPlatforms([]);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
              >
                取消
              </button>
              <button
                onClick={handlePublish}
                disabled={platforms.length === 0}
                className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
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