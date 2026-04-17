import { request } from './index';

export interface McpToolInfo {
  name: string;
  description: string;
  category: string;
  parameters: ParameterInfo[];
}

export interface ParameterInfo {
  name: string;
  description: string;
  required: boolean;
  type: string;
}

export const mcpApi = {
  getTools: () => request<McpToolInfo[]>('/mcp/tools'),
};
