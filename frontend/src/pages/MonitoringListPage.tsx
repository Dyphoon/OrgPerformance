import { useState, useEffect } from 'react';
import { Table, Button, Space, Modal, Form, Select, DatePicker, message, Steps, Tag, Card, Row, Col, Progress } from 'antd';
import { PlusOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { api } from '../api';
import type { Monitoring } from '../types';

interface CollectorTaskSummary {
  collectorUserId: number;
  collectorName: string;
  collectorEmpNo: string;
  totalIndicators: number;
  completedIndicators: number;
  status: '待录入' | '已完成';
  indicatorNames: string[];
}

export default function MonitoringListPage() {
  const [data, setData] = useState<Monitoring[]>([]);
  const [systems, setSystems] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [modalVisible, setModalVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentMonitoring, setCurrentMonitoring] = useState<Monitoring | null>(null);
  const [collectionTasks, setCollectionTasks] = useState<CollectorTaskSummary[]>([]);
  const [confirmationTasks, setConfirmationTasks] = useState<any[]>([]);
  const [collectorDetailVisible, setCollectorDetailVisible] = useState(false);
  const [collectorDetailData, setCollectorDetailData] = useState<{ configs: any[]; rows: any[] }>({ configs: [], rows: [] });
  const [collectorDetailTitle, setCollectorDetailTitle] = useState('');
  const [form] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [monitoringsRes, systemsRes] = await Promise.all([
        api.monitorings.list({ page, pageSize: 10 }),
        api.systems.list({ pageSize: 100 }),
      ]);
      setData(monitoringsRes.data || []);
      setTotal(monitoringsRes.total || 0);
      setSystems(systemsRes.data || []);
    } catch (error) {
      message.error('Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [page]);

  const handleCreate = async (values: any) => {
    try {
      await api.monitorings.create({
        systemId: values.systemId,
        year: values.period.year(),
        month: values.period.month() + 1,
        deadline: values.deadline,
        approvalRequired: values.approvalRequired,
      });
      message.success('Monitoring created');
      setModalVisible(false);
      form.resetFields();
      fetchData();
    } catch (error: any) {
      message.error(error.message || 'Failed to create');
    }
  };

  const handleBatchGenerate = async (id: number) => {
    try {
      await api.monitorings.batchGenerate(id);
      message.success('Reports generation started');
      setDetailVisible(false);
      fetchData();
    } catch (error: any) {
      message.error(error.message || 'Failed to generate reports');
    }
  };

  const handlePublish = async (id: number) => {
    try {
      await api.monitorings.publish(id);
      message.success('Published');
      setDetailVisible(false);
      fetchData();
    } catch (error: any) {
      message.error(error.message || 'Failed to publish');
    }
  };

  const handleRollback = async (id: number) => {
    try {
      await api.monitorings.rollback(id);
      message.success('已回退到收数中状态');
      setDetailVisible(false);
      fetchData();
    } catch (error: any) {
      message.error(error.message || '回退失败');
    }
  };

  const handleRollbackToConfirming = async (id: number) => {
    try {
      await api.monitorings.rollbackToConfirming(id);
      message.success('已回退到确认中状态');
      setDetailVisible(false);
      fetchData();
    } catch (error: any) {
      message.error(error.message || '回退失败');
    }
  };

  const handleDelete = (record: Monitoring) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除"${record.systemName}"${record.period}的监测吗？此操作将删除所有相关数据，包括数据库记录和MinIO中的文件，且无法恢复。`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await api.monitorings.delete(record.id);
          message.success('删除成功');
          fetchData();
        } catch (error: any) {
          message.error(error.message || '删除失败');
        }
      },
    });
  };

  const openDetail = async (record: Monitoring) => {
    setCurrentMonitoring(record);
    setDetailVisible(true);

    if (record.status === 'COLLECTING') {
      try {
        const tasks = await api.monitorings.getAllTasks(record.id);
        // 按收数人聚合任务
        const collectorMap = new Map<number, CollectorTaskSummary>();
        for (const task of tasks || []) {
          const collectorId = task.collectorUserId;
          if (!collectorMap.has(collectorId)) {
            collectorMap.set(collectorId, {
              collectorUserId: collectorId,
              collectorName: task.collectorName || '',
              collectorEmpNo: task.collectorEmpNo || '',
              totalIndicators: 0,
              completedIndicators: 0,
              status: '待录入',
              indicatorNames: [],
            });
          }
          const summary = collectorMap.get(collectorId)!;
          summary.totalIndicators++;
          if (task.status === 'submitted') {
            summary.completedIndicators++;
          }
          if (task.collectionIndicatorName && !summary.indicatorNames.includes(task.collectionIndicatorName)) {
            summary.indicatorNames.push(task.collectionIndicatorName);
          }
        }
        // 更新状态
        for (const summary of collectorMap.values()) {
          summary.status = summary.completedIndicators === summary.totalIndicators ? '已完成' : '待录入';
        }
        setCollectionTasks(Array.from(collectorMap.values()));
        setConfirmationTasks([]);
      } catch (error) {
        console.error('Failed to fetch collection tasks', error);
      }
    } else if (record.status === 'CONFIRMING') {
      try {
        const tasks = await api.monitorings.getConfirmationTasks(record.id);
        setConfirmationTasks(tasks || []);
        setCollectionTasks([]);
      } catch (error) {
        console.error('Failed to fetch confirmation tasks', error);
      }
    } else {
      setCollectionTasks([]);
      setConfirmationTasks([]);
    }
  };

  const viewCollectorDetail = async (collector: CollectorTaskSummary) => {
    if (!currentMonitoring) return;
    try {
      const tasks = await api.monitorings.getMyTasks(currentMonitoring.id, collector.collectorUserId);
      setCollectorDetailTitle(`${collector.collectorName} (${collector.collectorEmpNo}) 的收数详情`);
      
      // 按收数指标名称去重
      const indicatorMap = new Map<string, { unit: string; taskId: number }>();
      for (const task of tasks || []) {
        const indicatorName = task.collectionIndicatorName || `${task.level1Name}${task.level2Name || ''}`;
        if (!indicatorMap.has(indicatorName)) {
          indicatorMap.set(indicatorName, { unit: task.collectionUnit || task.unit || '', taskId: task.id });
        }
      }
      const configs = Array.from(indicatorMap.entries()).map(([name, info]) => ({
        indicatorName: name,
        unit: info.unit,
      }));
      
      // 按机构分组
      const institutionMap = new Map<string, Record<string, { value: any; status: string }>>();
      for (const task of tasks || []) {
        const indicatorName = task.collectionIndicatorName || `${task.level1Name}${task.level2Name || ''}`;
        if (!institutionMap.has(task.institutionName)) {
          institutionMap.set(task.institutionName, {});
        }
        institutionMap.get(task.institutionName)![indicatorName] = {
          value: task.actualValue,
          status: task.status,
        };
      }
      const rows = Array.from(institutionMap.entries()).map(([name, data]) => ({
        institutionName: name,
        data,
      }));
      
      setCollectorDetailData({ configs, rows });
      setCollectorDetailVisible(true);
    } catch (error) {
      message.error('获取收数详情失败');
    }
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      PENDING: 'default',
      COLLECTING: 'processing',
      PROCESSING: 'processing',
      CONFIRMING: 'warning',
      PUBLISHED: 'success',
    };
    return colors[status] || 'default';
  };

  const getStatusText = (status: string) => {
    const texts: Record<string, string> = {
      PENDING: '待发起',
      COLLECTING: '收数中',
      PROCESSING: '计算中',
      CONFIRMING: '确认中',
      PUBLISHED: '已发布',
    };
    return texts[status] || status;
  };

  const getStepIndex = (status: string) => {
    const steps = ['PENDING', 'COLLECTING', 'PROCESSING', 'CONFIRMING', 'PUBLISHED'];
    return steps.indexOf(status);
  };

  const columns = [
    { title: '体系', dataIndex: 'systemName', key: 'systemName' },
    { title: '期间', dataIndex: 'period', key: 'period' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (status: string) => <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag> },
    { title: '进度', key: 'process',
      render: (_: any, record: Monitoring) => (
        record.processStatus === 'processing' ? <Progress percent={record.processPercent} size="small" /> : `${record.processPercent || 0}%`
      ) },
    { title: '机构确认', key: 'confirm',
      render: (_: any, record: Monitoring) => 
        `${record.confirmedInstitutions || 0}/${record.totalInstitutions || 0}` },
    { title: '任务进度', key: 'tasks',
      render: (_: any, record: Monitoring) =>
        `${record.submittedTasks || 0}/${record.totalTasks || 0}` },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Monitoring) => (
        <Space>
          <Button type="link" onClick={() => openDetail(record)}>详情</Button>
          {record.status === 'PUBLISHED' && (
            <Button type="link" danger onClick={() => handleRollbackToConfirming(record.id)}>回退</Button>
          )}
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record)}>删除</Button>
        </Space>
      ),
    },
  ];

  const allTasksCompleted = collectionTasks.length > 0 && collectionTasks.every(t => t.status === '已完成');

  const collectionTaskColumns = [
    { title: '收数人', dataIndex: 'collectorName', key: 'collectorName' },
    { title: '工号', dataIndex: 'collectorEmpNo', key: 'collectorEmpNo' },
    { title: '负责指标', dataIndex: 'indicatorNames', key: 'indicatorNames',
      render: (names: string[]) => names.join('、')
    },
    { title: '完成进度', key: 'progress',
      render: (_: any, record: CollectorTaskSummary) => `${record.completedIndicators}/${record.totalIndicators}`
    },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={s === '已完成' ? 'success' : 'warning'}>{s}</Tag>
    },
    { title: '操作', key: 'action',
      render: (_: any, record: CollectorTaskSummary) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => viewCollectorDetail(record)}>
          查看详情
        </Button>
      )
    },
  ];

  const confirmationTaskColumns = [
    { title: '机构', dataIndex: 'institutionName', key: 'institutionName' },
    { title: '负责人', dataIndex: 'leaderName', key: 'leaderName' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s: string) => <Tag color={s === 'confirmed' ? 'success' : 'warning'}>{s === 'confirmed' ? '已确认' : '待确认'}</Tag>
    },
    { title: '确认时间', dataIndex: 'confirmedAt', key: 'confirmedAt' },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>月度监测</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          发起监测
        </Button>
      </div>

      <Table
        dataSource={data}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{
          current: page,
          total,
          onChange: setPage,
          showSizeChanger: false,
        }}
      />

      <Modal
        title="发起月度监测"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={500}
      >
        <Form form={form} onFinish={handleCreate} layout="vertical">
          <Form.Item name="systemId" label="考核体系" rules={[{ required: true }]}>
            <Select>
              {systems.map(s => <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="period" label="监测期间" rules={[{ required: true }]}>
            <DatePicker picker="month" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="deadline" label="收数截止时间">
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="approvalRequired" label="需要审批" valuePropName="checked">
            <input type="checkbox" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>发起</Button>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="监测详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={900}
      >
        {currentMonitoring && (
          <div>
            <Steps current={getStepIndex(currentMonitoring.status)} items={[
              { title: '待发起' },
              { title: '收数中' },
              { title: '计算中' },
              { title: '确认中' },
              { title: '已发布' },
            ]} style={{ marginBottom: 24 }} />

            <Row gutter={16}>
              <Col span={8}>
                <Card size="small" title="基本信息">
                  <p>体系：{currentMonitoring.systemName}</p>
                  <p>期间：{currentMonitoring.period}</p>
                  <p>状态：<Tag color={getStatusColor(currentMonitoring.status)}>{getStatusText(currentMonitoring.status)}</Tag></p>
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small" title="机构确认">
                  <p>已确认：{currentMonitoring.confirmedInstitutions}</p>
                  <p>总计：{currentMonitoring.totalInstitutions}</p>
                </Card>
              </Col>
              <Col span={8}>
                <Card size="small" title="任务进度">
                  <p>已提交：{currentMonitoring.submittedTasks}</p>
                  <p>总计：{currentMonitoring.totalTasks}</p>
                </Card>
              </Col>
            </Row>

            {currentMonitoring.processStatus === 'processing' && (
              <div style={{ marginTop: 16 }}>
                <Progress percent={currentMonitoring.processPercent} status="active" />
                <p style={{ textAlign: 'center', color: '#666' }}>{currentMonitoring.processMsg}</p>
              </div>
            )}

            {currentMonitoring.status === 'COLLECTING' && (
              <div style={{ marginTop: 24 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                  <h4>收数任务列表</h4>
                  <Button 
                    type="primary" 
                    onClick={() => handleBatchGenerate(currentMonitoring.id)}
                    disabled={!allTasksCompleted}
                  >
                    批量生成绩效报告
                  </Button>
                </div>
                <Table 
                  dataSource={collectionTasks} 
                  columns={collectionTaskColumns} 
                  rowKey="id" 
                  size="small"
                  pagination={false}
                />
                {!allTasksCompleted && (
                  <p style={{ color: '#999', marginTop: 8 }}>还有收数任务未完成，无法生成报告</p>
                )}
              </div>
            )}

            {currentMonitoring.status === 'CONFIRMING' && (
              <div style={{ marginTop: 24 }}>
                <h4>确认任务列表</h4>
                <Table 
                  dataSource={confirmationTasks} 
                  columns={confirmationTaskColumns} 
                  rowKey="id" 
                  size="small"
                  pagination={false}
                />
                <div style={{ marginTop: 16, textAlign: 'center' }}>
                  <Space>
                    <Button danger onClick={() => handleRollback(currentMonitoring.id)}>
                      回退到收数
                    </Button>
                    <Button type="primary" onClick={() => handlePublish(currentMonitoring.id)}>
                      发布
                    </Button>
                  </Space>
                </div>
              </div>
            )}
          </div>
        )}
      </Modal>

      <Modal
        title={collectorDetailTitle}
        open={collectorDetailVisible}
        onCancel={() => setCollectorDetailVisible(false)}
        footer={null}
        width={Math.max(900, 180 + (collectorDetailData.configs?.length || 0) * 140)}
        style={{ top: 20 }}
        bodyStyle={{ maxHeight: 'calc(100vh - 200px)', overflow: 'auto' }}
      >
        {collectorDetailData.configs && collectorDetailData.rows && (
          <Table
            dataSource={collectorDetailData.rows}
            rowKey="institutionName"
            pagination={false}
            scroll={{ x: 'max-content' }}
            columns={[
              {
                title: '机构',
                dataIndex: 'institutionName',
                key: 'institutionName',
                width: 120,
                fixed: 'left' as const,
                render: (text: string) => <strong>{text}</strong>,
              },
              ...collectorDetailData.configs.map(config => ({
                title: (
                  <div style={{ padding: '4px 0' }}>
                    <div style={{ fontWeight: 500, whiteSpace: 'normal', lineHeight: 1.3 }}>{config.indicatorName}</div>
                    <div style={{ fontSize: 11, color: '#999', fontWeight: 'normal' }}>{config.unit}</div>
                  </div>
                ),
                key: config.indicatorName,
                width: 150,
                align: 'center' as const,
                render: (_: any, record: any) => {
                  const cellData = record.data?.[config.indicatorName];
                  return <span>{cellData?.value ?? '-'}</span>;
                },
              })),
            ]}
          />
        )}
      </Modal>
    </div>
  );
}
