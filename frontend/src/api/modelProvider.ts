const API_BASE = '/api';

export interface ModelProvider {
  id: number;
  name: string;
  code: string;
  baseUrl: string;
  apiKey: string;
  modelName: string;
  modelType: string;
  maxTokens: number;
  temperature: number;
  sortOrder: number;
  status: number;
  isDefault: boolean;
}

export const modelProviderApi = {
  list: () => request<ModelProvider[]>('/model-providers'),
  listActive: () => request<ModelProvider[]>('/model-providers/active'),
  getDefault: () => request<ModelProvider>('/model-providers/default'),
  getById: (id: number) => request<ModelProvider>(`/model-providers/${id}`),
  create: (data: Partial<ModelProvider>) =>
    request<ModelProvider>('/model-providers', { method: 'POST', body: data }),
  update: (id: number, data: Partial<ModelProvider>) =>
    request<ModelProvider>(`/model-providers/${id}`, { method: 'PUT', body: data }),
  delete: (id: number) =>
    request<void>(`/model-providers/${id}`, { method: 'DELETE' }),
  setDefault: (id: number) =>
    request<void>(`/model-providers/${id}/default`, { method: 'PUT' }),
  init: () => request<string>('/model-providers/init', { method: 'POST' }),
};

interface RequestOptions {
  method?: string;
  body?: any;
  headers?: Record<string, string>;
}

function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const token = localStorage.getItem('token');

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return fetch(`${API_BASE}${url}`, {
    method: options.method || 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  }).then(async response => {
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
  });
}