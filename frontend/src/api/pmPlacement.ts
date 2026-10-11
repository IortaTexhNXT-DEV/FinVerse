import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** A Consolidated Placement Update Report of the repository (BDOI FRS FRPM.007.01). */
export interface PlacementReport {
  id: number;
  reportDate: string;
  periodFrom: string;
  periodTo: string;
  fileName: string;
  recordCount: number;
  scope: string;
  trigger: string;
  generatedBy: string;
  generatedAt: string;
}

export interface PlacementSettings {
  day: string;
  time: string;
  periodDays: number;
  scope: string;
  nextRun: string;
}

export interface PlacementPreview {
  report: PlacementReport;
  columns: { key: string; label: string }[];
  rows: Record<string, string>[];
}

const base = '/product-maintenance/placement-reports';

/** The repository of the Consolidated Placement Update Report. */
export const pmPlacementApi = {
  list: (companyId: number, page = 0) =>
    api.get<PageResponse<PlacementReport>>(`${base}${toQuery({ companyId, page, size: 20 })}`),
  settings: () => api.get<PlacementSettings>(`${base}/settings`),
  generate: (companyId: number) => api.post<PlacementReport>(`${base}/generate`, { companyId }),
  preview: (id: number) => api.get<PlacementPreview>(`${base}/${id}/preview`),
  file: (id: number) => api.getFile(`${base}/${id}/file`),
};
