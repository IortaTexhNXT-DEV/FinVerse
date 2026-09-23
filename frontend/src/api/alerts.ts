import { api, toQuery } from './client';
import type { PageResponse } from './types';
import type { JobRun } from './system';

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface AlertItem {
  id: number;
  exceptionCode: string;
  severity: AlertSeverity;
  module: string;
  companyId?: number;
  branchId?: number;
  entityType?: string;
  entityId?: string;
  message: string;
  amount?: number;
  status: AlertStatus;
  raisedAt: string;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  resolvedBy?: string;
  resolvedAt?: string;
  statusComment?: string;
}

export interface AlertSummary {
  live: number;
  bySeverity: Partial<Record<AlertSeverity, number>>;
}

export interface ExceptionCodeInfo {
  code: string;
  name: string;
  description: string;
  module: string;
  severity: AlertSeverity;
  thresholdAmount?: number;
  thresholdDays?: number;
  active: boolean;
  updatedBy?: string;
  updatedAt?: string;
}

export interface AlertFilters {
  status?: AlertStatus;
  severity?: AlertSeverity;
  code?: string;
  companyId?: number;
  page?: number;
}

export const alertsApi = {
  search: (f: AlertFilters) =>
    api.get<PageResponse<AlertItem>>(`/alerts${toQuery({ ...f, size: 50 })}`),
  summary: () => api.get<AlertSummary>('/alerts/summary'),
  acknowledge: (id: number, comment?: string) =>
    api.post<AlertItem>(`/alerts/${id}/acknowledge`, { comment }),
  resolve: (id: number, reason: string) => api.post<AlertItem>(`/alerts/${id}/resolve`, { reason }),
  runChecks: () => api.post<JobRun>('/alerts/checks/run'),
  codes: () => api.get<ExceptionCodeInfo[]>('/alerts/exception-codes'),
  configure: (
    code: string,
    body: Pick<ExceptionCodeInfo, 'severity' | 'thresholdAmount' | 'thresholdDays' | 'active'>,
  ) => api.put<ExceptionCodeInfo>(`/alerts/exception-codes/${code}`, body),
};
