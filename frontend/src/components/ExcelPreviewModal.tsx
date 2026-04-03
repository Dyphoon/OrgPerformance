import { useState, useEffect, useRef, useCallback } from 'react';
import { Modal, Spin, message, Button } from 'antd';
import * as XLSX from 'xlsx';
import { api } from '../api';
import { DownloadOutlined, FileExcelOutlined } from '@ant-design/icons';

declare global {
  interface Window {
    luckysheet: any;
  }
}

interface ExcelPreviewModalProps {
  systemId: number | null;
  visible: boolean;
  onClose: () => void;
}

const LUCKYSHEET_CONTAINER_ID = 'luckysheet-excel-preview';

export default function ExcelPreviewModal({ systemId, visible, onClose }: ExcelPreviewModalProps) {
  const [loading, setLoading] = useState(false);
  const [systemInfo, setSystemInfo] = useState<any>(null);
  const [excelUrl, setExcelUrl] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const sheetDataRef = useRef<any[]>([]);
  const initAttemptRef = useRef(0);

  useEffect(() => {
    if (visible && systemId) {
      loadData();
    }
    if (!visible) {
      destroyLuckysheet();
      setReady(false);
      sheetDataRef.current = [];
    }
  }, [visible, systemId]);

  useEffect(() => {
    return () => {
      destroyLuckysheet();
    };
  }, []);

  const destroyLuckysheet = useCallback(() => {
    try {
      if (window.luckysheet) {
        window.luckysheet.destroy();
      }
    } catch (e) {
      console.error('Destroy luckysheet error:', e);
    }
  }, []);

  const loadData = async () => {
    if (!systemId) return;
    setLoading(true);
    destroyLuckysheet();
    setReady(false);
    sheetDataRef.current = [];

    try {
      const sysRes = await api.systems.getById(systemId);
      setSystemInfo(sysRes);

      const url = await api.systems.getTemplateUrl(systemId);
      setExcelUrl(url);

      const response = await fetch(url);
      const blob = await response.blob();
      const arrayBuffer = await blob.arrayBuffer();
      const workbook = XLSX.read(new Uint8Array(arrayBuffer), { type: 'array' });

      const data = convertToLuckysheetData(workbook);
      sheetDataRef.current = data;
      initAttemptRef.current = 0;

      setTimeout(() => {
        setReady(true);
      }, 100);
    } catch (error: any) {
      console.error('Failed to load template:', error);
      message.error(error.message || '加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (ready && sheetDataRef.current.length > 0 && visible) {
      initLuckysheet();
    }
  }, [ready, visible]);

  const convertToLuckysheetData = (workbook: XLSX.WorkBook) => {
    const sheets: any[] = [];

    workbook.SheetNames.forEach((sheetName, index) => {
      const sheet = workbook.Sheets[sheetName];
      const ref = sheet['!ref'] || 'A1';
      const range = XLSX.utils.decode_range(ref);
      const rowCount = Math.min(range.e.r + 1, 500);
      const colCount = Math.min(range.e.c + 1, 50);

      const celldata: any[] = [];
      for (let r = 0; r < rowCount; r++) {
        for (let c = 0; c < colCount; c++) {
          const cellAddress = XLSX.utils.encode_cell({ r, c });
          const cell = sheet[cellAddress];

          if (cell && cell.v !== undefined && cell.v !== null) {
            let cellValue = cell.v;
            let cellText = cell.w || String(cellValue);

            celldata.push({
              r: r,
              c: c,
              v: {
                v: cellValue,
                m: cellText,
                ct: { fa: 'General', t: 'g' },
              },
            });
          }
        }
      }

      const config: any = {};

      if (sheet['!merges'] && sheet['!merges'].length > 0) {
        const mergeObj: any = {};
        sheet['!merges'].forEach((merge) => {
          mergeObj[`${merge.s.r}_${merge.s.c}`] = {
            r: merge.s.r,
            c: merge.s.c,
            rs: merge.e.r - merge.s.r + 1,
            cs: merge.e.c - merge.s.c + 1,
          };
        });
        config.merge = mergeObj;
      }

      if (sheet['!cols']) {
        const colLen: Record<number, number> = {};
        sheet['!cols'].forEach((col: any, i: number) => {
          if (col && col.wch) {
            colLen[i] = Math.round(col.wch * 8);
          }
        });
        if (Object.keys(colLen).length > 0) {
          config.columnlen = colLen;
        }
      }

      sheets.push({
        name: sheetName,
        index: `sheet_${index}`,
        status: index === 0 ? 1 : 0,
        order: index,
        celldata: celldata,
        config: config,
        row: rowCount,
        column: colCount,
      });
    });

    return sheets;
  };

  const initLuckysheet = () => {
    if (!window.luckysheet) {
      if (initAttemptRef.current < 3) {
        initAttemptRef.current++;
        setTimeout(initLuckysheet, 500);
        return;
      }
      message.error('Luckysheet 未加载，请刷新页面重试');
      return;
    }

    const container = document.getElementById(LUCKYSHEET_CONTAINER_ID);
    if (!container) {
      setTimeout(initLuckysheet, 100);
      return;
    }

    try {
      window.luckysheet.destroy();
    } catch (e) {
      // ignore
    }

    const options = {
      container: LUCKYSHEET_CONTAINER_ID,
      data: sheetDataRef.current,
      showinfobar: false,
      showsheetbar: sheetDataRef.current.length > 1,
      showsheetbarConfig: {
        add: false,
        menu: false,
      },
      showstatisticBar: false,
      showstatisticBarConfig: {
        count: false,
        view: false,
        zoom: false,
      },
      enableAddRow: false,
      enableAddBackTop: false,
      userInfo: false,
      showConfigWindowResize: false,
      forceCalculation: false,
      rowHeaderWidth: 46,
      columnHeaderHeight: 20,
      defaultColWidth: 73,
      defaultRowHeight: 19,
      allowEdit: false,
    };

    try {
      window.luckysheet.create(options);
    } catch (e) {
      console.error('Init luckysheet error:', e);
      message.error('初始化预览失败');
    }
  };

  const handleDownload = () => {
    if (excelUrl) {
      window.open(excelUrl, '_blank');
    }
  };

  const handleModalClose = () => {
    destroyLuckysheet();
    setReady(false);
    onClose();
  };

  return (
    <Modal
      title={
        <span>
          <FileExcelOutlined style={{ marginRight: 8, color: '#52c41a' }} />
          体系模板预览 {systemInfo ? `- ${systemInfo.name}` : ''}
        </span>
      }
      open={visible}
      onCancel={handleModalClose}
      width={1200}
      style={{ top: 20 }}
      styles={{ body: { height: 600, padding: 0, overflow: 'hidden' } }}
      footer={[
        <Button key="download" icon={<DownloadOutlined />} onClick={handleDownload} disabled={!excelUrl}>
          下载模板
        </Button>,
        <Button key="close" onClick={handleModalClose}>
          关闭
        </Button>,
      ]}
    >
      <Spin spinning={loading} tip="加载中...">
        <div
          id={LUCKYSHEET_CONTAINER_ID}
          style={{
            width: '100%',
            height: 600,
            position: 'relative',
          }}
        />
      </Spin>
    </Modal>
  );
}
