const API_BASE = '/api';

interface RequestOptions {
  method?: string;
  body?: any;
  headers?: Record<string, string>;
}

function buildUrl(url: string, params?: Record<string, any>): string {
  if (!params) return url;
  const searchParams = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.append(key, String(value));
    }
  }
  const queryString = searchParams.toString();
  return queryString ? `${url}?${queryString}` : url;
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const token = localStorage.getItem('token');
  
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${url}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 401) {
    localStorage.removeItem('token');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }

  const data = await response.json();
  
  if (data.code !== 200) {
    throw new Error(data.message || 'Request failed');
  }

  return data.data;
}

export const api = {
  auth: {
    login: (username: string, password: string) =>
      request<{ token: string; user: any; roles: string[] }>('/auth/login', {
        method: 'POST',
        body: { username, password },
      }),
    getCurrentUser: () =>
      request<any>('/auth/current-user'),
  },
  
  systems: {
    list: (params: { name?: string; status?: number; page?: number; pageSize?: number }) =>
      request<any>(buildUrl('/systems', params)),
    getById: (id: number) => request<any>(`/systems/${id}`),
    getInstitutions: (id: number) => request<any[]>(`/systems/${id}/institutions`),
    getIndicators: (id: number) => request<any[]>(`/systems/${id}/indicators`),
    getGroups: (id: number) => request<string[]>(`/systems/${id}/groups`),
    getTemplateUrl: (id: number) => request<string>(`/systems/${id}/template-url`),
    create: (data: any) => request<number>('/systems', { method: 'POST', body: data }),
    createWithFile: (name: string, description: string, needApproval: boolean, file: File) => {
      const formData = new FormData();
      formData.append('name', name);
      formData.append('description', description || '');
      formData.append('needApproval', String(needApproval));
      formData.append('file', file);
      return fetch(`${API_BASE}/systems/with-file`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
        body: formData,
      }).then(async r => {
        const data = await r.json();
        if (!r.ok || data.code !== 200) {
          const error = new Error(data.message || 'Request failed');
          (error as any).error = data;
          throw error;
        }
        return data;
      });
    },
    update: (id: number, data: any) => request<void>(`/systems/${id}`, { method: 'PUT', body: data }),
    delete: (id: number) => request<void>(`/systems/${id}`, { method: 'DELETE' }),
  },

  monitorings: {
    list: (params: { systemId?: number; status?: string; year?: number; month?: number; page?: number; pageSize?: number }) =>
      request<any>(buildUrl('/monitorings', params)),
    getById: (id: number) => request<any>(`/monitorings/${id}`),
    create: (data: any) => request<number>('/monitorings', { method: 'POST', body: data }),
    close: (id: number) => request<void>(`/monitorings/${id}/close`, { method: 'POST' }),
    startConfirming: (id: number) => request<void>(`/monitorings/${id}/start-confirming`, { method: 'POST' }),
    confirm: (id: number, institutionId: number, remark?: string) =>
      request<void>(`/monitorings/${id}/confirm?institutionId=${institutionId}${remark ? `&remark=${remark}` : ''}`, { method: 'POST' }),
    publish: (id: number) => request<void>(`/monitorings/${id}/publish`, { method: 'POST' }),
    getProcessStatus: (id: number) => request<any>(`/monitorings/${id}/process-status`),
    getTasks: (id: number, status?: string) =>
      request<any[]>(status ? `/monitorings/${id}/tasks?status=${status}` : `/monitorings/${id}/tasks`),
    getAllTasks: (id: number) => request<any[]>(`/monitorings/${id}/all-tasks`),
    getMyTasks: (id: number, collectorUserId: number) =>
      request<any[]>(`/monitorings/${id}/my-tasks?collectorUserId=${collectorUserId}`),
    submitTask: (taskId: number, actualValue: number) =>
      request<void>(`/monitorings/tasks/${taskId}`, { method: 'PUT', body: actualValue }),
    batchUpdateTasks: (updates: { taskId: number; actualValue: number }[]) =>
      request<void>('/monitorings/tasks/batch', { method: 'PUT', body: updates }),
    uploadDataCollection: (id: number, file: File) => {
      const formData = new FormData();
      formData.append('file', file);
      return fetch(`${API_BASE}/monitorings/${id}/collect/upload`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
        body: formData,
      }).then(r => r.json());
    },
    getCollectorFileUrl: (monitoringId: number, collectorUserId: number) =>
      request<string>(`/monitorings/${monitoringId}/collector-file?collectorUserId=${collectorUserId}`),
    fixCollectorTasks: (monitoringId: number) =>
      request<void>(`/monitorings/${monitoringId}/fix-collector-tasks`, { method: 'POST' }),
    generateReports: (monitoringId: number) =>
      request<number>(`/monitorings/${monitoringId}/generate-reports`, { method: 'POST' }),
    batchGenerate: (monitoringId: number) =>
      request<void>(`/monitorings/${monitoringId}/batch-generate`, { method: 'POST' }),
    getConfirmationTasks: (monitoringId: number) =>
      request<any[]>(`/monitorings/${monitoringId}/confirmation-tasks`),
    regenerateConfirmationTasks: (monitoringId: number) =>
      request<void>(`/monitorings/${monitoringId}/regenerate-confirmation-tasks`, { method: 'POST' }),
    rollback: (monitoringId: number) =>
      request<void>(`/monitorings/${monitoringId}/rollback`, { method: 'POST' }),
    rollbackToConfirming: (monitoringId: number) =>
      request<void>(`/monitorings/${monitoringId}/rollback-to-confirming`, { method: 'POST' }),
    delete: (monitoringId: number) =>
      request<void>(`/monitorings/${monitoringId}`, { method: 'DELETE' }),
  },

  reports: {
    getInstitutionReport: (monitoringId: number, institutionId: number) =>
      request<any>(`/reports/institution/${institutionId}?monitoringId=${monitoringId}`),
    getOverview: (monitoringId: number) => request<any>(`/reports/overview?monitoringId=${monitoringId}`),
    downloadInstitutionReport: async (monitoringId: number, institutionId: number) => {
      const token = localStorage.getItem('token');
      const response = await fetch(`/api/reports/institution/${institutionId}/download?monitoringId=${monitoringId}`, {
        headers: { 'Authorization': `Bearer ${token}` },
      });
      const contentType = response.headers.get('content-type');
      if (!response.ok || !contentType?.includes('application/vnd.openxmlformats')) {
        const err = await response.json().catch(() => ({ error: '下载失败' }));
        throw new Error(err.error || '下载失败');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const disposition = response.headers.get('content-disposition');
      const fileName = disposition?.match(/filename="(.+)"/)?.[1] || 'Performance_Report.xlsx';
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    },
  },

  notifications: {
    list: (userId: number) => request<any[]>(`/notifications?userId=${userId}`),
    getUnreadCount: (userId: number) => request<number>(`/notifications/unread-count?userId=${userId}`),
    markAsRead: (id: number) => request<void>(`/notifications/${id}/read`, { method: 'PUT' }),
  },
};
