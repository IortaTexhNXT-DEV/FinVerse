import { api } from '@/api/client';
import type { PrintBatch, PrintCopy, PrintLine } from './cashieringQueueTypes';

/**
 * Printing of ARs and ORs (FRS.CSH.02.04) and the AR and OR forms (FRS.CSH.02.06): the copy
 * printed, the ZIP of the receipts, skipping failures, the documents kept with a receipt and the
 * versions of the forms with their approval.
 */

const BASE = '/cashiering';

export const MAX_PRINT = 500;

export const COPY_LABELS: Record<PrintCopy, string> = {
  CLIENT: "Client's Copy",
  COMPANY: 'Company Copy',
  BOTH: "Client's Copy and Company Copy",
};

export const LINE_STATUS_LABELS: Record<PrintLine['status'], string> = {
  PRINTED: 'Printed',
  FAILED: 'Not printed',
  SKIPPED: 'Skipped',
};

export interface ReceiptDocumentFile {
  id: number;
  fileName: string;
  createdAt: string;
  createdBy: string;
}

export type FormKind = 'AR' | 'OR';

export interface FormText {
  companyName?: string;
  companyDescription?: string;
  companyAddress?: string;
  companyVat?: string;
  noteLine?: string;
  footer1?: string;
  footer2?: string;
  footer3?: string;
  footer4?: string;
}

export interface ReceiptFormVersion {
  id: number;
  formKind: FormKind;
  versionNo: number;
  text: FormText;
  effectiveFrom: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED';
  createdBy: string;
  createdAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionRemarks?: string;
}

/** The selection refused before it is sent: none, or more than the 500 of one print batch. */
export function selectionError(count: number): string | undefined {
  return count < 1 || count > MAX_PRINT
    ? `Select between 1 and ${MAX_PRINT} receipts to print`
    : undefined;
}

/** The failed receipts of a batch that can be printed again or skipped. */
export function failedLines(batch: Pick<PrintBatch, 'lines'>): PrintLine[] {
  return batch.lines.filter((l) => l.status === 'FAILED');
}

/** The version of a form in use on a date: the latest approved one effective on or before it. */
export function formInUse(
  versions: readonly ReceiptFormVersion[],
  kind: FormKind,
  date: string,
): ReceiptFormVersion | undefined {
  return versions
    .filter((v) => v.formKind === kind && v.status === 'APPROVED' && v.effectiveFrom <= date)
    .sort((a, b) =>
      a.effectiveFrom === b.effectiveFrom
        ? b.versionNo - a.versionNo
        : b.effectiveFrom.localeCompare(a.effectiveFrom),
    )[0];
}

export const printApi = {
  print: (companyId: number, ids: number[], criteria: string, copy: PrintCopy) =>
    api.post<PrintBatch>(`${BASE}/print-batches`, { companyId, ids, criteria, copy }),
  skip: (id: number, ids: number[]) =>
    api.post<PrintBatch>(`${BASE}/print-batches/${id}/skip`, { ids }),
  zip: (id: number) => api.getFile(`${BASE}/print-batches/${id}/zip`),
  documents: (receiptId: number) =>
    api.get<ReceiptDocumentFile[]>(`${BASE}/receipts/${receiptId}/documents`),
  document: (receiptId: number, fileId: number) =>
    api.getFile(`${BASE}/receipts/${receiptId}/documents/${fileId}`),
  forms: (companyId: number) =>
    api.get<ReceiptFormVersion[]>(`${BASE}/receipt-forms?companyId=${companyId}`),
  proposeForm: (companyId: number, formKind: FormKind, text: FormText, effectiveFrom: string) =>
    api.post<ReceiptFormVersion>(`${BASE}/receipt-forms`, {
      companyId,
      formKind,
      text,
      effectiveFrom,
    }),
  approveForm: (id: number) => api.post<ReceiptFormVersion>(`${BASE}/receipt-forms/${id}/approve`),
  rejectForm: (id: number, remarks: string) =>
    api.post<ReceiptFormVersion>(`${BASE}/receipt-forms/${id}/reject`, { remarks }),
};
