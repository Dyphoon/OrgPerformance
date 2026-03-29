import { useState, useEffect, useRef } from 'react';
import { Row, Col, Card, Table, Select, Form, Button, message } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { api } from '../api';
import type { Monitoring } from '../types';
import * as echarts from 'echarts';
import { useAuth } from '../store/AuthContext';

export default function BranchReportPage() {
  const { user } = useAuth();
  const [systems, setSystems] = useState<any[]>([]);
  const [institutions, setInstitutions] = useState<any[]>([]);
  const [selectedSystem, setSelectedSystem] = useState<number | null>(null);
  const [monitorings, setMonitorings] = useState<Monitoring[]>([]);
  const [selectedMonitoringId, setSelectedMonitoringId] = useState<number | null>(null);
  const [selectedInstitutionId, setSelectedInstitutionId] = useState<number | null>(null);
  const [reportData, setReportData] = useState<any>(null);
  const dimensionChartRef = useRef<HTMLDivElement>(null);
  const categoryChartRef = useRef<HTMLDivElement>(null);
  const completionChartRef = useRef<HTMLDivElement>(null);
  const dimensionChart = useRef<echarts.ECharts | null>(null);
  const categoryChart = useRef<echarts.ECharts | null>(null);
  const completionChart = useRef<echarts.ECharts | null>(null);

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
      Promise.all([
        api.systems.getInstitutions(selectedSystem),
        api.monitorings.list({ systemId: selectedSystem, status: 'PUBLISHED', pageSize: 100 })
      ]).then(([instData, monData]) => {
        const insts = instData || [];
        const mons = monData.data || [];
        setInstitutions(insts);
        setMonitorings(mons);
        if (mons.length > 0) {
          setSelectedMonitoringId(mons[0].id);
        }
        // Auto-select institution
        if (user?.institutionId) {
          const userInst = insts.find((i: any) => i.id === user.institutionId);
          if (userInst) {
            setSelectedInstitutionId(user.institutionId);
          } else if (insts.length > 0) {
            setSelectedInstitutionId(insts[0].id);
          }
        } else if (insts.length > 0) {
          setSelectedInstitutionId(insts[0].id);
        }
      }).catch(() => {});
    } else {
      setInstitutions([]);
      setMonitorings([]);
    }
  }, [selectedSystem, user]);

  useEffect(() => {
    if (selectedMonitoringId && selectedInstitutionId) {
      api.reports.getInstitutionReport(selectedMonitoringId, selectedInstitutionId)
        .then(data => setReportData(data))
        .catch(() => {});
    } else {
      setReportData(null);
    }
  }, [selectedMonitoringId, selectedInstitutionId]);

  const handleDownload = async () => {
    if (selectedMonitoringId && selectedInstitutionId) {
      try {
        await api.reports.downloadInstitutionReport(selectedMonitoringId, selectedInstitutionId);
        message.success('下载成功');
      } catch (e: any) {
        message.error(e.message || '下载失败');
      }
    }
  };

  useEffect(() => {
    if (reportData && dimensionChartRef.current) {
      if (dimensionChart.current) dimensionChart.current.dispose();
      if (categoryChart.current) categoryChart.current.dispose();
      if (completionChart.current) completionChart.current.dispose();

      dimensionChart.current = echarts.init(dimensionChartRef.current);
      categoryChart.current = echarts.init(categoryChartRef.current);
      completionChart.current = echarts.init(completionChartRef.current);

      const dimOption = {
        title: { text: '维度得分分析', left: 'center' },
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          data: reportData.dimensionScores?.map((d: any) => ({ name: d.dimension, value: d.score })) || [],
          label: { show: true, formatter: '{b}: {c}' },
        }],
      };

      const catOption = {
        title: { text: '类别得分分析', left: 'center' },
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          data: reportData.categoryScores?.map((c: any) => ({ name: c.category, value: c.score })) || [],
          label: { show: true, formatter: '{b}: {c}' },
        }],
      };

      const completionOption = {
        title: { text: '指标完成度分析', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: reportData.indicators?.map((i: any) => i.level2Name || i.level1Name) || [] },
        yAxis: { type: 'value', name: '完成率%', max: 150 },
        series: [{
          type: 'bar',
          data: reportData.indicators?.map((i: any) => ({ 
            value: i.completionRate ? (i.completionRate * 100).toFixed(1) : 0 
          })) || [],
          itemStyle: { color: '#1890ff' },
          label: { show: true, position: 'top', formatter: '{c}%' },
        }],
      };

      dimensionChart.current.setOption(dimOption);
      categoryChart.current.setOption(catOption);
      completionChart.current.setOption(completionOption);
    }
  }, [reportData]);

  const indicatorColumns = [
    { title: '维度', dataIndex: 'dimension', key: 'dimension' },
    { title: '类别', dataIndex: 'category', key: 'category' },
    { title: '一级指标', dataIndex: 'level1Name', key: 'level1Name' },
    { title: '二级指标', dataIndex: 'level2Name', key: 'level2Name' },
    { title: '实际值', dataIndex: 'actualValue', key: 'actualValue', render: (v: number) => v ?? '-' },
    { title: '进度目标', dataIndex: 'progressTarget', key: 'progressTarget', render: (v: number) => v ?? '-' },
    { title: '进度完成率', dataIndex: 'progressCompletionRate', key: 'progressCompletionRate', render: (v: number) => v ? `${(v * 100).toFixed(1)}%` : '-' },
    { title: '得分', dataIndex: 'score', key: 'score', render: (v: number) => v?.toFixed(2) || '-' },
  ];

  return (
    <div>
      <h2>分支报表</h2>
      
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={16} justify="space-between" align="middle">
          <Col span={18}>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item label="体系" style={{ marginBottom: 0 }}>
                  <Select placeholder="请选择体系" value={selectedSystem} onChange={v => { setSelectedSystem(v); setSelectedInstitutionId(null); }} style={{ width: '100%' }}>
                    {systems.map(s => <Select.Option key={s.id} value={s.id}>{s.name}</Select.Option>)}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="月份" style={{ marginBottom: 0 }}>
                  <Select 
                    placeholder="请选择月份" 
                    value={selectedMonitoringId} 
                    onChange={v => setSelectedMonitoringId(v)} 
                    style={{ width: '100%' }}
                    disabled={!selectedSystem}
                  >
                    {monitorings.map(m => <Select.Option key={m.id} value={m.id}>{m.period}</Select.Option>)}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item label="机构" style={{ marginBottom: 0 }}>
                  <Select 
                    placeholder="请选择机构" 
                    value={selectedInstitutionId} 
                    onChange={setSelectedInstitutionId} 
                    style={{ width: '100%' }}
                    disabled={!selectedMonitoringId}
                  >
                    {institutions.map(i => <Select.Option key={i.id} value={i.id}>{i.orgName}</Select.Option>)}
                  </Select>
                </Form.Item>
              </Col>
            </Row>
          </Col>
          <Col>
            <Button 
              type="primary" 
              icon={<DownloadOutlined />} 
              onClick={handleDownload}
              disabled={!selectedMonitoringId || !selectedInstitutionId}
            >
              导出绩效报告
            </Button>
          </Col>
        </Row>
      </Card>

      {reportData && (
        <>
          <Row gutter={16}>
            <Col span={6}>
              <Card size="small">
                <p style={{ color: '#999', margin: 0 }}>总分</p>
                <h2 style={{ margin: '8px 0 0', color: '#1890ff' }}>{reportData.totalScore?.toFixed(2) || '-'}</h2>
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <p style={{ color: '#999', margin: 0 }}>总排名</p>
                <h2 style={{ margin: '8px 0 0', color: '#faad14' }}>{reportData.totalRank || '-'}</h2>
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <p style={{ color: '#999', margin: 0 }}>组内排名</p>
                <h2 style={{ margin: '8px 0 0', color: '#52c41a' }}>{reportData.groupRank || '-'}</h2>
              </Card>
            </Col>
            <Col span={6}>
              <Card size="small">
                <p style={{ color: '#999', margin: 0 }}>分组</p>
                <h3 style={{ margin: '8px 0 0' }}>{reportData.groupName || '-'}</h3>
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={12}>
              <Card title="维度得分饼状图">
                <div ref={dimensionChartRef} style={{ height: 300 }} />
              </Card>
            </Col>
            <Col span={12}>
              <Card title="类别得分饼状图">
                <div ref={categoryChartRef} style={{ height: 300 }} />
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card title="指标完成度分析">
                <div ref={completionChartRef} style={{ height: 300 }} />
              </Card>
            </Col>
          </Row>

          <Card title="指标明细" style={{ marginTop: 16 }}>
            <Table 
              dataSource={reportData.indicators} 
              columns={indicatorColumns} 
              rowKey={(r: any) => `${r.level1Name}-${r.level2Name}`}
              pagination={false}
              size="small"
              scroll={{ x: 'max-content' }}
            />
          </Card>
        </>
      )}

      {!selectedInstitutionId && (
        <div style={{ textAlign: 'center', color: '#999', padding: 48 }}>
          请选择体系、月份和机构查看报表
        </div>
      )}
    </div>
  );
}
