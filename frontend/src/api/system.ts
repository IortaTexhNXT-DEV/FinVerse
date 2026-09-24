import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type ParameterValueType =
  'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'INTEGER_LIST' | 'CODE_LIST';

export interface SystemParameter {
  key: string;
  value: string;
  valueType: ParameterValueType;
  category: string;
  description: string;
  minValue?: number;
  maxValue?: number;
  updatedBy?: string;
  updatedAt?: string;
}

export interface ConfigEntry {
  key: string;
  value: string;
}

export interface JobRun {
  id: number;
  jobName: string;
  trigger: 'SCHEDULED' | 'MANUAL';
  triggeredBy: string;
  startedAt: string;
  finishedAt?: string;
  status: 'RUNNING' | 'SUCCEEDED' | 'FAILED';
  itemsProcessed: number;
  message?: string;
}

export interface ScheduledJob {
  name: string;
  description: string;
  cron: string;
  nextRun?: string;
  lastRun?: JobRun;
}

export interface AboutInfo {
  product: string;
  vendor: string;
  version: string;
  buildTime?: string;
}

export interface SystemInfo {
  about: AboutInfo;
  javaVersion: string;
  databaseVersion: string;
  migrationVersion: string;
  migrationDescription: string;
  migrationsApplied: number;
  health: string;
  startedAt: string;
  uptimeSeconds: number;
  activeProfiles: string[];
}

export interface SessionPolicy {
  timeoutMinutes: number;
  /** Seconds before the inactivity sign-out at which the warning shows. */
  warningSeconds: number;
  /** Minutes before the absolute session end (token expiry) at which the user is warned. */
  expiryWarningMinutes?: number;
}

export const systemApi = {
  parameters: () => api.get<SystemParameter[]>('/system/parameters'),
  updateParameter: (key: string, value: string) =>
    api.put<SystemParameter>(`/system/parameters/${key}`, { value }),
  configuration: () => api.get<ConfigEntry[]>('/system/configuration'),
  info: () => api.get<SystemInfo>('/system/info'),
  about: () => api.get<AboutInfo>('/system/about'),
  sessionPolicy: () => api.get<SessionPolicy>('/system/session-policy'),
  jobs: () => api.get<ScheduledJob[]>('/system/jobs'),
  runs: (job?: string, page = 0) =>
    api.get<PageResponse<JobRun>>(`/system/jobs/runs${toQuery({ job, page, size: 25 })}`),
  runJob: (name: string) => api.post<JobRun>(`/system/jobs/${name}/run`),
};
