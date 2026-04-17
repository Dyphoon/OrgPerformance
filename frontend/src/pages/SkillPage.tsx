import { useState, useEffect } from 'react';
import { Card, Row, Col, Button, Tag, Spin, Empty, Tabs, Badge, Drawer, Modal, Form, Input, Select, message } from 'antd';
import { 
  BarChartOutlined, 
  FileTextOutlined, 
  DashboardOutlined, 
  BellOutlined, 
  PieChartOutlined,
  ToolOutlined,
  CheckCircleOutlined,
  PlusOutlined,
  MinusOutlined,
  EyeOutlined,
  CodeOutlined,
  InfoCircleOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { skillApi } from '../api/skill';
import type { Skill, ToolInfo } from '../api/skill';

const iconMap: Record<string, React.ReactNode> = {
  BarChartOutlined: <BarChartOutlined />,
  FileTextOutlined: <FileTextOutlined />,
  DashboardOutlined: <DashboardOutlined />,
  BellOutlined: <BellOutlined />,
  PieChartOutlined: <PieChartOutlined />,
  ToolOutlined: <ToolOutlined />,
};

const iconOptions = [
  { value: 'BarChartOutlined', label: '柱状图' },
  { value: 'FileTextOutlined', label: '文档' },
  { value: 'DashboardOutlined', label: '仪表盘' },
  { value: 'BellOutlined', label: '提醒' },
  { value: 'PieChartOutlined', label: '饼图' },
  { value: 'ToolOutlined', label: '工具' },
];

const categoryColors: Record<string, string> = {
  '数据分析': 'blue',
  '文档处理': 'green',
  '指标管理': 'orange',
  '任务管理': 'red',
  '数据可视化': 'purple',
  '自定义': 'cyan',
};

const defaultCategories = ['数据分析', '文档处理', '指标管理', '任务管理', '数据可视化', '自定义'];

export default function SkillPage() {
  const [skills, setSkills] = useState<Skill[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [installedSkills, setInstalledSkills] = useState<Skill[]>([]);
  const [availableTools, setAvailableTools] = useState<ToolInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [installing, setInstalling] = useState<number | null>(null);
  const [selectedSkill, setSelectedSkill] = useState<Skill | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [editVisible, setEditVisible] = useState(false);
  const [editingSkill, setEditingSkill] = useState<Skill | null>(null);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [skillsData, categoriesData, installedData, toolsData] = await Promise.all([
        skillApi.getAllSkills(),
        skillApi.getCategories(),
        skillApi.getInstalledSkills(),
        skillApi.getAvailableTools(),
      ]);
      setSkills(skillsData);
      const allCategories = [...new Set([...defaultCategories, ...categoriesData])];
      setCategories(allCategories);
      setInstalledSkills(installedData);
      setAvailableTools(toolsData);
    } catch (error) {
      console.error('Failed to load skills:', error);
    } finally {
      setLoading(false);
    }
  };

  const buildToolOptions = () => {
    const grouped = availableTools.reduce((acc, tool) => {
      if (!acc[tool.category]) {
        acc[tool.category] = [];
      }
      acc[tool.category].push({
        label: tool.name,
        value: tool.name,
        description: tool.description,
      });
      return acc;
    }, {} as Record<string, { label: string; value: string; description: string }[]>);

    return Object.entries(grouped).map(([category, options]) => ({
      label: category,
      options: options.map(opt => ({
        label: (
          <div>
            <div style={{ fontFamily: 'monospace', fontSize: 12 }}>{opt.label}</div>
            <div style={{ fontSize: 11, color: '#999' }}>{opt.description}</div>
          </div>
        ),
        value: opt.value,
      })),
    }));
  };

  const handleInstall = async (skill: Skill) => {
    setInstalling(skill.id);
    try {
      await skillApi.installSkill(skill.id);
      setSkills(prev => prev.map(s => s.id === skill.id ? { ...s, installed: true } : s));
      setInstalledSkills(prev => [...prev, { ...skill, installed: true }]);
      message.success(`已安装「${skill.name}」`);
    } catch (error) {
      message.error('安装失败');
    } finally {
      setInstalling(null);
    }
  };

  const handleUninstall = async (skill: Skill) => {
    setInstalling(skill.id);
    try {
      await skillApi.uninstallSkill(skill.id);
      setSkills(prev => prev.map(s => s.id === skill.id ? { ...s, installed: false } : s));
      setInstalledSkills(prev => prev.filter(s => s.id !== skill.id));
      message.success(`已卸载「${skill.name}」`);
    } catch (error) {
      message.error('卸载失败');
    } finally {
      setInstalling(null);
    }
  };

  const handleViewDetail = (skill: Skill) => {
    setSelectedSkill(skill);
    setDetailVisible(true);
  };

  const handleCreateSkill = () => {
    setEditingSkill(null);
    form.resetFields();
    setEditVisible(true);
  };

  const handleEditSkill = (skill: Skill, e: React.MouseEvent) => {
    e.stopPropagation();
    if (skill.installed) {
      message.warning('请先卸载技能后再编辑');
      return;
    }
    setEditingSkill(skill);
    const toolsArray = skill.tools ? skill.tools.split(',').map(t => t.trim()).filter(Boolean) : [];
    form.setFieldsValue({
      name: skill.name,
      description: skill.description,
      icon: skill.icon,
      category: skill.category,
      prompt: skill.prompt,
      tools: toolsArray,
      markdownContent: skill.markdownContent,
      scriptContent: skill.scriptContent,
      version: skill.version,
      author: skill.author,
    });
    setEditVisible(true);
  };

  const handleDeleteSkill = async (skill: Skill, e: React.MouseEvent) => {
    e.stopPropagation();
    if (skill.installed) {
      message.warning('请先卸载技能后再删除');
      return;
    }
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除技能「${skill.name}」吗？`,
      okText: '确认',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await skillApi.deleteSkill(skill.id);
          message.success('删除成功');
          loadData();
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const handleSubmitForm = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const submitData = {
        ...values,
        tools: Array.isArray(values.tools) ? values.tools.join(',') : values.tools,
      };
      if (editingSkill) {
        await skillApi.updateSkill(editingSkill.id, submitData);
        message.success('技能更新成功');
      } else {
        await skillApi.createSkill(submitData);
        message.success('技能创建成功');
      }
      setEditVisible(false);
      loadData();
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const filteredSkills = activeCategory 
    ? skills.filter(s => s.category === activeCategory)
    : skills;

  const groupedSkills = filteredSkills.reduce((acc, skill) => {
    if (!acc[skill.category]) {
      acc[skill.category] = [];
    }
    acc[skill.category].push(skill);
    return acc;
  }, {} as Record<string, Skill[]>);

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
            <ToolOutlined style={{ marginRight: 8 }} />
            AI 技能市场
          </span>
        }
        extra={
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <Tag color="blue">已安装 {installedSkills.length} 个技能</Tag>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateSkill} size="small">
              创建技能
            </Button>
          </div>
        }
      >
        <Tabs
          activeKey={activeCategory || 'all'}
          onChange={(key) => setActiveCategory(key === 'all' ? null : key)}
          items={[
            { key: 'all', label: '全部' },
            ...categories.map(cat => ({
              key: cat,
              label: <Badge count={skills.filter(s => s.category === cat && s.installed).length} offset={[10, 0]}><span>{cat}</span></Badge>
            }))
          ]}
          style={{ marginBottom: 16 }}
        />

        {Object.keys(groupedSkills).length === 0 ? (
          <Empty description="暂无可用技能" />
        ) : (
          Object.entries(groupedSkills).map(([category, categorySkills]) => (
            <div key={category} style={{ marginBottom: 24 }}>
              <div style={{ marginBottom: 12 }}>
                <Tag color={categoryColors[category] || 'default'}>{category}</Tag>
              </div>
              <Row gutter={[16, 16]}>
                {categorySkills.map(skill => (
                  <Col xs={24} sm={12} md={8} lg={6} key={skill.id}>
                    <Card
                      size="small"
                      hoverable
                      style={{ 
                        height: '100%',
                        borderColor: skill.installed ? '#1890ff' : undefined,
                      }}
                      bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                    >
                      <div style={{ display: 'flex', alignItems: 'flex-start', marginBottom: 8 }}>
                        <div style={{ 
                          fontSize: 24, 
                          marginRight: 12,
                          color: skill.installed ? '#1890ff' : '#999',
                        }}>
                          {iconMap[skill.icon] || <ToolOutlined />}
                        </div>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontWeight: 500, marginBottom: 4 }}>
                            {skill.name}
                            {skill.installed && (
                              <CheckCircleOutlined 
                                style={{ color: '#1890ff', marginLeft: 6, fontSize: 12 }} 
                              />
                            )}
                          </div>
                          <div style={{ fontSize: 12, color: '#666', lineHeight: 1.4 }}>
                            {skill.description}
                          </div>
                        </div>
                      </div>
                      <div style={{ marginTop: 'auto', paddingTop: 8 }}>
                        <Button
                          size="small"
                          icon={<EyeOutlined />}
                          onClick={() => handleViewDetail(skill)}
                          style={{ marginBottom: 8 }}
                          block
                        >
                          查看详情
                        </Button>
                        {skill.installed ? (
                          <Button 
                            size="small" 
                            icon={<MinusOutlined />}
                            onClick={() => handleUninstall(skill)}
                            loading={installing === skill.id}
                            danger
                            block
                          >
                            卸载
                          </Button>
                        ) : (
                          <div style={{ display: 'flex', gap: 8 }}>
                            <Button 
                              size="small" 
                              icon={<EditOutlined />}
                              onClick={(e) => handleEditSkill(skill, e)}
                              style={{ flex: 1 }}
                            >
                              编辑
                            </Button>
                            <Button 
                              size="small" 
                              danger
                              icon={<DeleteOutlined />}
                              onClick={(e) => handleDeleteSkill(skill, e)}
                            />
                          </div>
                        )}
                      </div>
                    </Card>
                  </Col>
                ))}
              </Row>
            </div>
          ))
        )}
      </Card>

      <Drawer
        title={
          selectedSkill && (
            <span>
              {iconMap[selectedSkill.icon] || <ToolOutlined />}
              <span style={{ marginLeft: 8 }}>{selectedSkill.name}</span>
            </span>
          )
        }
        placement="right"
        width={600}
        open={detailVisible}
        onClose={() => setDetailVisible(false)}
        extra={
          selectedSkill && (
            <Tag color={selectedSkill.installed ? 'blue' : 'default'}>
              {selectedSkill.installed ? '已安装' : '未安装'}
            </Tag>
          )
        }
      >
        {selectedSkill && (
          <div>
            <div style={{ marginBottom: 16 }}>
              <Tag color={categoryColors[selectedSkill.category] || 'default'}>
                {selectedSkill.category}
              </Tag>
              {selectedSkill.version && <Tag>{selectedSkill.version}</Tag>}
              {selectedSkill.author && <Tag><InfoCircleOutlined /> {selectedSkill.author}</Tag>}
            </div>

            <Card size="small" title="简介" style={{ marginBottom: 16 }}>
              <p style={{ margin: 0, lineHeight: 1.6 }}>{selectedSkill.description}</p>
            </Card>

            <Card size="small" title="技能提示词" style={{ marginBottom: 16 }}>
              <pre style={{ 
                background: '#f5f5f5', 
                padding: 12, 
                borderRadius: 6, 
                fontSize: 13,
                lineHeight: 1.5,
                overflow: 'auto',
                maxHeight: 200,
                margin: 0
              }}>
                {selectedSkill.prompt}
              </pre>
            </Card>

            {selectedSkill.tools && (
              <Card size="small" title="关联工具" style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {selectedSkill.tools.split(',').map((tool, idx) => (
                    <Tag key={idx} icon={<CodeOutlined />}>{tool.trim()}</Tag>
                  ))}
                </div>
              </Card>
            )}

            {selectedSkill.markdownContent && (
              <Card size="small" title="详细说明">
                <div style={{ 
                  maxHeight: 400, 
                  overflow: 'auto',
                  fontSize: 13,
                  lineHeight: 1.5,
                }}>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {selectedSkill.markdownContent}
                  </ReactMarkdown>
                </div>
              </Card>
            )}

            <div style={{ marginTop: 16, textAlign: 'right' }}>
              {selectedSkill.installed ? (
                <Button 
                  danger
                  icon={<MinusOutlined />}
                  onClick={() => {
                    handleUninstall(selectedSkill);
                    setDetailVisible(false);
                  }}
                  loading={installing === selectedSkill.id}
                >
                  卸载技能
                </Button>
              ) : (
                <Button 
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => {
                    handleInstall(selectedSkill);
                    setDetailVisible(false);
                  }}
                  loading={installing === selectedSkill.id}
                >
                  安装技能
                </Button>
              )}
            </div>
          </div>
        )}
      </Drawer>

      <Modal
        title={editingSkill ? '编辑技能' : '创建技能'}
        open={editVisible}
        onOk={handleSubmitForm}
        onCancel={() => setEditVisible(false)}
        width={700}
        okText={editingSkill ? '保存' : '创建'}
        cancelText="取消"
        confirmLoading={submitting}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="name" label="技能名称" rules={[{ required: true, message: '请输入技能名称' }]}>
                <Input placeholder="请输入技能名称" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="icon" label="图标">
                <Select options={iconOptions} placeholder="选择图标" />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="category" label="分类">
                <Select options={defaultCategories.map(c => ({ value: c, label: c }))} placeholder="选择分类" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="version" label="版本">
                <Input placeholder="如: 1.0.0" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="description" label="简介" rules={[{ required: true, message: '请输入技能简介' }]}>
            <Input.TextArea rows={2} placeholder="请输入技能简介" />
          </Form.Item>
          <Form.Item name="prompt" label="技能提示词" rules={[{ required: true, message: '请输入技能提示词' }]}>
            <Input.TextArea rows={4} placeholder="定义AI助手如何扮演这个角色/执行这个技能" />
          </Form.Item>
          <Form.Item name="tools" label="关联工具">
            <Select
              mode="multiple"
              placeholder="选择关联的工具（可多选）"
              allowClear
              showSearch
              options={buildToolOptions()}
              style={{ width: '100%' }}
              dropdownRender={(menu) => (
                <>
                  {menu}
                  <div style={{ padding: '8px 12px', borderTop: '1px solid #e8e8e8', background: '#fafafa' }}>
                    <div style={{ fontSize: 11, color: '#999' }}>
                      已选择 {form.getFieldValue('tools')?.length || 0} 个工具
                    </div>
                  </div>
                </>
              )}
            />
          </Form.Item>
          <Form.Item name="author" label="作者">
            <Input placeholder="请输入作者名称" />
          </Form.Item>
          <Form.Item name="markdownContent" label="详细说明(Markdown)">
            <Input.TextArea rows={8} placeholder="支持Markdown格式的详细说明文档" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
