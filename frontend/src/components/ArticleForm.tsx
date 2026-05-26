import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Sparkles, AlertCircle, StopCircle, Loader2, Eye } from 'lucide-react';
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
      .replace(/^## (.*$)/gim, '<h2 class="text-xl font-bold mt-6 mb-3">$1</h2>')
      .replace(/^### (.*$)/gim, '<h3 class="text-lg font-semibold mt-4 mb-2">$1</h3>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/^- (.*$)/gim, '<li class="ml-4">$1</li>')
      .replace(/^\d+\. (.*$)/gim, '<li class="ml-4">$1</li>')
      .replace(/```java\n([\s\S]*?)```/g, '<pre class="bg-gray-800 text-green-400 p-4 rounded-lg my-4 overflow-x-auto"><code>$1</code></pre>')
      .replace(/\n\n/g, '</p><p class="my-3">')
      .replace(/\n/g, '<br>');
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/articles')}
            className="p-2 text-gray-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-800">
              {isEdit ? '编辑文章' : '新建文章'}
            </h1>
            <p className="text-gray-500 mt-1">{isEdit ? '修改已有文章内容' : '创建一篇新文章'}</p>
          </div>
        </div>
        
        {formData.content && (
          <button
            type="button"
            onClick={() => setShowPreview(!showPreview)}
            className={`px-4 py-2 rounded-lg font-medium flex items-center gap-2 transition ${
              showPreview 
                ? 'bg-indigo-100 text-indigo-700' 
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            <Eye className="w-4 h-4" />
            {showPreview ? '编辑' : '预览'}
          </button>
        )}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />
          <span className="text-red-600">{error}</span>
          <button 
            onClick={() => setError('')}
            className="ml-auto text-red-400 hover:text-red-600"
          >
            关闭
          </button>
        </div>
      )}

      {generatingType && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-3">
          <Loader2 className="w-5 h-5 text-blue-500 animate-spin flex-shrink-0" />
          <span className="text-blue-600">
            正在{generatingType === 'summary' ? '生成摘要' : '生成正文'}...
            {formData[generatingType].length > 0 && 
              ` (${formData[generatingType].length} 字)`
            }
          </span>
          <button 
            onClick={handleStopGenerate}
            className="ml-auto text-red-500 hover:text-red-700 flex items-center gap-1"
          >
            <StopCircle className="w-4 h-4" />
            停止
          </button>
        </div>
      )}

      {showPreview && formData.content ? (
        <div className="bg-white rounded-xl shadow-sm p-8">
          <h1 className="text-2xl font-bold text-gray-800 mb-4">{formData.title}</h1>
          {formData.summary && (
            <p className="text-gray-600 italic border-l-4 border-indigo-300 pl-4 my-4">
              {formData.summary}
            </p>
          )}
          <div 
            className="prose max-w-none"
            dangerouslySetInnerHTML={{ __html: '<p>' + renderMarkdown(formData.content) + '</p>' }}
          />
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm p-6">
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">标题 *</label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                placeholder="请输入文章标题"
                disabled={loading}
              />
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-gray-700">概要</label>
                <button
                  type="button"
                  onClick={generatingType === 'summary' ? handleStopGenerate : handleGenerateSummary}
                  disabled={!formData.title.trim()}
                  className="text-sm text-indigo-600 hover:text-indigo-700 font-medium flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
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
                className="w-full h-24 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
                placeholder="文章概要描述"
                disabled={loading}
              />
              <p className="text-xs text-gray-400 mt-1">
                {formData.summary.length} 字 · 建议 100-200 字
              </p>
            </div>

            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="block text-sm font-medium text-gray-700">正文</label>
                <button
                  type="button"
                  onClick={generatingType === 'content' ? handleStopGenerate : handleGenerateContent}
                  disabled={!formData.title.trim()}
                  className="text-sm text-indigo-600 hover:text-indigo-700 font-medium flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
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
                className="w-full h-96 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none font-mono text-sm"
                placeholder="文章正文内容（支持 Markdown 格式）"
                disabled={loading}
              />
              <p className="text-xs text-gray-400 mt-1">
                {formData.content.length} 字 · 建议 1500 字以上
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">标签</label>
              <input
                type="text"
                value={formData.tags}
                onChange={(e) => setFormData(prev => ({ ...prev, tags: e.target.value }))}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                placeholder="多个标签用逗号分隔"
                disabled={loading}
              />
            </div>

            <div className="flex gap-3 pt-4">
              <button
                type="button"
                onClick={() => navigate('/articles')}
                className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
                disabled={loading}
              >
                取消
              </button>
              <button
                type="submit"
                disabled={!formData.title.trim() || submitting || loading}
                className="flex-1 px-4 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
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
