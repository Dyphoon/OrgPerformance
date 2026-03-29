import { useState, useEffect } from 'react';
import { Table, Button, Modal, message, Card, Typography, Space } from 'antd';
import { CheckCircleOutlined, FileExcelOutlined } from '@ant-design/icons';
import { api } from '../api';
import { useAuth } from '../store/AuthContext';
import type { Monitoring } from '../types';

const { Title, Text } = Typography;

interface ConfirmationTask {
  id: number;
  monitoringId: number;
  institutionId: number;
  institutionName: string;
  status: string;
  remark?: string;
  confirmedAt?: string;
}

export default function ConfirmationPage() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState<ConfirmationTask[]>([]);
  const [loading, setLoading] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState<ConfirmationTask | null>(null);
  const [reportData, setReportData] = useState<any>(null);
  const [reportLoading, setReportLoading] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const fetchTasks = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await api.monitorings.list({ status: 'CONFIRMING', pageSize: 100 });
      const monList: Monitoring[] = data.data || [];
      const allTasks: ConfirmationTask[] = [];
      for (const m of monList) {
        const confirmationTasks = await api.monitorings.getConfirmationTasks(m.id);
        const myTasks = confirmationTasks.filter((t: any) => t.userId === user.id);
        for (const t of myTasks) {
          allTasks.push({
            ...t,
            systemName: m.systemName,
            period: m.period,
          });
        }
      }
      setTasks(allTasks);
    } catch (error) {
      message.error('获取确认任务失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, [user]);

  const handlePreview = async (task: ConfirmationTask) => {
    setSelectedTask(task);
    setPreviewVisible(true);
    setReportLoading(true);
    try {
      const data = await api.reports.getInstitutionReport(task.monitoringId, task.institutionId);
      setReportData(data);
    } catch (error: any) {
      message.error(error.message || '获取绩效报告失败');
      setReportData(null);
    } finally {
      setReportLoading(false);
    }
  };

  const handleConfirm = async () => {
    if (!selectedTask) return;
    setConfirming(true);
    try {
      await api.monitorings.confirm(selectedTask.monitoringId, selectedTask.institutionId);
      message.success('确认成功');
      setPreviewVisible(false);
      fetchTasks();
    } catch (error: any) {
      message.error(error.message || '确认失败');
    } finally {
      setConfirming(false);
    }
  };

  const columns = [
    {
      title: '体系',
      dataIndex: 'systemName',
      key: 'systemName',
    },
    {
      title: '月份',
      dataIndex: 'period',
      key: 'period',
    },
    {
      title: '机构',
      dataIndex: 'institutionName',
      key: 'institutionName',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        status === 'confirmed' ? <Text type="success">已确认</Text> : <Text type="warning">待确认</Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: ConfirmationTask) => (
        record.status !== 'confirmed' ? (
          <Button
            type="primary"
            icon={<CheckCircleOutlined />}
            onClick={() => handlePreview(record)}
          >
            去确认
          </Button>
        ) : (
          <Button
            icon={<FileExcelOutlined />}
            onClick={() => handlePreview(record)}
          >
            查看
          </Button>
        )
      ),
    },
  ];

  const renderReportPreview = () => {
    if (!reportData) return null;

    return (
      <div style={{ padding: 24 }}>
        <Title level={4} style={{ textAlign: 'center' }}>
          {reportData.institutionName} - 绩效报告
        </Title>
        <Title level={5} style={{ textAlign: 'center', color: '#666' }}>
          {reportData.year}年{reportData.month}月
        </Title>

        <Card title="总体得分" style={{ marginBottom: 16 }}>
          <Space size="large">
            <div>
              <Text type="secondary">总分</Text>
              <Title level={2} style={{ margin: 0, color: '#1890ff' }}>
                {reportData.totalScore?.toFixed(2) || '-'}
              </Title>
            </div>
            <div>
              <Text type="secondary">排名</Text>
              <Title level={2} style={{ margin: 0, color: '#faad14' }}>
                {reportData.totalRank || '-'}
              </Title>
            </div>
          </Space>
        </Card>

        {reportData.dimensionScores && reportData.dimensionScores.length > 0 && (
          <Card title="维度得分" style={{ marginBottom: 16 }}>
            {reportData.dimensionScores.map((dim: any, idx: number) => (
              <div key={idx} style={{ marginBottom: 8 }}>
                <Text strong>{dim.dimension}: </Text>
                <Text>{dim.score?.toFixed(2) || '-'}</Text>
              </div>
            ))}
          </Card>
        )}

        {reportData.categoryScores && reportData.categoryScores.length > 0 && (
          <Card title="类别得分" style={{ marginBottom: 16 }}>
            {reportData.categoryScores.map((cat: any, idx: number) => (
              <div key={idx} style={{ marginBottom: 8 }}>
                <Text strong>{cat.category}: </Text>
                <Text>{cat.score?.toFixed(2) || '-'}</Text>
              </div>
            ))}
          </Card>
        )}

        {reportData.indicators && reportData.indicators.length > 0 && (
          <Card title="指标明细">
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #f0f0f0' }}>
                  <th style={{ textAlign: 'left', padding: 8 }}>维度</th>
                  <th style={{ textAlign: 'left', padding: 8 }}>类别</th>
                  <th style={{ textAlign: 'left', padding: 8 }}>一级指标</th>
                  <th style={{ textAlign: 'left', padding: 8 }}>二级指标</th>
                  <th style={{ textAlign: 'right', padding: 8 }}>实际值</th>
                  <th style={{ textAlign: 'right', padding: 8 }}>得分</th>
                </tr>
              </thead>
              <tbody>
                {reportData.indicators.map((ind: any, idx: number) => (
                  <tr key={idx} style={{ borderBottom: '1px solid #f0f0f0' }}>
                    <td style={{ padding: 8 }}>{ind.dimension || '-'}</td>
                    <td style={{ padding: 8 }}>{ind.category || '-'}</td>
                    <td style={{ padding: 8 }}>{ind.level1Name || '-'}</td>
                    <td style={{ padding: 8 }}>{ind.level2Name || '-'}</td>
                    <td style={{ textAlign: 'right', padding: 8 }}>{ind.actualValue ?? '-'}</td>
                    <td style={{ textAlign: 'right', padding: 8 }}>{ind.score?.toFixed(2) || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        )}

        <div style={{ marginTop: 24, textAlign: 'center' }}>
          <Button
            type="primary"
            size="large"
            icon={<CheckCircleOutlined />}
            onClick={handleConfirm}
            loading={confirming}
            disabled={selectedTask?.status === 'confirmed'}
          >
            确认
          </Button>
        </div>
      </div>
    );
  };

  return (
    <div>
      <Card
        title="绩效结果确认"
        extra={
          <Button onClick={fetchTasks} loading={loading}>
            刷新
          </Button>
        }
      >
        <Table
          columns={columns}
          dataSource={tasks}
          rowKey="id"
          loading={loading}
          locale={{ emptyText: '暂无待确认任务' }}
        />
      </Card>

      <Modal
        title={selectedTask ? `${selectedTask.institutionName} - 绩效报告预览` : '绩效报告预览'}
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={null}
        width={800}
      >
        {reportLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>加载中...</div>
        ) : (
          renderReportPreview()
        )}
      </Modal>
    </div>
  );
}
