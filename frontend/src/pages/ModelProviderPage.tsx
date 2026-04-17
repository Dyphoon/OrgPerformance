import { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, Switch, message, Space, Popconfirm, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import { modelProviderApi } from '../api/modelProvider';
import type { ModelProvider } from '../api/modelProvider';

const { Option } = Select;

export default function ModelProviderPage() {
  const [data, setData] = useState<ModelProvider[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const loadData = async () => {
    setLoading(true);
    try {
      const result = await modelProviderApi.list();
      setData(result || []);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAdd = () => {
    setEditingId(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: ModelProvider) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await modelProviderApi.delete(id);
      message.success('删除成功');
      loadData();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const submitData = {
        ...values,
        maxTokens: values.maxTokens ? Number(values.maxTokens) : 4096,
        temperature: values.temperature ? Number(values.temperature) : 0.7,
        sortOrder: values.sortOrder ? Number(values.sortOrder) : 0,
        status: values.status ? 1 : 0,
      };
      if (editingId) {
        await modelProviderApi.update(editingId, submitData);
        message.success('更新成功');
      } else {
        await modelProviderApi.create(submitData);
        message.success('创建成功');
      }
      setModalVisible(false);
      loadData();
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleInitDefault = async () => {
    try {
      await modelProviderApi.init();
      message.success('初始化默认供应商成功');
      loadData();
    } catch (error) {
      message.error('初始化失败');
    }
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 120,
    },
    {
      title: '代码',
      dataIndex: 'code',
      key: 'code',
      width: 100,
    },
    {
      title: '模型',
      dataIndex: 'modelName',
      key: 'modelName',
      width: 120,
    },
    {
      title: '类型',
      dataIndex: 'modelType',
      key: 'modelType',
      width: 90,
      render: (type: string) => (
        <Tag color={type === 'minimax' ? 'blue' : type === 'glm' ? 'green' : 'default'}>{type}</Tag>
      ),
    },
    {
      title: 'Base URL',
      dataIndex: 'baseUrl',
      key: 'baseUrl',
      width: 200,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 70,
      render: (status: number) => (status === 1 ? '启用' : '停用'),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: ModelProvider) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm
            title="确定删除？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h2>模型服务管理</h2>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleInitDefault}>
            初始化默认
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加模型服务
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingId ? '编辑模型服务' : '添加模型服务'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="如：MiniMax、智谱GLM" />
          </Form.Item>

          <Form.Item
            name="code"
            label="代码"
            rules={[{ required: true, message: '请输入代码' }]}
          >
            <Input placeholder="如：minimax、glm" />
          </Form.Item>

          <Form.Item name="modelType" label="类型" rules={[{ required: true }]}>
            <Select placeholder="选择类型">
              <Option value="minimax">MiniMax</Option>
              <Option value="glm">智谱 GLM</Option>
              <Option value="openai">OpenAI</Option>
              <Option value="other">其他</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="baseUrl"
            label="Base URL"
            rules={[{ required: true, message: '请输入 API 地址' }]}
          >
            <Input placeholder="如：https://api.minimaxi.com/v1" />
          </Form.Item>

          <Form.Item
            name="apiKey"
            label="API Key"
            rules={[{ required: true, message: '请输入 API Key' }]}
          >
            <Input.Password placeholder="请输入 API Key" />
          </Form.Item>

          <Form.Item
            name="modelName"
            label="模型名称"
            rules={[{ required: true, message: '请输入模型名称' }]}
          >
            <Input placeholder="如：MiniMax-M2.7、glm-4" />
          </Form.Item>

          <Form.Item name="maxTokens" label="最大 Token 数">
            <Input type="number" placeholder="默认 4096" />
          </Form.Item>

          <Form.Item name="temperature" label="温度参数">
            <Input type="number" placeholder="默认 0.7" step="0.1" />
          </Form.Item>

          <Form.Item name="sortOrder" label="排序">
            <Input type="number" placeholder="数字越小越靠前" />
          </Form.Item>

          <Form.Item name="status" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="停用" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}