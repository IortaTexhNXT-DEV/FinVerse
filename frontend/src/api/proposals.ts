import type { ItemInput } from './accounts';
import { api, toQuery } from './client';
import type { EmailInput } from './quotations';
import type { PageResponse } from './types';

/** Entity type of PRFs in the workflow, attachments and messages. */
export const PROPOSAL_ENTITY = 'ProposalRequest';

export type ProposalStatus =
  | 'DRAFT'
  | 'FOR_MKT_APPROVAL'
  | 'WITH_TSU'
  | 'QS_PREPARATION'
  | 'QS_FOR_APPROVAL'
  | 'QS_SENT'
  | 'TERMS_RECEIVED'
  | 'PS_FOR_APPROVAL'
  | 'PS_RELEASED'
  | 'SENT_TO_CLIENT'
  | 'ACCEPTED'
  | 'CONVERTED'
  | 'NOT_PROCEEDED'
  | 'VOIDED';

export type ResponseStatus = 'PENDING' | 'RECEIVED' | 'DECLINED';

export interface RiskSection {
  heading?: string;
  text?: string;
}

export interface ProposalSlips {
  submittedBy?: string;
  approvedBy?: string;
  qsNo?: string;
  qsTemplate?: string;
  qsReplyBy?: string;
  qsSubmittedBy?: string;
  qsApprovedBy?: string;
  qsSentAt?: string;
  termsClosed: boolean;
  chosenInsurer?: string;
  psNo?: string;
  psVersion: number;
  psSubmittedBy?: string;
  psApprovedBy?: string;
  sentAt?: string;
  acceptedAt?: string;
}

export interface Proposal {
  id: number;
  companyId: number;
  prfNo: string;
  arn: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  clientEmail?: string;
  productCode: string;
  lineCode: string;
  marketSegment?: string;
  sourceChannel?: string;
  currency: string;
  periodFrom?: string;
  periodTo?: string;
  totalSumInsured: number;
  sections: RiskSection[];
  items: { itemNo: number; riskGroup: number; data: ItemInput }[];
  groups: number[];
  insurers: string[];
  tsuRule?: string;
  tsuReason?: string;
  status: ProposalStatus;
  slips: ProposalSlips;
  acceptedGroups: number[];
  accountArns: string[];
  createdBy: string;
  createdAt: string;
}

export interface ProposalListItem {
  id: number;
  prfNo: string;
  arn: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  productCode: string;
  totalSumInsured: number;
  chosenInsurer?: string;
  qsNo?: string;
  psNo?: string;
  status: ProposalStatus;
  createdBy: string;
  createdAt: string;
}

export interface ProposalInput {
  companyId: number;
  clientId?: number;
  productCode: string;
  marketSegment?: string;
  sourceChannel?: string;
  currency?: string;
  periodFrom?: string;
  periodTo?: string;
  sections: RiskSection[];
  items: { riskGroup: number; risk: ItemInput }[];
  insurers: string[];
}

export interface InsurerResponse {
  id: number;
  insurerCode: string;
  insurerName: string;
  status: ResponseStatus;
  revision: number;
  premium?: number;
  rate?: number;
  deductibles?: string;
  conditions?: string;
  validUntil?: string;
  remarks?: string;
  documentId?: number;
  recommended: boolean;
  respondedAt?: string;
  updatedBy?: string;
}

export interface ResponseHistory {
  responseId: number;
  revision: number;
  status: ResponseStatus;
  premium?: number;
  rate?: number;
  validUntil?: string;
  recommended: boolean;
  changedBy: string;
  changedAt: string;
}

export interface TermsInput {
  status: ResponseStatus;
  premium?: number;
  rate?: number;
  deductibles?: string;
  conditions?: string;
  validUntil?: string;
  remarks?: string;
}

export interface ComparativeRow {
  insurerCode: string;
  insurerName: string;
  status: ResponseStatus;
  premium?: number;
  rate?: number;
  deductibles?: string;
  conditions?: string;
  validUntil?: string;
  remarks?: string;
  recommended: boolean;
  lowest: boolean;
}

