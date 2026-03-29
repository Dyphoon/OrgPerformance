import React, { useState, useEffect } from 'react';
import { Table, Tag, Space, Button, Card, Select, Row, Col, Statistic } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { ReportData } from '../api/report';
import ChartPanel from './ChartPanel';

interface DataTableProps {
  data: ReportData[];
  loading: boolean;
  onExport: () => void;
  filters: {
    categories: string[];
    departments: string[];
    periods: string[];
  };
}

const DataTable: React.FC<DataTableProps> = ({ data, loading, onExport, filters }) => {
  const [filteredData, setFilteredData] = useState<ReportData[]>(data);
  const [category, setCategory] = useState<string | undefined>();
  const [department, setDepartment] = useState<string | undefined>();
  const [viewMode, setViewMode] = useState<'table' | 'chart'>('table');

  useEffect(() => {
    let filtered = [...data];
    if (category) filtered = filtered.filter((d) => d.category === category);
    if (department) filtered = filtered.filter((d) => d.department === department);
    setFilteredData(filtered);
  }, [data, category, department]);

  const columns: ColumnsType<ReportData> = [
    { title: '类别', dataIndex: 'category', key: 'category', fixed: 'left', width: 120 },
    { title: '编码', dataIndex: 'itemCode', key: 'itemCode', width: 100 },
    { title: '指标名称', dataIndex: 'itemName', key: 'itemName', width: 180 },
    {
      title: '目标值',
      dataIndex: 'targetValue',
      key: 'targetValue',
      width: 100,
      render: (val: number) => val?.toLocaleString() || '-',
    },
    {
      title: '实际值',
      dataIndex: 'actualValue',
      key: 'actualValue',
      width: 100,
      render: (val: number) => val?.toLocaleString() || '-',
    },
    {
      title: '完成率',
      dataIndex: 'completionRate',
      key: 'completionRate',
      width: 100,
      render: (val: number) => (
        <Tag color={val >= 100 ? 'green' : val >= 80 ? 'blue' : 'red'}>
          {val?.toFixed(1)}%
        </Tag>
      ),
    },
    { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
    { title: '部门', dataIndex: 'department', key: 'department', width: 120 },
    { title: '周期', dataIndex: 'period', key: 'period', width: 100 },
  ];

  const avgCompletion =
    filteredData.length > 0
      ? filteredData.reduce((sum, d) => sum + (d.completionRate || 0), 0) / filteredData.length
      : 0;

  return (
    <div>
      <Card>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Statistic title="数据条数" value={filteredData.length} />
          </Col>
          <Col span={6}>
            <Statistic
              title="平均完成率"
              value={avgCompletion.toFixed(1)}
              suffix="%"
              valueStyle={{ color: avgCompletion >= 100 ? '#52c41a' : '#faad14' }}
            />
          </Col>
          <Col span={12} style={{ textAlign: 'right' }}>
            <Space>
              <Select
                placeholder="按类别筛选"
                allowClear
                style={{ width: 150 }}
                onChange={setCategory}
                options={filters.categories.map((c) => ({ label: c, value: c }))}
              />
              <Select
                placeholder="按部门筛选"
                allowClear
                style={{ width: 150 }}
                onChange={setDepartment}
                options={filters.departments.map((d) => ({ label: d, value: d }))}
              />
              <Button onClick={() => setViewMode(viewMode === 'table' ? 'chart' : 'table')}>
                {viewMode === 'table' ? '图表视图' : '表格视图'}
              </Button>
              <Button type="primary" icon={<DownloadOutlined />} onClick={onExport}>
                导出
              </Button>
            </Space>
          </Col>
        </Row>

        {viewMode === 'table' ? (
          <Table
            columns={columns}
            dataSource={filteredData}
            rowKey="id"
            loading={loading}
            scroll={{ x: 1200, y: 500 }}
            pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
          />
        ) : (
          <ChartPanel
            data={filteredData.map((d) => ({
              itemName: d.itemName,
              targetValue: d.targetValue || 0,
              actualValue: d.actualValue || 0,
              completionRate: d.completionRate || 0,
            }))}
            title="报表数据可视化"
          />
        )}
      </Card>
    </div>
  );
};

export default DataTable;
