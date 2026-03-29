import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 30000,
});

api.interceptors.response.use(
  (response) => {
    const data = response.data;
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || 'Request failed'));
    }
    return data.data;
  },
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

export interface ReportFile {
  id: number;
  fileName: string;
  originalName: string;
  fileSize: number;
  fileType: string;
  minioUrl?: string;
  description?: string;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface ReportData {
  id: number;
  reportFileId: number;
  sheetName: string;
  rowNumber: number;
  category: string;
  itemName: string;
  itemCode: string;
  targetValue: number;
  actualValue: number;
  completionRate: number;
  unit: string;
  department: string;
  period: string;
  reportDate: string;
  rawData?: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export const reportApi = {
  upload: (file: File, description?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (description) formData.append('description', description);
    return api.post<ApiResponse<ReportFile>>('/reports/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getAll: () => api.get<ReportFile[]>('/reports'),

  getById: (id: number) => api.get<ReportFile>(`/reports/${id}`),

  getData: (id: number) => api.get<ReportData[]>(`/reports/${id}/data`),

  getDataByCategory: (id: number) =>
    api.get<Record<string, ReportData[]>>(`/reports/${id}/data/category`),

  getDataByDepartment: (id: number) =>
    api.get<Record<string, ReportData[]>>(`/reports/${id}/data/department`),

  getFilters: (id: number) =>
    api.get<{ categories: string[]; departments: string[]; periods: string[] }>(
      `/reports/${id}/filters`
    ),

  export: (id: number) =>
    api.get(`/reports/${id}/export`, { responseType: 'blob' }),

  delete: (id: number) => api.delete(`/reports/${id}`),
};

export default api;
