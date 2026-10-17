import { api, toQuery } from './client';
import type { BatchOutcome } from './renewalTypes';

/** The client's answer, second approval, tags, payment status and billing files (FRRN.014 to 028). */
export interface ApprovalState {
  client: { status: string | null; remarks: string | null; by: string | null; at: string | null };
  approval: {
    required: boolean;
    status: string | null;
    remarks: string | null;
    by: string | null;
    at: string | null;
    paymentConfirmation: string | null;
  };
  bookingOnly: {
    tagged: boolean;
    policyNo: string | null;
    orNo: string | null;
    override: string | null;
    overrideReason: string | null;
    missing: string[];
  };
  directToInsurer: {
    option: string | null;
    status: string | null;
    decidedBy: string | null;
    reason: string | null;
  };
  payment: {
    status: string;
    premium: number | null;
    paid: number | null;
    outstanding: number | null;
  };
  amortized: boolean;
}

export interface BillingFileView {
  id: number;
  fileName: string;
  lineCode: string | null;
  builtIn: boolean;
  runKind: string;
  accounts: number;
  attachmentId: number | null;
  deliveryStatus: string | null;
  deliveryError: string | null;
  lmsStatus: string | null;
  generatedBy: string;
  generatedAt: string;
}

const R = '/renewal';
const co = (companyId: number) => toQuery({ companyId });
const c = (ref: string) => `${R}/candidates/${encodeURIComponent(ref)}`;

export const renewalApprovalApi = {
  state: (companyId: number, ref: string) =>
    api.get<ApprovalState>(`${c(ref)}/approval${co(companyId)}`),
  clientStatus: (companyId: number, ref: string, status: string, remarks: string) =>
    api.put<unknown>(`${c(ref)}/client-status${co(companyId)}`, { status, remarks }),
  approve: (companyId: number, refs: string[], paymentConfirmation: string) =>
    api.post<BatchOutcome>(`${R}/approvals/approve${co(companyId)}`, { refs, paymentConfirmation }),
  reject: (companyId: number, refs: string[], remarks: string) =>
    api.post<BatchOutcome>(`${R}/approvals/reject${co(companyId)}`, { refs, remarks }),
  resubmit: (companyId: number, ref: string) =>
    api.post<unknown>(`${c(ref)}/resubmit${co(companyId)}`, {}),
  submitForPlacement: (companyId: number, refs: string[]) =>
    api.post<BatchOutcome>(`${R}/approvals/submit-placement${co(companyId)}`, { refs }),
  tagBookingOnly: (companyId: number, refs: string[], tagged: boolean) =>
    api.post<BatchOutcome>(`${R}/booking-only${co(companyId)}`, { refs, tagged }),
  bookingOnlyDetails: (
    companyId: number,
    ref: string,
    body: { tagged: boolean; policyNo: string; orNo: string },
  ) => api.put<unknown>(`${c(ref)}/booking-only${co(companyId)}`, body),
  requestOverride: (companyId: number, ref: string, remarks: string) =>
    api.post<unknown>(`${c(ref)}/booking-only/override${co(companyId)}`, { remarks }),
  decideOverride: (companyId: number, ref: string, approve: boolean, remarks: string) =>
    api.post<unknown>(`${c(ref)}/booking-only/override/decision${co(companyId)}`, {
      approve,
      remarks,
    }),
  tagDirectToInsurer: (companyId: number, ref: string, option: string | null) =>
    api.put<unknown>(`${c(ref)}/direct-to-insurer${co(companyId)}`, { option }),
  decideDirectToInsurer: (companyId: number, ref: string, approve: boolean, remarks: string) =>
    api.post<unknown>(`${c(ref)}/direct-to-insurer/decision${co(companyId)}`, { approve, remarks }),
  dispositions: () => api.get<{ code: string; label: string }[]>(`${R}/dispositions`),
  billingFiles: (companyId: number) =>
    api.get<BillingFileView[]>(`${R}/billing-files${co(companyId)}`),
  generateBilling: (companyId: number, from: string, to: string) =>
    api.post<BillingFileView[]>(`${R}/billing-files${co(companyId)}`, { from, to }),
};

/** Labels of the client acceptance statuses. */
export const CLIENT_STATUS: Record<string, string> = {
  PENDING: 'Pending Client Response',
  ACCEPTED: 'Accepted',
  REJECTED: 'Rejected',
  REVISION: 'Request for Revision',
  NOT_APPLICABLE: 'Not Applicable',
};

/** Labels of the approval, override and payment statuses. */
export const STATUS_LABELS: Record<string, string> = {
  SUBMITTED: 'Submitted for Approval',
  APPROVED: 'Approved',
  RETURNED: 'Returned Account',
  REQUESTED: 'Override Requested',
  REJECTED: 'Rejected',
  PENDING: 'Pending Unit Head Approval',
  PAID: 'Paid',
  PARTIALLY_PAID: 'Partially Paid',
  UNPAID: 'Unpaid',
  BLANKET: 'With Blanket Approval',
  UNIT_HEAD: 'Route for UH Approval',
};
