import { useState, useEffect } from 'react';
import { Table, Button, message, Tag } from 'antd';
import { EyeOutlined, EditOutlined } from '@ant-design/icons';
import { api } from '../api';
import type { Monitoring, CollectionTask } from '../types';
import { useAuth } from '../store/AuthContext';
import TaskDetailModal from './TaskDetailModal';

interface CollectorTaskSummary {
  id: number;
  monitoringId: number;
  systemName: string;
  period: string;
  status: '待录入' | '已完成' | '已截止';
}

export default function DataCollectPage() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState<CollectorTaskSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [selectedTask, setSelectedTask] = useState<CollectorTaskSummary | null>(null);

  const fetchTasks = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await api.monitorings.list({ pageSize: 100 });
      const monitorings: Monitoring[] = data.data || [];
      
      const summaries: CollectorTaskSummary[] = [];
      for (const m of monitorings) {
        if (m.status !== 'COLLECTING' && m.status !== 'CLOSED') continue;
        
        const myTasks = await api.monitorings.getMyTasks(m.id, user.id);
        if (myTasks && myTasks.length > 0) {
          const allSubmitted = myTasks.every((t: CollectionTask) => t.status === 'submitted');
          summaries.push({
            id: m.id,
            monitoringId: m.id,
            systemName: m.systemName,
            period: m.period,
            status: m.status === 'CLOSED' ? '已截止' : (allSubmitted ? '已完成' : '待录入'),
          });
        }
      }
      setTasks(summaries);
    } catch (error) {
      message.error('获取任务列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, [user]);

  const handleView = (record: CollectorTaskSummary) => {
    setSelectedTask(record);
    setDetailModalVisible(true);
  };

  const handleEdit = (record: CollectorTaskSummary) => {
    setSelectedTask(record);
    setDetailModalVisible(true);
  };

  const handleSubmitSuccess = () => {
    setDetailModalVisible(false);
    fetchTasks();
  };

  const columns = [
    { title: '考核体系', dataIndex: 'systemName', key: 'systemName' },
    { title: '监测期间', dataIndex: 'period', key: 'period' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s: string) => {
        const color = s === '已完成' ? 'success' : s === '待录入' ? 'warning' : 'default';
        return <Tag color={color}>{s}</Tag>;
      }
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: CollectorTaskSummary) => {
        if (record.status === '已完成' || record.status === '已截止') {
          return (
            <Button type="link" icon={<EyeOutlined />} onClick={() => handleView(record)}>
              查看
            </Button>
          );
        }
        return (
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            录入
          </Button>
        );
      },
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2>数据填写</h2>
        <Button onClick={fetchTasks}>刷新</Button>
      </div>

      <Table
        dataSource={tasks}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />

      <TaskDetailModal
        visible={detailModalVisible}
        task={selectedTask}
        onClose={() => setDetailModalVisible(false)}
        onSubmitSuccess={handleSubmitSuccess}
      />
    </div>
  );
}
