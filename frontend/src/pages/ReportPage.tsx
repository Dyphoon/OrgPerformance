import { useState, useEffect } from 'react';
import { Row, Col, Card, Table, Select } from 'antd';
import { api } from '../api';
import type { Monitoring, Overview, Report } from '../types';
import * as echarts from 'echarts';

export default function ReportPage() {
  const [monitorings, setMonitorings] = useState<Monitoring[]>([]);
  const [selectedMonitoring, setSelectedMonitoring] = useState<number | null>(null);
  const [overview, setOverview] = useState<Overview | null>(null);
  const [reports, setReports] = useState<Record<number, Report>>({});
  const [selectedInstitution, setSelectedInstitution] = useState<number | null>(null);

  useEffect(() => {
    api.monitorings.list({ pageSize: 100, status: 'PUBLISHED' })
      .then(res => setMonitorings(res.data || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (selectedMonitoring) {
      api.reports.getOverview(selectedMonitoring)
        .then(setOverview)
        .catch(() => {});
    }
  }, [selectedMonitoring]);

  useEffect(() => {
    if (selectedMonitoring && selectedInstitution) {
      api.reports.getInstitutionReport(selectedMonitoring, selectedInstitution)
        .then(data => setReports(prev => ({ ...prev, [selectedInstitution]: data })))
        .catch(() => {});
    }
  }, [selectedMonitoring, selectedInstitution]);

  useEffect(() => {
    if (overview) {
      const chartDom = document.getElementById('rankChart');
      if (chartDom) {
        const myChart = echarts.init(chartDom);
        const option = {
          title: { text: '机构排名', left: 'center' },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          xAxis: { type: 'value', name: '得分' },
          yAxis: { type: 'category', data: overview.institutionRanks.map(r => r.institutionName).reverse(), inverse: true },
          series: [{
            type: 'bar',
            data: overview.institutionRanks.map(r => ({ value: r.totalScore, itemStyle: { color: '#2E4057' } })).reverse(),
            label: { show: true, position: 'right' },
          }],
        };
        myChart.setOption(option);
      }
    }
  }, [overview]);

  useEffect(() => {
    if (overview) {
      const chartDom = document.getElementById('groupChart');
      if (chartDom) {
        const myChart = echarts.init(chartDom);
        const option = {
          title: { text: '分组对比', left: 'center' },
          tooltip: { trigger: 'item' },
          xAxis: { type: 'category', data: overview.groupOverviews.map(g => g.groupName) },
          yAxis: { type: 'value', name: '平均分' },
          series: [{
            type: 'bar',
            data: overview.groupOverviews.map(g => ({ value: g.avgScore, itemStyle: { color: '#048A81' } })),
            label: { show: true, position: 'top' },
          }],
        };
        myChart.setOption(option);
      }
    }
  }, [overview]);

  const columns = [
    { title: '排名', key: 'rank', width: 60, render: (_: any, record: any) => record.rank },
    { title: '机构', dataIndex: 'institutionName', key: 'institutionName' },
    { title: '分组', dataIndex: 'groupName', key: 'groupName' },
    { title: '总分', dataIndex: 'totalScore', key: 'totalScore', render: (v: number) => v?.toFixed(2) || '-' },
    { title: '组内排名', key: 'groupRank', render: (_: any, record: any) => record.groupRank },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <a onClick={() => setSelectedInstitution(record.institutionId)}>查看详情</a>
      ),
    },
  ];

  const indicatorColumns = [
    { title: '维度', dataIndex: 'dimension', key: 'dimension' },
    { title: '类别', dataIndex: 'category', key: 'category' },
    { title: '一级指标', dataIndex: 'level1Name', key: 'level1Name' },
    { title: '二级指标', dataIndex: 'level2Name', key: 'level2Name' },
    { title: '实际值', dataIndex: 'actualValue', key: 'actualValue' },
    { title: '进度目标', dataIndex: 'progressTarget', key: 'progressTarget' },
    { title: '进度完成率', dataIndex: 'progressCompletionRate', key: 'progressCompletionRate', render: (v: number) => v ? `${(v * 100).toFixed(1)}%` : '-' },
    { title: '得分', dataIndex: 'score', key: 'score', render: (v: number) => v?.toFixed(2) || '-' },
  ];

  const currentReport = selectedInstitution ? reports[selectedInstitution] : null;

  return (
    <div>
      <h2>绩效报表</h2>
      
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={8}>
            <Select
              placeholder="选择监测"
              value={selectedMonitoring}
              onChange={setSelectedMonitoring}
              style={{ width: '100%' }}
            >
              {monitorings.map(m => (
                <Select.Option key={m.id} value={m.id}>
                  {m.systemName} - {m.period}
                </Select.Option>
              ))}
            </Select>
          </Col>
        </Row>
      </Card>

      {overview && (
        <Row gutter={16}>
          <Col span={12}>
            <Card title="机构排名">
              <Table 
                dataSource={overview.institutionRanks} 
                columns={columns} 
                rowKey="institutionId" 
                pagination={false}
                size="small"
              />
            </Card>
          </Col>
          <Col span={12}>
            <Card title="排名图表">
              <div id="rankChart" style={{ height: 300 }} />
            </Card>
          </Col>
        </Row>
      )}

      {overview && (
        <Row gutter={16} style={{ marginTop: 16 }}>
          <Col span={24}>
            <Card title="分组对比">
              <div id="groupChart" style={{ height: 300 }} />
            </Card>
          </Col>
        </Row>
      )}

      {currentReport && (
        <Card title={`${currentReport.institutionName} - 详细数据`} style={{ marginTop: 16 }}>
          <Row gutter={16}>
            <Col span={6}>
              <Card size="small">
                <p>总分</p>
                <h3>{currentReport.totalScore?.toFixed(2) || '-'}</h3>
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <p>总排名</p>
                <h3>{currentReport.totalRank || '-'}</h3>
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <p>组内排名</p>
                <h3>{currentReport.groupRank || '-'}</h3>
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <p>分组</p>
                <h3>{currentReport.groupName}</h3>
              </Card>
            </Col>
          </Row>

          <Table 
            dataSource={currentReport.indicators} 
            columns={indicatorColumns} 
            rowKey={(record: any) => `${record.level1Name}-${record.level2Name}`}
            pagination={false}
            size="small"
            style={{ marginTop: 16 }}
          />
        </Card>
      )}

      {!selectedMonitoring && (
        <div style={{ textAlign: 'center', color: '#999', padding: 48 }}>
          请选择监测任务查看报表
        </div>
      )}
    </div>
  );
}
