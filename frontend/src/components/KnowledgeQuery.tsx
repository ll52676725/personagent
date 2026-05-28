import { useState, useEffect, useRef } from 'react';
import { MessageCircle, Send, Library } from 'lucide-react';
import { knowledgeApi } from '@/api';
import { KnowledgeBase } from '@/types';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: { knowledgeId: number; title: string; content: string; similarity: number }[];
  streaming?: boolean;
}

export default function KnowledgeQuery() {
  const [bases, setBases] = useState<KnowledgeBase[]>([]);
  const [selectedBaseId, setSelectedBaseId] = useState<number | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    knowledgeApi.getBases().then(res => {
      if (res.code === 200) setBases(res.data);
    }).catch(() => undefined);
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || loading) return;
    const question = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: question }]);
    setLoading(true);

    const assistantMsg: ChatMessage = { role: 'assistant', content: '', streaming: true };
    setMessages(prev => [...prev, assistantMsg]);

    try {
      knowledgeApi.queryStream(
        question,
        selectedBaseId,
        (chunk) => {
          setMessages(prev => {
            const updated = [...prev];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg.role === 'assistant') {
              updated[updated.length - 1] = { ...lastMsg, content: lastMsg.content + chunk };
            }
            return updated;
          });
        },
        () => {
          setMessages(prev => {
            const updated = [...prev];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg.role === 'assistant') {
              updated[updated.length - 1] = { ...lastMsg, streaming: false };
            }
            return updated;
          });
          setLoading(false);
        },
        (error) => {
          setMessages(prev => {
            const updated = [...prev];
            const lastMsg = updated[updated.length - 1];
            if (lastMsg.role === 'assistant') {
              updated[updated.length - 1] = { ...lastMsg, content: `错误：${error}`, streaming: false };
            }
            return updated;
          });
          setLoading(false);
        }
      );
    } catch (err) {
      setMessages(prev => {
        const updated = [...prev];
        const lastMsg = updated[updated.length - 1];
        if (lastMsg.role === 'assistant') {
          updated[updated.length - 1] = { ...lastMsg, content: '查询失败，请稍后重试', streaming: false };
        }
        return updated;
      });
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-10rem)]">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-3xl font-bold text-white">智能问答</h1>
          <p className="text-gray-400 mt-1">基于知识库的 AI 问答</p>
        </div>
        <div className="flex items-center gap-2">
          <Library className="w-4 h-4 text-gray-400" />
          <select
            value={selectedBaseId || ''}
            onChange={(e) => setSelectedBaseId(e.target.value ? Number(e.target.value) : null)}
            className="input-field py-2 px-3 text-sm"
          >
            <option value="">全部知识库</option>
            {bases.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </div>
      </div>

      <div className="flex-1 glass-card rounded-3xl p-6 overflow-y-auto scrollbar-thin mb-4">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="w-20 h-20 rounded-full bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center mb-6">
              <MessageCircle className="w-10 h-10 text-white" />
            </div>
            <h3 className="text-white font-semibold text-xl mb-2">知识库问答</h3>
            <p className="text-gray-400 max-w-md">向 AI 提问，它将基于您的知识库内容给出精准回答</p>
          </div>
        ) : (
          <div className="space-y-4">
            {messages.map((msg, index) => (
              <div key={index} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] rounded-2xl p-4 ${
                  msg.role === 'user'
                    ? 'bg-gradient-to-br from-indigo-500 to-cyan-400 text-white'
                    : 'bg-white/5 text-gray-200'
                }`}>
                  <div className="whitespace-pre-wrap text-sm leading-relaxed">{msg.content}</div>
                  {msg.streaming && <span className="inline-block w-2 h-4 bg-cyan-400 animate-pulse ml-1" />}
                  {msg.sources && msg.sources.length > 0 && !msg.streaming && (
                    <div className="mt-3 pt-3 border-t border-white/10">
                      <p className="text-xs text-gray-400 mb-2">参考来源：</p>
                      {msg.sources.map((src, i) => (
                        <div key={i} className="text-xs text-gray-400 mb-1">
                          <span className="text-cyan-400">{src.title}</span>
                          <span className="ml-2">相似度: {(src.similarity * 100).toFixed(1)}%</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      <div className="glass-card rounded-2xl p-4 flex items-center gap-3">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && handleSend()}
          className="input-field flex-1"
          placeholder="输入您的问题..."
          disabled={loading}
        />
        <button
          onClick={handleSend}
          disabled={loading || !input.trim()}
          className="btn-primary p-3 rounded-xl disabled:opacity-50"
        >
          <Send className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
}
