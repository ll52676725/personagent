import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Library, Plus, Trash2, BookOpen, Sparkles, X } from 'lucide-react';
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
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center mx-auto mb-4 animate-pulse">
            <Library className="w-8 h-8 text-white" />
          </div>
          <p className="text-gray-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">知识库</h1>
          <p className="text-gray-400 mt-1">管理您的专属知识库，构建智能问答基础</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="btn-primary flex items-center gap-2">
          <Plus className="w-5 h-5" />
          创建知识库
        </button>
      </div>

      {bases.length === 0 ? (
        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
            <Library className="w-10 h-10 text-gray-500" />
          </div>
          <h3 className="text-white font-semibold text-lg mb-2">暂无知识库</h3>
          <p className="text-gray-400 mb-6">创建您的第一个知识库，开始积累知识</p>
          <button onClick={() => setShowCreate(true)} className="btn-primary">
            创建知识库
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {bases.map((base, index) => (
            <div key={base.id} className="glass-card rounded-3xl p-6 glass-card-hover animate-fadeIn opacity-0" style={{ animationDelay: `${index * 80}ms`, animationFillMode: 'forwards' }}>
              <div className="flex items-start justify-between mb-4">
                <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center shadow-lg">
                  <Library className="w-7 h-7 text-white" />
                </div>
                <button onClick={() => handleDelete(base.id)} className="p-2 rounded-xl hover:bg-red-500/10 text-gray-400 hover:text-red-400 transition-all">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
              <h3 className="text-lg font-semibold text-white mb-2 cursor-pointer hover:text-cyan-300 transition-colors" onClick={() => navigate(`/knowledge/bases/${base.id}`)}>
                {base.name}
              </h3>
              <p className="text-gray-400 text-sm mb-4 line-clamp-2">{base.description || '暂无描述'}</p>
              <div className="flex items-center gap-4 text-sm text-gray-500">
                <span className="flex items-center gap-1"><BookOpen className="w-4 h-4" />{base.knowledgeCount} 条</span>
                <span className="flex items-center gap-1"><Sparkles className="w-4 h-4" />{base.chunkCount} 片段</span>
              </div>
              <button onClick={() => navigate(`/knowledge/bases/${base.id}`)} className="btn-primary text-sm px-4 py-2 mt-4 w-full">
                查看详情
              </button>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => { setShowCreate(false); setNewName(''); setNewDesc(''); setError(''); }} />
          <div className="relative glass-card rounded-3xl p-6 w-full max-w-md animate-fadeIn">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-bold text-white">创建知识库</h3>
              <button onClick={() => { setShowCreate(false); setNewName(''); setNewDesc(''); setError(''); }} className="p-2 rounded-xl hover:bg-white/5 text-gray-400"><X className="w-5 h-5" /></button>
            </div>
            {error && <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 mb-4 text-red-300 text-sm">{error}</div>}
            <input value={newName} onChange={(e) => { setNewName(e.target.value); setError(''); }} className="input-field w-full mb-4" placeholder="知识库名称" />
            <textarea value={newDesc} onChange={(e) => setNewDesc(e.target.value)} className="input-field w-full h-24 resize-none mb-4" placeholder="描述（可选）" />
            <div className="flex gap-3">
              <button onClick={() => { setShowCreate(false); setNewName(''); setNewDesc(''); setError(''); }} className="flex-1 btn-secondary">取消</button>
              <button onClick={handleCreate} className="flex-1 btn-primary">创建</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
