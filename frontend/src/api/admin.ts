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
  unlockUser: (id: number) => api.post<UserProfile>(`/admin/users/${id}/unlock`),
  roles: () => api.get<Role[]>('/admin/roles'),
  audit: (params: AuditQuery) =>
    api.get<PageResponse<AuditEntry>>(`/audit-logs${toQuery({ ...params, size: 50 })}`),
};
