import { useState, useEffect } from 'react';
import { Settings, Plus, Trash2, X, Download, Clock, CheckCircle, FileText } from 'lucide-react';
import { rulesApi } from '@/api';
import { RuleConfig, RuleCategoryOption, ConflictStrategyOption } from '@/types';

const CATEGORY_OPTIONS: RuleCategoryOption[] = [
  { code: 'global', label: '全局规则', icon: '🌍', description: '个人通用开发习惯和约束' },
  { code: 'project', label: '项目规则', icon: '📁', description: '特定项目的开发规则' },
  { code: 'coding_standard', label: '编码规范', icon: '💻', description: '代码风格、命名规范等' },
  { code: 'documentation', label: '文档规范', icon: '📝', description: '注释、API文档、任务完结文档' },
  { code: 'ai_tool', label: 'AI工具规则', icon: '🤖', description: 'Trae、Cursor、Copilot等工具配置' },
];

const STRATEGY_OPTIONS: ConflictStrategyOption[] = [
  { code: 'ask', label: '询问用户', description: '检测到冲突时询问处理方式', color: 'blue' },
  { code: 'overwrite', label: '直接覆盖', description: '使用模板内容覆盖本地文件', color: 'red' },
  { code: 'keep_local', label: '保留本地', description: '保留本地文件，不做修改', color: 'green' },
  { code: 'merge', label: '智能合并', description: '尝试自动合并，失败转人工', color: 'purple' },
  { code: 'rename', label: '重命名保存', description: '新规则重命名保存，保留原文件', color: 'amber' },
];

const getStrategyColor = (code: string) => {
  const colors: Record<string, string> = {
    ask: 'text-blue-400 bg-blue-500/20',
    overwrite: 'text-red-400 bg-red-500/20',
    keep_local: 'text-green-400 bg-green-500/20',
    merge: 'text-purple-400 bg-purple-500/20',
    rename: 'text-amber-400 bg-amber-500/20',
  };
  return colors[code] || 'text-gray-400 bg-gray-500/20';
};

