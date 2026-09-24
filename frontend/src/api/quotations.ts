import type { AccountPremium, ItemInput } from './accounts';
import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** Entity type of quotations in the workflow, attachments and messages. */
export const QUOTATION_ENTITY = 'Quotation';

/** Entity type of quotation requests (attachments). */
export const REQUEST_ENTITY = 'QuotationRequest';

export type QuotationStatus =
  | 'DRAFT'
  | 'FOR_REVIEW'
  | 'APPROVED'
  | 'SENT_TO_CLIENT'
  | 'ACCEPTED'
  | 'CONVERTED'
  | 'NOT_PROCEEDED'
  | 'VOIDED';

export type RequestStatus = 'NEW' | 'QUOTED' | 'CLOSED';

export interface QuotationItemView {
  itemNo: number;
  riskGroup: number;
  label: string;
  sumInsured?: number;
  ratePercent?: number;
  premium?: number;
  data: ItemInput;
}

export interface QuotationContent {
  insurerCode?: string;
  insurerBranch?: string;
  periodFrom?: string;
  periodTo?: string;
  validUntil: string;
  directPayment: boolean;
  ratingBasis?: string;
  remarks?: string;
  items: QuotationItemView[];
  premium: AccountPremium;
  totalSumInsured: number;
  groups: number[];
  rated: boolean;
}

export interface Quotation {
  id: number;
  companyId: number;
  quotationNo: string;
  arn: string;
  requestId?: number;
  clientId: number;
  clientCode: string;
  clientName: string;
  clientEmail?: string;
  productCode: string;
  lineCode: string;
  marketSegment?: string;
  sourceChannel?: string;
  currency: string;
  templateVersion: string;
  currentVersion: number;
  versionOpen: boolean;
  status: QuotationStatus;
  tsuRequired: boolean;
  tsuReason?: string;
  submittedBy?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  sentAt?: string;
  acceptedAt?: string;
  acceptedGroups: number[];
  accountArns: string[];
  createdBy: string;
  createdAt: string;
  content: QuotationContent;
}

export interface QuotationListItem {
  id: number;
  quotationNo: string;
  arn: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  productCode: string;
  insurerCode?: string;
  totalSumInsured: number;
  grossPremium?: number;
  currency: string;
  validUntil: string;
  directPayment: boolean;
  riskGroups: number;
  currentVersion: number;
  status: QuotationStatus;
  createdBy: string;
  createdAt: string;
}

export interface QuotationVersion {
  versionNo: number;
  frozen: boolean;
  frozenBy?: string;
  frozenAt?: string;
  totalSumInsured: number;
  grossPremium?: number;
  createdBy: string;
  createdAt: string;
  updatedAt?: string;
}

export type ItemChangeKind = 'ADDED' | 'REMOVED' | 'CHANGED';

export interface QuotationDiff {
  fromVersion: number;
  toVersion: number;
  fields: { field: string; from: string; to: string }[];
  items: {
    change: ItemChangeKind;
    risk: string;
    fromGroup?: number;
    toGroup?: number;
    fromSumInsured?: number;
    toSumInsured?: number;
    fromPremium?: number;
    toPremium?: number;
  }[];
  grossFrom?: number;
  grossTo?: number;
  grossDelta?: number;
}

export interface QuotationItemInput {
  riskGroup: number;
  item: ItemInput;
}

export interface QuotationInput {
  companyId: number;
  clientId?: number;
  productCode: string;
  marketSegment?: string;
  sourceChannel?: string;
  requestId?: number;
  currency?: string;
  insurerCode?: string;
  insurerBranch?: string;
  periodFrom?: string;
  periodTo?: string;
  validUntil?: string;
  directPayment: boolean;
  ratingBasis?: string;
  remarks?: string;
  items: QuotationItemInput[];
}

export interface QuotationPreview {
  content: QuotationContent;
  tsuRequired: boolean;
  tsuReason?: string;
}

export interface QuotationSearch {
  text?: string;
  status?: QuotationStatus[];
  product?: string;
  mine?: boolean;
  expiring?: boolean;
  clientId?: number;
}

