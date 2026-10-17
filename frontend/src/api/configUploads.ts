import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** Uploads of the configuration screens, approved by a second user before they apply. */

export type ConfigUploadStatus = 'VALIDATED' | 'SUBMITTED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED';
export type ConfigUploadRowStatus = 'VALID' | 'INVALID' | 'COMMITTED' | 'FAILED';
export type ConfigUploadAction = 'ADD' | 'UPDATE';

export interface ConfigUploadType {
  code: string;
  /** Tab of the master data and configuration workbook, e.g. D0-02. */
  templateId: string;
  title: string;
  screen: string;
  filledBy: string;
  rules: string[];
  mayUpload: boolean;
  mayApprove: boolean;
}

export interface ConfigUploadJob {
  id: number;
  jobNo: string;
  handlerCode: string;
  fileName: string;
  status: ConfigUploadStatus;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  committedRows: number;
  failedRows: number;
  createdBy: string;
  createdAt: string;
  completedAt?: string | null;
  submittedBy?: string | null;
  submittedAt?: string | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionNote?: string | null;
}

export interface ConfigUploadWithType {
  job: ConfigUploadJob;
  type: ConfigUploadType;
}

export interface ConfigUploadRow {
  rowNo: number;
  status: ConfigUploadRowStatus;
  messages?: string | null;
  resultRef?: string | null;
  action?: ConfigUploadAction | null;
  values: Record<string, string>;
}

const BASE = '/config-uploads';

export const configUploadsApi = {
  types: () => api.get<ConfigUploadType[]>(`${BASE}/types`),
  type: (code: string) => api.get<ConfigUploadType>(`${BASE}/types/${code}`),
  /** The template in the layout of the tab of the workbook. */
  template: (code: string, companyId: number) =>
    api.getFile(`${BASE}/types/${code}/template${toQuery({ companyId })}`),
  /** The current data in the layout of the template. */
  exportData: (code: string, companyId: number) =>
    api.getFile(`${BASE}/types/${code}/export${toQuery({ companyId })}`),
  upload: (companyId: number, type: string, file: File) => {
    const form = new FormData();
    form.append('companyId', String(companyId));
    form.append('type', type);
    form.append('file', file);
    return api.upload<ConfigUploadWithType>(`${BASE}/jobs`, form);
  },
  jobs: (companyId: number, type: string, page = 0) =>
    api.get<PageResponse<ConfigUploadJob>>(
      `${BASE}/jobs${toQuery({ companyId, type, page, size: 10 })}`,
    ),
  waiting: () => api.get<ConfigUploadWithType[]>(`${BASE}/waiting`),
  job: (id: number) => api.get<ConfigUploadWithType>(`${BASE}/jobs/${id}`),
  rows: (id: number, status?: ConfigUploadRowStatus, page = 0) =>
    api.get<PageResponse<ConfigUploadRow>>(
      `${BASE}/jobs/${id}/rows${toQuery({ status, page, size: 50 })}`,
    ),
  submit: (id: number) => api.post<ConfigUploadWithType>(`${BASE}/jobs/${id}/submit`),
  approve: (id: number, note: string) =>
    api.post<ConfigUploadWithType>(`${BASE}/jobs/${id}/approve`, { note }),
  reject: (id: number, note: string) =>
    api.post<ConfigUploadWithType>(`${BASE}/jobs/${id}/reject`, { note }),
  cancel: (id: number) => api.post<ConfigUploadWithType>(`${BASE}/jobs/${id}/cancel`),
  report: (id: number) => api.getFile(`${BASE}/jobs/${id}/report`),
  errorFile: (id: number) => api.getFile(`${BASE}/jobs/${id}/error-file`),
};
