import { api } from './client';

/** Product modules switched off in this deployment (read by every signed-in user). */
export interface ModulesInUse {
  switchedOff: string[];
  /** Permissions that grant nothing while those modules are off. */
  inactivePermissions: string[];
}

/** A product module switch on the Product Modules screen. */
export interface ModuleSwitch {
  code: string;
  name: string;
  enabled: boolean;
  /** Names of the modules this one needs. */
  needs: string[];
  /** Requested state waiting for approval. */
  pendingEnabled?: boolean | null;
  pendingReason?: string | null;
  pendingBy?: string | null;
  pendingAt?: string | null;
  changedReason?: string | null;
  changedBy?: string | null;
  changedAt?: string | null;
  approvedBy?: string | null;
}

/** A named set of module switches. */
export interface ModuleProfile {
  code: string;
  name: string;
  description: string;
  /** Names of the modules the profile switches off. */
  modulesOff: string[];
}

export const modulesApi = {
  inUse: () => api.get<ModulesInUse>('/system/modules'),
  list: () => api.get<ModuleSwitch[]>('/admin/modules'),
  profiles: () => api.get<ModuleProfile[]>('/admin/modules/profiles'),
  request: (code: string, enabled: boolean, reason: string) =>
    api.post<ModuleSwitch>(`/admin/modules/${code}/change`, { enabled, reason }),
  applyProfile: (code: string, reason: string) =>
    api.post<ModuleSwitch[]>(`/admin/modules/profiles/${code}/apply`, { reason }),
  approve: (code: string) => api.post<ModuleSwitch>(`/admin/modules/${code}/approve`),
  reject: (code: string, reason?: string) =>
    api.post<ModuleSwitch>(`/admin/modules/${code}/reject`, { reason }),
};
