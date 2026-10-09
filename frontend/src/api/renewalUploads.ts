import { api, toQuery } from './client';

/** Upload outcome summaries of the Renewal uploads (BDOI Renewal FRS FRRN.012, 013, 015). */
export type UploadKind = 'LAMD' | 'BDOFC' | 'INSURER' | 'UPDATE';

export interface UploadSummary {
  uploadId: string;
  handler: string;
  fileName: string;
  uploadedAt: string;
  uploadedBy: string;
  status: string;
  remarks: string;
  total: number;
  processed: number;
  failed: number;
}

export interface UploadRecord {
  rowNo: number;
  values: Record<string, string>;
  matchingStatus: string | null;
  processingStatus: string;
  reason: string | null;
}

export interface RmuOfficer {
  id: number;
  aoCode: string;
  aoName: string;
  active: boolean;
  remarks: string | null;
  createdBy: string;
  createdAt: string;
  updatedBy: string | null;
  updatedAt: string | null;
}

const U = '/renewal/uploads';

export const renewalUploadsApi = {
  uploads: (companyId: number, kind: UploadKind, search?: string, status?: string) =>
    api.get<UploadSummary[]>(`${U}${toQuery({ companyId, kind, search, status })}`),
  records: (companyId: number, jobNo: string, matching?: string, search?: string) =>
    api.get<UploadRecord[]>(
      `${U}/${encodeURIComponent(jobNo)}/records${toQuery({ companyId, matching, search })}`,
    ),
  result: (companyId: number, kind: UploadKind, jobNo: string) =>
    api.getFile(`${U}/${encodeURIComponent(jobNo)}/result${toQuery({ companyId, kind })}`),
  rmuOfficers: (companyId: number) =>
    api.get<RmuOfficer[]>(`/renewal/rmu-officers${toQuery({ companyId })}`),
  addRmuOfficer: (companyId: number, body: { aoCode: string; aoName: string; remarks?: string }) =>
    api.post<RmuOfficer>(`/renewal/rmu-officers${toQuery({ companyId })}`, body),
  updateRmuOfficer: (
    companyId: number,
    id: number,
    body: { aoName: string; remarks?: string; active: boolean },
  ) => api.put<RmuOfficer>(`/renewal/rmu-officers/${String(id)}${toQuery({ companyId })}`, body),
};