export interface EmailInput {
  to: string[];
  cc?: string[];
  subject: string;
  body: string;
  passwordHint?: string;
}

export interface BatchSendResult {
  quotations: number;
  clients: number;
  references: string[];
}

export interface QuotationRequest {
  id: number;
  requestNo: string;
  channel: string;
  externalRef?: string;
  receivedAt: string;
  clientId?: number;
  prospectName?: string;
  prospectEmail?: string;
  prospectMobile?: string;
  productCode?: string;
  marketSegment?: string;
  requestedCover?: string;
  status: RequestStatus;
  quotationId?: number;
  closeReason?: string;
  createdBy: string;
}

export interface RequestInput {
  companyId: number;
  channel: string;
  externalRef?: string;
  clientCode?: string;
  prospectName?: string;
  prospectEmail?: string;
  prospectMobile?: string;
  productCode?: string;
  marketSegment?: string;
  requestedCover: string;
}

const base = '/quotations';
const multi = (values?: string[]) => (values && values.length > 0 ? values.join(',') : undefined);
const comment = (text?: string) => ({ comment: text });

/** Package quotations (BRNB.020-045): list, detail, versions, documents and actions. */
export const quotationsApi = {
  search: (companyId: number, s: QuotationSearch, page = 0, size = 20) =>
    api.get<PageResponse<QuotationListItem>>(
      `${base}${toQuery({ companyId, ...s, status: multi(s.status), page, size })}`,
    ),
  get: (id: number) => api.get<Quotation>(`${base}/${id}`),
  versions: (id: number) => api.get<QuotationVersion[]>(`${base}/${id}/versions`),
  version: (id: number, versionNo: number) =>
    api.get<{ versionNo: number; content: QuotationContent }>(
      `${base}/${id}/versions/${versionNo}`,
    ),
  diff: (id: number, from: number, to: number) =>
    api.get<QuotationDiff>(`${base}/${id}/diff${toQuery({ from, to })}`),
  pdf: (id: number) => api.getFile(`${base}/${id}/document.pdf`),
  xlsx: (id: number) => api.getFile(`${base}/${id}/document.xlsx`),
  preview: (input: QuotationInput) => api.post<QuotationPreview>(`${base}/preview`, input),
  create: (input: QuotationInput) => api.post<Quotation>(base, input),
  update: (id: number, input: QuotationInput) => api.put<Quotation>(`${base}/${id}`, input),
  submit: (id: number, text?: string) => api.post<Quotation>(`${base}/${id}/submit`, comment(text)),
  approve: (id: number, text?: string) =>
    api.post<Quotation>(`${base}/${id}/approve`, comment(text)),
  revise: (id: number, text?: string) => api.post<Quotation>(`${base}/${id}/revise`, comment(text)),
  decline: (id: number, text?: string) =>
    api.post<Quotation>(`${base}/${id}/decline`, comment(text)),
  createAccounts: (id: number, text?: string) =>
    api.post<Quotation>(`${base}/${id}/create-accounts`, comment(text)),
  send: (id: number, email: EmailInput) => api.post<Quotation>(`${base}/${id}/send`, email),
  batchSend: (ids: number[], passwordHint?: string) =>
    api.post<BatchSendResult>(`${base}/batch-send`, { ids, passwordHint }),
  accept: (id: number, groups: number[], text?: string) =>
    api.post<Quotation>(`${base}/${id}/accept`, { groups, comment: text }),
};

/** Quotation request inbox (BRNB.041). */
export const quotationRequestsApi = {
  search: (companyId: number, status: RequestStatus | '', text: string, page = 0) =>
    api.get<PageResponse<QuotationRequest>>(
      `/quotation-requests${toQuery({ companyId, status, text, page, size: 20 })}`,
    ),
  get: (id: number) => api.get<QuotationRequest>(`/quotation-requests/${id}`),
  create: (input: RequestInput) => api.post<QuotationRequest>('/quotation-requests', input),
  prospect: (id: number) => api.post<QuotationRequest>(`/quotation-requests/${id}/prospect`),
  close: (id: number, reason: string) =>
    api.post<QuotationRequest>(`/quotation-requests/${id}/close`, { reason }),
};
