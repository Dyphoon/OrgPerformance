import { request } from './index';

export interface Skill {
  id: number;
  name: string;
  description: string;
  icon: string;
  category: string;
  prompt: string;
  tools: string;
  installed: boolean;
  markdownContent?: string;
  scriptPath?: string;
  scriptContent?: string;
  version?: string;
  author?: string;
}

export interface SkillFormData {
  name: string;
  description: string;
  icon: string;
  category: string;
  prompt: string;
  tools: string;
  markdownContent: string;
  scriptContent: string;
  version: string;
  author: string;
}

export interface ToolInfo {
  name: string;
  description: string;
  category: string;
}

export const skillApi = {
  getAllSkills: (category?: string) => {
    const url = category ? `/skills?category=${encodeURIComponent(category)}` : '/skills';
    return request<Skill[]>(url);
  },

  getCategories: () => {
    return request<string[]>('/skills/categories');
  },

  getAvailableTools: () => {
    return request<ToolInfo[]>('/skills/tools');
  },

  getInstalledSkills: () => {
    return request<Skill[]>('/skills/installed');
  },

  getSkillById: (id: number) => {
    return request<Skill>(`/skills/${id}`);
  },

  createSkill: (data: SkillFormData) => {
    return request<number>('/skills', { method: 'POST', body: data });
  },

  updateSkill: (id: number, data: SkillFormData) => {
    return request<void>(`/skills/${id}`, { method: 'PUT', body: data });
  },

  deleteSkill: (id: number) => {
    return request<void>(`/skills/${id}`, { method: 'DELETE' });
  },

  installSkill: (id: number) => {
    return request<void>(`/skills/${id}/install`, { method: 'POST' });
  },

  uninstallSkill: (id: number) => {
    return request<void>(`/skills/${id}/uninstall`, { method: 'POST' });
  },
};
