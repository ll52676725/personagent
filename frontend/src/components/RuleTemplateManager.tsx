import { useState, useEffect } from 'react';
import { FileText, Plus, Trash2, Copy, Eye, X, BookOpen, Code, FileCheck, Settings, Bot, ArrowRight, Download } from 'lucide-react';
import { rulesApi } from '@/api';
import { RuleTemplate, RuleCategoryOption } from '@/types';

const CATEGORY_OPTIONS: RuleCategoryOption[] = [
  { code: 'global', label: '全局规则', icon: '🌍', description: '个人通用开发习惯和约束' },
  { code: 'project', label: '项目规则', icon: '📁', description: '特定项目的开发规则' },
  { code: 'coding_standard', label: '编码规范', icon: '💻', description: '代码风格、命名规范等' },
  { code: 'documentation', label: '文档规范', icon: '📝', description: '注释、API文档、任务完结文档' },
  { code: 'ai_tool', label: 'AI工具规则', icon: '🤖', description: 'Trae、Cursor、Copilot等工具配置' },
];

const getCategoryIcon = (category: string) => {
  const icons: Record<string, any> = {
    global: Settings,
    project: FileText,
    coding_standard: Code,
    documentation: FileCheck,
    ai_tool: Bot,
  };
  return icons[category] || FileText;
};

const getCategoryGradient = (category: string) => {
  const gradients: Record<string, string> = {
    global: 'from-blue-500 via-cyan-500 to-teal-500',
    project: 'from-violet-500 via-purple-500 to-fuchsia-500',
    coding_standard: 'from-emerald-500 via-green-500 to-lime-500',
    documentation: 'from-amber-500 via-orange-500 to-red-500',
    ai_tool: 'from-pink-500 via-rose-500 to-red-500',
  };
  return gradients[category] || 'from-gray-500 to-gray-600';
};