export interface Comparative {
  rows: ComparativeRow[];
  recommendedInsurer?: string;
}

export interface ChecklistItem {
  documentType: string;
  attached: boolean;
}

export interface ProposalSearch {
  text?: string;
  status?: ProposalStatus[];
  mine?: boolean;
  clientId?: number;
}

const base = '/proposals';
const comment = (text?: string) => ({ comment: text });
const joined = (values?: string[]) =>
  values === undefined || values.length === 0 ? undefined : values.join(',');

/** Proposal Request Forms (BRNB.005-017): Marketing side and TSU side. */
export const proposalsApi = {
  search: (companyId: number, s: ProposalSearch, page = 0, size = 20) =>
    api.get<PageResponse<ProposalListItem>>(
      `${base}${toQuery({ companyId, ...s, status: joined(s.status), page, size })}`,
    ),
  get: (id: number) => api.get<Proposal>(`${base}/${id}`),
  checklist: (id: number) => api.get<ChecklistItem[]>(`${base}/${id}/checklist`),
  create: (input: ProposalInput) => api.post<Proposal>(base, input),
  update: (id: number, input: ProposalInput) => api.put<Proposal>(`${base}/${id}`, input),
  submit: (id: number, text?: string) => api.post<Proposal>(`${base}/${id}/submit`, comment(text)),
  approve: (id: number, text?: string) =>
    api.post<Proposal>(`${base}/${id}/approve`, comment(text)),
  send: (id: number, email: EmailInput) => api.post<Proposal>(`${base}/${id}/send`, email),
  accept: (id: number, groups: number[], text?: string) =>
    api.post<Proposal>(`${base}/${id}/accept`, { groups, comment: text }),
  createAccounts: (id: number, text?: string) =>
    api.post<Proposal>(`${base}/${id}/create-accounts`, comment(text)),
  selectInsurers: (id: number, insurers: string[]) =>
    api.put<Proposal>(`${base}/${id}/insurers`, { insurers }),
  submitQuotationSlip: (id: number, replyBy?: string, text?: string) =>
    api.post<Proposal>(`${base}/${id}/quotation-slip/submit`, { replyBy, comment: text }),
  approveQuotationSlip: (id: number, text?: string) =>
    api.post<Proposal>(`${base}/${id}/quotation-slip/approve`, comment(text)),
  quotationSlipPdf: (id: number) => api.getFile(`${base}/${id}/quotation-slip.pdf`),
  responses: (id: number) => api.get<InsurerResponse[]>(`${base}/${id}/responses`),
  history: (id: number) => api.get<ResponseHistory[]>(`${base}/${id}/responses/history`),
  recordTerms: (id: number, responseId: number, terms: TermsInput) =>
    api.put<InsurerResponse>(`${base}/${id}/responses/${responseId}`, terms),
  attachResponse: (id: number, responseId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<InsurerResponse>(`${base}/${id}/responses/${responseId}/document`, form);
  },
  recommend: (id: number, responseId: number) =>
    api.post<InsurerResponse>(`${base}/${id}/responses/${responseId}/recommend`),
  termsComplete: (id: number, closePending: boolean, text?: string) =>
    api.post<Proposal>(`${base}/${id}/terms-complete`, { closePending, comment: text }),
  comparative: (id: number) => api.get<Comparative>(`${base}/${id}/comparative`),
  comparativePdf: (id: number) => api.getFile(`${base}/${id}/comparative.pdf`),
  comparativeXlsx: (id: number) => api.getFile(`${base}/${id}/comparative.xlsx`),
  submitProposalSlip: (id: number, insurerCode?: string, text?: string) =>
    api.post<Proposal>(`${base}/${id}/proposal-slip/submit`, { insurerCode, comment: text }),
  approveProposalSlip: (id: number, text?: string) =>
    api.post<Proposal>(`${base}/${id}/proposal-slip/approve`, comment(text)),
  proposalSlipPdf: (id: number) => api.getFile(`${base}/${id}/proposal-slip.pdf`),
};
