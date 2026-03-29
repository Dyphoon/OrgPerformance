import React, { useState } from 'react';
import { Upload, Button, message, Space } from 'antd';
import { UploadOutlined, InboxOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';

const { Dragger } = Upload;

interface UploadPanelProps {
  onSuccess: () => void;
}

const UploadPanel: React.FC<UploadPanelProps> = ({ onSuccess }) => {
  const [loading, setLoading] = useState(false);

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    accept: '.xls,.xlsx',
    showUploadList: false,
    beforeUpload: async (file) => {
      setLoading(true);
      try {
        const { reportApi } = await import('../api/report');
        await reportApi.upload(file);
        message.success(`${file.name} 上传并解析成功`);
        onSuccess();
      } catch (error: any) {
        message.error(error?.response?.data?.message || '上传失败');
      } finally {
        setLoading(false);
      }
      return false;
    },
  };

  return (
    <div style={{ padding: 24 }}>
      <Dragger {...uploadProps} style={{ padding: 40 }}>
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽 Excel 文件上传</p>
        <p className="ant-upload-hint">
          支持 .xls 和 .xlsx 格式的 Excel 文件
        </p>
      </Dragger>
      <Space style={{ marginTop: 16 }}>
        <Button icon={<UploadOutlined />} loading={loading}>
          上传文件
        </Button>
      </Space>
    </div>
  );
};

export default UploadPanel;
