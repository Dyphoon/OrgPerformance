import { useState, useEffect } from 'react';
import { Modal, Table, InputNumber, Button, Space, message, Upload, Card, Typography } from 'antd';
import { UploadOutlined, CheckOutlined, FileExcelOutlined } from '@ant-design/icons';
import { api } from '../api';
import type { CollectionTask } from '../types';
import { useAuth } from '../store/AuthContext';

const { Text } = Typography;

interface TaskDetailModalProps {
  visible: boolean;
  task: TaskSummary | null;
  onClose: () => void;
  onSubmitSuccess: () => void;
}

interface TaskSummary {
  id: number;
  monitoringId: number;
  systemName: string;
  period: string;
  status: string;
}

// 收数指标配置
interface IndicatorConfig {
  indicatorName: string;
  unit: string;
  taskId: number;
  status: string;
}

// 按机构分组的行数据
interface InstitutionRow {
  institutionName: string;
  indicators: Record<string, number | null>; // key: indicatorName, value: actualValue
  statusMap: Record<string, string>; // key: indicatorName, value: status
}

export default function TaskDetailModal({ visible, task, onClose, onSubmitSuccess }: TaskDetailModalProps) {
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [indicatorConfigs, setIndicatorConfigs] = useState<IndicatorConfig[]>([]);
  const [institutionRows, setInstitutionRows] = useState<InstitutionRow[]>([]);
  const [editedValues, setEditedValues] = useState<Record<string, Record<string, number | null>>>({});
  const [collectorFileUrl, setCollectorFileUrl] = useState<string | null>(null);
  const [allMyTasksSubmitted, setAllMyTasksSubmitted] = useState(false);
  const isViewMode = task?.status === '已完成' || task?.status === '已截止' || allMyTasksSubmitted;

  useEffect(() => {
    if (visible && task && user) {
      fetchMyTasks().then(() => {
        fetchCollectorFile();
      });
    }
  }, [visible, task, user]);

  const fetchMyTasks = async () => {
    if (!task || !user) return;
    setLoading(true);
    try {
      const allTasks = await api.monitorings.getAllTasks(task.monitoringId);
      const myTasks = (allTasks as CollectionTask[]).filter(t => t.collectorEmpNo === user.empNo);
      
      if (myTasks.length === 0) {
        setIndicatorConfigs([]);
        setInstitutionRows([]);
        setAllMyTasksSubmitted(false);
        return;
      }

      // 提取唯一的指标配置（按收数指标名称去重）
      const indicatorMap = new Map<string, IndicatorConfig>();
      for (const t of myTasks) {
        const indicatorName = t.collectionIndicatorName || `${t.level1Name}${t.level2Name || ''}`;
        if (!indicatorMap.has(indicatorName)) {
          indicatorMap.set(indicatorName, {
            indicatorName,
            unit: t.collectionUnit || t.unit || '',
            taskId: t.id,
            status: t.status,
          });
        }
      }
      const configs = Array.from(indicatorMap.values());
      setIndicatorConfigs(configs);

      // 检查当前收数人的任务是否全部提交
      const allSubmitted = myTasks.every(t => t.status === 'submitted');
      setAllMyTasksSubmitted(allSubmitted);

      // 按机构分组
      const institutionMap = new Map<string, InstitutionRow>();
      for (const t of myTasks) {
        const instName = t.institutionName;
        const indicatorName = t.collectionIndicatorName || `${t.level1Name}${t.level2Name || ''}`;
        
        if (!institutionMap.has(instName)) {
          institutionMap.set(instName, {
            institutionName: instName,
            indicators: {},
            statusMap: {},
          });
        }
        
        const row = institutionMap.get(instName)!;
        row.indicators[indicatorName] = t.actualValue;
        row.statusMap[indicatorName] = t.status;
      }

      const rows = Array.from(institutionMap.values());
      setInstitutionRows(rows);

      // 初始化编辑值
      const initialValues: Record<string, Record<string, number | null>> = {};
      for (const row of rows) {
        initialValues[row.institutionName] = { ...row.indicators };
      }
      setEditedValues(initialValues);

    } catch (error) {
      message.error('获取任务详情失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchCollectorFile = async () => {
    if (!task || !user) return;
    try {
      const result = await api.monitorings.getCollectorFileUrl(task.monitoringId, user.id);
      if (result) {
        setCollectorFileUrl(result);
      }
    } catch (error) {
      console.error('Failed to get collector file URL:', error);
    }
  };

  const handleValueChange = (institutionName: string, indicatorName: string, value: number | null) => {
    setEditedValues(prev => ({
      ...prev,
      [institutionName]: {
        ...prev[institutionName],
        [indicatorName]: value,
      }
    }));
  };

  const handleSubmit = async () => {
    if (!task) return;
    setSaving(true);
    try {
      const allTasks = await api.monitorings.getAllTasks(task.monitoringId);
      const myTasks = (allTasks as CollectionTask[]).filter(t => user && t.collectorEmpNo === user.empNo);
      
      const updates: { taskId: number; actualValue: number }[] = [];
      
      for (const row of institutionRows) {
        for (const config of indicatorConfigs) {
          const newValue = editedValues[row.institutionName]?.[config.indicatorName];
          const originalValue = row.indicators[config.indicatorName];
          
          // 找到对应的任务ID
          const task = myTasks.find(t => {
            const indicatorName = t.collectionIndicatorName || `${t.level1Name}${t.level2Name || ''}`;
            return t.institutionName === row.institutionName && indicatorName === config.indicatorName;
          });
          
          if (task && newValue !== null && newValue !== undefined && newValue !== originalValue) {
            updates.push({ taskId: task.id, actualValue: newValue });
          }
        }
      }
      
      if (updates.length === 0) {
        message.warning('没有可提交的数据');
        return;
      }
      
      await api.monitorings.batchUpdateTasks(updates);
      message.success('提交成功');
      setAllMyTasksSubmitted(true);
      onSubmitSuccess();
    } catch (error) {
      message.error('提交失败');
    } finally {
      setSaving(false);
    }
  };

  const handleDownloadTemplate = () => {
    if (!collectorFileUrl) {
      message.warning('收数文档不存在');
      return;
    }
    window.open(collectorFileUrl, '_blank');
  };

  const handleUploadExcel = async (file: File) => {
    if (!task) return false;
    try {
      const result = await api.monitorings.uploadDataCollection(task.monitoringId, file);
      if (result.code === 200) {
        message.success('上传成功');
        fetchMyTasks();
      } else {
        message.error(result.message || '上传失败');
      }
    } catch (error) {
      message.error('上传失败');
    }
    return false;
  };

  // 生成表格列
  const columns = [
    {
      title: '机构',
      dataIndex: 'institutionName',
      key: 'institutionName',
      width: 120,
      fixed: 'left' as const,
      render: (text: string) => <strong>{text}</strong>,
    },
    ...indicatorConfigs.map(config => ({
      title: (
        <div style={{ padding: '4px 0' }}>
          <div style={{ fontWeight: 500, whiteSpace: 'normal', lineHeight: 1.3 }}>{config.indicatorName}</div>
          <div style={{ fontSize: 11, color: '#999', fontWeight: 'normal' }}>{config.unit}</div>
        </div>
      ),
      key: config.indicatorName,
      width: 150,
      align: 'center' as const,
      render: (_: any, record: InstitutionRow) => {
        const value = editedValues[record.institutionName]?.[config.indicatorName];
        const status = record.statusMap[config.indicatorName];
        const allSubmitted = status === 'submitted';
        
        if (isViewMode) {
          return <span>{value ?? '-'}</span>;
        }
        return (
          <InputNumber
            value={value ?? null}
            onChange={(val) => handleValueChange(record.institutionName, config.indicatorName, val)}
            style={{ width: '100%' }}
            placeholder="请输入"
            status={allSubmitted ? 'success' : undefined}
          />
        );
      },
    })),
  ];

  // 统计
  let totalCount = 0;
  let completedCount = 0;
  for (const row of institutionRows) {
    for (const config of indicatorConfigs) {
      totalCount++;
      if (row.statusMap[config.indicatorName] === 'submitted') {
        completedCount++;
      }
    }
  }

  return (
    <Modal
      title={`${task?.systemName} - ${task?.period} 数据填写`}
      open={visible}
      onCancel={onClose}
      width={Math.max(900, 180 + indicatorConfigs.length * 140)}
      style={{ top: 20, minWidth: 900 }}
      bodyStyle={{ maxHeight: 'calc(100vh - 200px)', overflow: 'auto' }}
      destroyOnClose
      footer={
        isViewMode ? (
          <Button onClick={onClose}>关闭</Button>
        ) : (
          <Space>
            <Button icon={<FileExcelOutlined />} onClick={handleDownloadTemplate}>
              下载收数文档
            </Button>
            <Upload showUploadList={false} beforeUpload={handleUploadExcel}>
              <Button icon={<UploadOutlined />}>上传收数文档</Button>
            </Upload>
            <Button type="primary" icon={<CheckOutlined />} onClick={handleSubmit} loading={saving}>
              提交
            </Button>
          </Space>
        )
      }
    >
      <Card size="small" style={{ marginBottom: 16 }}>
        <Space direction="vertical" size="small">
          <Text>收数员：<strong>{user?.name}</strong> ({user?.empNo})</Text>
          <Text>负责指标数：<strong>{indicatorConfigs.length}</strong> 个指标 × <strong>{institutionRows.length}</strong> 个机构</Text>
          <Text>任务进度：已完成 <strong>{completedCount}</strong> / {totalCount}</Text>
          {totalCount - completedCount > 0 && <Text type="warning">还有 {totalCount - completedCount} 项待录入</Text>}
        </Space>
      </Card>

      {indicatorConfigs.length === 0 ? (
        <Text type="secondary">暂无分配的任务</Text>
      ) : (
        <Table
          dataSource={institutionRows}
          columns={columns}
          rowKey="institutionName"
          loading={loading}
          scroll={{ x: 180 + indicatorConfigs.length * 150, y: 500 }}
          pagination={false}
          size="small"
        />
      )}
    </Modal>
  );
}