export default function RuleConfigManager() {
  const [configs, setConfigs] = useState<RuleConfig[]>([]);
  const [templates, setTemplates] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [pullingId, setPullingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    templateId: '' as any,
    projectPath: '',
    category: 'global' as any,
    targetTool: '',
    fileName: '',
    targetPath: '',
    conflictStrategy: 'ask',
    content: '',
  });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [configsRes, templatesRes] = await Promise.all([
        rulesApi.getConfigs(),
        rulesApi.getTemplates(),
      ]);
      if (configsRes.code === 200) setConfigs(configsRes.data);
      if (templatesRes.code === 200) setTemplates(templatesRes.data);
    } catch (err) {
      console.error('Failed to fetch data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTemplateChange = (templateId: string) => {
    const template = templates.find(t => t.id === Number(templateId));
    if (template) {
      setFormData({
        ...formData,
        templateId,
        category: template.category,
        fileName: template.fileName,
        targetPath: template.filePath || `./${template.fileName}`,
        content: template.content || '',
      });
    } else {
      setFormData({...formData, templateId, content: ''});
    }
  };

  const handleCreate = async () => {
    if (!formData.fileName.trim()) {
      setError('请输入文件名');
      return;
    }
    if (!formData.targetPath.trim()) {
      setError('请输入目标路径');
      return;
    }
    setError('');
    try {
      const data = {
        ...formData,
        templateId: formData.templateId ? Number(formData.templateId) : undefined,
      };
      const res = await rulesApi.createConfig(data);
      if (res.code === 200) {
        setShowCreate(false);
        resetForm();
        fetchData();
      } else {
        setError(res.message || '创建失败');
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '创建失败';
      setError(errorMsg);
    }
  };

  const handlePull = async (config: RuleConfig) => {
    if (!confirm(`确定要拉取规则到 ${config.targetPath}？`)) return;
    try {
      setPullingId(config.id);
      const res = await rulesApi.pullRules({ configId: config.id, previewOnly: true });
      if (res.code === 200) {
        const result = res.data;
        if (result.hasConflict) {
          if (confirm(`检测到冲突：${result.conflictType}\n\n是否执行实际拉取并使用设置的策略（${config.conflictStrategy}）处理？`)) {
            const pullRes = await rulesApi.pullRules({ 
              configId: config.id, 
              conflictStrategy: config.conflictStrategy,
              previewOnly: false 
            });
            if (pullRes.code === 200) {
              if (pullRes.data.status === 'CONFLICT') {
                alert('冲突需要手动处理，请前往冲突处理页面');
              } else {
                alert('规则拉取成功');
              }
              fetchData();
            }
          }
        } else {
          if (confirm('无冲突，是否执行实际写入？')) {
            await rulesApi.pullRules({ configId: config.id, previewOnly: false });
            alert('规则拉取成功');
            fetchData();
          }
        }
      }
    } catch (err) {
      console.error('Pull failed:', err);
      alert('拉取失败');
    } finally {
      setPullingId(null);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('确定删除该配置？')) return;
    try {
      await rulesApi.deleteConfig(id);
      fetchData();
    } catch (err) {
      console.error('Delete failed:', err);
    }
  };

  const resetForm = () => {
    setFormData({
      templateId: '',
      projectPath: '',
      category: 'global',
      targetTool: '',
      fileName: '',
      targetPath: '',
      conflictStrategy: 'ask',
      content: '',
    });
    setError('');
  };

  if (loading && configs.length === 0) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-emerald-500 to-teal-600 rounded-3xl animate-pulse" />
            <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
              <Settings className="w-10 h-10 text-white" />
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
            <Settings className="w-4 h-4 text-emerald-400" />
            <span className="text-sm text-emerald-400 font-medium">规则配置</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            拉取<span className="gradient-text-aurora">配置</span>
          </h1>
          <p className="text-gray-400 text-lg">
            配置规则如何应用到您的项目
          </p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="btn-primary flex items-center gap-2 self-start md:self-auto text-base px-6 py-3.5 group"
        >
          <Plus className="w-5 h-5 group-hover:rotate-90 transition-transform duration-300" />
          添加配置
        </button>
      </div>

      {configs.length === 0 ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong py-20">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-emerald-500/20 to-transparent rounded-full blur-3xl" />
          <div className="relative text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-emerald-500/20 to-teal-500/20 rounded-full animate-pulse" />
              <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                <Settings className="w-10 h-10 text-gray-400" />
              </div>
            </div>
            <h3 className="text-white font-semibold text-xl mb-2">暂无拉取配置</h3>
            <p className="text-gray-400 mb-6 text-lg">添加配置，一键拉取规则到项目</p>
            <button
              onClick={() => setShowCreate(true)}
              className="group text-cyan-400 hover:text-cyan-300 font-medium transition-colors"
            >
              点击添加第一个配置
            </button>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {configs.map((config, index) => {
            const template = templates.find(t => t.id === config.templateId);
            return (
              <div
                key={config.id}
                className="group relative overflow-hidden rounded-2xl glass-card glass-card-hover p-5 animate-fadeIn opacity-0"
                style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
              >
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
                
                <div className="flex flex-col md:flex-row md:items-center gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <span className="text-xl">
                        {CATEGORY_OPTIONS.find(c => c.code === config.category)?.icon || '📄'}
                      </span>
                      <h3 className="font-semibold text-white">{config.fileName}</h3>
                      <span className={`px-2 py-0.5 text-xs rounded-full ${getStrategyColor(config.conflictStrategy)}`}>
                        {STRATEGY_OPTIONS.find(s => s.code === config.conflictStrategy)?.label}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center gap-4 text-sm text-gray-400">
                      <span className="flex items-center gap-1">
                        <FileText className="w-4 h-4" />
                        目标: {config.targetPath}
                      </span>
                      {template && (
                        <span className="flex items-center gap-1">
                          <Download className="w-4 h-4" />
                          模板: {template.name}
                        </span>
                      )}
                      {config.lastPullAt && (
                        <span className="flex items-center gap-1">
                          <Clock className="w-4 h-4" />
                          上次拉取: {new Date(config.lastPullAt).toLocaleString()}
                        </span>
                      )}
                      <span className="flex items-center gap-1">
                        <CheckCircle className="w-4 h-4" />
                        已拉取 {config.pullCount} 次
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handlePull(config)}
                      disabled={pullingId === config.id}
                      className="btn-primary text-sm py-2 px-4 flex items-center gap-2 disabled:opacity-50"
                    >
                      {pullingId === config.id ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                          拉取中...
                        </>
                      ) : (
                        <>
                          <Download className="w-4 h-4" />
                          拉取规则
                        </>
                      )}
                    </button>
                    <button
                      onClick={() => handleDelete(config.id)}
                      className="btn-ghost text-sm py-2 px-3 text-red-400 hover:text-red-300 hover:bg-red-500/10"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
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
              <h2 className="text-xl font-semibold text-white">添加拉取配置</h2>
              <button onClick={() => { setShowCreate(false); resetForm(); }} className="p-2 hover:bg-white/10 rounded-xl transition-colors">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="p-6 space-y-5">
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-300 mb-2">关联模板（可选）</label>
                <select
                  value={formData.templateId}
                  onChange={(e) => handleTemplateChange(e.target.value)}
                  className="input-field w-full"
                >
                  <option value="">-- 不关联模板，手动填写 --</option>
                  {templates.map((t) => (
                    <option key={t.id} value={t.id}>{t.name} ({t.fileName})</option>
                  ))}
                </select>
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
                    placeholder="如：Trae"
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
                  <label className="block text-sm font-medium text-gray-300 mb-2">冲突处理策略</label>
                  <select
                    value={formData.conflictStrategy}
                    onChange={(e) => setFormData({...formData, conflictStrategy: e.target.value})}
                    className="input-field w-full"
                  >
                    {STRATEGY_OPTIONS.map((s) => (
                      <option key={s.code} value={s.code}>{s.label}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-300 mb-2">项目路径（可选）</label>
                <input
                  type="text"
                  value={formData.projectPath}
                  onChange={(e) => setFormData({...formData, projectPath: e.target.value})}
                  className="input-field w-full"
                  placeholder="如：d:/projects/my-project"
                />
              </div>
              <div className="w-full">
                <label className="block text-sm font-medium text-gray-300 mb-2">目标路径</label>
                <input
                  type="text"
                  value={formData.targetPath}
                  onChange={(e) => setFormData({...formData, targetPath: e.target.value})}
                  className="input-field w-full"
                  placeholder="如：./docs/project_rules.md"
                />
              </div>
              {!formData.templateId && (
                <div className="w-full">
                  <label className="block text-sm font-medium text-gray-300 mb-2">规则内容（Markdown）</label>
                  <textarea
                    value={formData.content}
                    onChange={(e) => setFormData({...formData, content: e.target.value})}
                    className="input-field w-full min-h-[150px] font-mono text-sm resize-y"
                    placeholder="不关联模板时可手动填写内容..."
                  />
                </div>
              )}
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
                添加配置
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
