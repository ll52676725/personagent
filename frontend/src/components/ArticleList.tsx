import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Plus, Edit, Trash2, Eye, Calendar, Sparkles } from 'lucide-react';
import { articleApi, generateApi } from '@/api';
import { Article, GenerateResult } from '@/types';

export default function ArticleList() {
  const navigate = useNavigate();
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [generateType, setGenerateType] = useState<'title' | 'summary' | 'content'>('title');
  const [generateInput, setGenerateInput] = useState('');
  const [generateResult, setGenerateResult] = useState<GenerateResult | null>(null);

  useEffect(() => {
    fetchArticles();
  }, []);

  const fetchArticles = async () => {
    try {
      const response = await articleApi.getAll();
      if (response.code === 200) {
        setArticles(response.data);
      }
    } catch (err) {
      console.error('Failed to fetch articles:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async () => {
    if (!generateInput.trim()) return;
    
    setGenerating(true);
    setGenerateResult(null);
    
    try {
      let response;
      if (generateType === 'title') {
        response = await generateApi.title(generateInput);
      } else if (generateType === 'summary') {
        response = await generateApi.summary(generateInput);
      } else {
        response = await generateApi.content(generateInput);
      }
      
      if (response.code === 200) {
        setGenerateResult(response.data);
      }
    } catch (err) {
      console.error('Failed to generate:', err);
    } finally {
      setGenerating(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除这篇文章吗？')) return;
    
    try {
      const response = await articleApi.delete(id);
      if (response.code === 200) {
        setArticles(prev => prev.filter(a => a.id !== id));
      }
    } catch (err) {
      console.error('Failed to delete:', err);
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

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">文章管理</h1>
          <p className="text-gray-500 mt-1">管理您的技术文章</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => setShowGenerateModal(true)}
            className="px-4 py-2 border border-indigo-600 text-indigo-600 rounded-lg hover:bg-indigo-50 transition flex items-center gap-2"
          >
            <Sparkles className="w-4 h-4" />
            AI生成
          </button>
          <button
            onClick={() => navigate('/articles/new')}
            className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            新建文章
          </button>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">标题</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">创建时间</th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {articles.length > 0 ? (
              articles.map((article) => {
                const status = getStatusLabel(article.status);
                return (
                  <tr key={article.id} className="hover:bg-gray-50 transition">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <BookOpen className="w-5 h-5 text-indigo-500" />
                        <span className="font-medium text-gray-800">{article.title}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${status.color}`}>
                        {status.label}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="flex items-center gap-1 text-gray-500 text-sm">
                        <Calendar className="w-4 h-4" />
                        {new Date(article.createdAt).toLocaleDateString()}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => navigate(`/articles/${article.id}`)}
                          className="p-2 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                          title="查看"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => navigate(`/articles/${article.id}/edit`)}
                          className="p-2 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                          title="编辑"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(article.id)}
                          className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                          title="删除"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan={4} className="px-6 py-12 text-center">
                  <BookOpen className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                  <p className="text-gray-500">暂无文章</p>
                  <button
                    onClick={() => navigate('/articles/new')}
                    className="text-indigo-600 hover:text-indigo-700 font-medium mt-2"
                  >
                    创建第一篇文章
                  </button>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showGenerateModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-lg mx-4 max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">AI内容生成</h3>
            
            <div className="flex gap-2 mb-4">
              {(['title', 'summary', 'content'] as const).map((type) => (
                <button
                  key={type}
                  onClick={() => {
                    setGenerateType(type);
                    setGenerateResult(null);
                  }}
                  className={`flex-1 px-4 py-2 rounded-lg text-sm font-medium transition ${
                    generateType === type
                      ? 'bg-indigo-600 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {type === 'title' ? '生成标题' : type === 'summary' ? '生成概要' : '生成正文'}
                </button>
              ))}
            </div>

            {generateType === 'title' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">输入主题</label>
                <input
                  type="text"
                  value={generateInput}
                  onChange={(e) => {
                    setGenerateInput(e.target.value);
                    setGenerateResult(null);
                  }}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="例如：React Hooks 最佳实践"
                />
              </div>
            )}
            
            {generateType === 'summary' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">输入文章标题</label>
                <input
                  type="text"
                  value={generateInput}
                  onChange={(e) => {
                    setGenerateInput(e.target.value);
                    setGenerateResult(null);
                  }}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="例如：深入理解 React Hooks"
                />
              </div>
            )}
            
            {generateType === 'content' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">输入文章标题</label>
                <input
                  type="text"
                  value={generateInput}
                  onChange={(e) => {
                    setGenerateInput(e.target.value);
                    setGenerateResult(null);
                  }}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  placeholder="例如：Spring Boot 入门教程"
                />
              </div>
            )}

            <button
              onClick={handleGenerate}
              disabled={!generateInput.trim() || generating}
              className="w-full mt-4 px-4 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {generating ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                  生成中...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  生成内容
                </>
              )}
            </button>

            {generateResult && (
              <div className="mt-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">生成结果</label>
                {generateResult.items ? (
                  <div className="space-y-2">
                    {generateResult.items.map((item, index) => (
                      <div key={index} className="p-3 bg-gray-50 rounded-lg text-gray-800">
                        {item}
                      </div>
                    ))}
                  </div>
                ) : generateResult.content ? (
                  <textarea
                    value={generateResult.content}
                    readOnly
                    className="w-full h-64 p-3 border border-gray-300 rounded-lg bg-gray-50 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
                  />
                ) : null}
              </div>
            )}

            <button
              onClick={() => {
                setShowGenerateModal(false);
                setGenerateInput('');
                setGenerateResult(null);
              }}
              className="w-full mt-4 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
            >
              关闭
            </button>
          </div>
        </div>
      )}
    </div>
  );
}