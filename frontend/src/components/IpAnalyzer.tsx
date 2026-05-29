import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Globe,
  ArrowLeft,
  Loader2,
  AlertCircle,
  Wifi,
  WifiOff,
  Network,
  Search,
  Activity,
  ChevronDown,
  ChevronUp,
  MapPin,
  Shield,
  Zap,
  Radio,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Copy,
} from 'lucide-react';
import { toolsApi } from '@/api';
import type {
  CurrentIpInfo,
  PingResult,
  TracerouteResult,
  DnsResult,
  LanScanResult,
  ConnectivityAnalysis,
} from '@/types';

type AnalysisTab = 'current' | 'ping' | 'traceroute' | 'dns' | 'lan' | 'connectivity';

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).catch(() => {});
}

function CurrentIpPanel({ ipInfo }: { ipInfo: CurrentIpInfo }) {
  const [expandedIfaces, setExpandedIfaces] = useState<Set<string>>(new Set());

  const toggleIface = (name: string) => {
    setExpandedIfaces(prev => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  };

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">公网IP</p>
          <div className="flex items-center gap-2">
            <p className="text-white font-bold text-lg">{ipInfo.publicIp || '获取失败'}</p>
            {ipInfo.publicIp && (
              <button onClick={() => copyToClipboard(ipInfo.publicIp)} className="text-gray-500 hover:text-cyan-400 transition-colors">
                <Copy className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
          <p className="text-gray-500 text-xs mt-1">{ipInfo.publicIpSource}</p>
        </div>
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">主机名</p>
          <p className="text-white font-bold text-lg">{ipInfo.hostname}</p>
        </div>
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">默认网关</p>
          <p className="text-cyan-400 font-bold text-lg">{ipInfo.defaultGateway || '未检测到'}</p>
        </div>
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">DNS服务器</p>
          <p className="text-purple-400 font-bold text-lg">{ipInfo.dnsServer || '未检测到'}</p>
        </div>
      </div>

      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-5 flex items-center gap-2">
          <Network className="w-5 h-5 text-cyan-400" />
          网络接口
          <span className="text-sm font-normal text-gray-400 ml-2">({ipInfo.networkInterfaces.length} 个)</span>
        </h2>
        <div className="space-y-3">
          {ipInfo.networkInterfaces.map(iface => {
            const expanded = expandedIfaces.has(iface.name);
            return (
              <div key={iface.name} className="bg-white/5 rounded-2xl overflow-hidden">
                <button
                  onClick={() => toggleIface(iface.name)}
                  className="w-full p-4 flex items-center gap-3 text-left hover:bg-white/5 transition-colors"
                >
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan-500/20 to-blue-600/20 flex items-center justify-center shrink-0">
                    <Wifi className="w-5 h-5 text-cyan-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-white font-medium text-sm">{iface.displayName}</p>
                      {iface.ipv4Address && (
                        <span className="px-2 py-0.5 text-xs rounded-full bg-cyan-500/20 text-cyan-300 font-mono">
                          {iface.ipv4Address}
                        </span>
                      )}
                    </div>
                    <p className="text-gray-500 text-xs">{iface.name}</p>
                  </div>
                  {expanded ? (
                    <ChevronUp className="w-4 h-4 text-gray-400 shrink-0" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-gray-400 shrink-0" />
                  )}
                </button>
                {expanded && (
                  <div className="px-4 pb-4 animate-fadeIn">
                    <div className="grid grid-cols-2 gap-3">
                      <div className="bg-white/5 rounded-xl p-3">
                        <p className="text-gray-400 text-xs">IPv4</p>
                        <p className="text-white font-bold font-mono text-sm">{iface.ipv4Address || '-'}</p>
                      </div>
                      <div className="bg-white/5 rounded-xl p-3">
                        <p className="text-gray-400 text-xs">子网掩码</p>
                        <p className="text-white font-bold font-mono text-sm">{iface.subnetMask || '-'}</p>
                      </div>
                      <div className="bg-white/5 rounded-xl p-3">
                        <p className="text-gray-400 text-xs">MAC地址</p>
                        <p className="text-white font-bold font-mono text-sm">{iface.macAddress || '-'}</p>
                      </div>
                      <div className="bg-white/5 rounded-xl p-3">
                        <p className="text-gray-400 text-xs">MTU</p>
                        <p className="text-white font-bold text-sm">{iface.mtu || '-'}</p>
                      </div>
                      {iface.ipv6Address && (
                        <div className="bg-white/5 rounded-xl p-3 col-span-2">
                          <p className="text-gray-400 text-xs">IPv6</p>
                          <p className="text-white font-bold font-mono text-sm break-all">{iface.ipv6Address}</p>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function PingPanel({ pingResult, loading, onPing, target, setTarget }: {
  pingResult: PingResult | null;
  loading: boolean;
  onPing: () => void;
  target: string;
  setTarget: (v: string) => void;
}) {
  return (
    <div className="space-y-6">
      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <Activity className="w-5 h-5 text-cyan-400" />
          Ping 测试
        </h2>
        <div className="flex gap-3">
          <input
            type="text"
            value={target}
            onChange={e => setTarget(e.target.value)}
            placeholder="输入IP地址或域名，如 8.8.8.8 或 baidu.com"
            className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
            onKeyDown={e => e.key === 'Enter' && !loading && target.trim() && onPing()}
          />
          <button
            onClick={onPing}
            disabled={loading || !target.trim()}
            className="btn-primary flex items-center justify-center gap-2 shrink-0"
          >
            {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Activity className="w-5 h-5" />}
            Ping
          </button>
        </div>
      </div>

      {pingResult && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">目标地址</p>
              <p className="text-white font-bold font-mono">{pingResult.ipAddress || pingResult.target}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">可达性</p>
              <p className={`font-bold text-lg flex items-center gap-1 ${pingResult.reachable ? 'text-emerald-400' : 'text-red-400'}`}>
                {pingResult.reachable ? <CheckCircle2 className="w-5 h-5" /> : <XCircle className="w-5 h-5" />}
                {pingResult.reachable ? '可达' : '不可达'}
              </p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">平均延迟</p>
              <p className={`${pingResult.pingTimeMs > 100 ? 'text-amber-400' : 'text-cyan-400'} font-bold text-lg`}>
                {pingResult.pingTimeMs}ms
              </p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">丢包率</p>
              <p className={`${pingResult.packetLossRate > 0 ? 'text-amber-400' : 'text-emerald-400'} font-bold text-lg`}>
                {pingResult.packetLossRate.toFixed(1)}%
              </p>
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h3 className="text-lg font-bold text-white mb-4">Ping 详情</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
                <span className="text-gray-400 text-sm">发送包数</span>
                <span className="text-white font-bold">{pingResult.packetsSent}</span>
              </div>
              <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
                <span className="text-gray-400 text-sm">接收包数</span>
                <span className="text-white font-bold">{pingResult.packetsReceived}</span>
              </div>
              <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
                <span className="text-gray-400 text-sm">TTL</span>
                <span className="text-white font-bold">{pingResult.ttl ?? 'N/A'}</span>
              </div>
              {pingResult.errorMessage && (
                <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-xl">
                  <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                  <p className="text-red-300 text-sm">{pingResult.errorMessage}</p>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function TraceroutePanel({ traceResult, loading, onTrace, target, setTarget }: {
  traceResult: TracerouteResult | null;
  loading: boolean;
  onTrace: () => void;
  target: string;
  setTarget: (v: string) => void;
}) {
  const getLatencyColor = (latency: number) => {
    if (latency < 0) return 'text-gray-500';
    if (latency < 10) return 'text-emerald-400';
    if (latency < 50) return 'text-cyan-400';
    if (latency < 100) return 'text-amber-400';
    return 'text-red-400';
  };

  return (
    <div className="space-y-6">
      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <MapPin className="w-5 h-5 text-cyan-400" />
          路由追踪
        </h2>
        <div className="flex gap-3">
          <input
            type="text"
            value={target}
            onChange={e => setTarget(e.target.value)}
            placeholder="输入目标IP或域名，如 8.8.8.8"
            className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
            onKeyDown={e => e.key === 'Enter' && !loading && target.trim() && onTrace()}
          />
          <button
            onClick={onTrace}
            disabled={loading || !target.trim()}
            className="btn-primary flex items-center justify-center gap-2 shrink-0"
          >
            {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <MapPin className="w-5 h-5" />}
            追踪
          </button>
        </div>
        <p className="text-gray-500 text-xs mt-2">路由追踪可能需要较长时间（最长约2分钟），请耐心等待</p>
      </div>

      {loading && (
        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-cyan-500/20 to-blue-600/20 flex items-center justify-center mx-auto mb-4">
            <Loader2 className="w-10 h-10 text-cyan-400 animate-spin" />
          </div>
          <h3 className="text-xl font-semibold text-white mb-2">正在追踪路由...</h3>
          <p className="text-gray-400 text-sm">正在逐跳探测到目标的网络路径，请耐心等待</p>
          <div className="mt-4 max-w-xs mx-auto">
            <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
              <div className="h-full rounded-full bg-gradient-to-r from-cyan-500 to-blue-600 animate-shimmer" style={{ width: '60%' }} />
            </div>
          </div>
        </div>
      )}

      {traceResult && !loading && (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">目标地址</p>
              <p className="text-white font-bold font-mono">{traceResult.targetIp || traceResult.target}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">总跳数</p>
              <p className="text-cyan-400 font-bold text-lg">{traceResult.totalHops}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">是否到达</p>
              <p className={`font-bold text-lg flex items-center gap-1 ${traceResult.reachedTarget ? 'text-emerald-400' : 'text-red-400'}`}>
                {traceResult.reachedTarget ? <CheckCircle2 className="w-5 h-5" /> : <XCircle className="w-5 h-5" />}
                {traceResult.reachedTarget ? '是' : '否'}
              </p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">耗时</p>
              <p className="text-purple-400 font-bold text-lg">{(traceResult.durationMs / 1000).toFixed(1)}s</p>
            </div>
          </div>

          {!traceResult.reachedTarget && traceResult.blockHop && (
            <div className="glass-card rounded-2xl p-5 border border-red-500/30 bg-gradient-to-r from-red-500/10 to-orange-500/10">
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-xl bg-red-500/20 flex items-center justify-center shrink-0">
                  <AlertTriangle className="w-5 h-5 text-red-400" />
                </div>
                <div>
                  <h3 className="text-red-300 font-bold mb-1">路由中断点</h3>
                  <p className="text-gray-300 text-sm">
                    在第 <span className="text-red-400 font-bold">{traceResult.blockHop}</span> 跳检测到网络中断
                    {traceResult.blockIp && <span>，阻断IP: <span className="font-mono text-amber-400">{traceResult.blockIp}</span></span>}
                  </p>
                  {traceResult.blockAnalysis && (
                    <p className="text-gray-400 text-sm mt-2">{traceResult.blockAnalysis}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          <div className="glass-card rounded-3xl p-6">
            <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
              <Radio className="w-5 h-5 text-cyan-400" />
              路由路径
            </h3>
            <div className="relative">
              <div className="absolute left-5 top-0 bottom-0 w-0.5 bg-white/10" />
              <div className="space-y-0">
                {traceResult.hops.map((hop, idx) => {
                  const isBlock = traceResult.blockHop === hop.hop;
                  const avgLatency = hop.timeout ? -1 : Math.round((Math.max(hop.latency1, 0) + Math.max(hop.latency2, 0) + Math.max(hop.latency3, 0)) / 3);
                  return (
                    <div key={hop.hop} className={`relative flex items-center gap-4 p-3 rounded-xl ${isBlock ? 'bg-red-500/10' : idx % 2 === 0 ? 'bg-white/[0.02]' : ''}`}>
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 z-10 ${
                        hop.timeout
                          ? 'bg-gray-600/30'
                          : isBlock
                          ? 'bg-red-500/30'
                          : 'bg-cyan-500/20'
                      }`}>
                        <span className={`text-sm font-bold ${hop.timeout ? 'text-gray-500' : isBlock ? 'text-red-400' : 'text-cyan-400'}`}>
                          {hop.hop}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <p className={`font-mono text-sm ${hop.timeout ? 'text-gray-500' : 'text-white'}`}>
                            {hop.timeout ? '* * * 请求超时' : hop.ip || hop.host}
                          </p>
                          {isBlock && (
                            <span className="px-2 py-0.5 text-xs rounded-full bg-red-500/20 text-red-400">中断</span>
                          )}
                        </div>
                        {!hop.timeout && hop.host && hop.ip && hop.host !== hop.ip && hop.host !== '*' && (
                          <p className="text-gray-500 text-xs truncate">{hop.host}</p>
                        )}
                      </div>
                      <div className="text-right shrink-0">
                        {!hop.timeout ? (
                          <div className="space-y-0.5">
                            <p className={`text-sm font-mono ${getLatencyColor(hop.latency1)}`}>{hop.latency1 < 0 ? '*' : `${hop.latency1}ms`}</p>
                            <p className={`text-xs font-mono ${getLatencyColor(avgLatency)}`}>avg: {avgLatency}ms</p>
                          </div>
                        ) : (
                          <p className="text-gray-600 text-sm">超时</p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function DnsPanel({ dnsResult, loading, onResolve, domain, setDomain }: {
  dnsResult: DnsResult | null;
  loading: boolean;
  onResolve: () => void;
  domain: string;
  setDomain: (v: string) => void;
}) {
  return (
    <div className="space-y-6">
      <div className="glass-card rounded-3xl p-6">
        <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
          <Search className="w-5 h-5 text-cyan-400" />
          DNS 解析
        </h2>
        <div className="flex gap-3">
          <input
            type="text"
            value={domain}
            onChange={e => setDomain(e.target.value)}
            placeholder="输入域名，如 baidu.com"
            className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
            onKeyDown={e => e.key === 'Enter' && !loading && domain.trim() && onResolve()}
          />
          <button
            onClick={onResolve}
            disabled={loading || !domain.trim()}
            className="btn-primary flex items-center justify-center gap-2 shrink-0"
          >
            {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Search className="w-5 h-5" />}
            解析
          </button>
        </div>
      </div>

      {dnsResult && (
        <div className="glass-card rounded-3xl p-6">
          <div className="space-y-3">
            <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
              <span className="text-gray-400 text-sm">域名</span>
              <span className="text-white font-bold font-mono">{dnsResult.domain}</span>
            </div>
            <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
              <span className="text-gray-400 text-sm">解析结果</span>
              <div className="flex items-center gap-2">
                <span className={`font-bold font-mono ${dnsResult.success ? 'text-emerald-400' : 'text-red-400'}`}>
                  {dnsResult.resolvedIp || '解析失败'}
                </span>
                {dnsResult.success && dnsResult.resolvedIp && (
                  <button onClick={() => copyToClipboard(dnsResult.resolvedIp!)} className="text-gray-500 hover:text-cyan-400 transition-colors">
                    <Copy className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>
            <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
              <span className="text-gray-400 text-sm">DNS服务器</span>
              <span className="text-purple-400 font-bold font-mono">{dnsResult.dnsServer || '默认'}</span>
            </div>
            <div className="flex items-center justify-between bg-white/5 rounded-xl p-3">
              <span className="text-gray-400 text-sm">查询耗时</span>
              <span className="text-cyan-400 font-bold">{dnsResult.queryTimeMs}ms</span>
            </div>
            {dnsResult.errorMessage && (
              <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-xl">
                <AlertCircle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                <p className="text-red-300 text-sm">{dnsResult.errorMessage}</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function LanScanPanel({ lanResult, loading, onScan }: {
  lanResult: LanScanResult | null;
  loading: boolean;
  onScan: () => void;
}) {
  return (
    <div className="space-y-6">
      <div className="glass-card rounded-3xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Radio className="w-5 h-5 text-cyan-400" />
            局域网扫描
          </h2>
          <button
            onClick={onScan}
            disabled={loading}
            className="btn-primary flex items-center justify-center gap-2"
          >
            {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
            扫描
          </button>
        </div>
        <p className="text-gray-400 text-sm">通过ARP缓存表扫描局域网内已发现的设备</p>
      </div>

      {lanResult && (
        <>
          <div className="grid grid-cols-3 gap-4">
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">子网</p>
              <p className="text-cyan-400 font-bold font-mono">{lanResult.subnet || '未知'}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">发现设备</p>
              <p className="text-amber-400 font-bold text-lg">{lanResult.totalDevices}</p>
            </div>
            <div className="stat-card">
              <p className="text-gray-400 text-xs mb-1">扫描耗时</p>
              <p className="text-purple-400 font-bold text-lg">{(lanResult.scanDurationMs / 1000).toFixed(1)}s</p>
            </div>
          </div>

          <div className="glass-card rounded-3xl p-6">
            <h3 className="text-lg font-bold text-white mb-4">ARP 表</h3>
            {lanResult.arpEntries.length === 0 ? (
              <div className="text-center py-8">
                <WifiOff className="w-12 h-12 text-gray-600 mx-auto mb-3" />
                <p className="text-gray-400">未发现局域网设备</p>
                <p className="text-gray-500 text-sm mt-1">可能需要先与其他设备通信以填充ARP缓存</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/10">
                      <th className="text-left text-gray-400 text-xs py-3 px-3">IP 地址</th>
                      <th className="text-left text-gray-400 text-xs py-3 px-3">MAC 地址</th>
                      <th className="text-left text-gray-400 text-xs py-3 px-3">类型</th>
                      <th className="text-left text-gray-400 text-xs py-3 px-3">接口</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lanResult.arpEntries.map((entry, idx) => (
                      <tr key={idx} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                        <td className="py-3 px-3 text-white font-mono text-sm">{entry.ipAddress}</td>
                        <td className="py-3 px-3 text-cyan-400 font-mono text-sm">{entry.macAddress}</td>
                        <td className="py-3 px-3">
                          <span className={`px-2 py-0.5 text-xs rounded-full ${
                            entry.type === '动态' ? 'bg-cyan-500/20 text-cyan-300' : 'bg-purple-500/20 text-purple-300'
                          }`}>
                            {entry.type}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-gray-400 text-sm">{entry.interfaceName || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function ConnectivityPanel({ analysis, loading, onAnalyze, target, setTarget }: {
  analysis: ConnectivityAnalysis | null;
  loading: boolean;
  onAnalyze: () => void;
  target: string;
  setTarget: (v: string) => void;
}) {
  if (!analysis) {
    return (
      <div className="space-y-6">
        <div className="glass-card rounded-3xl p-6">
          <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
            <Zap className="w-5 h-5 text-cyan-400" />
            连通性分析
          </h2>
          <p className="text-gray-400 text-sm mb-4">
            一键分析当前网络到目标地址的完整连通性，包括DNS解析、Ping测试、路由追踪，并自动定位卡点
          </p>
          <div className="flex gap-3">
            <input
              type="text"
              value={target}
              onChange={e => setTarget(e.target.value)}
              placeholder="输入目标IP或域名，如 google.com 或 192.168.1.1"
              className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
              onKeyDown={e => e.key === 'Enter' && !loading && target.trim() && onAnalyze()}
            />
            <button
              onClick={onAnalyze}
              disabled={loading || !target.trim()}
              className="btn-primary flex items-center justify-center gap-2 shrink-0"
            >
              {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Zap className="w-5 h-5" />}
              分析
            </button>
          </div>
        </div>
      </div>
    );
  }

  const statusConfig: Record<string, { color: string; bgColor: string; icon: any }> = {
    '连接正常': { color: 'text-emerald-400', bgColor: 'bg-emerald-500/20', icon: CheckCircle2 },
    'DNS解析失败': { color: 'text-red-400', bgColor: 'bg-red-500/20', icon: XCircle },
    '目标不可达': { color: 'text-red-400', bgColor: 'bg-red-500/20', icon: XCircle },
  };

  const getStatusStyle = (status: string) => {
    if (status.startsWith('连接中断')) return { color: 'text-red-400', bgColor: 'bg-red-500/20', icon: AlertTriangle };
    if (status.startsWith('连接不稳定')) return { color: 'text-amber-400', bgColor: 'bg-amber-500/20', icon: AlertTriangle };
    return statusConfig[status] || { color: 'text-gray-400', bgColor: 'bg-gray-500/20', icon: AlertCircle };
  };

  const statusStyle = getStatusStyle(analysis.overallStatus);
  const StatusIcon = statusStyle.icon;

  return (
    <div className="space-y-6">
      <div className="glass-card rounded-3xl p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Zap className="w-5 h-5 text-cyan-400" />
            连通性分析
          </h2>
          <div className="flex items-center gap-2">
            <input
              type="text"
              value={target}
              onChange={e => setTarget(e.target.value)}
              placeholder="目标地址"
              className="w-60 bg-white/5 border border-white/10 rounded-xl px-3 py-2 text-white text-sm placeholder-gray-500 focus:outline-none focus:border-cyan-400/50 transition-colors"
              onKeyDown={e => e.key === 'Enter' && !loading && target.trim() && onAnalyze()}
            />
            <button
              onClick={onAnalyze}
              disabled={loading || !target.trim()}
              className="btn-secondary flex items-center gap-2 text-sm !px-3 !py-2"
            >
              {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
              重新分析
            </button>
          </div>
        </div>
      </div>

      <div className={`glass-card rounded-2xl p-5 border ${
        analysis.overallStatus === '连接正常'
          ? 'border-emerald-500/30 bg-gradient-to-r from-emerald-500/10 to-cyan-500/10'
          : analysis.overallStatus.includes('不稳定')
          ? 'border-amber-500/30 bg-gradient-to-r from-amber-500/10 to-orange-500/10'
          : 'border-red-500/30 bg-gradient-to-r from-red-500/10 to-orange-500/10'
      }`}>
        <div className="flex items-start gap-4">
          <div className={`w-14 h-14 rounded-2xl ${statusStyle.bgColor} flex items-center justify-center shrink-0`}>
            <StatusIcon className={`w-7 h-7 ${statusStyle.color}`} />
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <h2 className="text-xl font-bold text-white">{analysis.overallStatus}</h2>
              <span className={`px-2 py-0.5 text-xs rounded-full ${analysis.isLan ? 'bg-blue-500/20 text-blue-300' : 'bg-purple-500/20 text-purple-300'}`}>
                {analysis.isLan ? '局域网' : '互联网'}
              </span>
            </div>
            <p className="text-gray-300 text-sm leading-relaxed">{analysis.diagnosis}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">源IP</p>
          <p className="text-white font-bold font-mono text-sm">{analysis.sourceIp}</p>
        </div>
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">目标IP</p>
          <p className="text-white font-bold font-mono text-sm">{analysis.targetIp}</p>
        </div>
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">网络类型</p>
          <p className={`font-bold ${analysis.isLan ? 'text-blue-400' : 'text-purple-400'}`}>
            {analysis.isLan ? '局域网' : '互联网'}
          </p>
        </div>
        <div className="stat-card">
          <p className="text-gray-400 text-xs mb-1">Ping状态</p>
          <p className={`font-bold flex items-center gap-1 ${analysis.pingable ? 'text-emerald-400' : 'text-red-400'}`}>
            {analysis.pingable ? <CheckCircle2 className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
            {analysis.pingable ? '可达' : '不可达'}
          </p>
        </div>
      </div>

      {analysis.dnsResult && (
        <div className="glass-card rounded-3xl p-6">
          <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <Search className="w-5 h-5 text-purple-400" />
            DNS 解析结果
          </h3>
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-gray-400 text-xs">解析状态</p>
              <p className={`font-bold ${analysis.dnsResult.success ? 'text-emerald-400' : 'text-red-400'}`}>
                {analysis.dnsResult.success ? '成功' : '失败'}
              </p>
            </div>
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-gray-400 text-xs">解析IP</p>
              <p className="text-white font-bold font-mono">{analysis.dnsResult.resolvedIp || '-'}</p>
            </div>
          </div>
        </div>
      )}

      {analysis.pingResult && (
        <div className="glass-card rounded-3xl p-6">
          <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <Activity className="w-5 h-5 text-cyan-400" />
            Ping 测试结果
          </h3>
          <div className="grid grid-cols-4 gap-3">
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-gray-400 text-xs">可达性</p>
              <p className={`font-bold ${analysis.pingResult.reachable ? 'text-emerald-400' : 'text-red-400'}`}>
                {analysis.pingResult.reachable ? '可达' : '不可达'}
              </p>
            </div>
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-gray-400 text-xs">平均延迟</p>
              <p className={`font-bold ${analysis.pingResult.pingTimeMs > 100 ? 'text-amber-400' : 'text-cyan-400'}`}>
                {analysis.pingResult.pingTimeMs}ms
              </p>
            </div>
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-gray-400 text-xs">丢包率</p>
              <p className={`font-bold ${analysis.pingResult.packetLossRate > 0 ? 'text-amber-400' : 'text-emerald-400'}`}>
                {analysis.pingResult.packetLossRate.toFixed(1)}%
              </p>
            </div>
            <div className="bg-white/5 rounded-xl p-3">
              <p className="text-gray-400 text-xs">TTL</p>
              <p className="text-white font-bold">{analysis.pingResult.ttl ?? 'N/A'}</p>
            </div>
          </div>
        </div>
      )}

      {analysis.tracerouteResult && (
        <div className="glass-card rounded-3xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <MapPin className="w-5 h-5 text-amber-400" />
              路由追踪结果
            </h3>
            <span className="text-gray-400 text-sm">{analysis.tracerouteResult.totalHops} 跳</span>
          </div>
          {!analysis.tracerouteResult.reachedTarget && analysis.tracerouteResult.blockHop && (
            <div className="mb-4 flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-xl">
              <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
              <div>
                <p className="text-red-300 text-sm">
                  卡点：第 <span className="font-bold">{analysis.tracerouteResult.blockHop}</span> 跳
                  {analysis.tracerouteResult.blockIp && <span> (IP: <span className="font-mono text-amber-400">{analysis.tracerouteResult.blockIp}</span>)</span>}
                </p>
                {analysis.tracerouteResult.blockAnalysis && (
                  <p className="text-gray-400 text-xs mt-1">{analysis.tracerouteResult.blockAnalysis}</p>
                )}
              </div>
            </div>
          )}
          <div className="space-y-1 max-h-64 overflow-y-auto">
            {analysis.tracerouteResult.hops.map(hop => (
              <div key={hop.hop} className={`flex items-center gap-3 py-2 px-3 rounded-lg text-sm ${
                analysis.tracerouteResult!.blockHop === hop.hop ? 'bg-red-500/10' : 'hover:bg-white/5'
              }`}>
                <span className="text-gray-500 w-6 text-right">{hop.hop}</span>
                <span className={`font-mono flex-1 ${hop.timeout ? 'text-gray-600' : 'text-white'}`}>
                  {hop.timeout ? '* * * 超时' : hop.ip || hop.host}
                </span>
                <span className="text-cyan-400 font-mono w-16 text-right">
                  {hop.timeout ? '-' : `${Math.round((Math.max(hop.latency1, 0) + Math.max(hop.latency2, 0) + Math.max(hop.latency3, 0)) / 3)}ms`}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {analysis.suggestions && analysis.suggestions.length > 0 && (
        <div className="glass-card rounded-3xl p-6">
          <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <Shield className="w-5 h-5 text-amber-400" />
            诊断建议
          </h3>
          <div className="space-y-2">
            {analysis.suggestions.map((s, idx) => (
              <div key={idx} className="flex items-start gap-3 p-3 bg-white/5 rounded-xl">
                <span className="w-6 h-6 rounded-full bg-cyan-500/20 flex items-center justify-center shrink-0 text-xs font-bold text-cyan-400">
                  {idx + 1}
                </span>
                <p className="text-gray-300 text-sm">{s}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

const tabs: { id: AnalysisTab; label: string; icon: any }[] = [
  { id: 'current', label: '当前IP', icon: Globe },
  { id: 'connectivity', label: '连通性分析', icon: Zap },
  { id: 'ping', label: 'Ping', icon: Activity },
  { id: 'traceroute', label: '路由追踪', icon: MapPin },
  { id: 'dns', label: 'DNS解析', icon: Search },
  { id: 'lan', label: '局域网扫描', icon: Radio },
];

export default function IpAnalyzer() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<AnalysisTab>('current');
  const [error, setError] = useState('');

  const [ipInfo, setIpInfo] = useState<CurrentIpInfo | null>(null);
  const [loadingIp, setLoadingIp] = useState(false);

  const [pingTarget, setPingTarget] = useState('');
  const [pingResult, setPingResult] = useState<PingResult | null>(null);
  const [loadingPing, setLoadingPing] = useState(false);

  const [traceTarget, setTraceTarget] = useState('');
  const [traceResult, setTraceResult] = useState<TracerouteResult | null>(null);
  const [loadingTrace, setLoadingTrace] = useState(false);

  const [dnsDomain, setDnsDomain] = useState('');
  const [dnsResult, setDnsResult] = useState<DnsResult | null>(null);
  const [loadingDns, setLoadingDns] = useState(false);

  const [lanResult, setLanResult] = useState<LanScanResult | null>(null);
  const [loadingLan, setLoadingLan] = useState(false);

  const [connTarget, setConnTarget] = useState('');
  const [connAnalysis, setConnAnalysis] = useState<ConnectivityAnalysis | null>(null);
  const [loadingConn, setLoadingConn] = useState(false);

  useEffect(() => {
    fetchCurrentIp();
  }, []);

  const fetchCurrentIp = async () => {
    setLoadingIp(true);
    setError('');
    try {
      const res = await toolsApi.getCurrentIpInfo();
      if (res.code === 200 && res.data) {
        setIpInfo(res.data);
      } else {
        setError(res.message || '获取IP信息失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '获取IP信息失败');
    } finally {
      setLoadingIp(false);
    }
  };

  const handlePing = async () => {
    if (!pingTarget.trim()) return;
    setLoadingPing(true);
    setError('');
    setPingResult(null);
    try {
      const res = await toolsApi.ping(pingTarget.trim(), 4);
      if (res.code === 200 && res.data) {
        setPingResult(res.data);
      } else {
        setError(res.message || 'Ping失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ping失败');
    } finally {
      setLoadingPing(false);
    }
  };

  const handleTraceroute = async () => {
    if (!traceTarget.trim()) return;
    setLoadingTrace(true);
    setError('');
    setTraceResult(null);
    try {
      const res = await toolsApi.traceroute(traceTarget.trim());
      if (res.code === 200 && res.data) {
        setTraceResult(res.data);
      } else {
        setError(res.message || '路由追踪失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '路由追踪失败');
    } finally {
      setLoadingTrace(false);
    }
  };

  const handleDns = async () => {
    if (!dnsDomain.trim()) return;
    setLoadingDns(true);
    setError('');
    setDnsResult(null);
    try {
      const res = await toolsApi.resolveDns(dnsDomain.trim());
      if (res.code === 200 && res.data) {
        setDnsResult(res.data);
      } else {
        setError(res.message || 'DNS解析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'DNS解析失败');
    } finally {
      setLoadingDns(false);
    }
  };

  const handleLanScan = async () => {
    setLoadingLan(true);
    setError('');
    setLanResult(null);
    try {
      const res = await toolsApi.scanLan();
      if (res.code === 200 && res.data) {
        setLanResult(res.data);
      } else {
        setError(res.message || '局域网扫描失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '局域网扫描失败');
    } finally {
      setLoadingLan(false);
    }
  };

  const handleConnectivity = async () => {
    if (!connTarget.trim()) return;
    setLoadingConn(true);
    setError('');
    setConnAnalysis(null);
    try {
      const res = await toolsApi.analyzeConnectivity(connTarget.trim());
      if (res.code === 200 && res.data) {
        setConnAnalysis(res.data);
      } else {
        setError(res.message || '连通性分析失败');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '连通性分析失败');
    } finally {
      setLoadingConn(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/tools')}
          className="p-3 rounded-xl hover:bg-white/5 transition-all"
        >
          <ArrowLeft className="w-5 h-5 text-gray-400" />
        </button>
        <div>
          <h1 className="text-3xl font-bold text-white mb-1">IP 地址分析器</h1>
          <p className="text-gray-400">查看当前IP信息、路由追踪、连通性分析，定位网络卡点</p>
        </div>
      </div>

      {error && (
        <div className="glass-card rounded-2xl p-4 border border-red-500/30 bg-red-500/10">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0" />
            <p className="text-red-300 text-sm">{error}</p>
          </div>
        </div>
      )}

      <div className="glass-card rounded-2xl p-2">
        <div className="flex gap-1 overflow-x-auto">
          {tabs.map(tab => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium whitespace-nowrap transition-all ${
                  isActive
                    ? 'bg-gradient-to-r from-cyan-500/20 to-blue-600/20 text-cyan-400'
                    : 'text-gray-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Icon className="w-4 h-4" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {activeTab === 'current' && (
        loadingIp ? (
          <div className="glass-card rounded-3xl p-12 text-center">
            <Loader2 className="w-10 h-10 text-cyan-400 animate-spin mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-white mb-2">正在获取IP信息...</h3>
            <p className="text-gray-400 text-sm">正在收集网络接口和公网IP信息</p>
          </div>
        ) : ipInfo ? (
          <CurrentIpPanel ipInfo={ipInfo} />
        ) : (
          <div className="glass-card rounded-3xl p-12 text-center">
            <Globe className="w-12 h-12 text-gray-600 mx-auto mb-3" />
            <p className="text-gray-400">点击上方刷新按钮获取IP信息</p>
          </div>
        )
      )}

      {activeTab === 'ping' && (
        <PingPanel
          pingResult={pingResult}
          loading={loadingPing}
          onPing={handlePing}
          target={pingTarget}
          setTarget={setPingTarget}
        />
      )}

      {activeTab === 'traceroute' && (
        <TraceroutePanel
          traceResult={traceResult}
          loading={loadingTrace}
          onTrace={handleTraceroute}
          target={traceTarget}
          setTarget={setTraceTarget}
        />
      )}

      {activeTab === 'dns' && (
        <DnsPanel
          dnsResult={dnsResult}
          loading={loadingDns}
          onResolve={handleDns}
          domain={dnsDomain}
          setDomain={setDnsDomain}
        />
      )}

      {activeTab === 'lan' && (
        <LanScanPanel
          lanResult={lanResult}
          loading={loadingLan}
          onScan={handleLanScan}
        />
      )}

      {activeTab === 'connectivity' && (
        <ConnectivityPanel
          analysis={connAnalysis}
          loading={loadingConn}
          onAnalyze={handleConnectivity}
          target={connTarget}
          setTarget={setConnTarget}
        />
      )}
    </div>
  );
}
