import { useState, useEffect } from 'react';
import { Modal, Tabs, Table, Descriptions, Spin, message, Button, Space } from 'antd';
import * as XLSX from 'xlsx';
import { api } from '../api';
import { DownloadOutlined, FileExcelOutlined } from '@ant-design/icons';

interface SystemPreviewModalProps {
  systemId: number | null;
  visible: boolean;
  onClose: () => void;
}

interface SheetData {
  name: string;
  headers: string[];
  rows: any[][];
}

export default function SystemPreviewModal({ systemId, visible, onClose }: SystemPreviewModalProps) {
  const [loading, setLoading] = useState(false);
  const [systemInfo, setSystemInfo] = useState<any>(null);
  const [institutions, setInstitutions] = useState<any[]>([]);
  const [indicators, setIndicators] = useState<any[]>([]);
  const [sheetData, setSheetData] = useState<SheetData[]>([]);
  const [excelUrl, setExcelUrl] = useState<string | null>(null);

  useEffect(() => {
    if (visible && systemId) {
      loadData();
    }
  }, [visible, systemId]);

  const loadData = async () => {
    if (!systemId) return;
    setLoading(true);
    try {
      const [sysRes, instRes, indRes] = await Promise.all([
        api.systems.getById(systemId),
        api.systems.getInstitutions(systemId),
        api.systems.getIndicators(systemId),
      ]);
      setSystemInfo(sysRes);
      setInstitutions(instRes);
      setIndicators(indRes);

      // 获取并解析Excel模板
      try {
        const url = await api.systems.getTemplateUrl(systemId);
        setExcelUrl(url);
        
        const response = await fetch(url);
        const blob = await response.blob();
        const arrayBuffer = await blob.arrayBuffer();
        const workbook = XLSX.read(new Uint8Array(arrayBuffer), { type: 'array' });
        
        const sheets: SheetData[] = [];
        workbook.SheetNames.forEach(sheetName => {
          const sheet = workbook.Sheets[sheetName];
          const jsonData = XLSX.utils.sheet_to_json(sheet, { header: 1 }) as any[][];
          if (jsonData.length > 0) {
            sheets.push({
              name: sheetName,
              headers: jsonData[0] || [],
              rows: jsonData.slice(1),
            });
          }
        });
        setSheetData(sheets);
      } catch (e) {
        console.error('Failed to load template:', e);
        setExcelUrl(null);
        setSheetData([]);
      }
    } catch (error: any) {
      message.error(error.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = () => {
    if (excelUrl) {
      window.open(excelUrl, '_blank');
    }
  };

  const renderSheetPreview = (sheet: SheetData) => {
    if (!sheet.rows.length) {
      return <p style={{ textAlign: 'center', color: '#999' }}>暂无数据</p>;
    }

    const columns = sheet.headers.map((h, i) => ({
      title: h || `列${i + 1}`,
      dataIndex: `col${i}`,
      key: `col${i}`,
      width: 150,
      ellipsis: true,
    }));

    const data = sheet.rows.map((row, ri) => {
      const obj: any = { key: ri };
      row.forEach((cell, ci) => {
        obj[`col${ci}`] = cell ?? '';
      });
      return obj;
    });

    return (
      <Table
        size="small"
        columns={columns}
        dataSource={data}
        pagination={false}
        scroll={{ x: 'max-content', y: 400 }}
        style={{ maxHeight: 450, overflow: 'auto' }}
      />
    );
  };

  const tabItems = [
    {
      key: 'info',
      label: '基本信息',
      children: (
        <Descriptions bordered column={2}>
          <Descriptions.Item label="体系名称">{systemInfo?.name}</Descriptions.Item>
          <Descriptions.Item label="状态">{systemInfo?.status ? '启用' : '禁用'}</Descriptions.Item>
          <Descriptions.Item label="需要审批">{systemInfo?.needApproval ? '是' : '否'}</Descriptions.Item>
          <Descriptions.Item label="创建人">{systemInfo?.createdBy}</Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>{systemInfo?.description || '-'}</Descriptions.Item>
          <Descriptions.Item label="机构数量">{institutions.length}</Descriptions.Item>
          <Descriptions.Item label="指标数量">{indicators.length}</Descriptions.Item>
        </Descriptions>
      ),
    },
    {
      key: 'institutions',
      label: `机构 (${institutions.length})`,
      children: (
        <Table
          size="small"
          dataSource={institutions}
          rowKey="id"
          pagination={false}
          scroll={{ y: 400 }}
          columns={[
            { title: '机构名称', dataIndex: 'orgName', width: 150 },
            { title: '机构ID', dataIndex: 'orgId', width: 100 },
            { title: '分组', dataIndex: 'groupName', width: 100 },
            { title: '负责人', dataIndex: 'leaderName', width: 100 },
            { title: '工号', dataIndex: 'leaderEmpNo', width: 100 },
          ]}
        />
      ),
    },
    {
      key: 'indicators',
      label: `指标 (${indicators.length})`,
      children: (
        <Table
          size="small"
          dataSource={indicators}
          rowKey="id"
          pagination={false}
          scroll={{ x: 'max-content', y: 400 }}
          columns={[
            { title: '维度', dataIndex: 'dimension', width: 100 },
            { title: '类别', dataIndex: 'category', width: 100 },
            { title: '一级指标', dataIndex: 'level1Name', width: 120 },
            { title: '二级指标', dataIndex: 'level2Name', width: 120 },
            { title: '权重', dataIndex: 'weight', width: 80, render: (v: number) => v?.toFixed(4) },
            { title: '单位', dataIndex: 'unit', width: 80 },
            { title: '年度目标', dataIndex: 'annualTarget', width: 100 },
            { title: '进度目标', dataIndex: 'progressTarget', width: 100 },
          ]}
        />
      ),
    },
    {
      key: 'excel',
      label: `Excel模板`,
      children: (
        <div>
          <Space style={{ marginBottom: 12 }}>
            <Button 
              type="primary" 
              icon={<DownloadOutlined />} 
              onClick={handleDownload}
              disabled={!excelUrl}
            >
              下载Excel文件
            </Button>
            <span style={{ color: '#666', fontSize: 12 }}>
              {excelUrl ? '点击下载后在Excel/WPS中编辑' : '暂无模板文件'}
            </span>
          </Space>
          <Tabs
            items={sheetData.map((sheet, idx) => ({
              key: `sheet_${idx}`,
              label: sheet.name,
              children: (
                <div>
                  <div style={{ marginBottom: 8, color: '#999', fontSize: 12 }}>
                    <FileExcelOutlined /> {sheet.name} - {sheet.rows.length} 行数据
                  </div>
                  {renderSheetPreview(sheet)}
                </div>
              ),
            }))}
          />
        </div>
      ),
    },
  ];

  return (
    <Modal
      title="体系详情"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={1000}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <Tabs items={tabItems} />
      </Spin>
    </Modal>
  );
}
