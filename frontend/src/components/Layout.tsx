import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '@/api';
import { 
  LayoutDashboard, 
  BookOpen, 
  Settings, 
  LogOut, 
  Menu, 
  X,
  User,
  Sparkles
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

interface LayoutProps {
  children: React.ReactNode;
}

const navItems = [
  { id: 'dashboard', label: '控制台', icon: LayoutDashboard },
  { id: 'agents', label: 'Agent列表', icon: Sparkles },
  { id: 'articles', label: '文章管理', icon: BookOpen },
  { id: 'settings', label: '设置', icon: Settings },
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
    <div className="flex min-h-screen bg-gray-50">
      <aside 
        className={`${sidebarOpen ? 'w-64' : 'w-20'} bg-indigo-600 text-white transition-all duration-300 flex flex-col`}
      >
        <div className="p-4 border-b border-indigo-500">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
              <Sparkles className="w-6 h-6" />
            </div>
            {sidebarOpen && (
              <div>
                <h1 className="font-bold text-lg">Agent平台</h1>
                <p className="text-indigo-200 text-sm">智能助手</p>
              </div>
            )}
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentPath === item.id;
            return (
              <button
                key={item.id}
                onClick={() => navigate(item.id === 'dashboard' ? '/dashboard' : `/${item.id}`)}
                className={`w-full flex items-center gap-3 px-3 py-3 rounded-lg transition ${
                  isActive 
                    ? 'bg-white/20 text-white' 
                    : 'text-indigo-200 hover:bg-white/10 hover:text-white'
                }`}
              >
                <Icon className="w-5 h-5" />
                {sidebarOpen && <span className="font-medium">{item.label}</span>}
              </button>
            );
          })}
        </nav>

        <div className="p-4 border-t border-indigo-500">
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-3 py-3 rounded-lg text-indigo-200 hover:bg-white/10 hover:text-white transition"
          >
            <LogOut className="w-5 h-5" />
            {sidebarOpen && <span className="font-medium">退出登录</span>}
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col">
        <header className="bg-white shadow-sm px-6 py-4 flex items-center justify-between">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-2 rounded-lg hover:bg-gray-100 transition"
          >
            {sidebarOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
          
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-indigo-100 rounded-full flex items-center justify-center">
                <User className="w-5 h-5 text-indigo-600" />
              </div>
              <div>
                <p className="font-medium text-gray-800">{user?.username}</p>
                <p className="text-sm text-gray-500">{user?.email}</p>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 p-6">
          {children}
        </main>
      </div>
    </div>
  );
}