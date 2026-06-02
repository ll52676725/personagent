import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Lock, Mail, User, AlertCircle, CheckCircle, Zap, ArrowRight } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

export default function Register() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess(false);

    if (formData.password !== formData.confirmPassword) {
      setError('两次输入的密码不一致');
      setLoading(false);
      return;
    }

    const fakeUser = {
      id: 1,
      username: formData.username,
      email: formData.email,
      avatar: undefined,
    };
    const fakeAccessToken = 'fake-access-token-' + Date.now();
    const fakeRefreshToken = 'fake-refresh-token-' + Date.now();
    login(fakeUser, fakeAccessToken, fakeRefreshToken, 86400);
    navigate('/dashboard');
    setLoading(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden py-8">
      <div className="absolute inset-0 bg-grid opacity-30" />
      <div className="absolute inset-0 bg-radial" />
      
      <div className="absolute top-10 right-10 w-80 h-80 bg-purple-500/20 rounded-full blur-3xl animate-float" />
      <div className="absolute bottom-10 left-10 w-96 h-96 bg-pink-500/15 rounded-full blur-3xl animate-float delay-200" />
      
      <div className="relative z-10 w-full max-w-md mx-4">
        <div className="text-center mb-8 animate-fadeIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-violet-500 to-purple-600 rounded-3xl animate-pulse" />
            <div className="absolute inset-0.5 bg-[#0a0f1a] rounded-[1.35rem] flex items-center justify-center">
              <Zap className="w-10 h-10 text-white" />
            </div>
          </div>
          <h1 className="text-4xl font-bold text-white mb-3 tracking-tight">
            Agent<span className="gradient-text-aurora">AI</span>
          </h1>
          <p className="text-gray-400 text-lg">智能创作平台 · 赋能内容生产</p>
        </div>

        <div className="relative overflow-hidden glass-card rounded-3xl p-8 animate-fadeIn delay-100">
          <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          <div className="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-40 h-40 bg-gradient-to-tr from-indigo-500/15 to-transparent rounded-full blur-2xl" />
          
          <div className="relative">
            <div className="text-center mb-8">
              <h2 className="text-2xl font-bold text-white">创建<span className="gradient-text-aurora">账户</span></h2>
              <p className="text-gray-400 mt-2">开始您的 AI 创作之旅</p>
            </div>

          {success ? (
            <div className="relative overflow-hidden bg-emerald-500/10 border border-emerald-500/30 rounded-2xl p-8 text-center animate-fadeIn">
              <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-emerald-500/20 to-transparent rounded-full blur-2xl" />
              <div className="relative">
                <div className="w-16 h-16 rounded-full bg-emerald-500/20 flex items-center justify-center mx-auto mb-4">
                  <CheckCircle className="w-10 h-10 text-emerald-400" />
                </div>
                <h3 className="text-xl font-semibold text-emerald-300 mb-2">注册成功</h3>
                <p className="text-emerald-400/80">正在跳转到登录页面...</p>
              </div>
            </div>
          ) : (
            <>
              {error && (
                <div className="relative overflow-hidden bg-red-500/10 border border-red-500/30 rounded-2xl p-4 mb-6 flex items-start gap-3 animate-fadeIn">
                  <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-red-500/15 to-transparent rounded-full blur-xl" />
                  <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5 relative" />
                  <span className="text-red-300 relative">{error}</span>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-5">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    用户名
                  </label>
                  <div className="relative">
                    <User className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      type="text"
                      value={formData.username}
                      onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                      className="input-field w-full pl-12"
                      placeholder="4-20位字母、数字或下划线"
                      disabled={loading}
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    邮箱
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      className="input-field w-full pl-12"
                      placeholder="请输入邮箱地址"
                      disabled={loading}
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    密码
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
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

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">
                    确认密码
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input
                      type="password"
                      value={formData.confirmPassword}
                      onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                      className="input-field w-full pl-12"
                      placeholder="请再次输入密码"
                      disabled={loading}
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading || !formData.username || !formData.email || !formData.password || !formData.confirmPassword}
                  className="w-full btn-primary py-4 text-lg rounded-2xl flex items-center justify-center gap-2 group disabled:opacity-50 disabled:cursor-not-allowed mt-6"
                >
                  {loading ? (
                    <>
                      <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                      注册中...
                    </>
                  ) : (
                    <>
                      注 册
                      <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                    </>
                  )}
                </button>
              </form>
            </>
          )}

          <div className="mt-8 text-center">
            <p className="text-gray-400">
              已有账户？{' '}
              <Link 
                to="/login" 
                className="gradient-text-aurora font-semibold hover:underline"
              >
                立即登录 →
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
