import { useState, useEffect } from 'react';
import { Sparkles, CheckCircle, Clock, XCircle, Send, AlertCircle, Bot } from 'lucide-react';
import { agentApi } from '@/api';
import { Agent } from '@/types';

export default function AgentList() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [permissions, setPermissions] = useState<Map<number, { status: number; message?: string }>>(new Map());
  const [applyModal, setApplyModal] = useState<number | null>(null);
  const [applyReason, setApplyReason] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        const agentsRes = await agentApi.getAll();
        if (agentsRes.code === 200) {
          setAgents(agentsRes.data);
          const perms = new Map<number, { status: number; message?: string }>();
          for (const agent of agentsRes.data) {
            try {
              const statusRes = await agentApi.getPermissionStatus(agent.id);
              if (statusRes.code === 200) {
                perms.set(agent.id, statusRes.data);
              }
            } catch (err) {
              perms.set(agent.id, { status: -1, message: '未申请' });
            }
          }
          setPermissions(perms);
        }
      } catch (err) {
        console.error('Failed to fetch agents:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleApply = async (agentId: number) => {
    if (!applyReason.trim()) {
      setError('请填写申请理由');
      return;
    }
    
    try {
      const response = await agentApi.applyPermission(agentId, applyReason);
      if (response.code === 200) {
        setPermissions(prev => {
          const newPerms = new Map(prev);
          newPerms.set(agentId, { status: 0, message: '申请已提交' });
          return newPerms;
        });
        setApplyModal(null);
        setApplyReason('');
        setError('');
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError('申请失败，请稍后重试');
    }
  };

  const getStatusInfo = (status: number) => {
    switch (status) {
      case -1: return { label: '未申请', color: 'text-gray-400', bg: 'bg-white/5', icon: XCircle };
      case 0: return { label: '审核中', color: 'text-amber-400', bg: 'bg-amber-500/20', icon: Clock };
      case 1: return { label: '已通过', color: 'text-emerald-400', bg: 'bg-emerald-500/20', icon: CheckCircle };
      case 2: return { label: '已拒绝', color: 'text-red-400', bg: 'bg-red-500/20', icon: XCircle };
      default: return { label: '未知', color: 'text-gray-400', bg: 'bg-white/5', icon: AlertCircle };
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 rounded-3xl animate-pulse" />
            <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
              <Bot className="w-10 h-10 text-white" />
            </div>
          </div>
          <p className="text-gray-400 animate-pulse">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
          <Bot className="w-4 h-4 text-cyan-400" />
          <span className="text-sm text-cyan-400 font-medium">智能助手中心</span>
        </div>
        <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
          Agent<span className="gradient-text-aurora">列表</span>
        </h1>
        <p className="text-gray-400 text-lg">
          浏览和申请使用平台上的智能 Agent
        </p>
      </div>

      {agents.length === 0 ? (
        <div className="relative overflow-hidden rounded-3xl glass-card-strong py-20">
          <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-3xl" />
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-gradient-to-tr from-indigo-500/15 to-transparent rounded-full blur-2xl" />
          <div className="relative text-center">
            <div className="relative w-24 h-24 mx-auto mb-6">
              <div className="absolute inset-0 bg-gradient-to-br from-violet-500/20 to-indigo-500/20 rounded-full animate-pulse" />
              <div className="absolute inset-2 glass-card rounded-full flex items-center justify-center">
                <Bot className="w-10 h-10 text-gray-400" />
              </div>
            </div>
            <h3 className="text-white font-semibold text-xl mb-2">暂无可用 Agent</h3>
            <p className="text-gray-400 text-lg">系统正在准备中，请稍后再来查看</p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {agents.map((agent, index) => {
            const permission = permissions.get(agent.id) || { status: -1 };
            const statusInfo = getStatusInfo(permission.status);
            const StatusIcon = statusInfo.icon;
            
            return (
              <div
                key={agent.id}
                className="group relative overflow-hidden rounded-2xl glass-card glass-card-hover p-5 animate-fadeIn opacity-0"
                style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'forwards' }}
              >
                <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/5 to-transparent" />
                <div className="absolute -top-1/2 -right-1/2 w-full h-full bg-gradient-to-br from-violet-500/10 to-transparent rounded-full blur-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
                
                <div className="relative">
                  <div className="flex items-start justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-violet-500 via-purple-500 to-fuchsia-500 flex items-center justify-center shadow-lg shadow-violet-500/25 group-hover:scale-110 transition-transform duration-300">
                      <Sparkles className="w-6 h-6 text-white" />
                    </div>
                    <span className={`relative px-2.5 py-1 text-xs rounded-full font-medium overflow-hidden ${statusInfo.bg} ${statusInfo.color} border border-white/10 flex items-center gap-1`}>
                      <StatusIcon className="w-3 h-3" />
                      {statusInfo.label}
                    </span>
                  </div>
                  
                  <h3 className="font-semibold text-white text-lg mb-2">{agent.name}</h3>
                  <p className="text-gray-400 text-sm mb-4 line-clamp-2 h-10">{agent.description}</p>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-500">模块: {agent.moduleName}</span>
                    {permission.status === 1 ? (
                      <button 
                        className="btn-primary text-sm px-4 py-2"
                        onClick={() => {}}
                      >
                        立即使用
                      </button>
                    ) : permission.status === 0 ? (
                      <button 
                        className="btn-secondary text-sm px-4 py-2 cursor-not-allowed opacity-70"
                        disabled
                      >
                        审核中...
                      </button>
                    ) : (
                      <button 
                        className="btn-secondary text-sm px-4 py-2 flex items-center gap-2"
                        onClick={() => setApplyModal(agent.id)}
                      >
                        <Send className="w-4 h-4" />
                        申请使用
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {applyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-md" onClick={() => { setApplyModal(null); setApplyReason(''); setError(''); }} />
          <div className="relative glass-card-strong rounded-3xl max-w-md w-full max-h-[90vh] overflow-hidden flex flex-col animate-scaleIn shadow-2xl">
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/10 to-transparent" />
            <div className="absolute top-0 right-0 w-48 h-48 bg-gradient-to-br from-violet-500/20 to-transparent rounded-full blur-2xl" />
            
            <div className="p-6 relative">
              <h3 className="text-xl font-bold text-white mb-2">申请使用 Agent</h3>
              <p className="text-gray-400 text-sm mb-4">请填写申请理由，审核通过后即可使用</p>
              
              {error && (
                <div className="p-4 bg-red-500/10 border border-red-500/30 rounded-xl text-red-400 text-sm animate-fadeIn">
                  {error}
                </div>
              )}
              
              <textarea
                value={applyReason}
                onChange={(e) => {
                  setApplyReason(e.target.value);
                  setError('');
                }}
                className="input-field w-full h-32 resize-none mt-4"
                placeholder="请说明您的使用场景和需求..."
              />
              
              <div className="flex gap-3 mt-6">
                <button
                  onClick={() => {
                    setApplyModal(null);
                    setApplyReason('');
                    setError('');
                  }}
                  className="flex-1 btn-secondary"
                >
                  取消
                </button>
                <button
                  onClick={() => handleApply(applyModal)}
                  className="flex-1 btn-primary"
                >
                  提交申请
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
