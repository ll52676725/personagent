import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Library, Plus, Trash2, BookOpen, Sparkles, X, ArrowRight } from 'lucide-react';
import { knowledgeApi } from '@/api';
import { KnowledgeBase } from '@/types';

export default function KnowledgeBaseList() {
  const navigate = useNavigate();
  const [bases, setBases] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    fetchBases();
  }, []);

  const fetchBases = async () => {
    try {
      const res = await knowledgeApi.getBases();
      if (res.code === 200) setBases(res.data);
    } catch (err) {
      console.error('Failed to fetch knowledge bases:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!newName.trim()) {
      setError('请输入知识库名称');
      return;
    }
    setError('');
    try {
      const res = await knowledgeApi.createBase(newName, newDesc);
      if (res.code === 200) {
        setShowCreate(false);
        setNewName('');
        setNewDesc('');
        setError('');
        fetchBases();
      } else {
        setError(res.message || '创建失败，请稍后重试');
      }
    } catch (err: any) {
      console.error('Failed to create knowledge base:', err);
      const errorMsg = err.response?.data?.message || err.message || '创建失败，请检查网络连接后重试';
      setError(errorMsg);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定删除该知识库？所有知识条目将一并删除。')) return;
    try {
      await knowledgeApi.deleteBase(id);
      fetchBases();
    } catch (err) {
      console.error('Delete failed:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-violet-500 to-purple-600 rounded-3xl animate-pulse" />
            <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
              <Library className="w-10 h-10 text-white" />
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
            <Library className="w-4 h-4 text-violet-400" />
            <span className="text-sm text-violet-400 font-medium">知识资产管理</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            知识<span className="gradient-text-aurora">库</span>
          </h1>
          <p className="text-gray-400 text-lg">
            管理您的专属知识库，构建智能问答基础
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="btn-primary flex items-center gap-2 self-start md:self-auto text-base px-6 py-3.5 group"
        >
          <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
          创建知识库
        </button>
      </div>

      {bases.length === 0 ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong py-20">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/15 to-transparent rounded-full blur-2xl" />
          <div className="relative text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-violet-500/20 to-indigo-500/20 rounded-full animate-pulse" />
              <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                <Library className="w-10 h-10 text-gray-400" />
              </div>
            </div>
            <h3 className="text-white font-semibold text-xl mb-2">暂无知识库</h3>
            <p className="text-gray-400 mb-6 text-lg">创建您的第一个知识库，开始积累知识</p>
            <button
              onClick={() => setShowCreate(true)}
              className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors inline-flex items-center gap-2"
            >
              点击创建第一个知识库
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {bases.map((base, index) => (
            <div
              key={base.id}
              className="group relative overflow-hidden rounded-2xl glass-card glass-card-hover p-5 cursor-pointer animate-fadeIn opacity-0"
              style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
              onClick={() => navigate(`/knowledge/bases/${base.id}`)}
            >
              <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
              <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-violet-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
              
              <div className="relative">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 flex items-center justify-center shadow-lg shadow-violet-500/25 group-hover:scale-110 transition-transform duration-300">
                      <Library className="w-6 h-6 text-white" />
                    </div>
                    <h3 className="font-semibold text-white text-lg">{base.name}</h3>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(base.id);
                    }}
                    className="p-2 text-gray-500 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-all duration-300 group/delete"
                  >
                    <Trash2 className="w-4 h-4 group-hover/delete:scale-110 transition-transform" />
                  </button>
                </div>
                
                <p className="text-gray-400 text-sm line-clamp-2 mb-4 h-10">
                  {base.description || '暂无描述'}
                </p>
                
                <div className="flex items-center gap-4 text-sm text-gray-500">
                  <span className="flex items-center gap-1.5">
                    <BookOpen className="w-4 h-4" />
                    {base.knowledgeCount} 条
                  </span>
                  <span className="flex items-center gap-1.5">
                    <Sparkles className="w-4 h-4" />
                    {base.chunkCount} 片段
                  </span>
                </div>

                <div className="flex items-center justify-between mt-4 pt-4 border-t border-white/5">
                  <span className="text-sm text-gray-500">
                    {new Date(base.createdAt).toLocaleDateString('zh-CN')}
                  </span>
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

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={() => { setShowCreate(false); setNewName(''); setNewDesc(''); setError(''); }} />
          <div className="relative glass-card-strong rounded-3xl max-w-lg w-full max-h-[90vh] overflow-hidden flex flex-col animate-scaleIn shadow-2xl">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
            <div className="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-indigo-500/20 to-transparent rounded-full blur-2xl" />
            
            <div className="flex items-center justify-between p-6 border-b border-white/5 flex-shrink-0 relative">
              <h2 className="text-xl font-bold text-white">创建知识库</h2>
              <button
                onClick={() => { setShowCreate(false); setNewName(''); setNewDesc(''); setError(''); }}
                className="p-2 hover:bg-white/5 rounded-xl text-gray-400 hover:text-white transition-colors flex-shrink-0"
                type="button"
                aria-label="关闭弹窗"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6 space-y-5 overflow-y-auto relative">
              {error && (
                <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm">
                  {error}
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">知识库名称 *</label>
                <input
                  type="text"
                  value={newName}
                  onChange={(e) => { setNewName(e.target.value); setError(''); }}
                  placeholder="请输入知识库名称"
                  className="input-field w-full"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">知识库描述</label>
                <textarea
                  value={newDesc}
                  onChange={(e) => setNewDesc(e.target.value)}
                  placeholder="请输入知识库描述（可选）"
                  rows={4}
                  className="input-field w-full resize-none"
                />
                <p className="text-xs text-gray-500 mt-2">详细的描述有助于更好地管理您的知识内容</p>
              </div>
            </div>
            <div className="p-6 border-t border-white/5 flex justify-end gap-3 flex-shrink-0">
              <button
                onClick={() => { setShowCreate(false); setNewName(''); setNewDesc(''); setError(''); }}
                className="btn-secondary"
                type="button"
              >
                取消
              </button>
              <button
                onClick={handleCreate}
                disabled={!newName.trim()}
                className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
                type="button"
              >
                创建
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
