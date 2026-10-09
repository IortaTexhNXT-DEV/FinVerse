import { api, toQuery } from './client';
import type { PageResponse, UserProfile } from './types';

export interface Role {
  id: number;
  code: string;
  name: string;
  permissions: string[];
  /** Inactive profiles grant nothing (BRD 3.002.3). */
  active?: boolean;
  description?: string;
  privilegeLevel?: 'LOW' | 'STANDARD' | 'HIGH' | 'ADMIN';
}

export type RoleInput = Pick<Role, 'code' | 'name' | 'permissions'>;

export interface UserInput {
  username: string;
  fullName: string;
  email?: string;
  homeBranchId?: number;
  authorizationLimit?: number;
  roleCodes: string[];
  enabled: boolean;
}

export interface AuditEntry {
  id: number;
  occurredAt: string;
  username: string;
  entityType: string;
  entityId?: string;
  action: string;
  summary: string;
  /** Module of the record. */
  module?: string;
  /** Windows ID of the user (BDOI's User Id). */
  windowsId?: string;
  /** Action in BDOI's words (Create, Update, Approve, Login, Timeout ...). */
  actionLabel?: string;
  /** Group profiles of the user at the time of the action. */
  roleNames?: string;
  oldValue?: string;
  newValue?: string;
  /** Source (IP) address of the action. */
  ipAddress?: string;
  /** Client or assured's name of the record (Product Maintenance). */
  subject?: string;
  /** Remarks of the action (approval, return or rejection comment). */
  remarks?: string;
}

/** Filters, sort and page of the Audit Trail. */
export interface AuditQuery {
  from: string;
  to: string;
  username?: string;
  entityType?: string;
  entityId?: string;
  action?: string;
  sort?: string;
  direction?: 'asc' | 'desc';
  page?: number;
}

export const adminApi = {
  users: () => api.get<UserProfile[]>('/admin/users'),
  createUser: (user: UserInput, initialPassword: string) =>
    api.post<UserProfile>('/admin/users', {
      user,
      initialPassword: { newPassword: initialPassword },
    }),
  updateUser: (id: number, user: UserInput) => api.put<UserProfile>(`/admin/users/${id}`, user),
  unlockUser: (id: number) => api.post<UserProfile>(`/admin/users/${id}/unlock`),
  resetPassword: (id: number, newPassword: string) =>
    api.post<undefined>(`/admin/users/${id}/reset-password`, { newPassword }),
  roles: () => api.get<Role[]>('/admin/roles'),
  permissions: () => api.get<string[]>('/admin/permissions'),
  updateRole: (id: number, body: RoleInput) => api.put<Role>(`/admin/roles/${id}`, body),
  createRole: (body: RoleInput) => api.post<Role>('/admin/roles', body),
  audit: (params: AuditQuery) =>
    api.get<PageResponse<AuditEntry>>(`/audit-logs${toQuery({ ...params, size: 50 })}`),
};
