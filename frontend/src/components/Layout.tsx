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
  Zap,
  Library,
  Wrench,
  Bell,
  Search,
  ChevronDown,
  FileText,
  GitMerge,
  Bot,
  Presentation
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
  { id: 'knowledge', label: '知识库', icon: Library, badge: '新' },
  { id: 'rules', label: '规则管理', icon: FileText, badge: '新' },
  { id: 'tech-report', label: '技术汇报', icon: Presentation, badge: '新' },
  { id: 'tools', label: '工具集', icon: Wrench, badge: '新' },
  { id: 'settings', label: '设置', icon: Settings, badge: '' },
];

const rulesSubNav = [
  { id: 'rules/templates', label: '规则模板', icon: FileText },
  { id: 'rules/configs', label: '拉取配置', icon: Settings },
  { id: 'rules/conflicts', label: '冲突处理', icon: GitMerge },
  { id: 'rules/ai-generator', label: 'AI生成规则', icon: Bot },
];

const techReportSubNav = [
  { id: 'tech-report', label: '汇报生成器', icon: Presentation },
];

export default function Layout({ children }: LayoutProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const logout = useAuthStore((state) => state.logout);
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [expandedMenus, setExpandedMenus] = useState<string[]>(['rules', 'tech-report']);

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

  const toggleMenu = (menuId: string) => {
    setExpandedMenus(prev => 
      prev.includes(menuId) 
        ? prev.filter(id => id !== menuId)
        : [...prev, menuId]
    );
  };

  const getSubNav = (itemId: string) => {
    if (itemId === 'rules') return rulesSubNav;
    if (itemId === 'tech-report') return techReportSubNav;
    return null;
  };

  const handleNavClick = (item: any) => {
    const subNav = getSubNav(item.id);
    if (subNav && sidebarOpen) {
      toggleMenu(item.id);
      if (!expandedMenus.includes(item.id)) {
        navigate(`/${subNav[0].id}`);
      }
    } else {
      navigate(item.id === 'dashboard' ? '/dashboard' : 
               item.id === 'knowledge' ? '/knowledge/bases' : 
               item.id === 'tools' ? '/tools' : 
               item.id === 'rules' ? '/rules/templates' :
               item.id === 'tech-report' ? '/tech-report' :
               `/${item.id}`);
    }
  };

  return (
    <div className="flex min-h-screen relative">
      <div className="fixed inset-0 bg-grid-dot opacity-40 pointer-events-none" />
      <div className="fixed inset-0 bg-radial pointer-events-none" />
      
      <aside 
        className={`relative z-10 flex flex-col transition-all duration-500 ease-out ${
          sidebarOpen ? 'w-72' : 'w-20'
        }`}
      >
        <div className="absolute inset-0 glass-card-strong border-r border-white/5" />
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-indigo-500/30 to-transparent" />
        
        <div className="relative p-6 border-b border-white/5">
          <div className="flex items-center gap-4">
            <div className="relative">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-cyan-400 flex items-center justify-center glow-effect flex-shrink-0">
                <Zap className="w-6 h-6 text-white" />
              </div>
              <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-emerald-500 rounded-full border-2 border-[#0a0f1a]">
                <span className="absolute inset-0 bg-emerald-400 rounded-full animate-ping opacity-75" />
              </div>
            </div>
            {sidebarOpen && (
              <div className="overflow-hidden">
                <h1 className="font-bold text-xl text-white tracking-tight">
                  Agent<span className="gradient-text-aurora">AI</span>
                </h1>
                <p className="text-gray-400 text-sm">智能创作平台</p>
              </div>
            )}
          </div>
        </div>

        <nav className="relative flex-1 p-4 space-y-1.5 overflow-y-auto scrollbar-thin">
          {navItems.map((item, index) => {
            const Icon = item.icon;
            const ChevronIcon = ChevronDown;
            const isActive = currentPath.startsWith(item.id) || 
              (item.id === 'dashboard' && currentPath === 'dashboard') ||
              (item.id === 'knowledge' && currentPath.startsWith('knowledge')) ||
              (item.id === 'tools' && currentPath.startsWith('tools'));
            const isExpanded = expandedMenus.includes(item.id);
            const subNav = getSubNav(item.id);
            
            return (
              <div key={item.id} className="animate-slideIn opacity-0" style={{ animationDelay: `${index * 50}ms`, animationFillMode: 'forwards' }}>
                <button
                  onClick={() => handleNavClick(item)}
                  className="nav-item w-full"
                >
                  <div className={`p-2.5 rounded-xl transition-all ${
                    isActive 
                      ? 'bg-gradient-to-br from-indigo-500 to-cyan-400 text-white shadow-lg shadow-indigo-500/25' 
                      : 'text-gray-400 group-hover:text-white'
                  }`}>
                    <Icon className="w-5 h-5" />
                  </div>
                  {sidebarOpen && (
                    <div className="flex-1 flex items-center justify-between">
                      <span className="font-medium">{item.label}</span>
                      <div className="flex items-center gap-1.5">
                        {item.badge && (
                          <span className="relative px-2 py-0.5 text-xs bg-gradient-to-r from-pink-500 to-orange-400 text-white rounded-full font-medium overflow-hidden">
                            <span className="relative z-10">{item.badge}</span>
                            <span className="absolute inset-0 animate-shimmer" />
                          </span>
                        )}
                        {subNav && (
                          <ChevronIcon className={`w-4 h-4 transition-transform ${isExpanded ? 'rotate-180' : ''}`} />
                        )}
                      </div>
                    </div>
                  )}
                </button>
                
                {subNav && sidebarOpen && isExpanded && (
                  <div className="ml-3 mt-1 space-y-1 border-l border-white/10 pl-3">
                    {subNav.map((subItem) => {
                      const SubIcon = subItem.icon;
                      const isSubActive = currentPath === subItem.id;
                      return (
                        <button
                          key={subItem.id}
                          onClick={() => navigate(`/${subItem.id}`)}
                          className={`nav-item w-full text-sm py-2 ${
                            isSubActive ? 'text-cyan-400' : 'text-gray-500 hover:text-gray-300'
                          }`}
                        >
                          <div className="p-1.5 rounded-lg">
                            <SubIcon className="w-4 h-4" />
                          </div>
                          <span className="font-medium">{subItem.label}</span>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        <div className="relative p-4 border-t border-white/5">
          <div className="glass-card-strong rounded-2xl p-4 mb-4 relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-2xl" />
            <div className="flex items-center gap-3 relative">
              <div className="relative">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-violet-500 via-purple-500 to-pink-500 flex items-center justify-center flex-shrink-0 shadow-lg">
                  <User className="w-5 h-5 text-white" />
                </div>
                <div className="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 bg-emerald-500 rounded-full border-2 border-[#0a0f1a]" />
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
            className="nav-item w-full text-gray-400 hover:text-red-400 group"
          >
            <div className="p-2.5 rounded-xl group-hover:bg-red-500/10 transition-colors">
              <LogOut className="w-5 h-5" />
            </div>
            {sidebarOpen && <span className="font-medium">退出登录</span>}
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col min-w-0 relative z-10">
        <header className="relative mx-6 mt-6 mb-0">
          <div className="absolute inset-0 glass-card-strong rounded-2xl" />
          <div className="absolute inset-0 rounded-2xl overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
          </div>
          <div className="relative px-6 py-4 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-3 rounded-xl hover:bg-white/5 transition-all duration-300 group"
              >
                <div className="w-6 h-5 relative flex flex-col justify-between">
                  <span className={`block h-0.5 bg-gray-400 group-hover:bg-white rounded transition-all duration-300 ${
                    sidebarOpen ? 'rotate-45 translate-y-2' : ''
                  }`} />
                  <span className={`block h-0.5 bg-gray-400 group-hover:bg-white rounded transition-all duration-300 ${
                    sidebarOpen ? 'opacity-0' : ''
                  }`} />
                  <span className={`block h-0.5 bg-gray-400 group-hover:bg-white rounded transition-all duration-300 ${
                    sidebarOpen ? '-rotate-45 -translate-y-2' : ''
                  }`} />
                </div>
              </button>

              <div className="hidden lg:flex items-center gap-3 px-4 py-2.5 glass-card rounded-xl flex-1 max-w-md">
                <Search className="w-4 h-4 text-gray-500" />
                <input 
                  type="text" 
                  placeholder="搜索功能、Agent、文章..." 
                  className="flex-1 bg-transparent text-sm text-gray-300 placeholder-gray-500 outline-none"
                />
                <kbd className="hidden xl:inline-block px-2 py-0.5 text-xs text-gray-500 bg-white/5 rounded border border-white/10">
                  ⌘K
                </kbd>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <div className="hidden md:flex items-center gap-2 px-4 py-2.5 glass-card rounded-xl relative overflow-hidden group cursor-pointer">
                <div className="absolute inset-0 bg-gradient-to-r from-emerald-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                <Sparkles className="w-4 h-4 text-emerald-400 relative" />
                <span className="text-sm text-gray-300 relative">AI 服务正常</span>
                <span className="relative">
                  <span className="w-2.5 h-2.5 bg-emerald-400 rounded-full block" />
                  <span className="absolute inset-0 w-2.5 h-2.5 bg-emerald-400 rounded-full animate-ping opacity-60" />
                </span>
              </div>

              <button className="relative p-3 rounded-xl glass-card hover:bg-white/5 transition-all duration-300 group">
                <Bell className="w-5 h-5 text-gray-400 group-hover:text-white transition-colors" />
                <span className="absolute top-2 right-2 w-2 h-2 bg-pink-500 rounded-full" />
              </button>

              <div className="hidden sm:flex items-center gap-3 pl-4 border-l border-white/10">
                <div className="w-9 h-9 rounded-full bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center">
                  <User className="w-4 h-4 text-white" />
                </div>
                <div className="hidden lg:block">
                  <p className="text-sm font-medium text-white">{user?.username || '用户'}</p>
                  <p className="text-xs text-gray-500">专业版</p>
                </div>
                <ChevronDown className="w-4 h-4 text-gray-500" />
              </div>
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
