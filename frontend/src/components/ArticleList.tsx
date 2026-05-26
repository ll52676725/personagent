import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Plus, Edit, Trash2, Eye, Calendar, Sparkles, Search, X, Copy, Check, TrendingUp } from 'lucide-react';
import { articleApi, generateApi } from '@/api';
import { Article } from '@/types';

export default function ArticleList() {
  const navigate = useNavigate();
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [generateType, setGenerateType] = useState<'title' | 'summary' | 'content' | 'outline'>('title');
  const [generateInput, setGenerateInput] = useState('');
  const [generatedContent, setGeneratedContent] = useState('');
  const [copied, setCopied] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

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

  const handleGenerate = () => {
    if (!generateInput.trim()) return;
    
    setGenerating(true);
    setGeneratedContent('');
    
    const onChunk = (chunk: string) => {
      setGeneratedContent(prev => prev + chunk);
    };
    
    const onComplete = () => {
      setGenerating(false);
    };
    
    const onError = (error: string) => {
      console.error('Generate error:', error);
      setGenerating(false);
    };

    if (generateType === 'title') {
      generateApi.titleStream(generateInput, undefined, onChunk, onComplete, onError);
    } else if (generateType === 'summary') {
      generateApi.summaryStream(generateInput, undefined, onChunk, onComplete, onError);
    } else if (generateType === 'content') {
      generateApi.contentStream(generateInput, undefined, onChunk, onComplete, onError);
    } else {
      generateApi.outlineStream(generateInput, undefined, onChunk, onComplete, onError);
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

  const handleCopyContent = () => {
    navigator.clipboard.writeText(generatedContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const getStatusLabel = (status: number) => {
    switch (status) {
      case 0: return { label: '草稿', color: 'bg-amber-500/20 text-amber-400 border-amber-500/30' };
      case 1: return { label: '已发布', color: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30' };
      case 2: return { label: '已归档', color: 'bg-gray-500/20 text-gray-400 border-gray-500/30' };
      default: return { label: '未知', color: 'bg-gray-500/20 text-gray-400 border-gray-500/30' };
    }
  };

  const filteredArticles = articles.filter(a => 
    a.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center mx-auto mb-4 animate-pulse">
            <BookOpen className="w-8 h-8 text-white" />
          </div>
          <p className="text-gray-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-white mb-1">
            文章创作
          </h1>
          <p className="text-gray-400">
            管理您的所有文章内容
          </p>
        </div>
        <div className="flex gap-3 flex-wrap">
          <button
            onClick={() => setShowGenerateModal(true)}
            className="btn-secondary flex items-center gap-2"
          >
            <Sparkles className="w-5 h-5" />
            AI 生成
          </button>
          <button
            onClick={() => navigate('/articles/new')}
            className="btn-primary flex items-center gap-2"
          >
            <Plus className="w-5 h-5" />
            创建文章
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="stat-card">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center">
              <BookOpen className="w-6 h-6 text-white" />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{articles.length}</p>
              <p className="text-gray-400 text-sm">总文章数</p>
            </div>
          </div>
        </div>
        <div className="stat-card">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-400 flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-white" />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{articles.filter(a => a.status === 1).length}</p>
              <p className="text-gray-400 text-sm">已发布</p>
            </div>
          </div>
        </div>
        <div className="stat-card">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-amber-500 to-orange-400 flex items-center justify-center">
              <Calendar className="w-6 h-6 text-white" />
            </div>
            <div>
              <p className="text-2xl font-bold text-white">{articles.filter(a => a.status === 0).length}</p>
              <p className="text-gray-400 text-sm">草稿箱</p>
            </div>
          </div>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
          <h2 className="text-xl font-bold text-white">所有文章</h2>
          <div className="relative">
            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-500" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="搜索文章..."
              className="input-field pl-12 w-full md:w-72"
            />
          </div>
        </div>

        {filteredArticles.length > 0 ? (
          <div className="space-y-3">
            {filteredArticles.map((article, index) => {
              const status = getStatusLabel(article.status);
              return (
                <div
                  key={article.id}
                  className="group flex flex-col md:flex-row md:items-center gap-4 p-5 rounded-2xl hover:bg-white/5 transition-all cursor-pointer animate-fadeIn opacity-0"
                  style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}
                  onClick={() => navigate(`/articles/${article.id}`)}
                >
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500/20 to-cyan-500/20 flex items-center justify-center flex-shrink-0 group-hover:scale-110 transition-transform">
                    <BookOpen className="w-7 h-7 text-cyan-400" />
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-1">
                      <h3 className="text-white font-semibold text-lg truncate group-hover:text-cyan-300 transition-colors">
                        {article.title}
                      </h3>
                      <span className={`px-3 py-1 rounded-full text-xs font-medium border ${status.color} flex-shrink-0`}>
                        {status.label}
                      </span>
                    </div>
                    <p className="text-gray-400 text-sm truncate">
                      {article.summary || '暂无摘要'}
                    </p>
                    <div className="flex items-center gap-4 mt-2 text-xs text-gray-500">
                      <span className="flex items-center gap-1">
                        <Calendar className="w-3 h-3" />
                        {new Date(article.createdAt).toLocaleDateString('zh-CN')}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 flex-shrink-0" onClick={(e) => e.stopPropagation()}>
                    <button
                      onClick={() => navigate(`/articles/${article.id}`)}
                      className="p-3 rounded-xl hover:bg-white/5 text-gray-400 hover:text-white transition-all"
                      title="查看"
                    >
                      <Eye className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => navigate(`/articles/${article.id}/edit`)}
                      className="p-3 rounded-xl hover:bg-white/5 text-gray-400 hover:text-cyan-400 transition-all"
                      title="编辑"
                    >
                      <Edit className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleDelete(article.id)}
                      className="p-3 rounded-xl hover:bg-red-500/10 text-gray-400 hover:text-red-400 transition-all"
                      title="删除"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="text-center py-16">
            <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
              <BookOpen className="w-10 h-10 text-gray-500" />
            </div>
            <h3 className="text-white font-semibold text-lg mb-2">
              {searchTerm ? '未找到匹配的文章' : '还没有文章'}
            </h3>
            <p className="text-gray-400 mb-6">
              {searchTerm ? '尝试使用其他关键词搜索' : '开始创作您的第一篇文章吧'}
            </p>
            {!searchTerm && (
              <button
                onClick={() => navigate('/articles/new')}
                className="btn-primary"
              >
                创建第一篇文章
              </button>
            )}
          </div>
        )}
      </div>

      {showGenerateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowGenerateModal(false)} />
          <div className="relative glass-card rounded-3xl p-6 w-full max-w-3xl max-h-[85vh] overflow-hidden flex flex-col animate-fadeIn">
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-2xl font-bold text-white">AI 智能生成</h2>
                <p className="text-gray-400 text-sm mt-1">输入主题，AI 为您生成内容</p>
              </div>
              <button
                onClick={() => setShowGenerateModal(false)}
                className="p-3 rounded-xl hover:bg-white/10 text-gray-400 hover:text-white transition-all"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="flex gap-2 mb-6 flex-wrap">
              {([
                { id: 'title', label: '标题' },
                { id: 'summary', label: '摘要' },
                { id: 'outline', label: '大纲' },
                { id: 'content', label: '正文' },
              ] as const).map((type) => (
                <button
                  key={type.id}
                  onClick={() => {
                    setGenerateType(type.id);
                    setGeneratedContent('');
                  }}
                  className={`px-4 py-2 rounded-xl font-medium transition-all ${
                    generateType === type.id
                      ? 'bg-gradient-to-r from-indigo-500 to-cyan-400 text-white shadow-lg'
                      : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  {type.label}
                </button>
              ))}
            </div>

            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-300 mb-2">
                {generateType === 'title' || generateType === 'outline' ? '文章主题' : '文章标题'}
              </label>
              <input
                type="text"
                value={generateInput}
                onChange={(e) => setGenerateInput(e.target.value)}
                placeholder={generateType === 'title' || generateType === 'outline' ? '请输入文章主题，如：Java 并发编程' : '请输入文章标题'}
                className="input-field w-full text-lg"
                onKeyDown={(e) => e.key === 'Enter' && handleGenerate()}
                disabled={generating}
              />
            </div>

            <div className="flex-1 overflow-hidden flex flex-col min-h-[200px]">
              <div className="flex items-center justify-between mb-3">
                <span className="text-sm text-gray-400">
                  {generating ? '正在生成...' : '生成结果'}
                  {generatedContent && ` (${generatedContent.length} 字)`}
                </span>
                {generatedContent && (
                  <button
                    onClick={handleCopyContent}
                    className="text-sm text-cyan-400 hover:text-cyan-300 flex items-center gap-1 transition-colors"
                  >
                    {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                    {copied ? '已复制' : '复制'}
                  </button>
                )}
              </div>
              <div className="flex-1 overflow-auto glass-card rounded-2xl p-4 scrollbar-thin">
                {generating && !generatedContent ? (
                  <div className="flex items-center gap-3 text-gray-400">
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-cyan-400"></div>
                    <span>AI 正在思考中...</span>
                  </div>
                ) : generatedContent ? (
                  <div className="text-gray-300 whitespace-pre-wrap leading-relaxed">
                    {generatedContent}
                    {generating && <span className="inline-block w-2 h-5 bg-cyan-400 ml-1 animate-pulse align-middle" />}
                  </div>
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    <Sparkles className="w-10 h-10 mx-auto mb-3 opacity-50" />
                    <p>输入{generateType === 'title' || generateType === 'outline' ? '主题' : '标题'}后点击生成</p>
                  </div>
                )}
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button
                onClick={() => setShowGenerateModal(false)}
                className="flex-1 btn-secondary"
              >
                关闭
              </button>
              <button
                onClick={handleGenerate}
                disabled={generating || !generateInput.trim()}
                className="flex-1 btn-primary flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {generating ? (
                  <>
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                    生成中...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    开始生成
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
