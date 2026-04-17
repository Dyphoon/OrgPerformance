const API_BASE = '/api';

interface AgentChatResponse {
  success: boolean;
  sessionId: string;
  message?: string;
  error?: string;
}

interface FileUploadResponse {
  success: boolean;
  sessionId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  contentLength: number;
  contentPreview: string;
  error?: string;
}

export interface UploadedFile {
  fileName: string;
  fileType: string;
  fileSize: number;
  contentLength: number;
  contentPreview: string;
  uploadedAt: number;
  templateFileKey?: string;
  isTemplate?: boolean;
  templateValid?: boolean;
}

export const agentApi = {
  chat: async (sessionId: string, message: string, templateFileKey?: string): Promise<AgentChatResponse> => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_BASE}/agent/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify({ sessionId, message, templateFileKey }),
    });

    if (response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }

    return response.json();
  },

  clearSession: async (sessionId: string): Promise<void> => {
    const token = localStorage.getItem('token');
    
    await fetch(`${API_BASE}/agent/clear`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify({ sessionId }),
    });
  },

  uploadFile: async (sessionId: string, file: File): Promise<FileUploadResponse> => {
    const token = localStorage.getItem('token');
    const formData = new FormData();
    formData.append('file', file);
    formData.append('sessionId', sessionId);

    const response = await fetch(`${API_BASE}/agent/upload`, {
      method: 'POST',
      headers: {
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: formData,
    });

    if (response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }

    return response.json();
  },

  getSessionFiles: async (sessionId: string): Promise<{ success: boolean; files: UploadedFile[]; count: number }> => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_BASE}/agent/files`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify({ sessionId }),
    });

    if (response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }

    return response.json();
  },

  clearSessionFiles: async (sessionId: string): Promise<void> => {
    const token = localStorage.getItem('token');
    
    await fetch(`${API_BASE}/agent/files/clear`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify({ sessionId }),
    });
  },

  removeFile: async (sessionId: string, fileName: string): Promise<boolean> => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_BASE}/agent/files/remove`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify({ sessionId, fileName }),
    });

    if (response.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      throw new Error('Unauthorized');
    }

    const result = await response.json();
    return result.success;
  },
};
