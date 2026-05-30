import { useState, useEffect } from 'react';
import { AlertTriangle, Check, X, ArrowRightLeft, FileDown, Eye, Clock } from 'lucide-react';
import { rulesApi } from '@/api';
import { RulePullLog, ConflictStrategyOption } from '@/types';

const STRATEGY_OPTIONS: ConflictStrategyOption[] = [
  { code: 'overwrite', label: '覆盖本地', description: '使用远程模板内容覆盖本地文件', color: 'red' },
  { code: 'keep_local', label: '保留本地', description: '取消本次拉取，保留本地文件不变', color: 'green' },
  { code: 'merge', label: '智能合并', description: '尝试自动合并两者内容', color: 'purple' },
  { code: 'rename', label: '重命名保存', description: '将新内容另存为新文件', color: 'amber' },
];

const getConflictTypeLabel = (type: string) => {
  const labels: Record<string, { label: string; color: string }> = {
    NEW_FILE: { label: '新建文件', color: 'text-green-400' },
    MINOR_CHANGES: { label: '轻微差异', color: 'text-blue-400' },
    MODERATE_CHANGES: { label: '中度差异', color: 'text-amber-400' },
    MAJOR_CONFLICT: { label: '严重冲突', color: 'text-red-400' },
    LOCAL_EMPTY: { label: '本地为空', color: 'text-blue-400' },
    REMOTE_EMPTY: { label: '远程为空', color: 'text-gray-400' },
    IDENTICAL: { label: '内容相同', color: 'text-green-400' },
  };
  return labels[type] || { label: type, color: 'text-gray-400' };
};

const getStatusBadge = (status: string) => {
  const badges: Record<string, { label: string; color: string }> = {
    PENDING: { label: '待处理', color: 'bg-amber-500/20 text-amber-400' },
    CONFLICT: { label: '有冲突', color: 'bg-red-500/20 text-red-400' },
    RESOLVED: { label: '已解决', color: 'bg-green-500/20 text-green-400' },
    SUCCESS: { label: '成功', color: 'bg-emerald-500/20 text-emerald-400' },
    FAILED: { label: '失败', color: 'bg-red-500/20 text-red-400' },
    PREVIEW: { label: '预览', color: 'bg-blue-500/20 text-blue-400' },
  };
  return badges[status] || { label: status, color: 'bg-gray-500/20 text-gray-400' };
};

