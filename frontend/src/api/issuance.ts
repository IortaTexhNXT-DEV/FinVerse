import { api, toQuery } from './client';
import type { PageResponse } from './types';

/**
 * Issuance (issuance module): e-policy receipt and extraction review, Insurance Advice register
 * and encrypted e-policy dispatch.
 */

export type IssuanceTab = 'AWAITING_POLICY' | 'REVIEW' | 'READY_TO_DISPATCH' | 'IA_TO_GENERATE';

export interface IssuanceRow {
  accountId: number;
  arn: string;
  clientCode: string;
  clientName: string;
  status: string;
  productCode: string;
  lineCode: string;
  insurerCode?: string;
  mortgageeBank?: string;
  termYears: number;
  policyNumbers: string[];
  epolicyId?: number;
  epolicyStatus?: string;
  epolicyFile?: string;
  receivedAt?: string;
}

export interface IssuanceCounts {
  awaitingPolicy: number;
  toReview: number;
  readyToDispatch: number;
  adviceToGenerate: number;
}

export interface Epolicy {
  id: number;
  accountId: number;
  arn: string;
  attachmentId: number;
  fileName: string;
  matchMethod: string;
  status: 'RECEIVED' | 'REVIEW' | 'CONFIRMED' | 'REJECTED';
  extractedPolicyNumbers: string[];
  extractedPeriodFrom?: string;
  extractedPeriodTo?: string;
  extractedPremium?: number;
  extractionNote?: string;
  policyNumbers: string[];
  issueDate?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  rejectReason?: string;
  dispatchCount: number;
  dispatchedAt?: string;
  dispatchedTo?: string;
  createdAt: string;
  createdBy: string;
}

export interface AccountValues {
  accountId: number;
  arn: string;
  clientName: string;
  status: string;
  insurerCode?: string;
  productCode: string;
  periodFrom?: string;
  periodTo?: string;
  grossPremium?: number;
  termYears: number;
  policyNumbers: string[];
}

export interface Review {
  epolicy: Epolicy;
  account: AccountValues;
  differences: string[];
}

export interface UploadItem {
  id: number;
  lineNo: number;
  fileName: string;
  arn?: string;
  matchMethod?: string;
  message?: string;
  included: boolean;
  epolicyId?: number;
  outcome?: string;
}

export interface UploadBatch {
  id: number;
  status: 'REVIEW' | 'CONFIRMED' | 'DISCARDED';
  fileCount: number;
  createdAt: string;
  createdBy: string;
  items: UploadItem[];
}

export interface Advice {
  id: number;
  iaNo: string;
  accountId: number;
  arn: string;
  clientName: string;
  mortgageeBank: string;
  insurerCode?: string;
  policyNumbers?: string;
  triggerEvent: string;
  templateVersion: string;
  status: 'GENERATED' | 'SENT';
  sendCount: number;
  lastSentAt?: string;
  lastSentTo?: string;
  createdAt: string;
  createdBy: string;
}

export interface PolicyRecord {
  arn: string;
  status: string;
  policyNumbers: string[];
  issueDate?: string;
  epolicies: Epolicy[];
  advices: Advice[];
}

export interface Outcome {
  reference: string;
  ok: boolean;
  message: string;
}

export interface DispatchEmail {
  to: string[];
  cc: string[];
  subject: string;
  body: string;
  passwordHint?: string;
}

export interface DispatchLog {
  id: number;
  reference?: string;
  recipients: string;
  subject: string;
  status: string;
  attempts: number;
  lastError?: string;
  sentAt?: string;
  simulated: boolean;
  createdAt: string;
  createdBy: string;
}

export const issuanceApi = {
  workbench: (companyId: number, tab: IssuanceTab, text?: string, page = 0) =>
    api.get<PageResponse<IssuanceRow>>(
      `/issuance/workbench${toQuery({ companyId, tab, text, page, size: 20 })}`,
    ),
  counts: (companyId: number) =>
    api.get<IssuanceCounts>(`/issuance/workbench/counts${toQuery({ companyId })}`),
  policy: (arn: string) => api.get<PolicyRecord>(`/issuance/policies/${arn}`),
  receive: (companyId: number, file: File, arn?: string, policyNo?: string) => {
    const form = new FormData();
    form.append('companyId', String(companyId));
    form.append('file', file);
    if (arn) {
      form.append('arn', arn);
    }
    if (policyNo) {
      form.append('policyNo', policyNo);
    }
    return api.upload<Epolicy>('/issuance/epolicies', form);
  },
  review: (id: number) => api.get<Review>(`/issuance/epolicies/${id}`),
  extract: (id: number) => api.post<Review>(`/issuance/epolicies/${id}/extract`),
  confirm: (id: number, policyNumbers: string[], issueDate?: string) =>
    api.post<Review>(`/issuance/epolicies/${id}/confirm`, { policyNumbers, issueDate }),
  reject: (id: number, reasonCode: string, remarks?: string) =>
    api.post<Epolicy>(`/issuance/epolicies/${id}/reject`, { reasonCode, remarks }),
  bulkUpload: (companyId: number, files: File[]) => {
    const form = new FormData();
    form.append('companyId', String(companyId));
    files.forEach((f) => form.append('files', f));
    return api.upload<UploadBatch>('/issuance/epolicy-uploads', form);
  },
  bulk: (id: number) => api.get<UploadBatch>(`/issuance/epolicy-uploads/${id}`),
  chooseItem: (id: number, itemId: number, arn: string | undefined, included: boolean) =>
    api.post<UploadBatch>(`/issuance/epolicy-uploads/${id}/items/${itemId}`, { arn, included }),
  confirmBulk: (id: number) => api.post<UploadBatch>(`/issuance/epolicy-uploads/${id}/confirm`),
  discardBulk: (id: number) => api.post<UploadBatch>(`/issuance/epolicy-uploads/${id}/discard`),
  advices: (companyId: number, text?: string, page = 0) =>
    api.get<PageResponse<Advice>>(
      `/issuance/insurance-advice${toQuery({ companyId, text, page, size: 20 })}`,
    ),
  adviceFile: (id: number) => api.getFile(`/issuance/insurance-advice/${id}/file`),
  generateAdvices: (arns: string[]) =>
    api.post<Outcome[]>('/issuance/insurance-advice/generate', { arns }),
  sendAdvices: (adviceIds: number[], to: string[], cc: string[], passwordHint?: string) =>
    api.post<Advice[]>('/issuance/insurance-advice/send', { adviceIds, to, cc, passwordHint }),
  dispatchDraft: (epolicyId: number) =>
    api.get<DispatchEmail>(`/issuance/dispatch/${epolicyId}/draft`),
  dispatch: (epolicyId: number, email: DispatchEmail) =>
    api.post<Epolicy>(`/issuance/dispatch/${epolicyId}`, email),
  dispatchMany: (epolicyIds: number[], passwordHint?: string) =>
    api.post<Outcome[]>('/issuance/dispatch/batch', { epolicyIds, passwordHint }),
  dispatchLog: (text?: string, page = 0) =>
    api.get<PageResponse<DispatchLog>>(
      `/issuance/dispatch/log${toQuery({ text, page, size: 50 })}`,
    ),
};
