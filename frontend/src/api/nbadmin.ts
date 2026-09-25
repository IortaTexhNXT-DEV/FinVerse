import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type AccessRequestType =
  | 'CREATE_USER'
  | 'MODIFY_ROLES'
  | 'MODIFY_USER'
  | 'DISABLE_USER'
  | 'ENABLE_USER'
  | 'MODIFY_ROLE_PERMISSIONS'
  | 'CREATE_ROLE'
  | 'DEACTIVATE_ROLE'
  | 'REACTIVATE_ROLE';
export type AccessRequestStatus =
  | 'DRAFT'
  | 'PENDING'
  | 'PENDING_SECOND'
  | 'RETURNED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'APPROVED'
  | 'SCHEDULED'
  | 'FOR_IMPLEMENTATION'
  | 'IMPLEMENTED';
export type AccessUserType = 'INTERNAL' | 'EXTERNAL';
export type AccessScope = 'MINE' | 'ASSIGNED' | 'SECOND' | 'IMPLEMENTATION' | 'ALL';
export type PrivilegeLevel = 'LOW' | 'STANDARD' | 'HIGH' | 'ADMIN';
export type ApproverDecision = 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETURNED';

/** User data, role data, party, effective date and batch of a request. */
export interface AccessRequestDetails {
  windowsId?: string;
  businessUnitCode?: string;
  userLevel?: string;
  reasonCode?: string;
  unlock: boolean;
  roleName?: string;
  roleDescription?: string;
  privilegeLevel?: PrivilegeLevel;
  partyKind?: 'INSURER' | 'CLIENT';
  partyCode?: string;
  portalRole?: string;
  effectiveFrom?: string;
  batchId?: number;
}

export interface ApproverStep {
  sequence: number;
  approver: string;
  decision: ApproverDecision;
  remarks?: string;
  decidedAt?: string;
}

/** Where a request is in its lifecycle (BRD 1.008). */
export interface AccessRequestLifecycle {
  submittedBy?: string;
  submittedAt?: string;
  assignedApprover?: string;
  approvers: ApproverStep[];
  riskFlags: string[];
  secondApprovalRequired: boolean;
  cancelReason?: string;
  cancelledBy?: string;
  cancelledAt?: string;
  appliedAt?: string;
  implementedBy?: string;
  implementedAt?: string;
  applyError?: string;
}

/** An access request (BRD-11): a user, group-profile or external-user request. */
export interface AccessRequest {
  id: number;
  requestNo: string;
  type: AccessRequestType;
  userType: AccessUserType;
  summary: string;
  /** User of a user request; absent for a group-profile request. */
  username?: string;
  fullName?: string;
  email?: string;
  roleCodes: string[];
  homeBranchId?: number;
  justification?: string;
  status: AccessRequestStatus;
  requestedBy: string;
  requestedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionComment?: string;
  /** Role of a group-profile request. */
  roleCode?: string;
  permissionsAdded: string[];
  permissionsRemoved: string[];
  returnedCount: number;
  details: AccessRequestDetails;
  lifecycle: AccessRequestLifecycle;
}

export interface AccessRequestInput {
  type: AccessRequestType;
  username?: string;
  fullName?: string;
  email?: string;
  roleCodes?: string[];
  homeBranchId?: number;
  justification?: string;
  roleCode?: string;
  permissionsAdded?: string[];
  permissionsRemoved?: string[];
  windowsId?: string;
  businessUnitCode?: string;
  userLevel?: string;
  reasonCode?: string;
  unlock?: boolean;
  roleName?: string;
  roleDescription?: string;
  privilegeLevel?: PrivilegeLevel;
  partyKind?: 'INSURER' | 'CLIENT';
  partyCode?: string;
  portalRole?: string;
  effectiveFrom?: string;
  approvers?: string[];
}

export interface AccessDecision {
  request: AccessRequest;
  /** Temporary password of a created user, shown once. */
  temporaryPassword?: string;
}

export interface AccessRequestFilters {
  scope?: AccessScope;
  status?: AccessRequestStatus;
  type?: AccessRequestType;
  text?: string;
  requester?: string;
  approver?: string;
  from?: string;
  to?: string;
  groupProfiles?: boolean;
  page?: number;
}

export interface AccessRequestEvent {
  id: number;
  action: string;
  fromStatus?: AccessRequestStatus;
  toStatus: AccessRequestStatus;
  remarks?: string;
  actor: string;
  occurredAt: string;
}

export interface ApproverOption {
  username: string;
  fullName: string;
}

