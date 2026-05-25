import { create } from 'zustand';
import { Article } from '@/types';

interface ArticleStore {
  articles: Article[];
  currentArticle: Article | null;
  isLoading: boolean;
  error: string | null;
  
  setArticles: (articles: Article[]) => void;
  addArticle: (article: Article) => void;
  updateArticle: (article: Article) => void;
  deleteArticle: (id: number) => void;
  setCurrentArticle: (article: Article | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
}

export const useArticleStore = create<ArticleStore>((set) => ({
  articles: [],
  currentArticle: null,
  isLoading: false,
  error: null,

  setArticles: (articles) => set({ articles }),
  addArticle: (article) => set((state) => ({
    articles: [article, ...state.articles],
  })),
  updateArticle: (article) => set((state) => ({
    articles: state.articles.map((a) => (a.id === article.id ? article : a)),
  })),
  deleteArticle: (id) => set((state) => ({
    articles: state.articles.filter((a) => a.id !== id),
  })),
  setCurrentArticle: (article) => set({ currentArticle: article }),
  setLoading: (loading) => set({ isLoading: loading }),
  setError: (error) => set({ error }),
}));
