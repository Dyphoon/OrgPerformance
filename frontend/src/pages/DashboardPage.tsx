import { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Table, Tag } from 'antd';
import { api } from '../api';
import { useAuth } from '../store/AuthContext';
import type { System, Monitoring } from '../types';

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const [systems, setSystems] = useState<System[]>([]);
  const [monitorings, setMonitorings] = useState<Monitoring[]>([]);

  useEffect(() => {
    if (hasRole('admin')) {
      api.systems.list({ pageSize: 5 }).then(res => setSystems(res.data || [])).catch(() => {});
      api.monitorings.list({ pageSize: 5 }).then(res => setMonitorings(res.data || [])).catch(() => {});
    }
  }, [hasRole]);

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      PENDING: 'default',
      COLLECTING: 'processing',
      CLOSED: 'warning',
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
      CLOSED: '已截止',
      PROCESSING: '处理中',
      CONFIRMING: '待确认',
      PUBLISHED: '已发布',
    };
    return texts[status] || status;
  };

  const monitoringColumns = [
    { title: '体系', dataIndex: 'systemName', key: 'systemName' },
    { title: '期间', dataIndex: 'period', key: 'period' },
    { title: '状态', dataIndex: 'status', key: 'status', 
      render: (status: string) => <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag> },
    { title: '进度', dataIndex: 'processPercent', key: 'processPercent',
      render: (p: number) => `${p || 0}%` },
  ];

  const systemColumns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '机构数', dataIndex: 'institutionCount', key: 'institutionCount' },
    { title: '指标数', dataIndex: 'indicatorCount', key: 'indicatorCount' },
    { title: '状态', dataIndex: 'status', key: 'status',
      render: (s: number) => <Tag color={s ? 'success' : 'default'}>{s ? '启用' : '禁用'}</Tag> },
  ];

  return (
    <div>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>欢迎回来，{user?.name}</h1>
      
      <Row gutter={16}>
        {hasRole('admin') && (
          <>
            <Col span={6}>
              <Card><Statistic title="考核体系" value={systems.length} /></Card>
            </Col>
            <Col span={6}>
              <Card><Statistic title="进行中监测" value={monitorings.filter(m => m.status !== 'PUBLISHED').length} /></Card>
            </Col>
            <Col span={6}>
              <Card><Statistic title="已完成监测" value={monitorings.filter(m => m.status === 'PUBLISHED').length} /></Card>
            </Col>
          </>
        )}
        <Col span={6}>
          <Card><Statistic title="我的待办" value="-" /></Card>
        </Col>
      </Row>

      {hasRole('admin') && (
        <Row gutter={16} style={{ marginTop: 24 }}>
          <Col span={12}>
            <Card title="最新考核体系" size="small">
              <Table dataSource={systems} columns={systemColumns} rowKey="id" pagination={false} size="small" />
            </Card>
          </Col>
          <Col span={12}>
            <Card title="最新监测任务" size="small">
              <Table dataSource={monitorings} columns={monitoringColumns} rowKey="id" pagination={false} size="small" />
            </Card>
          </Col>
        </Row>
      )}
    </div>
  );
}
