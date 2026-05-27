import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '@/api';
import { 
  LayoutDashboard, 
  Settings, 
  LogOut, 
  User,
  Sparkles,
  PenTool,
  Layers,
  Zap
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

interface LayoutProps {
  children: React.ReactNode;
}

const navItems = [
  { id: 'dashboard', label: '控制台', icon: LayoutDashboard, badge: '' },
  { id: 'agents', label: 'Agent中心', icon: Sparkles, badge: '' },
  { id: 'articles', label: '文章创作', icon: PenTool, badge: '' },
  { id: 'collections', label: '合集管理', icon: Layers, badge: '新' },
  { id: 'settings', label: '设置', icon: Settings, badge: '' },
];

export default function Layout({ children }: LayoutProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const logout = useAuthStore((state) => state.logout);
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  
  const [sidebarOpen, setSidebarOpen] = useState(true);

  useEffect(() => {
    if (!user && localStorage.getItem('accessToken')) {
      authApi.getMe().then((res) => {
        if (res.code === 200 && res.data) {
          setUser(res.data);
        }
      }).catch(() => undefined);
    }
  }, [user, setUser]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const currentPath = location.pathname.replace(/^\//, '') || 'dashboard';

  return (
    <div className="flex min-h-screen relative">
      <div className="fixed inset-0 bg-grid opacity-30 pointer-events-none" />
      <div className="fixed inset-0 bg-radial pointer-events-none" />
      
      <aside 
        className={`relative z-10 flex flex-col transition-all duration-500 ease-out ${
          sidebarOpen ? 'w-72' : 'w-20'
        }`}
      >
        <div className="absolute inset-0 glass-card border-r" />
        
        <div className="relative p-6 border-b border-white/5">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center glow-effect flex-shrink-0">
              <Zap className="w-6 h-6 text-white" />
            </div>
            {sidebarOpen && (
              <div className="overflow-hidden">
                <h1 className="font-bold text-xl text-white tracking-tight">AgentAI</h1>
                <p className="text-gray-400 text-sm">智能创作平台</p>
              </div>
            )}
          </div>
        </div>

        <nav className="relative flex-1 p-4 space-y-2 overflow-y-auto scrollbar-thin">
          {navItems.map((item, index) => {
            const Icon = item.icon;
            const isActive = currentPath.startsWith(item.id) || 
              (item.id === 'dashboard' && currentPath === 'dashboard');
            return (
              <button
                key={item.id}
                onClick={() => navigate(item.id === 'dashboard' ? '/dashboard' : `/${item.id}`)}
                className={`nav-item w-full animate-slideIn opacity-0`}
                style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}
              >
                <div className={`p-2 rounded-xl transition-all ${
                  isActive 
                    ? 'bg-gradient-to-br from-indigo-500 to-cyan-400 text-white' 
                    : ''
                }`}>
                  <Icon className="w-5 h-5" />
                </div>
                {sidebarOpen && (
                  <div className="flex-1 flex items-center justify-between">
                    <span className="font-medium">{item.label}</span>
                    {item.badge && (
                      <span className="px-2 py-0.5 text-xs bg-gradient-to-r from-pink-500 to-orange-400 text-white rounded-full font-medium">
                        {item.badge}
                      </span>
                    )}
                  </div>
                )}
              </button>
            );
          })}
        </nav>

        <div className="relative p-4 border-t border-white/5">
          <div className="glass-card rounded-2xl p-4 mb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center flex-shrink-0">
                <User className="w-5 h-5 text-white" />
              </div>
              {sidebarOpen && (
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-white truncate">{user?.username || '用户'}</p>
                  <p className="text-sm text-gray-400 truncate">{user?.email}</p>
                </div>
              )}
            </div>
          </div>
          
          <button
            onClick={handleLogout}
            className="nav-item w-full text-gray-400 hover:text-red-400"
          >
            <div className="p-2 rounded-xl hover:bg-red-500/10">
              <LogOut className="w-5 h-5" />
            </div>
            {sidebarOpen && <span className="font-medium">退出登录</span>}
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 relative z-10">
        <header className="glass-card mx-6 mt-6 mb-0 rounded-2xl px-6 py-4 flex items-center justify-between">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-3 rounded-xl hover:bg-white/5 transition-all duration-300"
          >
            <div className="w-6 h-5 relative flex flex-col justify-between">
              <span className={`block h-0.5 bg-gray-400 rounded transition-all duration-300 ${
                sidebarOpen ? 'rotate-45 translate-y-2' : ''
              }`} />
              <span className={`block h-0.5 bg-gray-400 rounded transition-all duration-300 ${
                sidebarOpen ? 'opacity-0' : ''
              }`} />
              <span className={`block h-0.5 bg-gray-400 rounded transition-all duration-300 ${
                sidebarOpen ? '-rotate-45 -translate-y-2' : ''
              }`} />
            </div>
          </button>
          
          <div className="flex items-center gap-4">
            <div className="hidden md:flex items-center gap-2 px-4 py-2 glass-card rounded-xl">
              <Sparkles className="w-4 h-4 text-cyan-400" />
              <span className="text-sm text-gray-300">AI 服务正常</span>
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
            </div>
          </div>
        </header>

        <main className="flex-1 p-6 overflow-auto scrollbar-thin">
          <div className="animate-fadeIn">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