export interface AccessSettings {
  directRoleEdit: boolean;
  anyApprover: boolean;
  roleApplyOnApproval: boolean;
  userIdPattern: string;
}

export type AccessBatchStatus =
  'DRAFT' | 'PENDING' | 'RETURNED' | 'CANCELLED' | 'REJECTED' | 'APPROVED' | 'PARTIAL';

export interface AccessBatch {
  id: number;
  batchNo: string;
  lines: number;
  status: AccessBatchStatus;
  remarks?: string;
  createdBy: string;
  createdAt: string;
}

export interface AccessBatchLineOutcome {
  requestNo?: string;
  username?: string;
  status?: AccessRequestStatus;
  temporaryPassword?: string;
  error?: string;
}

export interface AccessBatchDecision {
  batch: AccessBatch;
  lines: AccessBatchLineOutcome[];
}

export interface UserAccess {
  username: string;
  fullName: string;
  enabled: boolean;
  roleCodes: string[];
  email?: string;
  homeBranchId?: number;
  windowsId?: string;
  businessUnitCode?: string;
  userLevel?: string;
  locked: boolean;
}

export interface RoleInfo {
  id: number;
  code: string;
  name: string;
  permissions: string[];
  active: boolean;
  description?: string;
  privilegeLevel: PrivilegeLevel;
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

const REQUESTS = '/nbadmin/access-requests';
const BATCHES = '/nbadmin/access-batches';
const one = (id: number) => `${REQUESTS}/${String(id)}`;
const batch = (id: number) => `${BATCHES}/${String(id)}`;

/** Broking administration: access requests, access matrix, data retention (nbadmin module). */
export const nbadminApi = {
  requests: (f: AccessRequestFilters) =>
    api.get<PageResponse<AccessRequest>>(`${REQUESTS}${toQuery({ ...f })}`),
  request: (id: number) => api.get<AccessRequest>(one(id)),
  history: (id: number) => api.get<AccessRequestEvent[]>(`${one(id)}/history`),
  create: (body: AccessRequestInput, draft: boolean) =>
    api.post<AccessRequest>(`${REQUESTS}${toQuery({ draft })}`, body),
  submit: (body: AccessRequestInput) => api.post<AccessRequest>(REQUESTS, body),
  edit: (id: number, body: AccessRequestInput) => api.put<AccessRequest>(one(id), body),
  submitSaved: (id: number, approvers: string[], remarks?: string) =>
    api.post<AccessRequest>(`${one(id)}/submit`, { approvers, remarks }),
  approve: (id: number, comment?: string) =>
    api.post<AccessDecision>(`${one(id)}/approve`, { comment }),
  secondApprove: (id: number, comment?: string) =>
    api.post<AccessDecision>(`${one(id)}/second-approve`, { comment }),
  reject: (id: number, comment: string) =>
    api.post<AccessDecision>(`${one(id)}/reject`, { comment }),
  returnRequest: (id: number, comment: string) =>
    api.post<AccessRequest>(`${one(id)}/return`, { comment }),
  cancel: (id: number, comment: string) =>
    api.post<AccessRequest>(`${one(id)}/cancel`, { comment }),
  implement: (id: number) => api.post<AccessRequest>(`${one(id)}/implement`),
  approvers: (userType: AccessUserType, subject?: string) =>
    api.get<ApproverOption[]>(`/nbadmin/approvers${toQuery({ userType, subject })}`),
  settings: () => api.get<AccessSettings>('/nbadmin/access-settings'),
  batches: (page = 0) => api.get<PageResponse<AccessBatch>>(`${BATCHES}${toQuery({ page })}`),
  batch: (id: number) => api.get<AccessBatch>(batch(id)),
  batchLines: (id: number) => api.get<AccessRequest[]>(`${batch(id)}/lines`),
  submitBatch: (id: number, approver: string, remarks: string) =>
    api.post<AccessBatch>(`${batch(id)}/submit`, { approver, remarks }),
  approveBatch: (id: number, comment?: string) =>
    api.post<AccessBatchDecision>(`${batch(id)}/approve`, { comment }),
  rejectBatch: (id: number, comment: string) =>
    api.post<AccessBatch>(`${batch(id)}/reject`, { comment }),
  returnBatch: (id: number, comment: string) =>
    api.post<AccessBatch>(`${batch(id)}/return`, { comment }),
  cancelBatch: (id: number, comment: string) =>
    api.post<AccessBatch>(`${batch(id)}/cancel`, { comment }),
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
