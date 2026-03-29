import { useState, useEffect } from 'react';
import { Table, Button, Space, Modal, Form, Input, Upload, message, Popconfirm, Checkbox } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, EyeOutlined, UploadOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import { api } from '../api';
import type { System } from '../types';
import SystemPreviewModal from '../components/SystemPreviewModal';

export default function SystemListPage() {
  const [data, setData] = useState<System[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingSystem, setEditingSystem] = useState<System | null>(null);
  const [form] = Form.useForm();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewSystemId, setPreviewSystemId] = useState<number | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await api.systems.list({ page, pageSize: 10 });
      setData(res.data || []);
      setTotal(res.total || 0);
    } catch (error) {
      message.error('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [page]);

  const handleAdd = () => {
    setEditingSystem(null);
    form.resetFields();
    setFileList([]);
    setModalVisible(true);
  };

  const handleEdit = (record: System) => {
    setEditingSystem(record);
    form.setFieldsValue(record);
    setFileList([]);
    setModalVisible(true);
  };

  const handleDelete = async (id: number, _name: string) => {
    try {
      await api.systems.delete(id);
      message.success('删除成功');
      fetchData();
    } catch (error: any) {
      message.error(error.message || '删除失败');
    }
  };

  const handlePreview = (record: System) => {
    setPreviewSystemId(record.id);
    setPreviewVisible(true);
  };

  const handleSubmit = async (values: any) => {
    if (!editingSystem && fileList.length === 0) {
      message.error('请上传Excel模板文件');
      return;
    }

    setSubmitting(true);
    try {
      if (editingSystem) {
        await api.systems.update(editingSystem.id, values);
        message.success('Updated');
      } else {
        const fileItem = fileList[0];
        const file = fileItem?.originFileObj;
        if (!file) {
          message.error('请上传Excel模板文件');
          setSubmitting(false);
          return;
        }
        const result = await api.systems.createWithFile(values.name, values.description || '', values.needApproval || false, file);
        console.log('Create result:', result);
        message.success('创建成功');
      }
      setModalVisible(false);
      fetchData();
    } catch (error: any) {
      console.error('Submit error:', error);
      message.error(error.message || error.error?.message || '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60, fixed: 'left' as const },
    { title: '体系名称', dataIndex: 'name', key: 'name', width: 200 },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true, width: 250 },
    { title: '机构数', dataIndex: 'institutionCount', key: 'institutionCount', width: 80 },
    { title: '指标数', dataIndex: 'indicatorCount', key: 'indicatorCount', width: 80 },
    { 
      title: '状态', 
      dataIndex: 'status', 
      key: 'status', 
      width: 80,
      render: (s: number) => <span style={{ color: s ? '#52c41a' : '#999' }}>{s ? '启用' : '禁用'}</span> 
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right' as const,
      render: (_: any, record: System) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handlePreview(record)}>查看</Button>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm
            title="确认删除"
            description={`确定要删除体系"${record.name}"吗？此操作将同时删除所有关联的监测任务、收集任务及相关文件，且不可恢复。`}
            onConfirm={() => handleDelete(record.id, record.name)}
            okText="确认删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>考核体系管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          创建体系
        </Button>
      </div>

      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        scroll={{ x: 'max-content' }}
        pagination={{
          current: page,
          total,
          onChange: setPage,
          showSizeChanger: false,
        }}
      />

      <Modal
        title={editingSystem ? '编辑体系' : '创建体系'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item name="name" label="体系名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="needApproval" label="需要审批" valuePropName="checked" initialValue={false}>
            <Checkbox>需要审批</Checkbox>
          </Form.Item>
          {!editingSystem && (
            <Form.Item label="上传模版文件" rules={[{ required: true, message: '请上传Excel模板文件' }]}>
              <Upload
                accept=".xlsx"
                fileList={fileList}
                beforeUpload={(file) => {
                  setFileList([{
                    uid: '-1',
                    name: file.name,
                    status: 'done' as const,
                    originFileObj: file,
                  }]);
                  return false;
                }}
                onRemove={() => setFileList([])}
                maxCount={1}
              >
                <Button icon={<UploadOutlined />}>选择Excel文件</Button>
              </Upload>
            </Form.Item>
          )}
          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              {editingSystem ? '更新' : '创建'}
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      <SystemPreviewModal
        systemId={previewSystemId}
        visible={previewVisible}
        onClose={() => setPreviewVisible(false)}
      />
    </div>
  );
}
