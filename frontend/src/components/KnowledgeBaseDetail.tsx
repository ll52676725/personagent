import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, Trash2, FileText, RefreshCw, ChevronLeft, X, Upload, Globe, FileUp, FileType, FileSpreadsheet, Presentation, Image, AlertCircle } from 'lucide-react';
import { knowledgeApi } from '@/api';
import { KnowledgeBase, KnowledgeItem } from '@/types';

const SUPPORTED_EXTENSIONS = [
  '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
  '.txt', '.md', '.markdown', '.html', '.htm', '.rtf',
  '.csv', '.json', '.xml', '.java', '.py', '.js', '.ts',
];

const MAX_FILE_SIZE = 50 * 1024 * 1024;

type AddMode = 'manual' | 'file' | 'url';

function getFileIcon(fileName: string) {
  const ext = fileName.split('.').pop()?.toLowerCase() || '';
  if (['pdf'].includes(ext)) return <FileType className="w-5 h-5 text-red-400" />;
  if (['doc', 'docx'].includes(ext)) return <FileType className="w-5 h-5 text-blue-400" />;
  if (['xls', 'xlsx', 'csv'].includes(ext)) return <FileSpreadsheet className="w-5 h-5 text-green-400" />;
  if (['ppt', 'pptx'].includes(ext)) return <Presentation className="w-5 h-5 text-orange-400" />;
  if (['jpg', 'jpeg', 'png', 'gif', 'svg'].includes(ext)) return <Image className="w-5 h-5 text-purple-400" />;
  return <FileText className="w-5 h-5 text-gray-400" />;
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function isFileSupported(fileName: string): boolean {
  const lowerName = fileName.toLowerCase();
  return SUPPORTED_EXTENSIONS.some(ext => lowerName.endsWith(ext));
}

export default function KnowledgeBaseDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [base, setBase] = useState<KnowledgeBase | null>(null);
  const [items, setItems] = useState<KnowledgeItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [addMode, setAddMode] = useState<AddMode>('file');
  const [newTitle, setNewTitle] = useState('');
  const [newContent, setNewContent] = useState('');
  const [newCategory, setNewCategory] = useState('');
  const [newTags, setNewTags] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [urlInput, setUrlInput] = useState('');
  const [urlTitle, setUrlTitle] = useState('');

  useEffect(() => {
    if (id) fetchData();
  }, [id]);

  const fetchData = async () => {
    try {
      const baseId = Number(id);
      const [baseRes, itemsRes] = await Promise.all([
        knowledgeApi.getBase(baseId),
        knowledgeApi.getItems(baseId),
      ]);
      if (baseRes.code === 200) setBase(baseRes.data);
      if (itemsRes.code === 200) setItems(itemsRes.data);
    } catch (err) {
      console.error('Failed to fetch data:', err);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = useCallback(() => {
    setNewTitle('');
    setNewContent('');
    setNewCategory('');
    setNewTags('');
    setSelectedFiles([]);
    setIsDragOver(false);
    setUrlInput('');
    setUrlTitle('');
    setError('');
    setSubmitting(false);
  }, []);

  const handleAddManual = async () => {
    if (!newTitle.trim()) { setError('请输入标题'); return; }
    setSubmitting(true);
    try {
      const res = await knowledgeApi.createItem(Number(id), {
        title: newTitle,
        content: newContent,
        sourceType: 'manual',
        category: newCategory,
        tags: newTags,
      });
      if (res.code === 200) {
        setShowAdd(false);
        resetForm();
        fetchData();
      }
    } catch (err) {
      setError('添加失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileUpload = async () => {
    if (selectedFiles.length === 0) { setError('请选择要上传的文件'); return; }
    const unsupported = selectedFiles.filter(f => !isFileSupported(f.name));
    if (unsupported.length > 0) {
      setError(`不支持的文件格式：${unsupported.map(f => f.name).join(', ')}`);
      return;
    }
    const oversized = selectedFiles.filter(f => f.size > MAX_FILE_SIZE);
    if (oversized.length > 0) {
      setError(`文件大小超过50MB限制：${oversized.map(f => f.name).join(', ')}`);
      return;
    }

    setSubmitting(true);
    try {
      if (selectedFiles.length === 1) {
        const res = await knowledgeApi.importFile(Number(id), selectedFiles[0], newCategory, newTags);
        if (res.code !== 200) {
          setError(res.message || '文件导入失败');
          return;
        }
      } else {
        const res = await knowledgeApi.importFiles(Number(id), selectedFiles, newCategory, newTags);
        if (res.code !== 200) {
          setError(res.message || '批量导入失败');
          return;
        }
      }
      setShowAdd(false);
      resetForm();
      fetchData();
    } catch (err: any) {
      setError(err?.response?.data?.message || '文件导入失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUrlImport = async () => {
    if (!urlInput.trim()) { setError('请输入URL地址'); return; }
    setSubmitting(true);
    try {
      const res = await knowledgeApi.importUrl(urlInput, Number(id), urlTitle || undefined, newCategory, newTags);
      if (res.code === 200) {
        setShowAdd(false);
        resetForm();
        fetchData();
      } else {
        setError(res.message || 'URL导入失败');
      }
    } catch (err: any) {
      setError(err?.response?.data?.message || 'URL导入失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (itemId: number) => {
    if (!confirm('确定删除该知识条目？')) return;
    try {
      await knowledgeApi.deleteItem(itemId);
      fetchData();
    } catch (err) {
      console.error('Delete failed:', err);
    }
  };

  const handleReprocess = async (itemId: number) => {
    try {
      await knowledgeApi.reprocess(itemId);
      fetchData();
    } catch (err) {
      console.error('Reprocess failed:', err);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    addFiles(files);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files);
      addFiles(files);
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const addFiles = (newFiles: File[]) => {
    const existingNames = new Set(selectedFiles.map(f => f.name));
    const uniqueNew = newFiles.filter(f => !existingNames.has(f.name));
    setSelectedFiles(prev => [...prev, ...uniqueNew]);
    setError('');
  };

  const removeFile = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const getSourceIcon = (item: KnowledgeItem) => {
    if (item.sourceType === 'url') return <Globe className="w-5 h-5 text-white" />;
    if (item.sourceType === 'file' && item.fileName) return getFileIcon(item.fileName);
    return <FileText className="w-5 h-5 text-white" />;
  };

  const getSourceBadge = (item: KnowledgeItem) => {
    if (item.sourceType === 'url') return <span className="text-xs bg-blue-500/20 text-blue-300 px-2 py-0.5 rounded-full">URL</span>;
    if (item.sourceType === 'file' && item.fileType) return <span className="text-xs bg-purple-500/20 text-purple-300 px-2 py-0.5 rounded-full uppercase">{item.fileType}</span>;
    return <span className="text-xs bg-gray-500/20 text-gray-300 px-2 py-0.5 rounded-full">手动</span>;
  };

  const chunkStatusMap: Record<number, { label: string; color: string }> = {
    0: { label: '未处理', color: 'text-gray-400' },
    1: { label: '处理中', color: 'text-amber-400' },
    2: { label: '已完成', color: 'text-emerald-400' },
    3: { label: '失败', color: 'text-red-400' },
  };

  const tabs: { key: AddMode; label: string; icon: React.ReactNode }[] = [
    { key: 'file', label: '文件上传', icon: <Upload className="w-4 h-4" /> },
    { key: 'manual', label: '手动输入', icon: <FileText className="w-4 h-4" /> },
    { key: 'url', label: 'URL导入', icon: <Globe className="w-4 h-4" /> },
  ];

  if (loading) {
    return <div className="flex items-center justify-center h-96"><p className="text-gray-400">加载中...</p></div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <button onClick={() => navigate('/knowledge/bases')} className="p-2 rounded-xl hover:bg-white/5 text-gray-400"><ChevronLeft className="w-6 h-6" /></button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-white">{base?.name || '知识库'}</h1>
          <p className="text-gray-400 mt-1">{base?.description || '暂无描述'} · {items.length} 条知识</p>
        </div>
        <button onClick={() => { setShowAdd(true); setAddMode('file'); }} className="btn-primary flex items-center gap-2">
          <Plus className="w-5 h-5" />添加知识
        </button>
      </div>

      {items.length === 0 ? (
        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
            <FileUp className="w-10 h-10 text-gray-500" />
          </div>
          <h3 className="text-white font-semibold text-lg mb-2">暂无知识条目</h3>
          <p className="text-gray-400 mb-6">上传文档、输入文本或导入网页，开始构建知识库</p>
          <div className="flex items-center justify-center gap-3 text-sm text-gray-500">
            <span className="flex items-center gap-1"><FileType className="w-4 h-4" /> PDF</span>
            <span className="flex items-center gap-1"><FileType className="w-4 h-4" /> Word</span>
            <span className="flex items-center gap-1"><Presentation className="w-4 h-4" /> PPT</span>
            <span className="flex items-center gap-1"><FileSpreadsheet className="w-4 h-4" /> Excel</span>
            <span className="flex items-center gap-1"><Globe className="w-4 h-4" /> URL</span>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {items.map((item, index) => {
            const status = chunkStatusMap[item.chunkStatus] || chunkStatusMap[0];
            return (
              <div key={item.id} className="glass-card rounded-2xl p-4 glass-card-hover animate-fadeIn opacity-0" style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}>
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center flex-shrink-0">
                    {getSourceIcon(item)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <h3 className="text-white font-medium truncate">{item.title}</h3>
                      {getSourceBadge(item)}
                      <span className={`text-xs ${status.color}`}>{status.label}</span>
                    </div>
                    <p className="text-gray-400 text-sm mt-1 line-clamp-2">{item.content?.substring(0, 150) || '暂无内容'}</p>
                    <div className="flex items-center gap-3 mt-2 text-xs text-gray-500">
                      {item.category && <span>分类: {item.category}</span>}
                      {item.tags && <span>标签: {item.tags}</span>}
                      {item.fileSize && <span>大小: {formatFileSize(item.fileSize)}</span>}
                      <span>{item.chunkCount} 片段</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button onClick={() => handleReprocess(item.id)} className="p-2 rounded-xl hover:bg-white/5 text-gray-400 hover:text-cyan-400 transition-all" title="重新处理">
                      <RefreshCw className="w-4 h-4" />
                    </button>
                    <button onClick={() => handleDelete(item.id)} className="p-2 rounded-xl hover:bg-red-500/10 text-gray-400 hover:text-red-400 transition-all" title="删除">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {showAdd && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => { setShowAdd(false); resetForm(); }} />
          <div className="relative glass-card rounded-3xl p-6 w-full max-w-xl animate-fadeIn max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-xl font-bold text-white">添加知识</h3>
              <button onClick={() => { setShowAdd(false); resetForm(); }} className="p-2 rounded-xl hover:bg-white/5 text-gray-400"><X className="w-5 h-5" /></button>
            </div>

            <div className="flex gap-1 mb-5 p-1 bg-white/5 rounded-xl">
              {tabs.map(tab => (
                <button
                  key={tab.key}
                  onClick={() => { setAddMode(tab.key); setError(''); }}
                  className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-lg text-sm font-medium transition-all ${
                    addMode === tab.key
                      ? 'bg-gradient-to-r from-indigo-500 to-cyan-400 text-white shadow-lg'
                      : 'text-gray-400 hover:text-white hover:bg-white/5'
                  }`}
                >
                  {tab.icon}{tab.label}
                </button>
              ))}
            </div>

            {error && (
              <div className="flex items-start gap-2 bg-red-500/10 border border-red-500/30 rounded-2xl p-4 mb-4 text-red-300 text-sm">
                <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                <span>{error}</span>
              </div>
            )}

            {addMode === 'manual' && (
              <div className="space-y-4">
                <input value={newTitle} onChange={(e) => { setNewTitle(e.target.value); setError(''); }} className="input-field w-full" placeholder="知识标题" />
                <textarea value={newContent} onChange={(e) => setNewContent(e.target.value)} className="input-field w-full h-48 resize-none" placeholder="知识内容（支持 Markdown）" />
                <div className="grid grid-cols-2 gap-3">
                  <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="input-field" placeholder="分类（可选）" />
                  <input value={newTags} onChange={(e) => setNewTags(e.target.value)} className="input-field" placeholder="标签，逗号分隔" />
                </div>
                <div className="flex gap-3">
                  <button onClick={() => { setShowAdd(false); resetForm(); }} className="flex-1 btn-secondary">取消</button>
                  <button onClick={handleAddManual} disabled={submitting} className="flex-1 btn-primary disabled:opacity-50">{submitting ? '添加中...' : '添加'}</button>
                </div>
              </div>
            )}

            {addMode === 'file' && (
              <div className="space-y-4">
                <div
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-all ${
                    isDragOver
                      ? 'border-cyan-400 bg-cyan-400/10'
                      : 'border-white/20 hover:border-white/40 hover:bg-white/5'
                  }`}
                >
                  <Upload className={`w-10 h-10 mx-auto mb-3 ${isDragOver ? 'text-cyan-400' : 'text-gray-500'}`} />
                  <p className="text-white font-medium mb-1">
                    {isDragOver ? '释放文件以上传' : '拖拽文件到此处，或点击选择'}
                  </p>
                  <p className="text-gray-500 text-sm">支持 PDF、Word、Excel、PPT、TXT、Markdown 等</p>
                  <p className="text-gray-600 text-xs mt-1">单个文件最大 50MB，支持多文件同时上传</p>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.markdown,.html,.htm,.rtf,.csv,.json,.xml,.java,.py,.js,.ts"
                    onChange={handleFileSelect}
                    className="hidden"
                  />
                </div>

                {selectedFiles.length > 0 && (
                  <div className="space-y-2">
                    <p className="text-sm text-gray-400">已选择 {selectedFiles.length} 个文件：</p>
                    {selectedFiles.map((file, idx) => {
                      const supported = isFileSupported(file.name);
                      const oversized = file.size > MAX_FILE_SIZE;
                      return (
                        <div key={idx} className={`flex items-center gap-3 p-3 rounded-xl ${supported && !oversized ? 'bg-white/5' : 'bg-red-500/10 border border-red-500/20'}`}>
                          <div className="flex-shrink-0">{getFileIcon(file.name)}</div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm text-white truncate">{file.name}</p>
                            <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                            {!supported && <p className="text-xs text-red-400">不支持的格式</p>}
                            {oversized && <p className="text-xs text-red-400">超过50MB限制</p>}
                          </div>
                          <button onClick={(e) => { e.stopPropagation(); removeFile(idx); }} className="p-1 rounded-lg hover:bg-white/10 text-gray-400 hover:text-red-400">
                            <X className="w-4 h-4" />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}

                <div className="grid grid-cols-2 gap-3">
                  <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="input-field" placeholder="分类（可选）" />
                  <input value={newTags} onChange={(e) => setNewTags(e.target.value)} className="input-field" placeholder="标签，逗号分隔" />
                </div>
                <div className="flex gap-3">
                  <button onClick={() => { setShowAdd(false); resetForm(); }} className="flex-1 btn-secondary">取消</button>
                  <button onClick={handleFileUpload} disabled={submitting || selectedFiles.length === 0} className="flex-1 btn-primary disabled:opacity-50">
                    {submitting ? '上传中...' : `上传 ${selectedFiles.length > 0 ? `(${selectedFiles.length}个文件)` : ''}`}
                  </button>
                </div>
              </div>
            )}

            {addMode === 'url' && (
              <div className="space-y-4">
                <div>
                  <label className="text-sm text-gray-400 mb-1 block">网页地址</label>
                  <div className="relative">
                    <Globe className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
                    <input
                      value={urlInput}
                      onChange={(e) => { setUrlInput(e.target.value); setError(''); }}
                      className="input-field w-full pl-10"
                      placeholder="https://example.com/article"
                    />
                  </div>
                </div>
                <input value={urlTitle} onChange={(e) => setUrlTitle(e.target.value)} className="input-field w-full" placeholder="标题（可选，留空自动获取）" />
                <div className="grid grid-cols-2 gap-3">
                  <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="input-field" placeholder="分类（可选）" />
                  <input value={newTags} onChange={(e) => setNewTags(e.target.value)} className="input-field" placeholder="标签，逗号分隔" />
                </div>
                <p className="text-xs text-gray-500">系统将自动爬取网页正文内容，提取纯文本后进行分片和向量化处理</p>
                <div className="flex gap-3">
                  <button onClick={() => { setShowAdd(false); resetForm(); }} className="flex-1 btn-secondary">取消</button>
                  <button onClick={handleUrlImport} disabled={submitting} className="flex-1 btn-primary disabled:opacity-50">{submitting ? '导入中...' : '导入'}</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
