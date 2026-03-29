import React from 'react';
import ReactECharts from 'echarts-for-react';
import { Card, Row, Col } from 'antd';

interface ChartData {
  itemName: string;
  targetValue: number;
  actualValue: number;
  completionRate: number;
}

interface ChartPanelProps {
  data: ChartData[];
  title: string;
}

const ChartPanel: React.FC<ChartPanelProps> = ({ data, title }) => {
  const getCompletionOption = () => ({
    title: { text: '目标完成率对比', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['目标值', '实际值'], top: 30 },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.itemName),
      axisLabel: { rotate: 45, interval: 0 },
    },
    yAxis: { type: 'value', name: '数值' },
    series: [
      {
        name: '目标值',
        type: 'bar',
        data: data.map((d) => d.targetValue),
        itemStyle: { color: '#1890ff' },
      },
      {
        name: '实际值',
        type: 'bar',
        data: data.map((d) => d.actualValue),
        itemStyle: { color: '#52c41a' },
      },
    ],
    grid: { bottom: 80 },
  });

  const getRateOption = () => ({
    title: { text: '完成率分布', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: data.map((d) => ({
          name: d.itemName,
          value: d.completionRate?.toFixed(1) || 0,
        })),
        label: { formatter: '{b}: {d}%' },
      },
    ],
  });

  const getGaugeOption = () => ({
    title: { text: '整体完成率', left: 'center', top: 10 },
    series: [
      {
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        splitNumber: 10,
        itemStyle: { color: '#52c41a' },
        progress: {
          show: true,
          width: 30,
        },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 30 } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { distance: 40, fontSize: 20 },
        anchor: { show: false },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 36,
          offsetCenter: [0, '70%'],
          formatter: '{value}%',
          color: '#52c41a',
        },
        data: [
          {
            value:
              data.length > 0
                ? (
                    data.reduce((sum, d) => sum + (d.completionRate || 0), 0) / data.length
                  ).toFixed(1)
                : 0,
          },
        ],
      },
    ],
  });

  return (
    <div>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title={title}>
            <ReactECharts option={getCompletionOption()} style={{ height: 400 }} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title={title}>
            <ReactECharts option={getRateOption()} style={{ height: 400 }} />
          </Card>
        </Col>
        <Col xs={24}>
          <Card>
            <ReactECharts option={getGaugeOption()} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default ChartPanel;
