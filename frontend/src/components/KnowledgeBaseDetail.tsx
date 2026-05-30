import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, Trash2, FileText, RefreshCw, ChevronLeft, X, Upload, Globe, FileUp, FileType, FileSpreadsheet, Presentation, Image, AlertCircle, BookOpen, ArrowRight } from 'lucide-react';
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
    return (
      <div className="flex items-center justify-center h-96">
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
        <div className="flex items-center gap-4">
          <button onClick={() => navigate('/knowledge/bases')} className="p-2 rounded-xl hover:bg-white/5 text-gray-400 transition-colors"><ChevronLeft className="w-6 h-6" /></button>
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
              <BookOpen className="w-4 h-4 text-cyan-400" />
              <span className="text-sm text-cyan-400 font-medium">知识库管理</span>
            </div>
            <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
              {base?.name || '知识库'}<span className="gradient-text-aurora">详情</span>
            </h1>
            <p className="text-gray-400 text-lg">
              {base?.description || '暂无描述'} · {items.length} 条知识
            </p>
          </div>
        </div>
        <button onClick={() => { setShowAdd(true); setAddMode('file'); }} className="btn-primary flex items-center gap-2 self-start md:self-auto text-base px-6 py-3.5 group">
          <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />添加知识
        </button>
      </div>

      {items.length === 0 ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong py-20">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-cyan-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/15 to-transparent rounded-full blur-2xl" />
          <div className="relative text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/20 to-indigo-500/20 rounded-full animate-pulse" />
              <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                <FileUp className="w-10 h-10 text-gray-400" />
              </div>
            </div>
            <h3 className="text-white font-semibold text-xl mb-2">暂无知识条目</h3>
            <p className="text-gray-400 mb-6 text-lg">上传文档、输入文本或导入网页，开始构建知识库</p>
            <div className="flex items-center justify-center gap-3 text-sm text-gray-500 mb-6">
              <span className="flex items-center gap-1"><FileType className="w-4 h-4" /> PDF</span>
              <span className="flex items-center gap-1"><FileType className="w-4 h-4" /> Word</span>
              <span className="flex items-center gap-1"><Presentation className="w-4 h-4" /> PPT</span>
              <span className="flex items-center gap-1"><FileSpreadsheet className="w-4 h-4" /> Excel</span>
              <span className="flex items-center gap-1"><Globe className="w-4 h-4" /> URL</span>
            </div>
            <button
              onClick={() => { setShowAdd(true); setAddMode('file'); }}
              className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors inline-flex items-center gap-2"
            >
              点击添加第一条知识
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((item, index) => {
            const status = chunkStatusMap[item.chunkStatus] || chunkStatusMap[0];
            return (
              <div key={item.id} className="group relative overflow-hidden rounded-2xl glass-card glass-card-hover p-5 animate-fadeIn opacity-0" style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}>
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
                <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-cyan-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                <div className="relative">
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-indigo-500 via-blue-500 to-cyan-400 flex items-center justify-center flex-shrink-0 shadow-lg shadow-indigo-500/25 group-hover:scale-110 transition-transform duration-300">
                      {getSourceIcon(item)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <h3 className="text-white font-medium text-lg truncate">{item.title}</h3>
                        {getSourceBadge(item)}
                        <span className={`text-xs ${status.color}`}>{status.label}</span>
                      </div>
                      <p className="text-gray-400 text-sm mt-1.5 line-clamp-2 leading-relaxed">{item.content?.substring(0, 150) || '暂无内容'}</p>
                      <div className="flex items-center gap-4 mt-3 text-xs text-gray-500">
                        {item.category && <span>分类: {item.category}</span>}
                        {item.tags && <span>标签: {item.tags}</span>}
                        {item.fileSize && <span>大小: {formatFileSize(item.fileSize)}</span>}
                        <span>{item.chunkCount} 片段</span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button onClick={() => handleReprocess(item.id)} className="p-2.5 rounded-xl hover:bg-white/5 text-gray-400 hover:text-cyan-400 transition-all duration-300 group/reprocess" title="重新处理">
                        <RefreshCw className="w-5 h-5 group-hover/reprocess:rotate-180 transition-transform duration-500" />
                      </button>
                      <button onClick={() => handleDelete(item.id)} className="p-2.5 rounded-xl hover:bg-red-500/10 text-gray-400 hover:text-red-400 transition-all duration-300 group/delete" title="删除">
                        <Trash2 className="w-5 h-5 group-hover/delete:scale-110 transition-transform" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {showAdd && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={() => { setShowAdd(false); resetForm(); }} />
          <div className="relative glass-card-strong rounded-3xl w-full max-w-xl max-h-[90vh] overflow-hidden flex flex-col animate-scaleIn shadow-2xl">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
            <div className="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-indigo-500/20 to-transparent rounded-full blur-2xl" />
            
            <div className="flex items-center justify-between p-6 border-b border-white/5 flex-shrink-0 relative">
              <h3 className="text-xl font-bold text-white">添加知识</h3>
              <button onClick={() => { setShowAdd(false); resetForm(); }} className="p-2 hover:bg-white/5 rounded-xl text-gray-400 hover:text-white transition-colors flex-shrink-0">
                <X className="w-5 h-5" />
              </button>
            </div>
            
            <div className="p-6 overflow-y-auto relative">
              <div className="flex gap-1 mb-5 p-1 bg-white/5 rounded-xl">
                {tabs.map(tab => (
                  <button
                    key={tab.key}
                    onClick={() => { setAddMode(tab.key); setError(''); }}
                    className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-lg text-sm font-medium transition-all ${
                      addMode === tab.key
                        ? 'bg-gradient-to-r from-indigo-500 to-cyan-400 text-white shadow-lg shadow-indigo-500/25'
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
              <div className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">知识标题 *</label>
                  <input value={newTitle} onChange={(e) => { setNewTitle(e.target.value); setError(''); }} className="input-field w-full" placeholder="请输入知识标题" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">知识内容</label>
                  <textarea value={newContent} onChange={(e) => setNewContent(e.target.value)} className="input-field w-full h-48 resize-none" placeholder="知识内容（支持 Markdown）" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">分类</label>
                    <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="input-field w-full" placeholder="分类（可选）" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">标签</label>
                    <input value={newTags} onChange={(e) => setNewTags(e.target.value)} className="input-field w-full" placeholder="标签，逗号分隔" />
                  </div>
                </div>
              </div>
            )}

            {addMode === 'file' && (
              <div className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">上传文件</label>
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
                </div>

                {selectedFiles.length > 0 && (
                  <div className="space-y-2">
                    <p className="text-sm text-gray-400 font-medium">已选择 {selectedFiles.length} 个文件：</p>
                    {selectedFiles.map((file, idx) => {
                      const supported = isFileSupported(file.name);
                      const oversized = file.size > MAX_FILE_SIZE;
                      return (
                        <div key={idx} className={`flex items-center gap-3 p-4 rounded-xl ${supported && !oversized ? 'bg-white/5 hover:bg-white/10 transition-colors' : 'bg-red-500/10 border border-red-500/20'}`}>
                          <div className="flex-shrink-0">{getFileIcon(file.name)}</div>
                          <div className="flex-1 min-w-0">
                            <p className="text-sm text-white truncate font-medium">{file.name}</p>
                            <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                            {!supported && <p className="text-xs text-red-400 mt-0.5">不支持的格式</p>}
                            {oversized && <p className="text-xs text-red-400 mt-0.5">超过50MB限制</p>}
                          </div>
                          <button onClick={(e) => { e.stopPropagation(); removeFile(idx); }} className="p-2 rounded-lg hover:bg-white/10 text-gray-400 hover:text-red-400 transition-colors">
                            <X className="w-4 h-4" />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                )}

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">分类</label>
                    <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="input-field w-full" placeholder="分类（可选）" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">标签</label>
                    <input value={newTags} onChange={(e) => setNewTags(e.target.value)} className="input-field w-full" placeholder="标签，逗号分隔" />
                  </div>
                </div>
              </div>
            )}

            {addMode === 'url' && (
              <div className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">网页地址 *</label>
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
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">网页标题</label>
                  <input value={urlTitle} onChange={(e) => setUrlTitle(e.target.value)} className="input-field w-full" placeholder="标题（可选，留空自动获取）" />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">分类</label>
                    <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} className="input-field w-full" placeholder="分类（可选）" />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">标签</label>
                    <input value={newTags} onChange={(e) => setNewTags(e.target.value)} className="input-field w-full" placeholder="标签，逗号分隔" />
                  </div>
                </div>
                <p className="text-xs text-gray-500 leading-relaxed">系统将自动爬取网页正文内容，提取纯文本后进行分片和向量化处理</p>
              </div>
            )}
            </div>
            
            <div className="p-6 border-t border-white/5 flex justify-end gap-3 flex-shrink-0">
              <button onClick={() => { setShowAdd(false); resetForm(); }} className="btn-secondary">
                取消
              </button>
              {addMode === 'manual' && (
                <button onClick={handleAddManual} disabled={submitting} className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed">
                  {submitting ? '添加中...' : '添加'}
                </button>
              )}
              {addMode === 'file' && (
                <button onClick={handleFileUpload} disabled={submitting || selectedFiles.length === 0} className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed">
                  {submitting ? '上传中...' : `上传 ${selectedFiles.length > 0 ? `(${selectedFiles.length}个文件)` : ''}`}
                </button>
              )}
              {addMode === 'url' && (
                <button onClick={handleUrlImport} disabled={submitting} className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed">
                  {submitting ? '导入中...' : '导入'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
