import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Plus, Trash2, Sparkles, Search, X, Check, Layers, ArrowRight } from 'lucide-react';
import { collectionApi } from '@/api';
import { Collection, CollectionOutline, Article } from '@/types';

export default function CollectionList() {
  const navigate = useNavigate();
  const [collections, setCollections] = useState<Collection[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [selectedCollection, setSelectedCollection] = useState<Collection | null>(null);
  const [collectionOutlines, setCollectionOutlines] = useState<CollectionOutline | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [articleCount, setArticleCount] = useState(5);
  const [generatingOutlines, setGeneratingOutlines] = useState(false);
  const [generatingArticle, setGeneratingArticle] = useState<number | null>(null);
  const [generatingAll, setGeneratingAll] = useState(false);
  const [generatedArticles, setGeneratedArticles] = useState<Article[]>([]);
  const [createError, setCreateError] = useState('');

  useEffect(() => {
    fetchCollections();
  }, []);

  const fetchCollections = async () => {
    try {
      const response = await collectionApi.getAll();
      if (response.code === 200) {
        setCollections(response.data);
      }
    } catch (err) {
      console.error('Failed to fetch collections:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!newTitle.trim()) {
      setCreateError('请输入合集标题');
      return;
    }
    
    setCreateError('');
    try {
      const response = await collectionApi.create(newTitle, newDescription);
      if (response.code === 200) {
        setCollections(prev => [response.data, ...prev]);
        setNewTitle('');
        setNewDescription('');
        setShowCreateModal(false);
      } else {
        setCreateError(response.message || '创建失败，请稍后重试');
      }
    } catch (err: any) {
      console.error('Failed to create collection:', err);
      const errorMsg = err.response?.data?.message || err.message || '创建失败，请检查网络连接后重试';
      setCreateError(errorMsg);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定要删除这个合集吗？')) return;
    
    try {
      const response = await collectionApi.delete(id);
      if (response.code === 200) {
        setCollections(prev => prev.filter(c => c.id !== id));
      }
    } catch (err) {
      console.error('Failed to delete:', err);
    }
  };

  const handleViewDetail = async (collection: Collection) => {
    setSelectedCollection(collection);
    setShowDetailModal(true);
    setGeneratedArticles([]);
    
    if (collection.outlines) {
      try {
        setCollectionOutlines(JSON.parse(collection.outlines));
      } catch (e) {
        setCollectionOutlines(null);
      }
    } else {
      setCollectionOutlines(null);
    }
  };

  const handleGenerateOutlines = async () => {
    if (!selectedCollection) return;
    
    setGeneratingOutlines(true);
    try {
      const response = await collectionApi.generateOutlines(selectedCollection.id, articleCount);
      if (response.code === 200) {
        setCollectionOutlines(response.data);
        const updatedResponse = await collectionApi.getById(selectedCollection.id);
        if (updatedResponse.code === 200) {
          setCollections(prev => prev.map(c => c.id === selectedCollection.id ? updatedResponse.data : c));
        }
      }
    } catch (err) {
      console.error('Failed to generate outlines:', err);
      alert('生成大纲失败: ' + (err as any).response?.data?.message || '未知错误');
    } finally {
      setGeneratingOutlines(false);
    }
  };

  const handleGenerateArticle = async (index: number) => {
    if (!selectedCollection) return;
    
    setGeneratingArticle(index);
    try {
      const response = await collectionApi.generateArticle(selectedCollection.id, index);
      if (response.code === 200) {
        setGeneratedArticles(prev => [...prev, response.data]);
      }
    } catch (err) {
      console.error('Failed to generate article:', err);
      alert('生成文章失败: ' + (err as any).response?.data?.message || '未知错误');
    } finally {
      setGeneratingArticle(null);
    }
  };

  const handleGenerateAll = async () => {
    if (!selectedCollection) return;
    
    setGeneratingAll(true);
    setGeneratedArticles([]);
    try {
      const response = await collectionApi.generateAllArticles(selectedCollection.id);
      if (response.code === 200) {
        setGeneratedArticles(response.data.articles);
      }
    } catch (err) {
      console.error('Failed to generate all articles:', err);
      alert('批量生成文章失败: ' + (err as any).response?.data?.message || '未知错误');
    } finally {
      setGeneratingAll(false);
    }
  };

  const handleViewArticle = (articleId: number) => {
    navigate(`/articles/${articleId}`);
  };

  const filteredCollections = collections.filter(c =>
    c.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.description && c.description.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <Layers className="w-4 h-4 text-violet-400" />
            <span className="text-sm text-violet-400 font-medium">系列内容管理</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            合集<span className="gradient-text-aurora">管理</span>
          </h1>
          <p className="text-gray-400 text-lg">
            创建和管理您的文章合集，批量生成系列文章
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn-primary flex items-center gap-2 self-start md:self-auto text-base px-6 py-3.5 group"
        >
          <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
          新建合集
        </button>
      </div>

      <div className="relative">
        <div className="absolute inset-0 glass-card-strong rounded-2xl" />
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent rounded-t-2xl" />
        <div className="relative p-2">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
            <input
              type="text"
              placeholder="搜索合集..."
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

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="text-center animate-scaleIn">
            <div className="relative w-20 h-20 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-violet-500 to-purple-600 rounded-3xl animate-pulse" />
              <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
                <Layers className="w-10 h-10 text-white" />
              </div>
            </div>
            <p className="text-gray-400 animate-pulse">加载中...</p>
          </div>
        </div>
      ) : filteredCollections.length === 0 ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong py-20">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/15 to-transparent rounded-full blur-2xl" />
          <div className="relative text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-violet-500/20 to-indigo-500/20 rounded-full animate-pulse" />
              <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                <Layers className="w-10 h-10 text-gray-400" />
              </div>
            </div>
            <p className="text-gray-400 mb-4 text-lg">暂无合集</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors inline-flex items-center gap-2"
            >
              点击创建第一个合集
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredCollections.map((collection, index) => (
            <div
              key={collection.id}
              className="group relative overflow-hidden rounded-2xl glass-card glass-card-hover p-5 cursor-pointer animate-fadeIn opacity-0"
              style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
              onClick={() => handleViewDetail(collection)}
            >
              <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
              <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-violet-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
              
              <div className="relative">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 flex items-center justify-center shadow-lg shadow-violet-500/25 group-hover:scale-110 transition-transform duration-300">
                      <BookOpen className="w-6 h-6 text-white" />
                    </div>
                    <h3 className="font-semibold text-white text-lg">{collection.title}</h3>
                  </div>
                  <span className={`relative px-2.5 py-1 text-xs rounded-full font-medium overflow-hidden ${
                    collection.status === 1 
                      ? 'bg-gradient-to-r from-emerald-500/20 to-teal-500/20 text-emerald-400 border border-emerald-500/30' 
                      : 'bg-gradient-to-r from-amber-500/20 to-orange-500/20 text-amber-400 border border-amber-500/30'
                  }`}>
                    {collection.status === 1 ? '已完成' : '草稿'}
                  </span>
                </div>
                
                {collection.description && (
                  <p className="text-gray-400 text-sm line-clamp-2 mb-4 h-10">
                    {collection.description}
                  </p>
                )}
                
                <div className="flex items-center gap-4 text-sm text-gray-500">
                  <span className="flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4" />
                    {collection.articleCount || 0} 篇
                  </span>
                  <span>{new Date(collection.createdAt).toLocaleDateString('zh-CN')}</span>
                </div>

                <div className="flex items-center justify-between mt-4 pt-4 border-t border-white/5">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(collection.id);
                    }}
                    className="p-2 text-gray-500 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-all duration-300 group/delete"
                  >
                    <Trash2 className="w-4 h-4 group-hover/delete:scale-110 transition-transform" />
                  </button>
                  <div className="flex items-center text-gray-500 group-hover:text-cyan-400 transition-colors">
                    <span className="text-sm font-medium">查看详情</span>
                    <ArrowRight className="w-4 h-4 ml-1 group-hover:translate-x-1 transition-transform" />
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={() => setShowCreateModal(false)} />
          <div className="relative glass-card-strong rounded-3xl max-w-lg w-full max-h-[90vh] overflow-hidden flex flex-col animate-scaleIn shadow-2xl">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
            <div className="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-indigo-500/20 to-transparent rounded-full blur-2xl" />
            
            <div className="flex items-center justify-between p-6 border-b border-white/5 flex-shrink-0 relative">
              <h2 className="text-xl font-bold text-white">新建合集</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-2 hover:bg-white/5 rounded-xl text-gray-400 hover:text-white transition-colors flex-shrink-0"
                type="button"
                aria-label="关闭弹窗"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6 space-y-5 overflow-y-auto relative">
              {createError && (
                <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm">
                  {createError}
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">合集标题 *</label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => { setNewTitle(e.target.value); setCreateError(''); }}
                  placeholder="请输入合集标题"
                  className="input-field w-full"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">合集描述</label>
                <textarea
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="请输入合集描述，LLM将根据此描述生成文章大纲"
                  rows={4}
                  className="input-field w-full resize-none"
                />
                <p className="text-xs text-gray-500 mt-2">详细的描述有助于AI生成更准确的文章大纲</p>
              </div>
            </div>
            <div className="p-6 border-t border-white/5 flex justify-end gap-3 flex-shrink-0">
              <button
                onClick={() => { setShowCreateModal(false); setCreateError(''); setNewTitle(''); setNewDescription(''); }}
                className="btn-secondary"
                type="button"
              >
                取消
              </button>
              <button
                onClick={handleCreate}
                disabled={!newTitle.trim()}
                className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                type="button"
              >
                创建
              </button>
            </div>
          </div>
        </div>
      )}

      {showDetailModal && selectedCollection && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={() => setShowDetailModal(false)} />
          <div className="relative glass-card-strong rounded-3xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col animate-scaleIn shadow-2xl">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
            <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-3xl" />
            
            <div className="flex items-center justify-between p-6 border-b border-white/5 flex-shrink-0 relative">
              <div>
                <h2 className="text-xl font-bold text-white">{selectedCollection.title}</h2>
                <p className="text-sm text-gray-400 mt-1">
                  {selectedCollection.articleCount || 0} 篇文章 · 创建于 {new Date(selectedCollection.createdAt).toLocaleDateString('zh-CN')}
                </p>
              </div>
              <button
                onClick={() => setShowDetailModal(false)}
                className="p-2 hover:bg-white/5 rounded-xl text-gray-400 hover:text-white transition-colors flex-shrink-0 ml-4"
                type="button"
                aria-label="关闭弹窗"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto relative">
              {selectedCollection.description && (
                <div className="mb-6 p-5 glass-card rounded-2xl">
                  <p className="text-sm text-gray-300 whitespace-pre-wrap leading-relaxed">{selectedCollection.description}</p>
                </div>
              )}

              <div className="mb-6">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-5">
                  <h3 className="font-semibold text-white text-lg">文章大纲</h3>
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2 px-3 py-2 glass-card rounded-xl">
                      <span className="text-sm text-gray-400">数量</span>
                      <input
                        type="number"
                        value={articleCount}
                        onChange={(e) => setArticleCount(Number(e.target.value))}
                        min={1}
                        max={20}
                        className="w-14 px-2 py-1 bg-white/5 border border-white/10 rounded-lg text-center text-white text-sm focus:outline-none focus:border-indigo-500/50"
                      />
                    </div>
                    <button
                      onClick={handleGenerateOutlines}
                      disabled={generatingOutlines || !selectedCollection.description}
                      className="btn-primary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {generatingOutlines ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                      ) : (
                        <Sparkles className="w-4 h-4" />
                      )}
                      生成大纲
                    </button>
                  </div>
                </div>

                {!selectedCollection.description && (
                  <div className="p-4 bg-amber-500/10 border border-amber-500/30 rounded-xl mb-5">
                    <p className="text-sm text-amber-400">请先编辑合集描述，以便AI生成更准确的大纲</p>
                  </div>
                )}

                {collectionOutlines && collectionOutlines.outlines.length > 0 && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-400">共 {collectionOutlines.outlines.length} 个大纲</span>
                      <button
                        onClick={handleGenerateAll}
                        disabled={generatingAll}
                        className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-emerald-500 to-teal-500 text-white rounded-xl hover:from-emerald-600 hover:to-teal-600 disabled:opacity-50 transition-all duration-300 shadow-lg shadow-emerald-500/25"
                      >
                        {generatingAll ? (
                          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                        ) : (
                          <Sparkles className="w-4 h-4" />
                        )}
                        批量生成全部
                      </button>
                    </div>

                    {collectionOutlines.outlines.map((outline, index) => (
                      <div
                        key={index}
                        className="glass-card rounded-2xl p-5 hover:border-white/10 transition-all duration-300 group"
                      >
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex items-center gap-3">
                            <span className="w-7 h-7 bg-gradient-to-br from-indigo-500 to-cyan-400 text-white rounded-full flex items-center justify-center text-sm font-medium shadow-lg shadow-indigo-500/25">
                              {index + 1}
                            </span>
                            <h4 className="font-medium text-white">{outline.title}</h4>
                          </div>
                          <button
                            onClick={() => handleGenerateArticle(index)}
                            disabled={generatingArticle === index}
                            className="flex items-center gap-1.5 px-3 py-1.5 text-sm bg-gradient-to-r from-indigo-500 to-cyan-400 text-white rounded-xl hover:from-indigo-600 hover:to-cyan-500 disabled:opacity-50 transition-all duration-300 shadow-lg shadow-indigo-500/25"
                          >
                            {generatingArticle === index ? (
                              <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-white"></div>
                            ) : (
                              <Sparkles className="w-3.5 h-3.5" />
                            )}
                            生成
                          </button>
                        </div>
                        {outline.summary && (
                          <p className="text-sm text-gray-400 mb-3 leading-relaxed">{outline.summary}</p>
                        )}
                        {outline.keyPoints && (
                          <div className="text-xs text-gray-500 whitespace-pre-wrap bg-white/5 p-3 rounded-xl leading-relaxed">
                            {outline.keyPoints}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {generatedArticles.length > 0 && (
                <div className="mt-6">
                  <h3 className="font-semibold text-white text-lg mb-4 flex items-center gap-2">
                    <Check className="w-5 h-5 text-emerald-400" />
                    已生成文章 ({generatedArticles.length})
                  </h3>
                  <div className="space-y-2">
                    {generatedArticles.map((article) => (
                      <div
                        key={article.id}
                        className="flex items-center justify-between p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl group hover:bg-emerald-500/15 transition-colors"
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-400 flex items-center justify-center">
                            <Check className="w-4 h-4 text-white" />
                          </div>
                          <span className="text-sm text-gray-200">{article.title}</span>
                        </div>
                        <button
                          onClick={() => handleViewArticle(article.id)}
                          className="text-sm text-cyan-400 hover:text-cyan-300 font-medium flex items-center gap-1 group/link"
                        >
                          查看详情
                          <ArrowRight className="w-4 h-4 group-hover/link:translate-x-1 transition-transform" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="p-6 border-t border-white/5 flex justify-end flex-shrink-0">
              <button
                onClick={() => setShowDetailModal(false)}
                className="btn-secondary"
                type="button"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}