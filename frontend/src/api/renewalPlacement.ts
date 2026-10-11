import { api, toQuery } from './client';
import type { BatchOutcome } from './renewalTypes';

/** Placement, Insurance Advice and e-policies of the renewal accounts (FRRN.029 to FRRN.033). */
export interface PlacementView {
  id: number;
  insurerCode: string;
  share: number;
  premium: number | null;
  sumInsured: number | null;
  docs: {
    slipFileName: string;
    slipAttachmentId: number | null;
    fileName: string | null;
    fileAttachmentId: number | null;
  };
  status: string;
  sending: {
    channel: string | null;
    messageNo: string | null;
    status: string | null;
    recipients: string | null;
    cc: string | null;
  };
  submitted: string | null;
  tat: { days: number; target: number; status: string; end: string } | null;
  withIssue: boolean;
  resolutionDate: string | null;
  response: {
    response: string | null;
    date: string | null;
    reason: string | null;
    remarks: string | null;
  };
  generatedBy: string;
  generatedOn: string | null;
}

export interface InsurerRecipients {
  insurerCode: string;
  insurerName: string;
  mft: boolean;
  recipients: string[];
  accounts: number;
}

export interface SendSummary {
  selected: number;
  submitted: string[];
  failed: Record<string, string>;
}

export interface AdviceView {
  versionNo: number;
  fileName: string;
  attachmentId: number | null;
  trigger: string;
  messageNo: string | null;
  generatedBy: string;
  generatedAt: string;
}

export interface EpolicyReceiptView {
  receiptNo: string;
  receiptType: string;
  summaryFile: string | null;
  zipFile: string | null;
  status: string;
  remarks: string | null;
  records: number;
  matched: number;
  receivedAt: string;
  receivedBy: string;
}

export interface EpolicyLine {
  seq: number | null;
  renewalRef: string | null;
  policyNo: string | null;
  pdfFile: string | null;
  documentTag: string | null;
  matchStatus: string;
  remarks: string | null;
  attachmentId: number | null;
}

export interface EpolicyAccount {
  renewalRef: string;
  clientName: string;
  policyNo: string | null;
  email: string | null;
  fileName: string | null;
  attachmentId: number | null;
  problem: string | null;
}

export interface EpolicySummary {
  selected: number;
  submitted: number;
  failed: number;
  pending: number;
  messages: Record<string, string>;
}

const R = '/renewal';
const co = (companyId: number) => toQuery({ companyId });
const c = (ref: string) => `${R}/candidates/${encodeURIComponent(ref)}`;

export const renewalPlacementApi = {
  generate: (companyId: number, refs: string[]) =>
    api.post<BatchOutcome>(`${R}/placements/generate${co(companyId)}`, { refs }),
  recipients: (companyId: number, refs: string[]) =>
    api.get<InsurerRecipients[]>(
      `${R}/placements/recipients${toQuery({ companyId, refs: refs.join(',') })}`,
    ),
  send: (
    companyId: number,
    body: { refs: string[]; recipients: Record<string, string[]>; cc: string[] },
  ) => api.post<SendSummary>(`${R}/placements/send${co(companyId)}`, body),
  returnToMarketing: (
    companyId: number,
    body: { refs: string[]; reasonCode: string; remarks: string },
  ) => api.post<BatchOutcome>(`${R}/placements/return${co(companyId)}`, body),
  of: (companyId: number, ref: string) =>
    api.get<PlacementView[]>(`${c(ref)}/placements${co(companyId)}`),
  issue: (
    companyId: number,
    ref: string,
    id: number,
    body: { withIssue: boolean; resolutionDate: string | null },
  ) => api.put<PlacementView>(`${c(ref)}/placements/${String(id)}/issue${co(companyId)}`, body),
  respond: (
    companyId: number,
    ref: string,
    body: {
      insurerCode?: string;
      approved: boolean;
      date?: string;
      reason?: string;
      remarks?: string;
    },
  ) => api.post<{ placementStatus: string }>(`${c(ref)}/placements/response${co(companyId)}`, body),
  cancel: (companyId: number, ref: string, remarks: string) =>
    api.post<unknown>(`${c(ref)}/placements/cancel${co(companyId)}`, { remarks }),
  advices: (companyId: number, ref: string) =>
    api.get<AdviceView[]>(`${c(ref)}/insurance-advices${co(companyId)}`),
  generateAdvice: (companyId: number, ref: string) =>
    api.post<AdviceView | null>(`${c(ref)}/insurance-advices${co(companyId)}`, {}),
  sendAdvices: (companyId: number, body: { refs: string[]; to: string[]; cc: string[] }) =>
    api.post<string[]>(`${R}/insurance-advices/send${co(companyId)}`, body),
  receipts: (companyId: number, search?: string, status?: string) =>
    api.get<EpolicyReceiptView[]>(`${R}/epolicy/receipts${toQuery({ companyId, search, status })}`),
  receiptLines: (companyId: number, receiptNo: string, match?: string, search?: string) =>
    api.get<EpolicyLine[]>(
      `${R}/epolicy/receipts/${encodeURIComponent(receiptNo)}/lines${toQuery({ companyId, match, search })}`,
    ),
  uploadEpolicies: (companyId: number, summary: File, zip: File) => {
    const form = new FormData();
    form.append('summary', summary);
    form.append('zip', zip);
    return api.upload<EpolicyReceiptView>(`${R}/epolicy/receipts${co(companyId)}`, form);
  },
  forSending: (companyId: number) =>
    api.get<EpolicyAccount[]>(`${R}/epolicy/sending${co(companyId)}`),
  sendEpolicies: (companyId: number, refs: string[], cc: string[]) =>
    api.post<EpolicySummary>(`${R}/epolicy/send${co(companyId)}`, { refs, cc }),
  placeMftFile: (kind: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<{ fileName: string }>(`${R}/channels/mft-inbox${toQuery({ kind })}`, form);
  },
  pollMft: () => api.post<{ files: number }>(`${R}/channels/mft-inbox/poll`, {}),
};

/** Labels of the placement codes. */
export const PLACEMENT_STATUS: Record<string, string> = {
  GENERATED: 'Generated',
  SENT: 'Sent to Insurer',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  CANCELLED: 'Cancelled',
};

/** Transmission statuses: MFT uploads read Pending Upload, Uploaded to MFT and Upload Failed. */
export function transmissionLabel(channel: string | null, status: string | null): string {
  if (status === null) {
    return '';
  }
  if (channel === 'MFT') {
    if (status === 'FAILED') return 'Upload Failed';
    return status === 'PENDING_TRANSMISSION' ? 'Pending Upload' : 'Uploaded to MFT';
  }
  const ccm: Record<string, string> = {
    PENDING_TRANSMISSION: 'Pending Transmission',
    SUBMITTED: 'Submitted to CCM',
    SENT: 'Sent',
    DELIVERED: 'Delivered',
    FAILED: 'Failed',
    CANCELLED: 'Cancelled',
  };
  return ccm[status] ?? status;
}

/** Splits a list of e-mail addresses typed with commas, semicolons or new lines. */
export function addresses(text: string): string[] {
  return text
    .split(/[,;\n]/)
    .map((s) => s.trim())
    .filter((s) => s !== '');
}
