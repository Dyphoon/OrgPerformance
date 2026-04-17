import { useState, useEffect } from 'react';
import { Card, Table, Tag, Spin, Empty, Drawer, Descriptions, Badge, Typography, Tooltip } from 'antd';
import {
  ApiOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  FieldNumberOutlined,
} from '@ant-design/icons';
import { mcpApi } from '../api/mcp';
import type { McpToolInfo } from '../api/mcp';

const { Text } = Typography;

const categoryColors: Record<string, string> = {
  '系统管理': 'blue',
  '监测管理': 'green',
  '任务管理': 'orange',
  '确认管理': 'gold',
  '报表管理': 'purple',
  '数据分析': 'cyan',
  '模板管理': 'lime',
  '其他': 'default',
};

function getTypeIcon(type: string): React.ReactNode {
  const lowerType = type.toLowerCase();
  if (lowerType === 'long' || lowerType === 'integer' || lowerType === 'double' || lowerType === 'bigdecimal') {
    return <FieldNumberOutlined style={{ color: '#1890ff' }} />;
  }
  if (lowerType === 'boolean') {
    return <CheckCircleOutlined style={{ color: '#faad14' }} />;
  }
  return <FieldNumberOutlined style={{ color: '#52c41a' }} />;
}

export default function McpPage() {
  const [tools, setTools] = useState<McpToolInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTool, setSelectedTool] = useState<McpToolInfo | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);

  useEffect(() => {
    loadTools();
  }, []);

  const loadTools = async () => {
    setLoading(true);
    try {
      const data = await mcpApi.getTools();
      setTools(data);
    } catch (error) {
      console.error('Failed to load MCP tools:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetail = (tool: McpToolInfo) => {
    setSelectedTool(tool);
    setDetailVisible(true);
  };

  const columns = [
    {
      title: '工具名称',
      dataIndex: 'name',
      key: 'name',
      width: 220,
      render: (name: string) => (
        <Text strong style={{ fontFamily: 'monospace', fontSize: 13 }}>
          {name}
        </Text>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (desc: string) => (
        <Tooltip title={desc}>
          <span>{desc}</span>
        </Tooltip>
      ),
    },
    {
      title: '分类',
      key: 'category',
      width: 120,
      render: (_: any, record: McpToolInfo) => {
        return <Tag color={categoryColors[record.category] || 'default'}>{record.category}</Tag>;
      },
    },
    {
      title: '参数',
      key: 'params',
      width: 100,
      render: (_: any, record: McpToolInfo) => (
        <span>
          <Badge 
            count={record.parameters?.length || 0} 
            showZero 
            style={{ backgroundColor: '#1890ff' }}
          />
          <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
            个参数
          </Text>
        </span>
      ),
    },
    {
      title: '必填',
      key: 'required',
      width: 80,
      render: (_: any, record: McpToolInfo) => {
        const requiredCount = record.parameters?.filter(p => p.required).length || 0;
        if (requiredCount === 0) {
          return <Text type="secondary">-</Text>;
        }
        return (
          <Tag color="red" icon={<CheckCircleOutlined />}>
            {requiredCount} 个
          </Tag>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: McpToolInfo) => (
        <a onClick={() => handleViewDetail(record)}>
          查看详情
        </a>
      ),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 50 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <span>
            <ApiOutlined style={{ marginRight: 8 }} />
            MCP 工具服务
          </span>
        }
        extra={
          <Tag color="blue">共 {tools.length} 个工具</Tag>
        }
      >
        {tools.length === 0 ? (
          <Empty description="暂无可用的 MCP 工具" />
        ) : (
          <Table
            dataSource={tools}
            columns={columns}
            rowKey="name"
            pagination={false}
            size="middle"
          />
        )}
      </Card>

      <Drawer
        title={
          selectedTool && (
            <span>
              <ApiOutlined style={{ marginRight: 8 }} />
              <code>{selectedTool.name}</code>
            </span>
          )
        }
        placement="right"
        width={500}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
      >
        {selectedTool && (
          <div>
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="工具名称">
                <Text code>{selectedTool.name}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="分类">
                <Tag color={categoryColors[selectedTool.category] || 'default'}>
                  {selectedTool.category}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="描述" contentStyle={{ whiteSpace: 'pre-wrap' }}>
                {selectedTool.description}
              </Descriptions.Item>
            </Descriptions>

            <Card 
              size="small" 
              title={
                <span>
                  <InfoCircleOutlined style={{ marginRight: 8 }} />
                  参数列表 ({selectedTool.parameters?.length || 0})
                </span>
              }
            >
              {selectedTool.parameters && selectedTool.parameters.length > 0 ? (
                <Descriptions column={1} size="small">
                  {selectedTool.parameters.map((param, index) => (
                    <Descriptions.Item 
                      key={index} 
                      label={
                        <span>
                          {getTypeIcon(param.type)}
                          <Text code style={{ marginLeft: 4 }}>{param.name}</Text>
                          {param.required && (
                            <Tag color="red" style={{ marginLeft: 8 }}>必填</Tag>
                          )}
                        </span>
                      }
                    >
                      <div>
                        <div style={{ lineHeight: 1.6 }}>{param.description}</div>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          类型: {param.type}
                        </Text>
                      </div>
                    </Descriptions.Item>
                  ))}
                </Descriptions>
              ) : (
                <Empty description="该工具无参数" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </Card>

            <Card 
              size="small" 
              title="调用示例" 
              style={{ marginTop: 16 }}
            >
              <pre style={{ 
                background: '#f5f5f5', 
                padding: 12, 
                borderRadius: 6, 
                fontSize: 12,
                lineHeight: 1.5,
                overflow: 'auto',
                margin: 0,
                fontFamily: 'monospace'
              }}>
{`{
  "name": "${selectedTool.name}",
  "arguments": {
${selectedTool.parameters?.map(p => `    "${p.name}": ${p.required ? `<${p.type}>` : `// ${p.type} (可选)`}`).join(',\n') || '    // parameters...'}
  }
}`}
              </pre>
            </Card>
          </div>
        )}
      </Drawer>
    </div>
  );
}
