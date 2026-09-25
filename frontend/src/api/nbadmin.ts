import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type AccessRequestType =
  'CREATE_USER' | 'MODIFY_ROLES' | 'DISABLE_USER' | 'ENABLE_USER' | 'MODIFY_ROLE_PERMISSIONS';
export type AccessRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

/** An access request (BRNB.085): a user request, or a role-permission change (PMADD05). */
export interface AccessRequest {
  id: number;
  requestNo: string;
  type: AccessRequestType;
  summary: string;
  /** User of a user request; absent for a role-permission change. */
  username?: string;
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
  /** Role of a role-permission change. */
  roleCode?: string;
  permissionsAdded: string[];
  permissionsRemoved: string[];
}

export interface AccessRequestInput {
  type: AccessRequestType;
  username?: string;
  fullName?: string;
  email?: string;
  roleCodes?: string[];
  homeBranchId?: number;
  justification: string;
  roleCode?: string;
  permissionsAdded?: string[];
  permissionsRemoved?: string[];
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

export type ActionClass = 'VIEW' | 'CREATE' | 'AMEND' | 'APPROVE';

export interface MatrixRole {
  code: string;
  name: string;
  enabledUsers: number;
}

/** User access matrix by permission, each permission with its area and action classes (PMADD05). */
export interface AccessMatrix {
  roles: MatrixRole[];
  permissions: { permission: string; roles: string[]; area?: string; actions: ActionClass[] }[];
}

/** Role-to-action matrix (PMADD05): one row per area and action class. */
export interface AccessActionMatrix {
  roles: MatrixRole[];
  rows: {
    area: string;
    action: ActionClass;
    permissions: string[];
    /** Role code -> the permissions of the row the role holds (roles holding none are absent). */
    grants: Record<string, string[] | undefined>;
  }[];
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
  matrixByAction: () => api.get<AccessActionMatrix>('/nbadmin/access-matrix/by-action'),
  exportMatrix: () => api.getFile('/nbadmin/access-matrix/export'),
  retentionRules: () => api.get<RetentionRule[]>('/nbadmin/retention/rules'),
  updateRetentionRule: (id: number, body: RetentionRuleInput) =>
    api.put<RetentionRule>(`/nbadmin/retention/rules/${String(id)}`, body),
  retentionEligible: (id: number) =>
    api.get<RetentionEligible>(`/nbadmin/retention/rules/${String(id)}/eligible?limit=200`),
  runRetentionReview: () => api.post<RetentionRule[]>('/nbadmin/retention/review'),
};
