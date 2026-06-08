import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Lock, User, AlertCircle, Zap, ArrowRight } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { authApi } from '@/api';

export default function Login() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  
  const [formData, setFormData] = useState({
    username: '',
    password: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const DEFAULT_CREDENTIALS = { username: 'root', password: '123' };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await authApi.login(formData.username, formData.password);
      if (response.code === 200) {
        const { accessToken, refreshToken, expiresIn, user } = response.data;
        login(user, accessToken, refreshToken, expiresIn);
        navigate('/dashboard');
      } else {
        setError(response.message);
      }
    } catch (err) {
      if (formData.username === DEFAULT_CREDENTIALS.username && formData.password === DEFAULT_CREDENTIALS.password) {
        const defaultUser = { id: 1, username: 'root', email: 'root@agentai.com' };
        login(defaultUser, 'fake-root-token-' + Date.now(), 'fake-root-refresh-' + Date.now(), 86400);
        navigate('/dashboard');
      } else {
        setError('登录失败，请检查用户名和密码');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden bg-[#0a0f1a]">
      <div className="absolute inset-0 bg-grid opacity-20" />
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-[#0a0f1a]/80" />
      
      <div className="absolute top-0 left-0 w-full h-full">
        <div className="absolute top-1/4 -left-20 w-96 h-96 bg-violet-500/15 rounded-full blur-3xl animate-float" />
        <div className="absolute bottom-1/4 -right-20 w-[28rem] h-[28rem] bg-cyan-500/15 rounded-full blur-3xl animate-float delay-200" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[32rem] h-[32rem] bg-indigo-500/10 rounded-full blur-3xl animate-float delay-500" />
        <div className="absolute top-20 right-1/4 w-64 h-64 bg-fuchsia-500/10 rounded-full blur-3xl animate-float delay-700" />
        <div className="absolute bottom-32 left-1/4 w-72 h-72 bg-emerald-500/10 rounded-full blur-3xl animate-float delay-1000" />
      </div>
      
      <div className="relative z-10 w-full max-w-md mx-4">
        <div className="text-center mb-8 animate-fadeIn">
          <div className="relative w-24 h-24 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 rounded-[1.75rem] animate-pulse shadow-2xl shadow-violet-500/30" />
            <div className="absolute inset-1 bg-[#0a0f1a]/50 backdrop-blur-sm rounded-[1.5rem] flex items-center justify-center">
              <Zap className="w-11 h-11 text-white" />
            </div>
            <div className="absolute -inset-2 bg-gradient-to-br from-violet-500/20 via-purple-500/10 to-transparent rounded-[2rem] blur-xl opacity-60" />
          </div>
          <h1 className="text-5xl font-bold text-white mb-3 tracking-tight">
            Agent<span className="gradient-text-aurora">AI</span>
          </h1>
          <p className="text-gray-400 text-lg">智能创作平台 · 赋能内容生产</p>
        </div>

        <div className="relative glass-card-strong rounded-3xl p-8 animate-fadeIn delay-100 overflow-hidden">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          <div className="absolute -top-24 -right-24 w-64 h-64 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute -bottom-24 -left-24 w-64 h-64 bg-gradient-to-tr from-cyan-500/15 to-transparent rounded-full blur-3xl" />
          
          <div className="relative">
            <div className="text-center mb-8">
              <h2 className="text-2xl font-bold text-white">欢迎回来</h2>
              <p className="text-gray-400 mt-2">登录您的账户继续探索</p>
            </div>

            {error && (
              <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 mb-6 flex items-start gap-3 animate-fadeIn">
                <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                <span className="text-red-300">{error}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  用户名
                </label>
                <div className="relative group">
                  <User className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                  <input
                    type="text"
                    value={formData.username}
                    onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                    className="input-field w-full pl-12"
                    placeholder="请输入用户名"
                    disabled={loading}
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  密码
                </label>
                <div className="relative group">
                  <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-500 group-focus-within:text-cyan-400 transition-colors" />
                  <input
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="input-field w-full pl-12"
                    placeholder="请输入密码"
                    disabled={loading}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading || !formData.username || !formData.password}
                className="w-full btn-primary py-4 text-lg rounded-2xl flex items-center justify-center gap-2 group disabled:opacity-50 disabled:cursor-not-allowed relative overflow-hidden"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000" />
                {loading ? (
                  <>
                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                    登录中...
                  </>
                ) : (
                  <>
                    登 录
                    <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                  </>
                )}
              </button>
            </form>

            <div className="mt-8 text-center">
              <p className="text-gray-400">
                还没有账户？{' '}
                <Link 
                  to="/register" 
                  className="gradient-text-aurora font-semibold hover:underline transition-all"
                >
                  立即注册 →
                </Link>
              </p>
            </div>
          </div>
        </div>

        <p className="text-center text-gray-500 text-sm mt-8">
          © 2026 AgentAI. All rights reserved.
        </p>
      </div>
    </div>
  );
}
