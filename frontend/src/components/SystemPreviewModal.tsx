import { useState, useEffect } from 'react';
import { Modal, Tabs, Table, Descriptions, Spin, message } from 'antd';
import { api } from '../api';

interface SystemPreviewModalProps {
  systemId: number | null;
  visible: boolean;
  onClose: () => void;
}

export default function SystemPreviewModal({ systemId, visible, onClose }: SystemPreviewModalProps) {
  const [loading, setLoading] = useState(false);
  const [systemInfo, setSystemInfo] = useState<any>(null);
  const [institutions, setInstitutions] = useState<any[]>([]);
  const [indicators, setIndicators] = useState<any[]>([]);

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
    } catch (error: any) {
      message.error(error.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
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
          ]}
        />
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
