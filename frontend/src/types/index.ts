export interface User {
  id: number;
  username: string;
  name: string;
  empNo: string;
  email: string;
  institutionId?: number;
}

export interface LoginResponse {
  token: string;
  user: User;
  roles: string[];
}

export interface System {
  id: number;
  name: string;
  description: string;
  templateFileKey: string;
  needApproval: boolean;
  status: number;
  createdBy: string;
  createdAt: string;
  institutionCount: number;
  indicatorCount: number;
  groupNames: string[];
}

export interface Institution {
  id: number;
  systemId: number;
  orgName: string;
  orgId: string;
  groupName: string;
  leaderName: string;
  leaderEmpNo: string;
}

export interface Indicator {
  id: number;
  systemId: number;
  dimension: string;
  category: string;
  level1Name: string;
  level2Name: string;
  weight: number;
  unit: string;
  annualTarget: number;
  progressTarget: number;
  rowIndex: number;
}

export interface Monitoring {
  id: number;
  systemId: number;
  systemName: string;
  year: number;
  month: number;
  period: string;
  status: string;
  deadline: string;
  approvalRequired: boolean;
  processPercent: number;
  processStatus: string;
  processMsg: string;
  createdBy: string;
  createdAt: string;
  totalInstitutions: number;
  confirmedInstitutions: number;
  pendingInstitutions: number;
  totalTasks: number;
  submittedTasks: number;
  pendingTasks: number;
}

export interface CollectionTask {
  id: number;
  monitoringId: number;
  indicatorId: number;
  institutionId: number;
  institutionName: string;
  // 考核指标信息（模版页）
  dimension: string;
  category: string;
  level1Name: string;
  level2Name: string;
  unit: string;
  annualTarget: number;
  progressTarget: number;
  // 收数指标信息（数据收集页）
  collectionIndicatorName: string;
  collectionUnit: string;
  collectionDimension: string;
  collectionCategory: string;
  collectorName: string;
  collectorEmpNo: string;
  collectorUserId: number;
  actualValue: number;
  status: string;
  fileKey: string;
}

export interface Report {
  monitoringId: number;
  systemName: string;
  year: number;
  month: number;
  institutionId: number;
  institutionName: string;
  groupName: string;
  totalScore: number;
  totalRank: number;
  groupRank: number;
  dimensionScores: DimensionScore[];
  indicators: IndicatorData[];
}

export interface DimensionScore {
  dimension: string;
  score: number;
}

export interface IndicatorData {
  dimension: string;
  category: string;
  level1Name: string;
  level2Name: string;
  unit: string;
  actualValue: number;
  target: number;
  progressTarget: number;
  completionRate: number;
  score: number;
  weight: number;
  weightedScore: number;
}

export interface Overview {
  monitoringId: number;
  systemName: string;
  year: number;
  month: number;
  institutionRanks: InstitutionRank[];
  dimensions: string[];
  groupOverviews: GroupOverview[];
}

export interface InstitutionRank {
  institutionId: number;
  institutionName: string;
  groupName: string;
  totalScore: number;
  rank: number;
  groupRank: number;
}

export interface GroupOverview {
  groupName: string;
  avgScore: number;
  institutions: InstitutionRank[];
}

export interface Notification {
  id: number;
  title: string;
  content: string;
  type: string;
  status: string;
  createdAt: string;
  isRead: boolean;
}

export const MonitoringStatus = {
  PENDING: 'PENDING',
  COLLECTING: 'COLLECTING',
  CLOSED: 'CLOSED',
  PROCESSING: 'PROCESSING',
  CONFIRMING: 'CONFIRMING',
  PUBLISHED: 'PUBLISHED',
} as const;

export const TaskStatus = {
  PENDING: 'pending',
  SUBMITTED: 'submitted',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const;
