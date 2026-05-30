import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Sparkles, AlertCircle, StopCircle, Loader2, Eye, PenTool } from 'lucide-react';
import { articleApi, generateApi } from '@/api';

interface FormData {
  title: string;
  summary: string;
  content: string;
  tags: string;
  coverImage: string;
}

type GeneratingType = 'summary' | 'content' | null;

export default function ArticleForm() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  
  const [formData, setFormData] = useState<FormData>({
    title: '',
    summary: '',
    content: '',
    tags: '',
    coverImage: '',
  });
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [generatingType, setGeneratingType] = useState<GeneratingType>(null);
  const [showPreview, setShowPreview] = useState(false);
  
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    if (isEdit && id) {
      const fetchArticle = async () => {
        try {
          const response = await articleApi.getById(parseInt(id));
          if (response.code === 200) {
            const article = response.data;
            setFormData({
              title: article.title,
              summary: article.summary || '',
              content: article.content || '',
              tags: article.tags || '',
              coverImage: article.coverImage || '',
            });
          }
        } catch (err) {
          console.error('Failed to fetch article:', err);
        }
      };
      fetchArticle();
    }
  }, [id, isEdit]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim()) return;
    
    setSubmitting(true);
    
    try {
      const tagsArray = formData.tags.split(',').map(t => t.trim()).filter(t => t);
      
      if (isEdit && id) {
        await articleApi.update(parseInt(id), {
          title: formData.title,
          summary: formData.summary,
          content: formData.content,
          tags: tagsArray,
        });
      } else {
        await articleApi.create(formData.title, formData.summary, formData.content, tagsArray);
      }
      
      navigate('/articles');
    } catch (err) {
      console.error('Failed to save:', err);
      setError('保存失败，请稍后重试');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStopGenerate = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setGeneratingType(null);
    setLoading(false);
  };

  const handleGenerateSummary = () => {
    if (!formData.title.trim()) {
      setError('请先输入文章标题');
      return;
    }
    
    setLoading(true);
    setError('');
    setGeneratingType('summary');
    setFormData(prev => ({ ...prev, summary: '' }));
    
    generateApi.summaryStream(
      formData.title,
      undefined,
      (chunk) => {
        setFormData(prev => ({ ...prev, summary: prev.summary + chunk }));
      },
      () => {
        setGeneratingType(null);
        setLoading(false);
      },
      (errorMsg) => {
        setError(errorMsg);
        setGeneratingType(null);
        setLoading(false);
      }
    );
  };

  const handleGenerateContent = () => {
    if (!formData.title.trim()) {
      setError('请先输入文章标题');
      return;
    }
    
    setLoading(true);
    setError('');
    setGeneratingType('content');
    setFormData(prev => ({ ...prev, content: '' }));
    
    generateApi.contentStream(
      formData.title,
      undefined,
      (chunk) => {
        setFormData(prev => ({ ...prev, content: prev.content + chunk }));
      },
      () => {
        setGeneratingType(null);
        setLoading(false);
      },
      (errorMsg) => {
        setError(errorMsg);
        setGeneratingType(null);
        setLoading(false);
      }
    );
  };

  const renderMarkdown = (text: string) => {
    return text
      .replace(/^## (.*$)/gim, '<h2 class="text-xl font-bold text-white mt-6 mb-3">$1</h2>')
      .replace(/^### (.*$)/gim, '<h3 class="text-lg font-semibold text-white mt-4 mb-2">$1</h3>')
      .replace(/\*\*(.*?)\*\*/g, '<strong class="text-white">$1</strong>')
      .replace(/^- (.*$)/gim, '<li class="ml-4 text-gray-300">$1</li>')
      .replace(/^\d+\. (.*$)/gim, '<li class="ml-4 text-gray-300">$1</li>')
      .replace(/```java\n([\s\S]*?)```/g, '<pre class="bg-white/5 border border-white/10 text-cyan-400 p-4 rounded-xl my-4 overflow-x-auto font-mono text-sm"><code>$1</code></pre>')
      .replace(/\n\n/g, '</p><p class="my-3 text-gray-300">')
      .replace(/\n/g, '<br>');
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <PenTool className="w-4 h-4 text-cyan-400" />
            <span className="text-sm text-cyan-400 font-medium">{isEdit ? '编辑模式' : '创作模式'}</span>
          </div>
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/articles')}
              className="p-3 rounded-xl hover:bg-white/5 text-gray-400 hover:text-white transition-all"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
                {isEdit ? '编辑' : '新建'}<span className="gradient-text-aurora">文章</span>
              </h1>
              <p className="text-gray-400 text-lg">
                {isEdit ? '修改已有文章内容' : '创建一篇新文章'}
              </p>
            </div>
          </div>
        </div>
        
        {formData.content && (
          <button
            type="button"
            onClick={() => setShowPreview(!showPreview)}
            className={`px-6 py-3 rounded-xl font-medium flex items-center gap-2 transition-all self-start md:self-auto ${
              showPreview 
                ? 'bg-gradient-to-r from-indigo-500 to-cyan-400 text-white shadow-lg shadow-indigo-500/25' 
                : 'bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white border border-white/10'
            }`}
          >
            <Eye className="w-5 h-5" />
            {showPreview ? '编辑' : '预览'}
          </button>
        )}
      </div>

      {error && (
        <div className="relative overflow-hidden glass-card rounded-2xl p-5 flex items-center gap-4 animate-fadeIn border border-red-500/30">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-red-500/30 to-transparent" />
          <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-red-500/10 to-transparent rounded-full blur-2xl" />
          <div className="relative flex items-center gap-4 w-full">
            <div className="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center flex-shrink-0">
              <AlertCircle className="w-5 h-5 text-red-400" />
            </div>
            <span className="text-red-300 flex-1">{error}</span>
            <button 
              onClick={() => setError('')}
              className="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-red-400 hover:text-red-300 transition-all text-sm font-medium"
            >
              关闭
            </button>
          </div>
        </div>
      )}

      {generatingType && (
        <div className="relative overflow-hidden glass-card rounded-2xl p-5 flex items-center gap-4 animate-fadeIn border border-cyan-500/30">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-cyan-500/30 to-transparent" />
          <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-cyan-500/10 to-transparent rounded-full blur-2xl" />
          <div className="relative flex items-center gap-4 w-full">
            <div className="w-10 h-10 rounded-xl bg-cyan-500/20 flex items-center justify-center flex-shrink-0">
              <Loader2 className="w-5 h-5 text-cyan-400 animate-spin" />
            </div>
            <span className="text-cyan-300 flex-1">
              正在{generatingType === 'summary' ? '生成摘要' : '生成正文'}...
              {formData[generatingType].length > 0 && 
                ` (${formData[generatingType].length} 字)`
              }
            </span>
            <button 
              onClick={handleStopGenerate}
              className="px-4 py-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-400 hover:text-red-300 flex items-center gap-2 transition-all text-sm font-medium"
            >
              <StopCircle className="w-4 h-4" />
              停止
            </button>
          </div>
        </div>
      )}

      {showPreview && formData.content ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong p-8 animate-fadeIn">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-indigo-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-cyan-500/15 to-transparent rounded-full blur-2xl" />
          
          <div className="relative">
            <h1 className="text-2xl font-bold text-white mb-4">{formData.title}</h1>
            {formData.summary && (
              <p className="text-gray-300 italic border-l-4 border-cyan-400/50 pl-4 my-4">
                {formData.summary}
              </p>
            )}
            <div 
              className="text-gray-200"
              dangerouslySetInnerHTML={{ __html: '<p>' + renderMarkdown(formData.content) + '</p>' }}
            />
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="relative overflow-hidden rounded-3xl glass-card-strong p-8 animate-fadeIn">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-violet-500/15 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/10 to-transparent rounded-full blur-2xl" />
          
          <div className="relative space-y-8">
            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '50ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-2">标题 *</label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                className="input-field w-full text-lg"
                placeholder="请输入文章标题"
                disabled={loading}
              />
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '100ms', animationFillMode: 'forwards' }}>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-gray-300">概要</label>
                <button
                  type="button"
                  onClick={generatingType === 'summary' ? handleStopGenerate : handleGenerateSummary}
                  disabled={!formData.title.trim()}
                  className="px-4 py-2 text-sm bg-gradient-to-r from-indigo-500/20 to-cyan-500/20 text-cyan-400 hover:from-indigo-500/30 hover:to-cyan-500/30 border border-cyan-500/30 rounded-xl font-medium flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300"
                >
                  {generatingType === 'summary' ? (
                    <>
                      <StopCircle className="w-4 h-4" />
                      停止生成
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      AI生成
                    </>
                  )}
                </button>
              </div>
              <textarea
                value={formData.summary}
                onChange={(e) => setFormData(prev => ({ ...prev, summary: e.target.value }))}
                className="input-field w-full h-24 resize-none"
                placeholder="文章概要描述"
                disabled={loading}
              />
              <p className="text-xs text-gray-500 mt-2">
                {formData.summary.length} 字 · 建议 100-200 字
              </p>
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '150ms', animationFillMode: 'forwards' }}>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-gray-300">正文</label>
                <button
                  type="button"
                  onClick={generatingType === 'content' ? handleStopGenerate : handleGenerateContent}
                  disabled={!formData.title.trim()}
                  className="px-4 py-2 text-sm bg-gradient-to-r from-indigo-500/20 to-cyan-500/20 text-cyan-400 hover:from-indigo-500/30 hover:to-cyan-500/30 border border-cyan-500/30 rounded-xl font-medium flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300"
                >
                  {generatingType === 'content' ? (
                    <>
                      <StopCircle className="w-4 h-4" />
                      停止生成
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      AI生成
                    </>
                  )}
                </button>
              </div>
              <textarea
                value={formData.content}
                onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
                className="input-field w-full h-96 resize-none font-mono text-sm"
                placeholder="文章正文内容（支持 Markdown 格式）"
                disabled={loading}
              />
              <p className="text-xs text-gray-500 mt-2">
                {formData.content.length} 字 · 建议 1500 字以上
              </p>
            </div>

            <div className="animate-fadeIn opacity-0" style={{ animationDelay: '200ms', animationFillMode: 'forwards' }}>
              <label className="block text-sm font-medium text-gray-300 mb-2">标签</label>
              <input
                type="text"
                value={formData.tags}
                onChange={(e) => setFormData(prev => ({ ...prev, tags: e.target.value }))}
                className="input-field w-full"
                placeholder="多个标签用逗号分隔"
                disabled={loading}
              />
            </div>

            <div className="flex gap-3 pt-6 animate-fadeIn opacity-0" style={{ animationDelay: '250ms', animationFillMode: 'forwards' }}>
              <button
                type="button"
                onClick={() => navigate('/articles')}
                className="flex-1 btn-secondary py-3.5 text-base"
                disabled={loading}
              >
                取消
              </button>
              <button
                type="submit"
                disabled={!formData.title.trim() || submitting || loading}
                className="flex-1 btn-primary disabled:opacity-50 disabled:cursor-not-allowed py-3.5 text-base"
              >
                {submitting ? '保存中...' : (isEdit ? '保存修改' : '创建文章')}
              </button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
}
