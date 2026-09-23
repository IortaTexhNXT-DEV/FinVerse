import { api, toQuery } from './client';
import type { PageResponse, UserProfile } from './types';

export interface Role {
  id: number;
  code: string;
  name: string;
  permissions: string[];
}

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
  updateRole: (id: number, body: Omit<Role, 'id'>) => api.put<Role>(`/admin/roles/${id}`, body),
  createRole: (body: Omit<Role, 'id'>) => api.post<Role>('/admin/roles', body),
  audit: (params: {
    from: string;
    to: string;
    username?: string;
    entityType?: string;
    page?: number;
  }) => api.get<PageResponse<AuditEntry>>(`/audit-logs${toQuery({ ...params, size: 50 })}`),
};
