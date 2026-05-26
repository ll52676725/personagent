import { useEffect, useState } from 'react';
import { User, Mail, Shield, Bell, Globe, Save, CheckCircle, Key, Database } from 'lucide-react';
import { authApi } from '@/api';
import { useAuthStore } from '@/store/authStore';
import type { SettingsSection } from '@/types';

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
  const [apiKey, setApiKey] = useState('');
  const [apiUrl, setApiUrl] = useState('');
  const [aiModel, setAiModel] = useState('');

  const handleSave = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  const settingsSections: SettingsSection[] = [
    {
      title: '个人资料',
      icon: User,
      gradient: 'from-violet-500 to-purple-600',
      displayFields: [
        { label: '用户名', value: userInfo.username, icon: User },
        { label: '邮箱', value: userInfo.email, icon: Mail },
      ],
      inputFields: undefined,
      toggles: undefined
    },
    {
      title: 'AI 配置',
      icon: Key,
      gradient: 'from-indigo-500 to-cyan-400',
      displayFields: undefined,
      inputFields: [
        { label: 'API Key', value: apiKey, onChange: setApiKey, icon: Key, type: 'password' as const, placeholder: 'sk-...' },
        { label: 'API URL', value: apiUrl, onChange: setApiUrl, icon: Globe, placeholder: 'https://api.openai.com/v1' },
        { label: '默认模型', value: aiModel, onChange: setAiModel, icon: Database, placeholder: 'gpt-4o-mini' },
      ],
      toggles: undefined
    },
    {
      title: '通知设置',
      icon: Bell,
      gradient: 'from-emerald-500 to-teal-400',
      displayFields: undefined,
      inputFields: undefined,
      toggles: [
        { label: '邮件通知', value: notifications.email, onChange: (v: boolean) => setNotifications(p => ({ ...p, email: v })) },
        { label: '推送通知', value: notifications.push, onChange: (v: boolean) => setNotifications(p => ({ ...p, push: v })) },
        { label: '周报推送', value: notifications.weekly, onChange: (v: boolean) => setNotifications(p => ({ ...p, weekly: v })) },
      ]
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">设置</h1>
          <p className="text-gray-400 mt-1">管理您的账户和应用设置</p>
        </div>
        <button
          onClick={handleSave}
          className="btn-primary flex items-center gap-2"
        >
          {saved ? (
            <>
              <CheckCircle className="w-5 h-5" />
              已保存
            </>
          ) : (
            <>
              <Save className="w-5 h-5" />
              保存设置
            </>
          )}
        </button>
      </div>

      <div className="space-y-6">
        {settingsSections.map((section) => {
          const Icon = section.icon;
          return (
            <div key={section.title} className="glass-card rounded-3xl p-6 glass-card-hover">
              <div className="flex items-center gap-4 mb-6">
                <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${section.gradient} flex items-center justify-center shadow-lg`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-white">{section.title}</h2>
                  <p className="text-gray-400 text-sm">
                    {section.title === '个人资料' && '您的基本信息'}
                    {section.title === 'AI 配置' && '连接您的 AI 服务'}
                    {section.title === '通知设置' && '管理通知偏好'}
                  </p>
                </div>
              </div>

              {section.displayFields && (
                <div className="space-y-4">
                  {section.displayFields.map((field) => {
                    const Icon = field.icon;
                    return (
                      <div key={field.label}>
                        <label className="block text-sm font-medium text-gray-300 mb-2">
                          {field.label}
                        </label>
                        <div className="relative">
                          <Icon className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-500" />
                          <input
                            type="text"
                            value={field.value}
                            disabled
                            className="input-field w-full pl-12 opacity-70"
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {section.inputFields && (
                <div className="space-y-4">
                  {section.inputFields.map((field) => {
                    const Icon = field.icon;
                    return (
                      <div key={field.label}>
                        <label className="block text-sm font-medium text-gray-300 mb-2">
                          {field.label}
                        </label>
                        <div className="relative">
                          <Icon className="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-500" />
                          <input
                            type={field.type || 'text'}
                            value={field.value}
                            onChange={(e) => field.onChange(e.target.value)}
                            placeholder={field.placeholder}
                            className="input-field w-full pl-12"
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {section.toggles && (
                <div className="space-y-4">
                  {section.toggles.map((toggle) => (
                    <div key={toggle.label} className="flex items-center justify-between py-3 border-b border-white/5 last:border-0">
                      <span className="text-gray-300">{toggle.label}</span>
                      <button
                        onClick={() => toggle.onChange(!toggle.value)}
                        className={`relative w-14 h-7 rounded-full transition-all ${
                          toggle.value 
                            ? 'bg-gradient-to-r from-indigo-500 to-cyan-400' 
                            : 'bg-white/10'
                        }`}
                      >
                        <span className={`absolute top-1 left-1 w-5 h-5 bg-white rounded-full shadow-lg transition-transform ${
                          toggle.value ? 'translate-x-7' : ''
                        }`} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}

        <div className="glass-card rounded-3xl p-6">
          <div className="flex items-center gap-4 mb-4">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-400 flex items-center justify-center shadow-lg">
              <Shield className="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">安全提示</h2>
              <p className="text-gray-400 text-sm">保护您的账户安全</p>
            </div>
          </div>
          <div className="bg-amber-500/10 border border-amber-500/30 rounded-2xl p-4">
            <p className="text-amber-300 text-sm">
              API Key 仅存储在本地浏览器中，不会上传到服务器。请妥善保管您的密钥，不要分享给他人。
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