export default function RuleTemplateManager() {
  const [templates, setTemplates] = useState<RuleTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [viewTemplate, setViewTemplate] = useState<RuleTemplate | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    category: 'global' as any,
    targetTool: '',
    fileName: '',
    filePath: '',
    content: '',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchTemplates();
  }, [activeCategory]);

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      let res;
      if (activeCategory) {
        res = await rulesApi.getTemplatesByCategory(activeCategory);
      } else {
        res = await rulesApi.getTemplates();
      }
      if (res.code === 200) setTemplates(res.data);
    } catch (err) {
      console.error('Failed to fetch templates:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('请输入模板名称');
      return;
    }
    if (!formData.fileName.trim()) {
      setError('请输入文件名');
      return;
    }
    setError('');
    try {
      const res = await rulesApi.createTemplate({
        ...formData,
        sourceType: 'custom',
        isPublic: false,
      });
      if (res.code === 200) {
        setShowCreate(false);
        resetForm();
        fetchTemplates();
      } else {
        setError(res.message || '创建失败');
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '创建失败';
      setError(errorMsg);
    }
  };

  const handleCopy = async (id: number) => {
    try {
      await rulesApi.copyTemplate(id);
      fetchTemplates();
    } catch (err) {
      console.error('Copy failed:', err);
    }
  };

  const handleDelete = async (id: number, isSystem: boolean) => {
    if (isSystem) {
      alert('系统预设模板不允许删除');
      return;
    }
    if (!confirm('确定删除该模板？')) return;
    try {
      await rulesApi.deleteTemplate(id);
      fetchTemplates();
    } catch (err) {
      console.error('Delete failed:', err);
    }
  };

  const handlePull = async (templateId: number) => {
    if (!confirm('确定将此模板拉取到当前项目？')) return;
    try {
      const res = await rulesApi.pullRules({ templateId, previewOnly: true });
      if (res.code === 200) {
        const result = res.data;
        if (result.hasConflict) {
          alert(`检测到冲突：${result.conflictType}\n请前往冲突处理页面解决`);
        } else {
          if (confirm('预览完成，是否执行实际写入？')) {
            await rulesApi.pullRules({ templateId, previewOnly: false });
            alert('规则拉取成功');
          }
        }
      }
    } catch (err) {
      console.error('Pull failed:', err);
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      category: 'global',
      targetTool: '',
      fileName: '',
      filePath: '',
      content: '',
    });
    setError('');
  };

  const filteredTemplates = activeCategory
    ? templates.filter(t => t.category === activeCategory)
    : templates;

  if (loading && templates.length === 0) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-cyan-600 rounded-3xl animate-pulse" />
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
            <BookOpen className="w-4 h-4 text-blue-400" />
            <span className="text-sm text-blue-400 font-medium">规则管理</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            规则<span className="gradient-text-aurora">模板</span>
          </h1>
          <p className="text-gray-400 text-lg">
            管理AI编码规则模板，一键拉取到项目
          </p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => setShowCreate(true)}
            className="btn-primary flex items-center gap-2 self-start md:self-auto text-base px-6 py-3.5 group"
          >
            <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
            创建模板
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        <button
          onClick={() => setActiveCategory(null)}
          className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
            activeCategory === null
              ? 'bg-white/10 text-white border border-white/20'
              : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white border border-transparent'
          }`}
        >
          全部
        </button>
        {CATEGORY_OPTIONS.map((cat) => (
          <button
            key={cat.code}
            onClick={() => setActiveCategory(cat.code)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all flex items-center gap-2 ${
              activeCategory === cat.code
                ? 'bg-white/10 text-white border border-white/20'
                : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white border border-transparent'
            }`}
          >
            <span>{cat.icon}</span>
            {cat.label}
          </button>
        ))}
      </div>

      {filteredTemplates.length === 0 ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong py-20">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-blue-500/20 to-transparent rounded-full blur-3xl" />
          <div className="relative text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-blue-500/20 to-cyan-500/20 rounded-full animate-pulse" />
              <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                <FileText className="w-10 h-10 text-gray-400" />
              </div>
            </div>
            <h3 className="text-white font-semibold text-xl mb-2">暂无规则模板</h3>
            <p className="text-gray-400 mb-6 text-lg">创建您的第一个规则模板</p>
            <button
              onClick={() => setShowCreate(true)}
              className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors inline-flex items-center gap-2"
            >
              点击创建第一个模板
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredTemplates.map((template, index) => {
            const CategoryIcon = getCategoryIcon(template.category);
            return (
              <div
                key={template.id}
                className="group relative overflow-hidden rounded-2xl glass-card glass-card-hover p-5 animate-fadeIn opacity-0"
                style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
              >
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
                <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-blue-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                
                <div className="relative">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-12 h-12 rounded-xl bg-gradient-to-br ${getCategoryGradient(template.category)} flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300`}>
                        <CategoryIcon className="w-6 h-6 text-white" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-white">{template.name}</h3>
                        <span className="text-xs text-gray-500">
                          {CATEGORY_OPTIONS.find(c => c.code === template.category)?.label || template.category}
                        </span>
                      </div>
                    </div>
                    {template.isSystem && (
                      <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-xs rounded-full">
                        系统预设
                      </span>
                    )}
                  </div>
                  
                  {template.description && (
                    <p className="text-gray-400 text-sm mb-4 line-clamp-2">
                      {template.description}
                    </p>
                  )}
                  
                  <div className="flex items-center justify-between text-xs text-gray-500 mb-4">
                    <span className="flex items-center gap-1">
                      <FileText className="w-3 h-3" />
                      {template.fileName}
                    </span>
                    <span className="flex items-center gap-1">
                      <Download className="w-3 h-3" />
                      {template.useCount} 次使用
                    </span>
                  </div>
                  
                  <div className="flex gap-2">
                    <button
                      onClick={() => setViewTemplate(template)}
                      className="flex-1 btn-ghost text-sm py-2 flex items-center justify-center gap-1"
                    >
                      <Eye className="w-4 h-4" />
                      预览
                    </button>
                    <button
                      onClick={() => handleCopy(template.id)}
                      className="btn-ghost text-sm py-2 px-3"
                      title="复制模板"
                    >
                      <Copy className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handlePull(template.id)}
                      className="btn-primary text-sm py-2 px-3 flex items-center gap-1"
                    >
                      <Download className="w-4 h-4" />
                      拉取
                    </button>
                    {!template.isSystem && (
                      <button
                        onClick={() => handleDelete(template.id, template.isSystem)}
                        className="btn-ghost text-sm py-2 px-3 text-red-400 hover:text-red-300 hover:bg-red-500/10"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {showCreate && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#0d1525] border border-white/10 rounded-3xl w-full max-w-2xl max-h-[90vh] overflow-y-auto animate-scaleIn">
            <div className="p-6 border-b border-white/10 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-white">创建规则模板</h2>
              <button onClick={() => { setShowCreate(false); resetForm(); }} className="p-2 hover:bg-white/10 rounded-xl transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="p-6 space-y-5">
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-300 mb-2">模板名称</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="input-field w-full"
                  placeholder="如：Java后端编码规范"
                />
              </div>
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-300 mb-2">描述</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="input-field w-full min-h-[80px] resize-y"
                  placeholder="简要描述此模板的用途和适用场景"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">规则分类</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({...formData, category: e.target.value as any})}
                    className="input-field w-full"
                  >
                    {CATEGORY_OPTIONS.map((cat) => (
                      <option key={cat.code} value={cat.code}>{cat.icon} {cat.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">目标工具</label>
                  <input
                    type="text"
                    value={formData.targetTool}
                    onChange={(e) => setFormData({...formData, targetTool: e.target.value})}
                    className="input-field w-full"
                    placeholder="如：Trae、Cursor、Copilot"
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">文件名</label>
                  <input
                    type="text"
                    value={formData.fileName}
                    onChange={(e) => setFormData({...formData, fileName: e.target.value})}
                    className="input-field w-full"
                    placeholder="如：project_rules.md"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">默认路径</label>
                  <input
                    type="text"
                    value={formData.filePath}
                    onChange={(e) => setFormData({...formData, filePath: e.target.value})}
                    className="input-field w-full"
                    placeholder="如：./docs/rules.md"
                  />
                </div>
              </div>
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-300 mb-2">规则内容（Markdown）</label>
                <textarea
                  value={formData.content}
                  onChange={(e) => setFormData({...formData, content: e.target.value})}
                  className="input-field w-full min-h-[200px] font-mono text-sm resize-y"
                  placeholder="# 规则标题&#10;&#10;## 1. 规范内容&#10;&#10;..."
                />
              </div>
              {error && (
                <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm">
                  {error}
                </div>
              )}
            </div>
            <div className="p-6 border-t border-white/10 flex justify-end gap-3">
              <button onClick={() => { setShowCreate(false); resetForm(); }} className="btn-ghost px-6 py-2.5">
                取消
              </button>
              <button onClick={handleCreate} className="btn-primary px-6 py-2.5">
                创建模板
              </button>
            </div>
          </div>
        </div>
      )}

      {viewTemplate && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-[#0d1525] border border-white/10 rounded-3xl w-full max-w-4xl max-h-[90vh] overflow-hidden animate-scaleIn flex flex-col">
            <div className="p-6 border-b border-white/10 flex items-center justify-between flex-shrink-0">
              <div>
                <h2 className="text-xl font-semibold text-white">{viewTemplate.name}</h2>
                <p className="text-sm text-gray-400 mt-1">{viewTemplate.fileName}</p>
              </div>
              <button onClick={() => setViewTemplate(null)} className="p-2 hover:bg-white/10 rounded-xl transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="p-6 overflow-y-auto flex-1">
              <div className="prose prose-invert max-w-none">
                <pre className="bg-[#0a0f1a] p-6 rounded-2xl overflow-x-auto text-sm whitespace-pre-wrap">
                  {viewTemplate.content || '（暂无内容）'}
                </pre>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
