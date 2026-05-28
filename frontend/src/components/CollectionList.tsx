import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, Plus, Trash2, Sparkles, Search, X, Check, Layers } from 'lucide-react';
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
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">合集管理</h1>
          <p className="text-gray-500 mt-1">创建和管理您的文章合集，批量生成系列文章</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
        >
          <Plus className="w-5 h-5" />
          新建合集
        </button>
      </div>

      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="搜索合集..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
        </div>
      ) : filteredCollections.length === 0 ? (
        <div className="text-center py-12">
          <Layers className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500">暂无合集，点击"新建合集"开始创建</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredCollections.map((collection) => (
            <div
              key={collection.id}
              className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 hover:shadow-md transition-shadow cursor-pointer"
              onClick={() => handleViewDetail(collection)}
            >
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-2">
                  <div className="p-2 bg-indigo-100 rounded-lg">
                    <BookOpen className="w-5 h-5 text-indigo-600" />
                  </div>
                  <h3 className="font-semibold text-gray-800">{collection.title}</h3>
                </div>
                <span className={`px-2 py-1 text-xs rounded-full ${
                  collection.status === 1 ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'
                }`}>
                  {collection.status === 1 ? '已完成' : '草稿'}
                </span>
              </div>
              
              {collection.description && (
                <p className="text-gray-600 text-sm line-clamp-2 mb-3">
                  {collection.description}
                </p>
              )}
              
              <div className="flex items-center gap-4 text-sm text-gray-500">
                <span className="flex items-center gap-1">
                  <BookOpen className="w-4 h-4" />
                  {collection.articleCount} 篇
                </span>
                <span>{new Date(collection.createdAt).toLocaleDateString()}</span>
              </div>

              <div className="flex items-center justify-end mt-4 pt-3 border-t border-gray-100">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDelete(collection.id);
                  }}
                  className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
                <span className="text-gray-400 ml-2">›</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowCreateModal(false)} />
          <div className="relative bg-white rounded-xl shadow-2xl max-w-lg w-full max-h-[90vh] overflow-hidden flex flex-col animate-fadeIn">
            <div className="flex items-center justify-between p-6 border-b border-gray-100 flex-shrink-0">
              <h2 className="text-xl font-bold text-gray-800">新建合集</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-gray-700 transition-colors flex-shrink-0"
                type="button"
                aria-label="关闭弹窗"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6 space-y-4 overflow-y-auto">
              {createError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                  {createError}
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">合集标题 *</label>
                <input
                  type="text"
                  value={newTitle}
                  onChange={(e) => { setNewTitle(e.target.value); setCreateError(''); }}
                  placeholder="请输入合集标题"
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-gray-800"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">合集描述</label>
                <textarea
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  placeholder="请输入合集描述，LLM将根据此描述生成文章大纲"
                  rows={4}
                  className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none text-gray-800"
                />
                <p className="text-xs text-gray-500 mt-1">详细的描述有助于AI生成更准确的文章大纲</p>
              </div>
            </div>
            <div className="p-6 border-t border-gray-100 flex justify-end gap-3 flex-shrink-0">
              <button
                onClick={() => { setShowCreateModal(false); setCreateError(''); setNewTitle(''); setNewDescription(''); }}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                type="button"
              >
                取消
              </button>
              <button
                onClick={handleCreate}
                disabled={!newTitle.trim()}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowDetailModal(false)} />
          <div className="relative bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col animate-fadeIn">
            <div className="flex items-center justify-between p-6 border-b border-gray-100 flex-shrink-0">
              <div>
                <h2 className="text-xl font-bold text-gray-800">{selectedCollection.title}</h2>
                <p className="text-sm text-gray-500 mt-1">
                  {selectedCollection.articleCount} 篇文章 · 创建于 {new Date(selectedCollection.createdAt).toLocaleDateString()}
                </p>
              </div>
              <button
                onClick={() => setShowDetailModal(false)}
                className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-gray-700 transition-colors flex-shrink-0 ml-4"
                type="button"
                aria-label="关闭弹窗"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto">
              {selectedCollection.description && (
                <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-700 whitespace-pre-wrap">{selectedCollection.description}</p>
                </div>
              )}

              <div className="mb-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="font-semibold text-gray-800">文章大纲</h3>
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      value={articleCount}
                      onChange={(e) => setArticleCount(Number(e.target.value))}
                      min={1}
                      max={20}
                      className="w-16 px-2 py-1 border border-gray-200 rounded-lg text-center text-gray-800"
                    />
                    <button
                      onClick={handleGenerateOutlines}
                      disabled={generatingOutlines || !selectedCollection.description}
                      className="flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
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
                  <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg mb-4">
                    <p className="text-sm text-yellow-700">请先编辑合集描述，以便AI生成更准确的大纲</p>
                  </div>
                )}

                {collectionOutlines && collectionOutlines.outlines.length > 0 && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-500">共 {collectionOutlines.outlines.length} 个大纲</span>
                      <button
                        onClick={handleGenerateAll}
                        disabled={generatingAll}
                        className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
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
                        className="border border-gray-200 rounded-lg p-4 hover:border-indigo-300 transition-colors"
                      >
                        <div className="flex items-start justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <span className="w-6 h-6 bg-indigo-100 text-indigo-600 rounded-full flex items-center justify-center text-sm font-medium">
                              {index + 1}
                            </span>
                            <h4 className="font-medium text-gray-800">{outline.title}</h4>
                          </div>
                          <button
                            onClick={() => handleGenerateArticle(index)}
                            disabled={generatingArticle === index}
                            className="flex items-center gap-1 px-3 py-1 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
                          >
                            {generatingArticle === index ? (
                              <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-white"></div>
                            ) : (
                              <Sparkles className="w-3 h-3" />
                            )}
                            生成
                          </button>
                        </div>
                        {outline.summary && (
                          <p className="text-sm text-gray-600 mb-2">{outline.summary}</p>
                        )}
                        {outline.keyPoints && (
                          <div className="text-xs text-gray-500 whitespace-pre-wrap bg-gray-50 p-2 rounded">
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
                  <h3 className="font-semibold text-gray-800 mb-4">已生成文章 ({generatedArticles.length})</h3>
                  <div className="space-y-2">
                    {generatedArticles.map((article) => (
                      <div
                        key={article.id}
                        className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg"
                      >
                        <div className="flex items-center gap-2">
                          <Check className="w-4 h-4 text-green-600" />
                          <span className="text-sm text-gray-700">{article.title}</span>
                        </div>
                        <button
                          onClick={() => handleViewArticle(article.id)}
                          className="text-sm text-indigo-600 hover:text-indigo-700"
                        >
                          查看详情
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="p-6 border-t border-gray-100 flex justify-end flex-shrink-0">
              <button
                onClick={() => setShowDetailModal(false)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
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