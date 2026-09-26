import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type BulkColumnType = 'TEXT' | 'NUMBER' | 'DATE' | 'YES_NO';
export type BulkJobStatus = 'VALIDATED' | 'COMPLETED' | 'CANCELLED';
export type BulkRowStatus = 'VALID' | 'INVALID' | 'COMMITTED' | 'FAILED';

export interface BulkColumn {
  header: string;
  description: string;
  required: boolean;
  type: BulkColumnType;
  example?: string;
}

export interface BulkHandler {
  code: string;
  title: string;
  columns: BulkColumn[];
  instructions: string;
}

export interface BulkJob {
  id: number;
  jobNo: string;
  handlerCode: string;
  fileName: string;
  status: BulkJobStatus;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  committedRows: number;
  failedRows: number;
  createdBy: string;
  createdAt: string;
  completedAt?: string;
}

export interface BulkRow {
  rowNo: number;
  status: BulkRowStatus;
  messages?: string;
  resultRef?: string;
  values: Record<string, string>;
}

/** Bulk processing framework: templates, upload and validation, commit, reports. */
export const bulkApi = {
  handlers: () => api.get<BulkHandler[]>('/bulk/handlers'),
  handler: (code: string) => api.get<BulkHandler>(`/bulk/handlers/${code}`),
  template: (code: string) => api.getFile(`/bulk/handlers/${code}/template`),
  upload: (companyId: number, handler: string, file: File, parameters?: Record<string, string>) => {
    const form = new FormData();
    form.append('companyId', String(companyId));
    form.append('handler', handler);
    if (parameters && Object.keys(parameters).length > 0) {
      form.append('parameters', JSON.stringify(parameters));
    }
    form.append('file', file);
    return api.upload<BulkJob>('/bulk/jobs', form);
  },
  jobs: (companyId: number, handler?: string, page = 0) =>
    api.get<PageResponse<BulkJob>>(`/bulk/jobs${toQuery({ companyId, handler, page, size: 20 })}`),
  job: (id: number) => api.get<BulkJob>(`/bulk/jobs/${id}`),
  rows: (id: number, status?: BulkRowStatus, page = 0) =>
    api.get<PageResponse<BulkRow>>(`/bulk/jobs/${id}/rows${toQuery({ status, page, size: 100 })}`),
  commit: (id: number) => api.post<BulkJob>(`/bulk/jobs/${id}/commit`),
  cancel: (id: number) => api.post<BulkJob>(`/bulk/jobs/${id}/cancel`),
  report: (id: number) => api.getFile(`/bulk/jobs/${id}/report`),
};
