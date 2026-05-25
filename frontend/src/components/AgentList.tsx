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
      case -1: return { label: '未申请', color: 'text-gray-500', bg: 'bg-gray-100', icon: XCircle };
      case 0: return { label: '审核中', color: 'text-yellow-500', bg: 'bg-yellow-100', icon: Clock };
      case 1: return { label: '已通过', color: 'text-green-500', bg: 'bg-green-100', icon: CheckCircle };
      case 2: return { label: '已拒绝', color: 'text-red-500', bg: 'bg-red-100', icon: XCircle };
      default: return { label: '未知', color: 'text-gray-500', bg: 'bg-gray-100', icon: AlertCircle };
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Agent列表</h1>
        <p className="text-gray-500 mt-1">浏览和申请使用平台上的智能Agent</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {agents.map((agent) => {
          const permission = permissions.get(agent.id) || { status: -1 };
          const statusInfo = getStatusInfo(permission.status);
          const StatusIcon = statusInfo.icon;
          
          return (
            <div 
              key={agent.id}
              className="bg-white rounded-xl shadow-sm p-6 hover:shadow-md transition"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="w-14 h-14 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center">
                  <Sparkles className="w-7 h-7 text-white" />
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-medium ${statusInfo.bg} ${statusInfo.color}`}>
                  <StatusIcon className="w-3 h-3 inline mr-1" />
                  {statusInfo.label}
                </span>
              </div>
              
              <h3 className="text-lg font-semibold text-gray-800 mb-2">{agent.name}</h3>
              <p className="text-gray-500 text-sm mb-4 line-clamp-2">{agent.description}</p>
              
              <div className="flex items-center justify-between">
                <span className="text-xs text-gray-400">模块: {agent.moduleName}</span>
                {permission.status === 1 ? (
                  <button 
                    className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 transition"
                    onClick={() => {}}
                  >
                    立即使用
                  </button>
                ) : permission.status === 0 ? (
                  <button 
                    className="px-4 py-2 bg-gray-100 text-gray-600 text-sm rounded-lg cursor-not-allowed"
                    disabled
                  >
                    审核中...
                  </button>
                ) : (
                  <button 
                    className="px-4 py-2 border border-indigo-600 text-indigo-600 text-sm rounded-lg hover:bg-indigo-50 transition flex items-center gap-2"
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

      {applyModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl p-6 w-full max-w-md mx-4">
            <h3 className="text-lg font-semibold text-gray-800 mb-2">申请使用Agent</h3>
            <p className="text-gray-500 text-sm mb-4">请填写申请理由，审核通过后即可使用</p>
            
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-red-600 text-sm">
                {error}
              </div>
            )}
            
            <textarea
              value={applyReason}
              onChange={(e) => {
                setApplyReason(e.target.value);
                setError('');
              }}
              className="w-full h-32 p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none resize-none"
              placeholder="请说明您的使用场景和需求..."
            />
            
            <div className="flex gap-3 mt-4">
              <button
                onClick={() => {
                  setApplyModal(null);
                  setApplyReason('');
                  setError('');
                }}
                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
              >
                取消
              </button>
              <button
                onClick={() => handleApply(applyModal)}
                className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
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