import { api, toQuery } from './client';
import type { RecordStatus } from './types';

export interface LovOption {
  code: string;
  label: string;
  parentCode?: string;
}

export interface LovType {
  code: string;
  name: string;
  description?: string;
  maintainable: boolean;
}

export interface LovValue {
  id: number;
  typeCode: string;
  code: string;
  label: string;
  sortOrder: number;
  parentCode?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  status: RecordStatus;
  maker?: string;
  authorizedBy?: string;
}

export interface LovValueRequest {
  code: string;
  label: string;
  sortOrder: number;
  parentCode?: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

/** Lists of values (BRNB.083): pick lists and maintenance. */
export const lovApi = {
  options: (type: string, date?: string) =>
    api.get<LovOption[]>(`/lov/${type}/options${toQuery({ date })}`),
  types: () => api.get<LovType[]>('/lov/types'),
  values: (type: string) => api.get<LovValue[]>(`/lov/${type}/values`),
  create: (type: string, body: LovValueRequest) => api.post<LovValue>(`/lov/${type}/values`, body),
  update: (id: number, body: LovValueRequest) => api.put<LovValue>(`/lov/values/${id}`, body),
  authorize: (id: number) => api.post<LovValue>(`/lov/values/${id}/authorize`),
  deactivate: (id: number) => api.post<LovValue>(`/lov/values/${id}/deactivate`),
};
