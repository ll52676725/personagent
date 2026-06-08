import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container,
  ArrowLeft,
  Loader2,
  AlertCircle,
  FolderOpen,
  FileCode2,
  FileText,
  Play,
  CheckCircle2,
  XCircle,
  ChevronDown,
  ChevronUp,
  Copy,
  Check,
  Settings2,
  Download,
  RefreshCw,
} from 'lucide-react';
import { toolsApi } from '@/api';
import { DockerGenerateRequest, DockerGenerateResult, DockerDeployResult } from '@/types';

type Step = 'config' | 'generate' | 'deploy';

export default function DockerGenerator() {
  const navigate = useNavigate();

  const [currentStep, setCurrentStep] = useState<Step>('config');
  const [dockerAvailable, setDockerAvailable] = useState<boolean | null>(null);
  const [generating, setGenerating] = useState(false);
  const [deploying, setDeploying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [generateResult, setGenerateResult] = useState<DockerGenerateResult | null>(null);
  const [deployResult, setDeployResult] = useState<DockerDeployResult | null>(null);
  const [expandedSection, setExpandedSection] = useState<string>('dockerfile');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showAdvanced, setShowAdvanced] = useState(false);

  const [request, setRequest] = useState<DockerGenerateRequest>({
    projectPath: '',
    port: 8080,
    jdkVersion: '',
    buildTool: '',
    includeDockerCompose: true,
    jvmOpts: '',
    springProfile: '',
    imageName: '',
    imageTag: 'latest',
  });

  useEffect(() => {
    toolsApi.checkDocker().then(res => {
      if (res.code === 200) {
        setDockerAvailable(res.data);
      } else {
        setDockerAvailable(false);
      }
    }).catch(() => setDockerAvailable(false));
  }, []);

  const handleGenerate = async () => {
    if (!request.projectPath.trim()) {
      setError('请输入项目路径');
      return;
    }
    setGenerating(true);
    setError(null);
    try {
      const res = await toolsApi.generateDockerfile(request);
      if (res.code === 200 && res.data) {
        setGenerateResult(res.data);
        setCurrentStep('generate');
      } else {
        setError(res.message || '生成失败');
      }
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || '生成失败');
    } finally {
      setGenerating(false);
    }
  };

  const handleDeploy = async () => {
    setDeploying(true);
    setError(null);
    try {
      const res = await toolsApi.deployDocker(request);
      if (res.code === 200 && res.data) {
        setDeployResult(res.data);
        setCurrentStep('deploy');
      } else {
        setError(res.message || '部署失败');
      }
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || '部署失败');
    } finally {
      setDeploying(false);
    }
  };

  const handleCopy = (content: string, id: string) => {
    navigator.clipboard.writeText(content).then(() => {
      setCopiedId(id);
      setTimeout(() => setCopiedId(null), 2000);
    });
  };

  const handleDownload = (content: string, filename: string) => {
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const updateRequest = (field: keyof DockerGenerateRequest, value: any) => {
    setRequest(prev => ({ ...prev, [field]: value }));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4 mb-2">
        <button
          onClick={() => navigate('/tools')}
          className="p-2.5 rounded-xl glass-card hover:bg-white/10 transition-all group"
        >
          <ArrowLeft className="w-5 h-5 text-gray-400 group-hover:text-white transition-colors" />
        </button>
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full glass-card-strong mb-2">
            <Container className="w-4 h-4 text-cyan-400" />
            <span className="text-sm text-cyan-400 font-medium">Docker 容器化工具</span>
          </div>
          <h1 className="text-3xl font-bold text-white tracking-tight">
            Docker<span className="gradient-text-aurora"> 生成器</span>
          </h1>
          <p className="text-gray-400 mt-1">自动分析 Java 项目，生成 Dockerfile 并一键部署</p>
        </div>
      </div>

      {dockerAvailable === false && (
        <div className="glass-card rounded-2xl p-4 border-amber-500/30 bg-amber-500/5">
          <div className="flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-amber-400 font-medium">Docker 未检测到</p>
              <p className="text-gray-400 text-sm mt-1">
                未检测到 Docker 环境。您仍可生成 Dockerfile 等文件，但一键部署功能需要安装 Docker。
              </p>
            </div>
          </div>
        </div>
      )}

      <div className="flex gap-3">
        {(['config', 'generate', 'deploy'] as Step[]).map((step, index) => {
          const isActive = currentStep === step;
          const isCompleted = ['config', 'generate', 'deploy'].indexOf(currentStep) > index;
          const labels = ['项目配置', '生成文件', '一键部署'];
          const icons = [Settings2, FileCode2, Play];
          const Icon = icons[index];
          return (
            <button
              key={step}
              onClick={() => {
                if (isCompleted || (index === 0) || (index === 1 && generateResult)) {
                  setCurrentStep(step);
                }
              }}
              className={`flex items-center gap-2 px-4 py-2.5 rounded-xl transition-all ${
                isActive
                  ? 'bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/25'
                  : isCompleted
                  ? 'glass-card text-cyan-400'
                  : 'glass-card text-gray-500 cursor-not-allowed'
              }`}
            >
              {isCompleted ? (
                <CheckCircle2 className="w-4 h-4" />
              ) : (
                <Icon className="w-4 h-4" />
              )}
              <span className="text-sm font-medium">{labels[index]}</span>
            </button>
          );
        })}
      </div>

      {error && (
        <div className="glass-card rounded-2xl p-4 border-red-500/30 bg-red-500/5">
          <div className="flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-red-400 font-medium">操作失败</p>
              <p className="text-gray-400 text-sm mt-1">{error}</p>
            </div>
          </div>
        </div>
      )}

      {currentStep === 'config' && (
        <div className="glass-card rounded-3xl p-6 space-y-6">
          <div>
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <FolderOpen className="w-5 h-5 text-cyan-400" />
              项目信息
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">
                  项目路径 <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={request.projectPath}
                  onChange={e => updateRequest('projectPath', e.target.value)}
                  placeholder="例如: D:\projects\my-spring-boot-app"
                  className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                />
                <p className="text-gray-500 text-xs mt-1.5">Java 项目的根目录，包含 pom.xml 或 build.gradle</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">镜像名称</label>
                  <input
                    type="text"
                    value={request.imageName || ''}
                    onChange={e => updateRequest('imageName', e.target.value)}
                    placeholder="留空自动从项目名生成"
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">镜像标签</label>
                  <input
                    type="text"
                    value={request.imageTag || ''}
                    onChange={e => updateRequest('imageTag', e.target.value)}
                    placeholder="latest"
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">服务端口</label>
                  <input
                    type="number"
                    value={request.port || ''}
                    onChange={e => updateRequest('port', e.target.value ? parseInt(e.target.value) : undefined)}
                    placeholder="8080"
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">构建工具</label>
                  <select
                    value={request.buildTool || ''}
                    onChange={e => updateRequest('buildTool', e.target.value || undefined)}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                  >
                    <option value="" className="bg-gray-800">自动检测</option>
                    <option value="maven" className="bg-gray-800">Maven</option>
                    <option value="gradle" className="bg-gray-800">Gradle</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-300 mb-2">JDK 版本</label>
                  <select
                    value={request.jdkVersion || ''}
                    onChange={e => updateRequest('jdkVersion', e.target.value || undefined)}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                  >
                    <option value="" className="bg-gray-800">自动检测</option>
                    <option value="8" className="bg-gray-800">JDK 8</option>
                    <option value="11" className="bg-gray-800">JDK 11</option>
                    <option value="17" className="bg-gray-800">JDK 17</option>
                    <option value="21" className="bg-gray-800">JDK 21</option>
                  </select>
                </div>
              </div>
            </div>
          </div>

          <div>
            <button
              onClick={() => setShowAdvanced(!showAdvanced)}
              className="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors"
            >
              {showAdvanced ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
              高级配置
            </button>
            {showAdvanced && (
              <div className="mt-4 space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">JVM 参数</label>
                    <input
                      type="text"
                      value={request.jvmOpts || ''}
                      onChange={e => updateRequest('jvmOpts', e.target.value)}
                      placeholder="-XX:+UseG1GC -Xmx512m"
                      className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">Spring Profile</label>
                    <input
                      type="text"
                      value={request.springProfile || ''}
                      onChange={e => updateRequest('springProfile', e.target.value)}
                      placeholder="prod"
                      className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-cyan-400/50 focus:outline-none focus:ring-1 focus:ring-cyan-400/30 transition-all"
                    />
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    id="includeCompose"
                    checked={request.includeDockerCompose !== false}
                    onChange={e => updateRequest('includeDockerCompose', e.target.checked)}
                    className="w-4 h-4 rounded bg-white/5 border-white/20 text-cyan-500 focus:ring-cyan-400/30"
                  />
                  <label htmlFor="includeCompose" className="text-sm text-gray-300">
                    同时生成 docker-compose.yml
                  </label>
                </div>
              </div>
            )}
          </div>

          <button
            onClick={handleGenerate}
            disabled={generating || !request.projectPath.trim()}
            className="w-full py-3.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-medium hover:shadow-lg hover:shadow-cyan-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {generating ? (
              <>
                <Loader2 className="w-5 h-5 animate-spin" />
                正在分析项目...
              </>
            ) : (
              <>
                <FileCode2 className="w-5 h-5" />
                分析项目并生成 Dockerfile
              </>
            )}
          </button>
        </div>
      )}

      {currentStep === 'generate' && generateResult && (
        <div className="space-y-4">
          <div className="glass-card rounded-2xl p-5">
            <h2 className="text-lg font-bold text-white mb-4">项目分析结果</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {[
                { label: '项目名称', value: generateResult.projectName, color: 'text-cyan-400' },
                { label: '构建工具', value: generateResult.buildTool.toUpperCase(), color: 'text-purple-400' },
                { label: 'JDK 版本', value: generateResult.jdkVersion, color: 'text-emerald-400' },
                { label: '打包方式', value: generateResult.packaging.toUpperCase(), color: 'text-amber-400' },
              ].map(item => (
                <div key={item.label} className="glass-card-strong rounded-xl p-3 text-center">
                  <p className="text-gray-400 text-xs mb-1">{item.label}</p>
                  <p className={`font-bold text-lg ${item.color}`}>{item.value}</p>
                </div>
              ))}
            </div>
            {generateResult.mainClass && (
              <div className="mt-3 glass-card-strong rounded-xl p-3">
                <p className="text-gray-400 text-xs mb-1">主启动类</p>
                <p className="text-white font-mono text-sm">{generateResult.mainClass}</p>
              </div>
            )}
            <div className="mt-3 flex flex-wrap gap-2">
              {generateResult.dockerfileWritten && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <CheckCircle2 className="w-3 h-3" /> Dockerfile 已写入
                </span>
              )}
              {generateResult.dockerignoreWritten && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <CheckCircle2 className="w-3 h-3" /> .dockerignore 已写入
                </span>
              )}
              {generateResult.dockerComposeWritten && (
                <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  <CheckCircle2 className="w-3 h-3" /> docker-compose.yml 已写入
                </span>
              )}
            </div>
          </div>

          <FilePreview
            title="Dockerfile"
            id="dockerfile"
            content={generateResult.dockerfileContent}
            expanded={expandedSection === 'dockerfile'}
            onToggle={() => setExpandedSection(expandedSection === 'dockerfile' ? '' : 'dockerfile')}
            copiedId={copiedId}
            onCopy={handleCopy}
            onDownload={() => handleDownload(generateResult.dockerfileContent, 'Dockerfile')}
            icon={<Container className="w-4 h-4" />}
          />

          <FilePreview
            title=".dockerignore"
            id="dockerignore"
            content={generateResult.dockerignoreContent}
            expanded={expandedSection === 'dockerignore'}
            onToggle={() => setExpandedSection(expandedSection === 'dockerignore' ? '' : 'dockerignore')}
            copiedId={copiedId}
            onCopy={handleCopy}
            onDownload={() => handleDownload(generateResult.dockerignoreContent, '.dockerignore')}
            icon={<FileText className="w-4 h-4" />}
          />

          {generateResult.dockerComposeContent && (
            <FilePreview
              title="docker-compose.yml"
              id="compose"
              content={generateResult.dockerComposeContent}
              expanded={expandedSection === 'compose'}
              onToggle={() => setExpandedSection(expandedSection === 'compose' ? '' : 'compose')}
              copiedId={copiedId}
              onCopy={handleCopy}
              onDownload={() => handleDownload(generateResult.dockerComposeContent, 'docker-compose.yml')}
              icon={<FileCode2 className="w-4 h-4" />}
            />
          )}

          <div className="flex gap-3">
            <button
              onClick={() => setCurrentStep('config')}
              className="flex-1 py-3.5 rounded-xl glass-card text-gray-300 font-medium hover:bg-white/10 transition-all"
            >
              返回修改
            </button>
            <button
              onClick={handleDeploy}
              disabled={deploying || dockerAvailable === false}
              className="flex-[2] py-3.5 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-medium hover:shadow-lg hover:shadow-emerald-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {deploying ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  正在构建并部署...
                </>
              ) : (
                <>
                  <Play className="w-5 h-5" />
                  一键构建并部署
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {currentStep === 'deploy' && deployResult && (
        <div className="space-y-4">
          <div className={`glass-card rounded-2xl p-6 ${deployResult.success ? 'border-emerald-500/30' : 'border-red-500/30'}`}>
            <div className="flex items-center gap-4 mb-4">
              {deployResult.success ? (
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-green-600 flex items-center justify-center">
                  <CheckCircle2 className="w-8 h-8 text-white" />
                </div>
              ) : (
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 flex items-center justify-center">
                  <XCircle className="w-8 h-8 text-white" />
                </div>
              )}
              <div>
                <h2 className="text-2xl font-bold text-white">
                  {deployResult.success ? '部署成功' : '部署失败'}
                </h2>
                <p className="text-gray-400 mt-1">
                  {deployResult.success
                    ? `容器 ${deployResult.containerName} 已启动运行`
                    : deployResult.errorMessage || '请检查 Docker 环境和项目配置'}
                </p>
              </div>
            </div>

            {deployResult.success && (
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {[
                  { label: '镜像名称', value: deployResult.imageName },
                  { label: '容器ID', value: deployResult.containerId },
                  { label: '容器名称', value: deployResult.containerName },
                  { label: '映射端口', value: deployResult.mappedPort?.toString() },
                ].map(item => (
                  <div key={item.label} className="glass-card-strong rounded-xl p-3">
                    <p className="text-gray-400 text-xs">{item.label}</p>
                    <p className="text-white font-mono text-sm mt-0.5 truncate">{item.value}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {(deployResult.buildLog || deployResult.runLog) && (
            <div className="glass-card rounded-2xl p-5">
              <h3 className="text-lg font-bold text-white mb-3 flex items-center gap-2">
                <FileText className="w-5 h-5 text-cyan-400" />
                构建日志
              </h3>
              <div className="bg-black/40 rounded-xl p-4 max-h-96 overflow-auto scrollbar-thin">
                <pre className="text-sm font-mono text-gray-300 whitespace-pre-wrap">
                  {deployResult.buildLog}
                </pre>
                {deployResult.runLog && (
                  <>
                    <div className="border-t border-white/10 my-3" />
                    <p className="text-xs text-cyan-400 mb-2">--- Run 输出 ---</p>
                    <pre className="text-sm font-mono text-gray-300 whitespace-pre-wrap">
                      {deployResult.runLog}
                    </pre>
                  </>
                )}
              </div>
            </div>
          )}

          <div className="flex gap-3">
            <button
              onClick={() => {
                setCurrentStep('generate');
                setDeployResult(null);
              }}
              className="flex-1 py-3.5 rounded-xl glass-card text-gray-300 font-medium hover:bg-white/10 transition-all"
            >
              返回查看文件
            </button>
            <button
              onClick={handleDeploy}
              disabled={deploying}
              className="flex-1 py-3.5 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-medium hover:shadow-lg hover:shadow-emerald-500/25 transition-all disabled:opacity-50 flex items-center justify-center gap-2"
            >
              {deploying ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <RefreshCw className="w-5 h-5" />
              )}
              重新部署
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function FilePreview({
  title,
  id,
  content,
  expanded,
  onToggle,
  copiedId,
  onCopy,
  onDownload,
  icon,
}: {
  title: string;
  id: string;
  content: string;
  expanded: boolean;
  onToggle: () => void;
  copiedId: string | null;
  onCopy: (content: string, id: string) => void;
  onDownload: () => void;
  icon: React.ReactNode;
}) {
  return (
    <div className="glass-card rounded-2xl overflow-hidden">
      <button
        onClick={onToggle}
        className="w-full px-5 py-4 flex items-center justify-between hover:bg-white/5 transition-all"
      >
        <div className="flex items-center gap-3">
          <div className="text-cyan-400">{icon}</div>
          <span className="font-medium text-white">{title}</span>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={e => {
              e.stopPropagation();
              onCopy(content, id);
            }}
            className="p-2 rounded-lg hover:bg-white/10 transition-all"
            title="复制"
          >
            {copiedId === id ? (
              <Check className="w-4 h-4 text-emerald-400" />
            ) : (
              <Copy className="w-4 h-4 text-gray-400" />
            )}
          </button>
          <button
            onClick={e => {
              e.stopPropagation();
              onDownload();
            }}
            className="p-2 rounded-lg hover:bg-white/10 transition-all"
            title="下载"
          >
            <Download className="w-4 h-4 text-gray-400" />
          </button>
          {expanded ? (
            <ChevronUp className="w-4 h-4 text-gray-400" />
          ) : (
            <ChevronDown className="w-4 h-4 text-gray-400" />
          )}
        </div>
      </button>
      {expanded && (
        <div className="border-t border-white/5">
          <div className="bg-black/40 p-4 overflow-auto max-h-96 scrollbar-thin">
            <pre className="text-sm font-mono text-gray-300 whitespace-pre-wrap">{content}</pre>
          </div>
        </div>
      )}
    </div>
  );
}
