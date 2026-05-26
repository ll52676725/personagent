import { useState, useEffect } from 'react';
import { Sparkles, CheckCircle, Clock, XCircle, Send, AlertCircle } from 'lucide-react';
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
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center mx-auto mb-4 animate-pulse">
            <Sparkles className="w-8 h-8 text-white" />
          </div>
          <p className="text-gray-400">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-white">Agent 列表</h1>
        <p className="text-gray-400 mt-1">浏览和申请使用平台上的智能 Agent</p>
      </div>

      {agents.length === 0 ? (
        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-20 h-20 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
            <Sparkles className="w-10 h-10 text-gray-500" />
          </div>
          <h3 className="text-white font-semibold text-lg mb-2">暂无可用 Agent</h3>
          <p className="text-gray-400">系统正在准备中，请稍后再来查看</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {agents.map((agent, index) => {
            const permission = permissions.get(agent.id) || { status: -1 };
            const statusInfo = getStatusInfo(permission.status);
            const StatusIcon = statusInfo.icon;
            
            return (
              <div 
                key={agent.id}
                className="glass-card rounded-3xl p-6 glass-card-hover animate-fadeIn opacity-0"
                style={{ animationDelay: `${index * 80}ms`, animationFillMode: 'forwards' }}
              >
                <div className="flex items-start justify-between mb-4">
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500 to-cyan-400 flex items-center justify-center shadow-lg">
                    <Sparkles className="w-7 h-7 text-white" />
                  </div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusInfo.bg} ${statusInfo.color} flex items-center gap-1`}>
                    <StatusIcon className="w-3 h-3" />
                    {statusInfo.label}
                  </span>
                </div>
                
                <h3 className="text-lg font-semibold text-white mb-2">{agent.name}</h3>
                <p className="text-gray-400 text-sm mb-4 line-clamp-2">{agent.description}</p>
                
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
            );
          })}
        </div>
      )}

      {applyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => { setApplyModal(null); setApplyReason(''); setError(''); }} />
          <div className="relative glass-card rounded-3xl p-6 w-full max-w-md animate-fadeIn">
            <h3 className="text-xl font-bold text-white mb-2">申请使用 Agent</h3>
            <p className="text-gray-400 text-sm mb-4">请填写申请理由，审核通过后即可使用</p>
            
            {error && (
              <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 mb-4 text-red-300 text-sm animate-fadeIn">
                {error}
              </div>
            )}
            
            <textarea
              value={applyReason}
              onChange={(e) => {
                setApplyReason(e.target.value);
                setError('');
              }}
              className="input-field w-full h-32 resize-none"
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
      )}
    </div>
  );
}
