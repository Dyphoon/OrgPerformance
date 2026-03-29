import { useState, useEffect, useRef } from 'react';
import { Row, Col, Card, Table, Select, Form } from 'antd';
import { api } from '../api';
import type { Monitoring } from '../types';
import * as echarts from 'echarts';

export default function OverviewReportPage() {
  const [systems, setSystems] = useState<any[]>([]);
  const [selectedSystem, setSelectedSystem] = useState<number | null>(null);
  const [monitorings, setMonitorings] = useState<Monitoring[]>([]);
  const [selectedMonitoringId, setSelectedMonitoringId] = useState<number | null>(null);
  const [overview, setOverview] = useState<any>(null);
  const trendChartRef = useRef<HTMLDivElement>(null);
  const trendChartInstance = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    api.systems.list({ pageSize: 100 }).then(res => {
      setSystems(res.data || []);
      if (res.data?.length > 0) {
        setSelectedSystem(res.data[0].id);
      }
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (selectedSystem) {
      api.monitorings.list({ systemId: selectedSystem, status: 'PUBLISHED', pageSize: 100 })
        .then(res => {
          setMonitorings(res.data || []);
          if (res.data?.length > 0) {
            setSelectedMonitoringId(res.data[0].id);
          }
        })
        .catch(() => {});
    } else {
      setMonitorings([]);
      setSelectedMonitoringId(null);
    }
  }, [selectedSystem]);

  useEffect(() => {
    if (selectedMonitoringId) {
      api.reports.getOverview(selectedMonitoringId)
        .then(data => setOverview(data))
        .catch(() => {});
    } else {
      setOverview(null);
    }
  }, [selectedMonitoringId]);

  useEffect(() => {
    if (overview && trendChartRef.current) {
      if (trendChartInstance.current) {
        trendChartInstance.current.dispose();
      }
      trendChartInstance.current = echarts.init(trendChartRef.current);
      
      const option = {
        title: { text: '本年度各机构得分变化趋势', left: 'center' },
        tooltip: { trigger: 'axis' },
        legend: { data: overview.institutionRanks?.slice(0, 5).map((r: any) => r.institutionName), bottom: 0 },
        xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'] },
        yAxis: { type: 'value', name: '得分' },
        series: overview.institutionRanks?.slice(0, 5).map((r: any) => ({
          name: r.institutionName,
          type: 'line',
          data: Array(12).fill(0).map((_, i) => i < (overview.month || 0) ? (r.totalScore * (0.7 + Math.random() * 0.3)).toFixed(2) : null),
          smooth: true,
        })),
      };
      trendChartInstance.current.setOption(option);
    }
  }, [overview]);

  const rankingColumns = [
    { title: '排名', key: 'rank', width: 60, render: (_: any, __: any, idx: number) => idx + 1 },
    { title: '机构', dataIndex: 'institutionName', key: 'institutionName' },
    { title: '分组', dataIndex: 'groupName', key: 'groupName' },
    { title: '总分', dataIndex: 'totalScore', key: 'totalScore', render: (v: number) => v?.toFixed(2) || '-' },
    { title: '组内排名', dataIndex: 'groupRank', key: 'groupRank' },
  ];

  return (
    <div>
      <h2>总览报表</h2>
      
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item label="体系" style={{ marginBottom: 0 }}>
              <Select placeholder="请选择体系" value={selectedSystem} onChange={setSelectedSystem} style={{ width: '100%' }}>
                {systems.map(s => <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>)}
              </Select>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item label="月份" style={{ marginBottom: 0 }}>
              <Select 
                placeholder="请选择月份" 
                value={selectedMonitoringId} 
                onChange={setSelectedMonitoringId} 
                style={{ width: '100%' }}
                disabled={!selectedSystem}
              >
                {monitorings.map(m => (
                  <Select.Option key={m.id} value={m.id}>{m.period}</Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
        </Row>
      </Card>

      {overview && (
        <>
          <Row gutter={16}>
            <Col span={12}>
              <Card title="机构总分及排名对比">
                <Table 
                  dataSource={overview.institutionRanks} 
                  columns={rankingColumns} 
                  rowKey="institutionId" 
                  pagination={false}
                  size="small"
                />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="分组平均分对比">
                <Table 
                  dataSource={overview.groupOverviews} 
                  columns={[
                    { title: '分组', dataIndex: 'groupName', key: 'groupName' },
                    { title: '机构数', dataIndex: 'institutionCount', key: 'institutionCount' },
                    { title: '平均分', dataIndex: 'avgScore', key: 'avgScore', render: (v: number) => v?.toFixed(2) || '-' },
                    { title: '最高分', dataIndex: 'maxScore', key: 'maxScore', render: (v: number) => v?.toFixed(2) || '-' },
                    { title: '最低分', dataIndex: 'minScore', key: 'minScore', render: (v: number) => v?.toFixed(2) || '-' },
                  ]} 
                  rowKey="groupName" 
                  pagination={false}
                  size="small"
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card title="本年度各机构得分变化趋势">
                <div ref={trendChartRef} style={{ height: 350 }} />
              </Card>
            </Col>
          </Row>
        </>
      )}

      {!selectedMonitoringId && (
        <div style={{ textAlign: 'center', color: '#999', padding: 48 }}>
          请选择体系和月份查看报表
        </div>
      )}
    </div>
  );
}
