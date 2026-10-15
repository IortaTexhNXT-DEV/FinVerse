import { api } from './client';

/** A widget a role home page can show. */
export interface HomeWidget {
  code: string;
  name: string;
}

/** The home page of a role on the Role Home Set-up screen. */
export interface RoleHome {
  roleCode: string;
  roleName: string;
  /** Widget codes shown, in order. */
  widgets: string[];
  /** True while the role shows the delivered widgets. */
  delivered: boolean;
  /** Requested widgets waiting for approval. */
  pendingWidgets?: string[] | null;
  pendingReason?: string | null;
  pendingBy?: string | null;
  pendingAt?: string | null;
  changedReason?: string | null;
  changedBy?: string | null;
  changedAt?: string | null;
  approvedBy?: string | null;
}

export const roleHomesApi = {
  /** The widgets of the signed-in user's home page, in order. */
  mine: () => api.get<string[]>('/role-homes/mine'),
  widgets: () => api.get<HomeWidget[]>('/role-homes/widgets'),
  list: () => api.get<RoleHome[]>('/role-homes'),
  request: (roleCode: string, widgets: string[], reason: string) =>
    api.post<RoleHome>(`/role-homes/${roleCode}/change`, { widgets, reason }),
  approve: (roleCode: string) => api.post<RoleHome>(`/role-homes/${roleCode}/approve`),
  reject: (roleCode: string, reason?: string) =>
    api.post<RoleHome>(`/role-homes/${roleCode}/reject`, { reason }),
};