export default function ConflictResolver() {
  const [conflicts, setConflicts] = useState<RulePullLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedConflict, setSelectedConflict] = useState<RulePullLog | null>(null);
  const [selectedStrategy, setSelectedStrategy] = useState<string>('');
  const [mergedContent, setMergedContent] = useState('');
  const [renameSuffix, setRenameSuffix] = useState('.new');
  const [resolving, setResolving] = useState(false);
  const [showDiff, setShowDiff] = useState(true);

  useEffect(() => {
    fetchConflicts();
  }, []);

  const fetchConflicts = async () => {
    try {
      setLoading(true);
      const res = await rulesApi.getConflicts();
      if (res.code === 200) setConflicts(res.data);
    } catch (err) {
      console.error('Failed to fetch conflicts:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectConflict = (conflict: RulePullLog) => {
    setSelectedConflict(conflict);
    setSelectedStrategy('');
    setMergedContent(conflict.mergedContent || '');
  };

  const handleResolve = async () => {
    if (!selectedConflict || !selectedStrategy) return;
    
    if (selectedStrategy === 'rename' && !renameSuffix) {
      alert('请输入文件后缀');
      return;
    }

    try {
      setResolving(true);
      const res = await rulesApi.resolveConflict({
        logId: selectedConflict.id,
        resolutionStrategy: selectedStrategy,
        mergedContent: selectedStrategy === 'merge' ? mergedContent : undefined,
        renameSuffix: selectedStrategy === 'rename' ? renameSuffix : undefined,
      });
      if (res.code === 200) {
        alert('冲突解决成功');
        setSelectedConflict(null);
        fetchConflicts();
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '处理失败';
      alert(errorMsg);
    } finally {
      setResolving(false);
    }
  };

  const pendingConflicts = conflicts.filter(c => c.status === 'PENDING' || c.status === 'CONFLICT');
  const resolvedConflicts = conflicts.filter(c => c.status === 'RESOLVED' || c.status === 'SUCCESS' || c.status === 'FAILED');

  if (loading && conflicts.length === 0) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center animate-scaleIn">
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 bg-gradient-to-br from-amber-500 to-orange-600 rounded-3xl animate-pulse" />
            <div className="absolute inset-1 bg-[#0a0f1a] rounded-[1.3rem] flex items-center justify-center">
              <AlertTriangle className="w-10 h-10 text-white" />
            </div>
          </div>
          <p className="text-gray-400 animate-pulse">加载中...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-3">
            <AlertTriangle className="w-4 h-4 text-amber-400" />
            <span className="text-sm text-amber-400 font-medium">冲突处理</span>
          </div>
          <h1 className="text-4xl font-bold text-white mb-2 tracking-tight">
            冲突<span className="gradient-text-aurora">解决</span>
          </h1>
          <p className="text-gray-400 text-lg">
            处理规则拉取过程中的文件冲突
          </p>
        </div>
        <div className="flex items-center gap-4">
          <div className="px-4 py-2 bg-amber-500/10 rounded-xl border border-amber-500/20">
            <span className="text-amber-400 font-medium">{pendingConflicts.length}</span>
            <span className="text-gray-400 ml-1 text-sm">待处理</span>
          </div>
          <button onClick={fetchConflicts} className="btn-ghost px-4 py-2 flex items-center gap-2">
            <Clock className="w-4 h-4" />
            刷新
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-white flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-amber-400" />
            待处理冲突 ({pendingConflicts.length})
          </h2>
          
          {pendingConflicts.length === 0 ? (
            <div className="glass-card-strong rounded-2xl p-8 text-center">
              <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-green-500/10 flex items-center justify-center">
                <Check className="w-8 h-8 text-green-400" />
              </div>
              <p className="text-gray-400">暂无待处理冲突</p>
            </div>
          ) : (
            <div className="space-y-3">
              {pendingConflicts.map((conflict) => {
                const typeInfo = getConflictTypeLabel(conflict.conflictType || '');
                const statusBadge = getStatusBadge(conflict.status);
                return (
                  <div
                    key={conflict.id}
                    onClick={() => handleSelectConflict(conflict)}
                    className={`p-4 rounded-xl cursor-pointer transition-all ${
                      selectedConflict?.id === conflict.id
                        ? 'bg-white/10 border border-white/20'
                        : 'glass-card hover:bg-white/5 border border-transparent'
                    }`}
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <div className="font-medium text-white">{conflict.targetPath}</div>
                        <div className="text-xs text-gray-500 mt-0.5">
                          {new Date(conflict.createdAt).toLocaleString()}
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`text-xs ${typeInfo.color}`}>{typeInfo.label}</span>
                        <span className={`px-2 py-0.5 text-xs rounded-full ${statusBadge.color}`}>
                          {statusBadge.label}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {resolvedConflicts.length > 0 && (
            <>
              <h2 className="text-lg font-semibold text-white flex items-center gap-2 mt-6">
                <Check className="w-5 h-5 text-green-400" />
                已处理记录
              </h2>
              <div className="space-y-2 opacity-60">
                {resolvedConflicts.slice(0, 5).map((conflict) => {
                  const statusBadge = getStatusBadge(conflict.status);
                  return (
                    <div key={conflict.id} className="p-3 rounded-xl glass-card">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-300">{conflict.targetPath}</span>
                        <span className={`px-2 py-0.5 text-xs rounded-full ${statusBadge.color}`}>
                          {statusBadge.label}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </>
          )}
        </div>

        <div>
          {!selectedConflict ? (
            <div className="glass-card-strong rounded-2xl p-12 text-center h-full flex flex-col items-center justify-center">
              <div className="w-20 h-20 mb-6 rounded-full bg-white/5 flex items-center justify-center">
                <ArrowRightLeft className="w-10 h-10 text-gray-500" />
              </div>
              <h3 className="text-white font-semibold text-lg mb-2">选择冲突进行处理</h3>
              <p className="text-gray-400 text-sm">从左侧列表选择一个冲突，查看详情并选择处理策略</p>
            </div>
          ) : (
            <div className="glass-card-strong rounded-2xl overflow-hidden">
              <div className="p-5 border-b border-white/10">
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-semibold text-white">冲突详情</h3>
                  <button onClick={() => setSelectedConflict(null)} className="p-1.5 hover:bg-white/10 rounded-lg">
                    <X className="w-4 h-4 text-gray-400" />
                  </button>
                </div>
                <p className="text-sm text-gray-400 break-all">{selectedConflict.targetPath}</p>
              </div>

              <div className="p-5 space-y-4 max-h-[500px] overflow-y-auto">
                <div className="flex gap-2">
                  <button
                    onClick={() => setShowDiff(true)}
                    className={`px-3 py-1.5 rounded-lg text-sm ${
                      showDiff ? 'bg-white/10 text-white' : 'text-gray-400 hover:text-white'
                    }`}
                  >
                    差异对比
                  </button>
                  <button
                    onClick={() => setShowDiff(false)}
                    className={`px-3 py-1.5 rounded-lg text-sm ${
                      !showDiff ? 'bg-white/10 text-white' : 'text-gray-400 hover:text-white'
                    }`}
                  >
                    并排查看
                  </button>
                </div>

                {showDiff ? (
                  <div className="bg-[#0a0f1a] rounded-xl p-4 overflow-x-auto">
                    <pre className="text-xs font-mono whitespace-pre-wrap text-gray-300">
                      {selectedConflict.diffResult || '（无差异信息）'}
                    </pre>
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <div className="text-sm text-amber-400 mb-2 flex items-center gap-1">
                        <FileDown className="w-4 h-4" />
                        本地内容
                      </div>
                      <div className="bg-[#0a0f1a] rounded-xl p-4 h-48 overflow-y-auto">
                        <pre className="text-xs font-mono whitespace-pre-wrap text-gray-300">
                          {selectedConflict.localContent || '（空）'}
                        </pre>
                      </div>
                    </div>
                    <div>
                      <div className="text-sm text-cyan-400 mb-2 flex items-center gap-1">
                        <Eye className="w-4 h-4" />
                        远程模板
                      </div>
                      <div className="bg-[#0a0f1a] rounded-xl p-4 h-48 overflow-y-auto">
                        <pre className="text-xs font-mono whitespace-pre-wrap text-gray-300">
                          {selectedConflict.remoteContent || '（空）'}
                        </pre>
                      </div>
                    </div>
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-3">选择处理策略</label>
                  <div className="grid grid-cols-2 gap-2">
                    {STRATEGY_OPTIONS.map((strategy) => (
                      <button
                        key={strategy.code}
                        onClick={() => setSelectedStrategy(strategy.code)}
                        className={`p-3 rounded-xl text-left transition-all ${
                          selectedStrategy === strategy.code
                            ? 'bg-white/10 border border-white/30'
                            : 'bg-white/5 border border-transparent hover:bg-white/10'
                        }`}
                      >
                        <div className="font-medium text-white text-sm">{strategy.label}</div>
                        <div className="text-xs text-gray-500 mt-1">{strategy.description}</div>
                      </button>
                    ))}
                  </div>
                </div>

                {selectedStrategy === 'merge' && (
                  <div className="w-full">
                    <label className="block text-sm font-medium text-gray-300 mb-2">合并结果（可编辑）</label>
                    <textarea
                      value={mergedContent}
                      onChange={(e) => setMergedContent(e.target.value)}
                      className="input-field w-full min-h-[150px] font-mono text-sm resize-y"
                      placeholder="编辑合并后的内容..."
                    />
                  </div>
                )}

                {selectedStrategy === 'rename' && (
                  <div className="w-full">
                    <label className="block text-sm font-medium text-gray-300 mb-2">文件后缀</label>
                    <input
                      type="text"
                      value={renameSuffix}
                      onChange={(e) => setRenameSuffix(e.target.value)}
                      className="input-field w-full"
                      placeholder="如：.new 或 .20250101"
                    />
                    <p className="text-xs text-gray-500 mt-1">
                      新文件名将为: {selectedConflict.targetPath.replace(/(\.[^.]+)$/, renameSuffix + '$1')}
                    </p>
                  </div>
                )}
              </div>

              <div className="p-5 border-t border-white/10 flex justify-end gap-3">
                <button onClick={() => setSelectedConflict(null)} className="btn-ghost px-5 py-2.5">
                  取消
                </button>
                <button
                  onClick={handleResolve}
                  disabled={!selectedStrategy || resolving}
                  className="btn-primary px-5 py-2.5 disabled:opacity-50 flex items-center gap-2"
                >
                  {resolving ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      处理中...
                    </>
                  ) : (
                    <>
                      <Check className="w-4 h-4" />
                      确认处理
                    </>
                  )}
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
