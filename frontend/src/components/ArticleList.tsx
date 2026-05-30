import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Plus, Edit, Trash2, Eye, Calendar, Sparkles, Search, X, Copy, Check, TrendingUp, ArrowRight } from 'lucide-react';
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
      <div className="flex items-center justify-center py-20">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-indigo-500 to-cyan-400 rounded-3xl animate-pulse" />
            <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
              <BookOpen className="w-10 h-10 text-white" />
            </div>
          </div>
          <p className="text-gray-400 animate-pulse">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <BookOpen className="w-4 h-4 text-cyan-400" />
            <span className="text-sm text-cyan-400 font-medium">单篇内容创作</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            文章<span className="gradient-text-aurora">创作</span>
          </h1>
          <p className="text-gray-400 text-lg">
            管理您的所有文章内容，AI 助力高效创作
          </p>
        </div>
        <div className="flex gap-3 flex-wrap self-start md:self-auto">
          <button
            onClick={() => setShowGenerateModal(true)}
            className="btn-secondary flex items-center gap-2 text-base px-5 py-3"
          >
            <Sparkles className="w-5 h-5" />
            AI 生成
          </button>
          <button
            onClick={() => navigate('/articles/new')}
            className="btn-primary flex items-center gap-2 text-base px-6 py-3.5 group"
          >
            <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
            创建文章
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {[
          { 
            label: '总文章数', 
            value: articles.length, 
            icon: BookOpen, 
            gradient: 'from-indigo-500 via-blue-500 to-cyan-400',
            glow: 'shadow-indigo-500/30',
            description: '内容资产累计'
          },
          { 
            label: '已发布', 
            value: articles.filter(a => a.status === 1).length, 
            icon: TrendingUp, 
            gradient: 'from-emerald-500 via-teal-500 to-cyan-400',
            glow: 'shadow-emerald-500/30',
            description: '对外展示内容'
          },
          { 
            label: '草稿箱', 
            value: articles.filter(a => a.status === 0).length, 
            icon: Calendar, 
            gradient: 'from-amber-500 via-orange-500 to-pink-500',
            glow: 'shadow-amber-500/30',
            description: '待完善内容'
          },
        ].map((stat, index) => {
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

      <div className="relative">
        <div className="absolute inset-0 glass-card-strong rounded-2xl" />
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent rounded-t-2xl" />
        <div className="relative p-2">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
            <input
              type="text"
              placeholder="搜索文章..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input-field w-full pl-12 pr-4 py-3.5 text-base"
            />
            <kbd className="hidden md:inline-block absolute right-4 top-1/2 -translate-y-1/2 px-2 py-0.5 text-xs text-gray-500 bg-white/5 rounded border border-white/10">
              ⌘K
            </kbd>
          </div>
        </div>
      </div>

      <div className="relative overflow-hidden rounded-3xl glass-card p-6">
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-br from-indigo-500/20 via-cyan-500/10 to-transparent rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-gradient-to-tr from-violet-500/15 to-transparent rounded-full blur-2xl translate-y-1/2 -translate-x-1/2" />
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
        
        <div className="relative">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-white">所有文章</h2>
              <p className="text-gray-400 text-sm mt-1">您的创作记录</p>
            </div>
            <button 
              onClick={() => navigate('/articles/new')}
              className="group text-cyan-400 hover:text-cyan-300 text-sm font-medium flex items-center gap-1 transition-colors"
            >
              新建文章
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>

        {filteredArticles.length > 0 ? (
          <div className="space-y-2">
            {filteredArticles.map((article, index) => {
              const status = getStatusLabel(article.status);
              return (
                <div
                  key={article.id}
                  className="group relative overflow-hidden flex flex-col md:flex-row md:items-center gap-4 p-5 rounded-2xl transition-all duration-500 cursor-pointer animate-fadeIn opacity-0 border border-transparent hover:border-white/5 glass-card-hover"
                  style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}
                  onClick={() => navigate(`/articles/${article.id}`)}
                >
                  <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-indigo-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                  <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                  
                  <div className="relative flex items-start w-full md:w-auto gap-4">
                    <div className={`w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0 transition-all duration-300 group-hover:scale-110 ${
                      article.status === 1 
                        ? 'bg-gradient-to-br from-emerald-500/20 to-teal-500/20' 
                        : 'bg-gradient-to-br from-indigo-500/20 to-cyan-500/20'
                    }`}>
                      <BookOpen className={`w-7 h-7 ${article.status === 1 ? 'text-emerald-400' : 'text-cyan-400'}`} />
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
                  </div>

                  <div className="relative flex items-center gap-2 flex-shrink-0 w-full md:w-auto justify-end" onClick={(e) => e.stopPropagation()}>
                    <button
                      onClick={() => navigate(`/articles/${article.id}`)}
                      className="p-3 rounded-xl hover:bg-white/5 text-gray-400 hover:text-white transition-all duration-300"
                      title="查看"
                    >
                      <Eye className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => navigate(`/articles/${article.id}/edit`)}
                      className="p-3 rounded-xl hover:bg-white/5 text-gray-400 hover:text-cyan-400 transition-all duration-300"
                      title="编辑"
                    >
                      <Edit className="w-5 h-5" />
                    </button>
                    <button
                      onClick={() => handleDelete(article.id)}
                      className="p-3 rounded-xl hover:bg-red-500/10 text-gray-400 hover:text-red-400 transition-all duration-300 group/delete"
                      title="删除"
                    >
                      <Trash2 className="w-5 h-5 group-hover/delete:scale-110 transition-transform" />
                    </button>
                    <ArrowRight className="w-5 h-5 text-gray-600 group-hover:text-cyan-400 group-hover:translate-x-1 transition-all duration-300 opacity-0 group-hover:opacity-100 ml-2 hidden md:block" />
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="relative text-center py-20">
            <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-indigo-500/20 to-transparent rounded-full blur-3xl" />
            <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-cyan-500/15 to-transparent rounded-full blur-2xl" />
            <div className="relative">
              <div className="relative w-24 h-24 mx-auto mb-6">
                <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/20 to-cyan-500/20 rounded-full animate-pulse" />
                <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                  <BookOpen className="w-10 h-10 text-gray-400" />
                </div>
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
                  className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors inline-flex items-center gap-2"
                >
                  创建第一篇文章
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </button>
              )}
            </div>
          </div>
        )}
        </div>
      </div>

      {showGenerateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={() => setShowGenerateModal(false)} />
          <div className="relative glass-card-strong rounded-3xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col animate-scaleIn shadow-2xl">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
            <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-indigo-500/20 to-transparent rounded-full blur-3xl" />
            <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-cyan-500/15 to-transparent rounded-full blur-2xl" />
            
            <div className="flex items-center justify-between p-6 border-b border-white/5 flex-shrink-0 relative">
              <div>
                <h2 className="text-2xl font-bold text-white">AI 智能生成</h2>
                <p className="text-gray-400 text-sm mt-1">输入主题，AI 为您生成内容</p>
              </div>
              <button
                onClick={() => setShowGenerateModal(false)}
                className="p-3 rounded-xl hover:bg-white/10 text-gray-400 hover:text-white transition-all duration-300 flex-shrink-0"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="p-6 space-y-6 overflow-y-auto relative flex-1">
              <div className="flex gap-2 flex-wrap">
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
                    className={`px-4 py-2 rounded-xl font-medium transition-all duration-300 ${
                      generateType === type.id
                        ? 'bg-gradient-to-r from-indigo-500 to-cyan-400 text-white shadow-lg shadow-indigo-500/25'
                        : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white'
                    }`}
                  >
                    {type.label}
                  </button>
                ))}
              </div>

              <div>
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

              <div className="flex-1 overflow-hidden flex flex-col min-h-[250px]">
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
                    <div className="text-center py-12 text-gray-500">
                      <div className="relative w-16 h-16 mx-auto mb-4">
                        <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/20 to-cyan-500/20 rounded-full animate-pulse" />
                        <div className="absolute inset-1 glass-card rounded-full flex items-center justify-center">
                          <Sparkles className="w-8 h-8 text-cyan-400" />
                        </div>
                      </div>
                      <p>输入{generateType === 'title' || generateType === 'outline' ? '主题' : '标题'}后点击生成</p>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="p-6 border-t border-white/5 flex gap-3 flex-shrink-0 relative">
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
