import React, { useState, useEffect, useCallback } from 'react';
import { Layout, Table, Button, Modal, Tag, message, Space, Breadcrumb } from 'antd';
import { UploadOutlined, FileExcelOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { reportApi } from '../api/report';
import type { ReportFile, ReportData } from '../api/report';
import UploadPanel from '../components/UploadPanel';
import DataTable from '../components/DataTable';

const { Header, Content } = Layout;

const HomePage: React.FC = () => {
  const [reports, setReports] = useState<ReportFile[]>([]);
  const [selectedReport, setSelectedReport] = useState<ReportFile | null>(null);
  const [reportData, setReportData] = useState<ReportData[]>([]);
  const [filters, setFilters] = useState<{ categories: string[]; departments: string[]; periods: string[] }>({
    categories: [],
    departments: [],
    periods: [],
  });
  const [loading, setLoading] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [uploadVisible, setUploadVisible] = useState(false);

  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reportApi.getAll();
      setReports(res.data);
    } catch (error) {
      message.error('加载报表列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const loadReportData = async (report: ReportFile) => {
    setSelectedReport(report);
    setDataLoading(true);
    try {
      const [dataRes, filtersRes] = await Promise.all([
        reportApi.getData(report.id),
        reportApi.getFilters(report.id),
      ]);
      setReportData(dataRes.data);
      setFilters(filtersRes.data);
    } catch (error) {
      message.error('加载报表数据失败');
    } finally {
      setDataLoading(false);
    }
  };

  const handleExport = async () => {
    if (!selectedReport) return;
    try {
      const res = await reportApi.export(selectedReport.id);
      const blob = new Blob([res as unknown as BlobPart], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${selectedReport.originalName || 'report'}_export.xlsx`;
      link.click();
      window.URL.revokeObjectURL(url);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    }
  };

  const handleDelete = async (report: ReportFile) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除报表 "${report.originalName}" 吗？`,
      onOk: async () => {
        try {
          await reportApi.delete(report.id);
          message.success('删除成功');
          if (selectedReport?.id === report.id) {
            setSelectedReport(null);
            setReportData([]);
          }
          loadReports();
        } catch (error) {
          message.error('删除失败');
        }
      },
    });
  };

  const statusMap: Record<number, { color: string; text: string }> = {
    0: { color: 'default', text: '待处理' },
    1: { color: 'processing', text: '处理中' },
    2: { color: 'success', text: '已完成' },
    3: { color: 'error', text: '失败' },
  };

  const columns: ColumnsType<ReportFile> = [
    {
      title: '文件名',
      dataIndex: 'originalName',
      key: 'originalName',
      render: (text) => (
        <Space>
          <FileExcelOutlined />
          <span>{text}</span>
        </Space>
      ),
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      render: (size: number) => (size / 1024 / 1024).toFixed(2) + ' MB',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: number) => (
        <Tag color={statusMap[status]?.color}>{statusMap[status]?.text}</Tag>
      ),
    },
    {
      title: '上传时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (time: string) => new Date(time).toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => loadReportData(record)}>
            查看
          </Button>
          <Button type="link" size="small" danger onClick={() => handleDelete(record)}>
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', color: 'white' }}>
        <div style={{ color: 'white', fontSize: 20, fontWeight: 'bold', marginRight: 40 }}>
          企业报表系统
        </div>
        <Button
          type="primary"
          icon={<UploadOutlined />}
          onClick={() => setUploadVisible(true)}
        >
          上传 Excel
        </Button>
      </Header>
      <Content style={{ padding: 24 }}>
        <Breadcrumb style={{ marginBottom: 16 }}>
          <Breadcrumb.Item>报表管理</Breadcrumb.Item>
          {selectedReport && <Breadcrumb.Item>{selectedReport.originalName}</Breadcrumb.Item>}
        </Breadcrumb>

        {selectedReport ? (
          <div>
            <Button onClick={() => setSelectedReport(null)} style={{ marginBottom: 16 }}>
              返回列表
            </Button>
            <DataTable
              data={reportData}
              loading={dataLoading}
              onExport={handleExport}
              filters={filters}
            />
          </div>
        ) : (
          <Table
            columns={columns}
            dataSource={reports}
            rowKey="id"
            loading={loading}
            pagination={{ pageSize: 10 }}
          />
        )}
      </Content>

      <Modal
        title="上传 Excel 文件"
        open={uploadVisible}
        onCancel={() => setUploadVisible(false)}
        footer={null}
        width={600}
      >
        <UploadPanel
          onSuccess={() => {
            setUploadVisible(false);
            loadReports();
          }}
        />
      </Modal>
    </Layout>
  );
};

export default HomePage;
