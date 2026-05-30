import { useState } from 'react';
import { Sparkles, FileText, Download, RefreshCw, Save, Copy, Check } from 'lucide-react';
import { rulesApi } from '@/api';
import { RuleTemplate, RuleCategoryOption, AIRuleGenerateRequest } from '@/types';

const CATEGORY_OPTIONS: RuleCategoryOption[] = [
  { code: 'global', label: '全局规则', icon: '🌍', description: '个人通用开发习惯和约束' },
  { code: 'project', label: '项目规则', icon: '📁', description: '特定项目的开发规则' },
  { code: 'coding_standard', label: '编码规范', icon: '💻', description: '代码风格、命名规范等' },
  { code: 'documentation', label: '文档规范', icon: '📝', description: '注释、API文档、任务完结文档' },
  { code: 'ai_tool', label: 'AI工具规则', icon: '🤖', description: 'Trae、Cursor、Copilot等工具配置' },
];

const TOOL_OPTIONS = [
  { code: 'trae', label: 'Trae' },
  { code: 'cursor', label: 'Cursor' },
  { code: 'copilot', label: 'GitHub Copilot' },
  { code: 'windsurf', label: 'Windsurf' },
  { code: 'continue', label: 'Continue' },
  { code: 'other', label: '其他' },
];

const LANGUAGE_OPTIONS = [
  { code: 'java', label: 'Java' },
  { code: 'typescript', label: 'TypeScript' },
  { code: 'javascript', label: 'JavaScript' },
  { code: 'python', label: 'Python' },
  { code: 'go', label: 'Go' },
  { code: 'rust', label: 'Rust' },
  { code: 'csharp', label: 'C#' },
  { code: 'other', label: '其他' },
];

const PROJECT_TYPE_OPTIONS = [
  { code: 'web', label: 'Web应用' },
  { code: 'backend', label: '后端服务' },
  { code: 'frontend', label: '前端应用' },
  { code: 'mobile', label: '移动应用' },
  { code: 'desktop', label: '桌面应用' },
  { code: 'library', label: '类库/SDK' },
  { code: 'other', label: '其他' },
];

