import { useEffect, useState } from 'react';
import { User, Mail, Shield, Bell, Globe, Save, CheckCircle } from 'lucide-react';
import { authApi } from '@/api';
import { useAuthStore } from '@/store/authStore';

export default function Settings() {
  const storeUser = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const [userInfo, setUserInfo] = useState({
    username: storeUser?.username ?? '',
    email: storeUser?.email ?? '',
    avatar: storeUser?.avatar ?? '',
  });

  useEffect(() => {
    const load = async () => {
      try {
        const res = await authApi.getMe();
        if (res.code === 200 && res.data) {
          setUser(res.data);
          setUserInfo({
            username: res.data.username,
            email: res.data.email,
            avatar: res.data.avatar ?? '',
          });
        }
      } catch {
        /* 保持本地缓存用户信息 */
      }
    };
    load();
  }, [setUser]);
  const [notifications, setNotifications] = useState({
    email: true,
    push: false,
    weekly: true,
  });
  const [saved, setSaved] = useState(false);

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">设置</h1>
        <p className="text-gray-500 mt-1">管理您的账户设置</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center gap-3 mb-6">
              <User className="w-5 h-5 text-indigo-600" />
              <h2 className="text-lg font-semibold text-gray-800">账户信息</h2>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">用户名</label>
                <input
                  type="text"
                  value={userInfo.username}
                  onChange={(e) => setUserInfo(prev => ({ ...prev, username: e.target.value }))}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">邮箱</label>
                <div className="flex items-center gap-3">
                  <input
                    type="email"
                    value={userInfo.email}
                    onChange={(e) => setUserInfo(prev => ({ ...prev, email: e.target.value }))}
                    className="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
                  />
                  <span className="px-3 py-2 bg-green-100 text-green-700 rounded-lg text-sm font-medium flex items-center gap-1">
                    <CheckCircle className="w-4 h-4" />
                    已验证
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center gap-3 mb-6">
              <Bell className="w-5 h-5 text-indigo-600" />
              <h2 className="text-lg font-semibold text-gray-800">通知设置</h2>
            </div>

            <div className="space-y-4">
              {[
                { key: 'email', label: '邮件通知', desc: '接收重要更新和新闻邮件', icon: Mail },
                { key: 'push', label: '推送通知', desc: '接收浏览器推送通知', icon: Globe },
                { key: 'weekly', label: '周报', desc: '每周收到平台更新摘要', icon: Shield },
              ].map((item) => {
                const Icon = item.icon;
                return (
                  <div key={item.key} className="flex items-center justify-between">
                    <div>
                      <div className="flex items-center gap-3">
                        <Icon className="w-5 h-5 text-gray-400" />
                        <span className="font-medium text-gray-800">{item.label}</span>
                      </div>
                      <p className="text-sm text-gray-500 ml-8">{item.desc}</p>
                    </div>
                    <button
                      onClick={() => setNotifications(prev => ({
                        ...prev,
                        [item.key]: !prev[item.key as keyof typeof prev],
                      }))}
                      className={`w-12 h-6 rounded-full transition-colors ${
                        notifications[item.key as keyof typeof notifications]
                          ? 'bg-indigo-600'
                          : 'bg-gray-300'
                      }`}
                    >
                      <span
                        className={`inline-block w-5 h-5 bg-white rounded-full shadow transition-transform ${
                          notifications[item.key as keyof typeof notifications]
                            ? 'translate-x-6'
                            : 'translate-x-0.5'
                        }`}
                      />
                    </button>
                  </div>
                );
              })}
            </div>
          </div>

          <button
            onClick={handleSave}
            className="w-full px-4 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition flex items-center justify-center gap-2"
          >
            {saved ? (
              <>
                <CheckCircle className="w-5 h-5" />
                已保存
              </>
            ) : (
              <>
                <Save className="w-5 h-5" />
                保存更改
              </>
            )}
          </button>
        </div>

        <div className="space-y-6">
          <div className="bg-gradient-to-br from-indigo-600 to-purple-600 rounded-xl p-6 text-white">
            <h3 className="text-lg font-semibold mb-2">安全提示</h3>
            <p className="text-indigo-100 text-sm mb-4">
              请定期更新密码，确保账户安全。建议使用强密码并启用双重认证。
            </p>
            <button className="w-full px-4 py-2 bg-white/20 text-white rounded-lg hover:bg-white/30 transition text-sm">
              修改密码
            </button>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">平台信息</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">版本</span>
                <span className="text-gray-800">1.0.0</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">上次更新</span>
                <span className="text-gray-800">2026-05-23</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">服务状态</span>
                <span className="flex items-center gap-1">
                  <span className="w-2 h-2 bg-green-500 rounded-full"></span>
                  <span className="text-green-600">正常</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}