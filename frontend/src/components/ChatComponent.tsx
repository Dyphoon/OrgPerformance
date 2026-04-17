import { useState, useRef, useEffect, useMemo } from 'react';
import { Button, Input, Avatar, Spin, Card, Collapse, Upload, Tag, message, Badge } from 'antd';
import { SendOutlined, RobotOutlined, UserOutlined, ClearOutlined, BulbOutlined, PaperClipOutlined, FileOutlined, FileWordOutlined, FileExcelOutlined, FilePptOutlined, DeleteOutlined } from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { agentApi } from '../api/agent';
import type { UploadedFile } from '../api/agent';
import { useAuth } from '../store/AuthContext';

interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  thinking?: string;
  timestamp: Date;
}

const parseContent = (content: string): { thinking?: string; main: string } => {
  const thinkMatch = content.match(/<think>([\s\S]*?)<\/think>/);
  if (thinkMatch) {
    return {
      thinking: thinkMatch[1].trim(),
      main: content.replace(/<think>[\s\S]*?<\/think>/g, '').trim(),
    };
  }
  return { main: content };
};

const getFileIcon = (fileType: string) => {
  switch (fileType) {
    case '.doc':
    case '.docx':
      return <FileWordOutlined style={{ color: '#1890ff' }} />;
    case '.xls':
    case '.xlsx':
      return <FileExcelOutlined style={{ color: '#52c41a' }} />;
    case '.ppt':
    case '.pptx':
      return <FilePptOutlined style={{ color: '#fa8c16' }} />;
    default:
      return <FileOutlined />;
  }
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const { TextArea } = Input;

export default function ChatComponent() {
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const sessionId = useMemo(() => `session_${user?.id || 'anonymous'}_${Date.now()}`, []);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    loadSessionFiles();
  }, [sessionId]);

  const loadSessionFiles = async () => {
    try {
      const result = await agentApi.getSessionFiles(sessionId);
      if (result.success) {
        setUploadedFiles(result.files);
      }
    } catch (error) {
      console.error('Failed to load session files:', error);
    }
  };

  const handleFileUpload = async (file: File) => {
    if (file.size > 10 * 1024 * 1024) {
      message.error('文件大小超过10MB限制');
      return false;
    }

    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    const supportedTypes = ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'];
    if (!supportedTypes.includes(ext)) {
      message.error('不支持的文件格式，支持: .doc, .docx, .xls, .xlsx, .ppt, .pptx');
      return false;
    }

    setUploading(true);
    try {
      const result = await agentApi.uploadFile(sessionId, file);
      if (result.success) {
        message.success(`已上传: ${result.fileName}`);
        setUploadedFiles(prev => [...prev, {
          fileName: result.fileName,
          fileType: result.fileType,
          fileSize: result.fileSize,
          contentLength: result.contentLength,
          contentPreview: result.contentPreview,
          uploadedAt: Date.now(),
        }]);
      } else {
        message.error(result.error || '上传失败');
      }
    } catch (error) {
      message.error('上传失败，请重试');
      console.error('Upload error:', error);
    } finally {
      setUploading(false);
    }
    return false;
  };

  const handleRemoveFile = async (fileName: string) => {
    try {
      const success = await agentApi.removeFile(sessionId, fileName);
      if (success) {
        setUploadedFiles(prev => prev.filter(f => f.fileName !== fileName));
        message.success('已移除文件');
      }
    } catch (error) {
      message.error('移除失败');
      console.error('Remove file error:', error);
    }
  };

  const handleClearAllFiles = async () => {
    try {
      await agentApi.clearSessionFiles(sessionId);
      setUploadedFiles([]);
      message.success('已清空所有文件');
    } catch (error) {
      message.error('清空失败');
      console.error('Clear files error:', error);
    }
  };

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return;

    const userMessage: Message = {
      id: `user_${Date.now()}`,
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setLoading(true);

    const templateFile = uploadedFiles.find(f => f.isTemplate && f.templateFileKey);
    const templateFileKey = templateFile?.templateFileKey;

    try {
      const response = await agentApi.chat(sessionId, userMessage.content, templateFileKey);
      
      const parsed = parseContent(response.message || response.error || '抱歉，我遇到了一些问题。');
      const assistantMessage: Message = {
        id: `assistant_${Date.now()}`,
        role: 'assistant',
        content: parsed.main,
        thinking: parsed.thinking,
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      const errorMessage: Message = {
        id: `error_${Date.now()}`,
        role: 'assistant',
        content: '网络错误，请稍后重试。',
        timestamp: new Date(),
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const handleClear = async () => {
    setMessages([]);
    setUploadedFiles([]);
    try {
      await agentApi.clearSession(sessionId);
      await agentApi.clearSessionFiles(sessionId);
    } catch (error) {
      console.error('Failed to clear session:', error);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Card
      title={
        <span>
          <RobotOutlined style={{ marginRight: 8 }} />
          AI 助手
        </span>
      }
      extra={
        <Button 
          type="text" 
          icon={<ClearOutlined />} 
          onClick={handleClear}
          size="small"
        >
          清空对话
        </Button>
      }
      styles={{ body: { padding: 0, display: 'flex', flexDirection: 'column', height: '600px' } }}
    >
      <div style={{ flex: 1, overflow: 'auto', padding: '16px', background: '#f5f5f5' }}>
        {messages.length === 0 && uploadedFiles.length === 0 && (
          <div style={{ textAlign: 'center', color: '#999', marginTop: 100 }}>
            <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
            <p>您好！我是组织绩效管理助手</p>
            <p style={{ fontSize: 12 }}>我可以帮您：</p>
            <ul style={{ fontSize: 12, textAlign: 'left', display: 'inline-block' }}>
              <li>创建评估体系</li>
              <li>发起监测任务</li>
              <li>录入和收集数据</li>
              <li>获取分析报告</li>
            </ul>
            <p style={{ fontSize: 12, marginTop: 16 }}>
              <PaperClipOutlined /> 支持上传 Office 文件 (Word/Excel/PPT) 作为对话上下文
            </p>
          </div>
        )}
        
        {messages.map(msg => (
          <div
            key={msg.id}
            style={{
              display: 'flex',
              justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
              marginBottom: 16,
            }}
          >
            {msg.role === 'assistant' && (
              <Avatar 
                icon={<RobotOutlined />} 
                style={{ marginRight: 8, background: '#1890ff', flexShrink: 0 }} 
              />
            )}
            <div style={{ maxWidth: '70%', display: 'flex', flexDirection: 'column' }}>
            {msg.role === 'assistant' && msg.thinking && (
              <Collapse
                ghost
                size="small"
                items={[{
                  key: 'thinking',
                  label: (
                    <span style={{ fontSize: 11, color: '#888' }}>
                      <BulbOutlined /> 深度思考
                    </span>
                  ),
                  children: (
                    <div style={{ 
                      backgroundColor: '#fffbe6', 
                      padding: '8px 12px', 
                      borderRadius: 6, 
                      fontSize: 12, 
                      color: '#666',
                      lineHeight: 1.3,
                      fontStyle: 'italic',
                      borderLeft: '3px solid #faad14',
                      marginBottom: msg.content ? 8 : 0,
                    }}>
                      {msg.thinking}
                    </div>
                  )
                }]}
                style={{ marginBottom: msg.content ? 8 : 0 }}
                className="agent-thinking-collapse"
              />
            )}
            <div
              style={{
                padding: '12px 16px',
                borderRadius: 12,
                background: msg.role === 'user' ? '#1890ff' : '#fff',
                color: msg.role === 'user' ? '#fff' : '#333',
                boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                textAlign: 'left',
              }}
            >
              {msg.role === 'assistant' && msg.content ? (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    h1: ({children}) => <h1 style={{fontSize: '1.4em', marginBottom: '0.15em', fontWeight: 'bold'}}>{children}</h1>,
                    h2: ({children}) => <h2 style={{fontSize: '1.2em', marginBottom: '0.1em', fontWeight: 'bold'}}>{children}</h2>,
                    h3: ({children}) => <h3 style={{fontSize: '1em', marginBottom: '0.08em', fontWeight: 'bold'}}>{children}</h3>,
                    p: ({children}) => <p style={{marginBottom: '0.15em', lineHeight: 1.2}}>{children}</p>,
                    ul: ({children}) => <ul style={{paddingLeft: '1.2em', marginBottom: '0.15em'}}>{children}</ul>,
                    ol: ({children}) => <ol style={{paddingLeft: '1.2em', marginBottom: '0.15em'}}>{children}</ol>,
                    li: ({children}) => <li style={{marginBottom: '0.05em', lineHeight: 1.2}}>{children}</li>,
                    code: ({children, className}) => {
                      const isInline = !className;
                      return isInline ? (
                        <code style={{backgroundColor: '#f0f0f0', padding: '1px 3px', borderRadius: 2, fontFamily: 'monospace'}}>{children}</code>
                      ) : (
                        <code style={{display: 'block', backgroundColor: '#f5f5f5', padding: '4px', borderRadius: 3, overflow: 'auto', fontFamily: 'monospace', marginBottom: '0.15em', lineHeight: 1.15}}>{children}</code>
                      );
                    },
                    pre: ({children}) => <pre style={{backgroundColor: '#f5f5f5', padding: '4px', borderRadius: 3, overflow: 'auto', marginBottom: '0.15em', lineHeight: 1.15}}>{children}</pre>,
                    table: ({children}) => <table style={{borderCollapse: 'collapse', width: '100%', marginBottom: '0.15em'}}>{children}</table>,
                    th: ({children}) => <th style={{border: '1px solid #ddd', padding: '3px', backgroundColor: '#f0f0f0'}}>{children}</th>,
                    td: ({children}) => <td style={{border: '1px solid #ddd', padding: '3px'}}>{children}</td>,
                    blockquote: ({children}) => <blockquote style={{borderLeft: '3px solid #ccc', paddingLeft: '0.8em', marginLeft: 0, color: '#666', fontStyle: 'italic', marginBottom: '0.15em'}}>{children}</blockquote>,
                    a: ({href, children}) => <a href={href} style={{color: '#1890ff', textDecoration: 'underline'}} target="_blank" rel="noopener noreferrer">{children}</a>,
                    strong: ({children}) => <strong style={{fontWeight: 'bold'}}>{children}</strong>,
                    em: ({children}) => <em style={{fontStyle: 'italic'}}>{children}</em>,
                    hr: () => <hr style={{border: 'none', borderTop: '1px solid #ddd', margin: '0.4em 0'}} />,
                  }}
                >
                  {msg.content}
                </ReactMarkdown>
              ) : (
                <span>{msg.content}</span>
              )}
            </div>
            </div>
            {msg.role === 'user' && (
              <Avatar
                icon={<UserOutlined />}
                style={{ marginLeft: 8, background: '#52c41a' }}
              />
            )}
          </div>
        ))}
        
        {loading && (
          <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: 16 }}>
            <Avatar icon={<RobotOutlined />} style={{ marginRight: 8, background: '#1890ff' }} />
            <div
              style={{
                padding: '12px 16px',
                borderRadius: 12,
                background: '#fff',
                boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
              }}
            >
              <Spin size="small" /> <span style={{ marginLeft: 8, color: '#999' }}>思考中...</span>
            </div>
          </div>
        )}
        
        <div ref={messagesEndRef} />
      </div>

      {uploadedFiles.length > 0 && (
        <div style={{ 
          padding: '8px 16px', 
          borderTop: '1px solid #eee', 
          background: '#fafafa',
          display: 'flex',
          flexWrap: 'wrap',
          gap: 8,
          alignItems: 'center',
        }}>
          <span style={{ fontSize: 12, color: '#666', marginRight: 8 }}>已上传文件:</span>
          {uploadedFiles.map((file, index) => (
            <Tag
              key={index}
              icon={getFileIcon(file.fileType)}
              closable
              onClose={() => handleRemoveFile(file.fileName)}
              style={{ display: 'flex', alignItems: 'center', gap: 4 }}
            >
              {file.fileName} ({formatFileSize(file.fileSize)})
            </Tag>
          ))}
          {uploadedFiles.length > 1 && (
            <Button 
              type="link" 
              size="small" 
              danger
              icon={<DeleteOutlined />}
              onClick={handleClearAllFiles}
              style={{ fontSize: 11, padding: '0 4px', height: 'auto' }}
            >
              清空
            </Button>
          )}
        </div>
      )}

      <div style={{ padding: 16, borderTop: '1px solid #eee', background: '#fff' }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <Upload
            beforeUpload={handleFileUpload}
            showUploadList={false}
            accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx"
          >
            <Button 
              icon={<PaperClipOutlined />} 
              loading={uploading}
              disabled={loading}
              title="上传 Office 文件"
            />
          </Upload>
          <TextArea
            value={inputValue}
            onChange={e => setInputValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="输入您的问题，按 Enter 发送..."
            autoSize={{ minRows: 1, maxRows: 4 }}
            style={{ flex: 1 }}
            disabled={loading}
          />
          <Button 
            type="primary" 
            icon={<SendOutlined />} 
            onClick={handleSend}
            loading={loading}
            disabled={!inputValue.trim()}
          >
            发送
          </Button>
        </div>
        <div style={{ marginTop: 8, fontSize: 11, color: '#999' }}>
          <PaperClipOutlined style={{ marginRight: 4 }} />
          支持上传 Word/Excel/PPT 文件，文件内容将作为对话上下文
          {uploadedFiles.length > 0 && <Badge count={uploadedFiles.length} size="small" style={{ marginLeft: 8 }} />}
        </div>
      </div>
    </Card>
  );
}