export default function AIRuleGenerator() {
  const [formData, setFormData] = useState<AIRuleGenerateRequest>({
    category: 'global',
    description: '',
    targetTool: '',
    projectType: '',
    codingLanguage: '',
    additionalRequirements: '',
    fileName: '',
    saveAsTemplate: true,
  });
  const [generatedContent, setGeneratedContent] = useState<string>('');
  const [generating, setGenerating] = useState(false);
  const [savedTemplate, setSavedTemplate] = useState<RuleTemplate | null>(null);
  const [copied, setCopied] = useState(false);

  const handleGenerate = async (save: boolean) => {
    if (formData.description.trim().length < 10) {
      alert('请输入更详细的规则描述（至少10个字符）');
      return;
    }
    
    try {
      setGenerating(true);
      setGeneratedContent('');
      setSavedTemplate(null);
      
      if (save) {
        const res = await rulesApi.generateAndSaveRules(formData);
        if (res.code === 200 && res.data) {
          setSavedTemplate(res.data);
          setGeneratedContent(res.data.content || '');
        }
      } else {
        const res = await rulesApi.generateRules(formData);
        if (res.code === 200) {
          setGeneratedContent(res.data);
        }
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '生成失败';
      alert(errorMsg);
    } finally {
      setGenerating(false);
    }
  };

  const handleCopy = async () => {
    if (!generatedContent) return;
    try {
      await navigator.clipboard.writeText(generatedContent);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      alert('复制失败');
    }
  };

  const handleDownload = () => {
    if (!generatedContent) return;
    const blob = new Blob([generatedContent], { type: 'text/markdown' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = formData.fileName || 'rules.md';
    a.click();
    URL.revokeObjectURL(url);
  };

  const resetForm = () => {
    setFormData({
      category: 'global',
      description: '',
      targetTool: '',
      projectType: '',
      codingLanguage: '',
      additionalRequirements: '',
      fileName: '',
      saveAsTemplate: true,
    });
    setGeneratedContent('');
    setSavedTemplate(null);
  };

  const getDefaultFileName = () => {
    const names: Record<string, string> = {
      global: 'global_rules.md',
      project: 'project_rules.md',
      coding_standard: 'coding_standard.md',
      documentation: 'documentation_rules.md',
      ai_tool: 'ai_tool_rules.md',
    };
    return names[formData.category] || 'rules.md';
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <Sparkles className="w-4 h-4 text-purple-400" />
            <span className="text-sm text-purple-400 font-medium">AI生成</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            AI<span className="gradient-text-aurora">规则生成</span>
          </h1>
          <p className="text-gray-400 text-lg">
            让AI帮您生成专业的编码规则
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-5">
          <div className="glass-card-strong rounded-2xl p-6 space-y-5">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-purple-400" />
              规则配置
            </h3>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">规则分类</label>
              <div className="grid grid-cols-2 gap-2">
                {CATEGORY_OPTIONS.map((cat) => (
                  <button
                    key={cat.code}
                    onClick={() => setFormData({...formData, category: cat.code as any, fileName: getDefaultFileName()})}
                    className={`p-3 rounded-xl text-left transition-all ${
                      formData.category === cat.code
                        ? 'bg-white/10 border border-white/30'
                        : 'bg-white/5 border border-transparent hover:bg-white/10'
                    }`}
                  >
                    <div className="text-white text-sm">{cat.icon} {cat.label}</div>
                    <div className="text-xs text-gray-500 mt-0.5">{cat.description}</div>
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">目标AI工具</label>
                <select
                  value={formData.targetTool}
                  onChange={(e) => setFormData({...formData, targetTool: e.target.value})}
                  className="input-field w-full"
                >
                    <option value="">不指定</option>
                    {TOOL_OPTIONS.map((t) => (
                      <option key={t.code} value={t.code}>{t.label}</option>
                    ))}
                  </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">编程语言</label>
                <select
                  value={formData.codingLanguage}
                  onChange={(e) => setFormData({...formData, codingLanguage: e.target.value})}
                  className="input-field w-full"
                >
                    <option value="">不指定</option>
                    {LANGUAGE_OPTIONS.map((l) => (
                      <option key={l.code} value={l.code}>{l.label}</option>
                    ))}
                  </select>
              </div>
            </div>

            <div className="w-full">
              <label className="block text-sm font-medium text-gray-300 mb-2">项目类型</label>
              <select
                value={formData.projectType}
                onChange={(e) => setFormData({...formData, projectType: e.target.value})}
                className="input-field w-full"
              >
                  <option value="">不指定</option>
                  {PROJECT_TYPE_OPTIONS.map((p) => (
                    <option key={p.code} value={p.code}>{p.label}</option>
                  ))}
                </select>
            </div>

            <div className="w-full">
              <label className="block text-sm font-medium text-gray-300 mb-2">规则描述 *</label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({...formData, description: e.target.value})}
                className="input-field w-full min-h-[120px] resize-y"
                placeholder="请详细描述您需要的规则内容，例如：&#10;- Java后端项目，使用Spring Boot框架&#10;- 需要严格的代码注释规范&#10;- 统一的异常处理机制&#10;- 数据库操作规范&#10;- 日志输出规范"
              />
            </div>

            <div className="w-full">
              <label className="block text-sm font-medium text-gray-300 mb-2">额外要求（可选）</label>
              <textarea
                value={formData.additionalRequirements}
                onChange={(e) => setFormData({...formData, additionalRequirements: e.target.value})}
                className="input-field w-full min-h-[100px] resize-y"
                placeholder="例如：&#10;- 遵循阿里巴巴Java开发手册&#10;- 禁止使用lombok的@SneakyThrows&#10;- ..."
              />
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="saveAsTemplate"
                  checked={formData.saveAsTemplate}
                  onChange={(e) => setFormData({...formData, saveAsTemplate: e.target.checked})}
                  className="w-4 h-4 rounded border-gray-600"
                />
                <label htmlFor="saveAsTemplate" className="text-sm text-gray-300">生成后保存为模板</label>
              </div>
              <div className="flex items-center gap-1 text-xs text-gray-500">
                <FileText className="w-4 h-4" />
                {formData.fileName || getDefaultFileName()}
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                onClick={() => handleGenerate(false)}
                disabled={generating}
                className="flex-1 btn-primary py-3 flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {generating ? (
                  <>
                    <RefreshCw className="w-5 h-5 animate-spin" />
                    生成中...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    仅生成预览
                  </>
                )}
              </button>
              <button
                onClick={() => handleGenerate(true)}
                disabled={generating}
                className="flex-1 bg-gradient-to-r from-purple-600 to-fuchsia-600 hover:from-purple-500 hover:to-fuchsia-500 text-white font-medium rounded-xl py-3 flex items-center justify-center gap-2 disabled:opacity-50 transition-all shadow-lg shadow-purple-500/25"
              >
                <Save className="w-5 h-5" />
                生成并保存
              </button>
            </div>

            {savedTemplate && (
              <div className="p-4 bg-green-500/10 border border-green-500/20 rounded-xl">
                <div className="flex items-center gap-2 text-green-400 text-sm">
                  <Check className="w-4 h-4" />
                  已保存为模板：{savedTemplate.name}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <div className="glass-card-strong rounded-2xl p-6 h-full flex flex-col">
            <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <FileText className="w-5 h-5 text-cyan-400" />
              生成结果
            </h3>
            {generatedContent && (
              <div className="flex gap-2">
                <button
                  onClick={handleCopy}
                  className="btn-ghost px-3 py-1.5 text-sm flex items-center gap-1"
                  title="复制内容"
                >
                  {copied ? <Check className="w-4 h-4 text-green-400" /> : <Copy className="w-4 h-4" />}
                  {copied ? '已复制' : '复制'}
                </button>
                <button
                  onClick={handleDownload}
                  className="btn-ghost px-3 py-1.5 text-sm flex items-center gap-1"
                  title="下载文件"
                >
                  <Download className="w-4 h-4" />
                  下载
                </button>
                <button
                  onClick={resetForm}
                  className="btn-ghost px-3 py-1.5 text-sm"
                  title="重新生成"
                >
                  <RefreshCw className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>

          {!generatedContent ? (
            <div className="flex-1 flex items-center justify-center">
              <div className="text-center text-gray-500">
                <Sparkles className="w-16 h-16 mx-auto mb-4 opacity-30" />
                <p>填写左侧配置后点击生成</p>
                <p className="text-sm">AI将为您生成专业规则</p>
              </div>
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto">
              <pre className="bg-[#0a0f1a rounded-xl p-5 text-sm font-mono whitespace-pre-wrap text-gray-300 max-h-[600px] overflow-y-auto">
                {generatedContent}
              </pre>
            </div>
          )}
          </div>
        </div>
      </div>
    </div>
  );
}
