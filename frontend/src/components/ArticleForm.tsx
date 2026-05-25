import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Sparkles } from 'lucide-react';
import { articleApi, generateApi } from '@/api';

interface FormData {
  title: string;
  summary: string;
  content: string;
  tags: string;
}

export default function ArticleForm() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  
  const [formData, setFormData] = useState<FormData>({
    title: '',
    summary: '',
    content: '',
    tags: '',
  });
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

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
    } finally {
      setSubmitting(false);
    }
  };

  const handleGenerateSummary = async () => {
    if (!formData.title.trim()) return;
    
    setLoading(true);
    try {
      const response = await generateApi.summary(formData.title);
      if (response.code === 200 && response.data.content) {
        setFormData(prev => ({ ...prev, summary: response.data.content ?? '' }));
      }
    } catch (err) {
      console.error('Failed to generate:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateContent = async () => {
    if (!formData.title.trim()) return;
    
    setLoading(true);
    try {
      const response = await generateApi.content(formData.title);
      if (response.code === 200 && response.data.content) {
        setFormData(prev => ({ ...prev, content: response.data.content ?? '' }));
      }
    } catch (err) {
      console.error('Failed to generate:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
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
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-sm font-medium text-gray-700">概要</label>
              <button
                type="button"
                onClick={handleGenerateSummary}
                disabled={!formData.title.trim() || loading}
                className="text-sm text-indigo-600 hover:text-indigo-700 font-medium flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Sparkles className="w-4 h-4" />
                AI生成
              </button>
            </div>
            <textarea
              value={formData.summary}
              onChange={(e) => setFormData(prev => ({ ...prev, summary: e.target.value }))}
              className="w-full h-24 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
              placeholder="文章概要描述"
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="block text-sm font-medium text-gray-700">正文</label>
              <button
                type="button"
                onClick={handleGenerateContent}
                disabled={!formData.title.trim() || loading}
                className="text-sm text-indigo-600 hover:text-indigo-700 font-medium flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Sparkles className="w-4 h-4" />
                AI生成
              </button>
            </div>
            <textarea
              value={formData.content}
              onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
              className="w-full h-80 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none font-mono text-sm"
              placeholder="文章正文内容（支持Markdown格式）"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">标签</label>
            <input
              type="text"
              value={formData.tags}
              onChange={(e) => setFormData(prev => ({ ...prev, tags: e.target.value }))}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
              placeholder="多个标签用逗号分隔"
            />
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={() => navigate('/articles')}
              className="flex-1 px-4 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={!formData.title.trim() || submitting}
              className="flex-1 px-4 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting ? '保存中...' : (isEdit ? '保存修改' : '创建文章')}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}