import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type AccessRequestType = 'CREATE_USER' | 'MODIFY_ROLES' | 'DISABLE_USER' | 'ENABLE_USER';
export type AccessRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/** A user access request (BRNB.085). */
export interface AccessRequest {
  id: number;
  requestNo: string;
  type: AccessRequestType;
  summary: string;
  username: string;
  fullName?: string;
  email?: string;
  roleCodes: string[];
  homeBranchId?: number;
  justification: string;
  status: AccessRequestStatus;
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionComment?: string;
}

export interface AccessRequestInput {
  type: AccessRequestType;
  username: string;
  fullName?: string;
  email?: string;
  roleCodes?: string[];
  homeBranchId?: number;
  justification: string;
}

export interface AccessDecision {
  request: AccessRequest;
  /** Temporary password of a created user, shown once. */
  temporaryPassword?: string;
}

export interface AccessRequestFilters {
  status?: AccessRequestStatus;
  type?: AccessRequestType;
  text?: string;
  page?: number;
}

export interface UserAccess {
  username: string;
  fullName: string;
  enabled: boolean;
  roleCodes: string[];
}

export interface RoleInfo {
  id: number;
  code: string;
  name: string;
  permissions: string[];
}

export interface AccessMatrix {
  roles: { code: string; name: string; enabledUsers: number }[];
  permissions: { permission: string; roles: string[] }[];
}

export type RetentionAction = 'REVIEW' | 'ARCHIVE';

export interface RetentionRule {
  id: number;
  recordType: string;
  statuses: string;
  yearsOnline: number;
  yearsArchive: number;
  action: RetentionAction;
  active: boolean;
  description: string;
  providerAvailable: boolean;
  lastRunAt?: string;
  lastCutoff?: string;
  lastEligibleCount?: number;
}

export interface RetentionRuleInput {
  statuses: string;
  yearsOnline: number;
  yearsArchive: number;
  action: RetentionAction;
  active: boolean;
  description: string;
}

export interface RetentionEligible {
  ruleId: number;
  recordType: string;
  providerAvailable: boolean;
  cutoff: string;
  records: {
    reference: string;
    description?: string;
    status: string;
    lastActivity: string;
    link?: string;
  }[];
}

/** Broking administration: access requests, access matrix, data retention (nbadmin module). */
export const nbadminApi = {
  requests: (f: AccessRequestFilters) =>
    api.get<PageResponse<AccessRequest>>(`/nbadmin/access-requests${toQuery({ ...f })}`),
  request: (id: number) => api.get<AccessRequest>(`/nbadmin/access-requests/${String(id)}`),
  submit: (body: AccessRequestInput) => api.post<AccessRequest>('/nbadmin/access-requests', body),
  approve: (id: number, comment?: string) =>
    api.post<AccessDecision>(`/nbadmin/access-requests/${String(id)}/approve`, { comment }),
  reject: (id: number, comment: string) =>
    api.post<AccessDecision>(`/nbadmin/access-requests/${String(id)}/reject`, { comment }),
  users: () => api.get<UserAccess[]>('/nbadmin/users'),
  roles: () => api.get<RoleInfo[]>('/nbadmin/roles'),
  matrix: () => api.get<AccessMatrix>('/nbadmin/access-matrix'),
  exportMatrix: () => api.getFile('/nbadmin/access-matrix/export'),
  retentionRules: () => api.get<RetentionRule[]>('/nbadmin/retention/rules'),
  updateRetentionRule: (id: number, body: RetentionRuleInput) =>
    api.put<RetentionRule>(`/nbadmin/retention/rules/${String(id)}`, body),
  retentionEligible: (id: number) =>
    api.get<RetentionEligible>(`/nbadmin/retention/rules/${String(id)}/eligible?limit=200`),
  runRetentionReview: () => api.post<RetentionRule[]>('/nbadmin/retention/review'),
};
