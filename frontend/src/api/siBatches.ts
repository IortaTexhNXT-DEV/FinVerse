import type { ServiceInvoice } from './booking';
import { api, toQuery } from './client';
import type { DownloadedFile } from './client';

/** Delivery of a daily service-invoice batch (G09). */
export type SiBatchStatus = 'GENERATED' | 'SENT' | 'FAILED' | 'RETRIED';

/** The service invoices issued to one insurer on one business date, sent in one e-mail. */
export interface SiBatch {
  id: number;
  companyId: number;
  insurerCode: string;
  insurerName: string;
  batchDate: string;
  /** 1 for the daily batch, 2 and up for a supplementary batch. */
  sequenceNo: number;
  itemCount: number;
  /** Null when the service invoices are in several currencies. */
  currency: string | null;
  commissionTotal: number;
  netTotal: number;
  status: SiBatchStatus;
  attempts: number;
  recipientEmail: string | null;
  lastError: string | null;
  generatedAt: string;
  sentAt: string | null;
}

/** Daily service-invoice batches per insurer: list, items and re-send. */
export const siBatchApi = {
  list: (companyId: number, from: string, to: string) =>
    api.get<SiBatch[]>(`/booking/si-batches${toQuery({ companyId, from, to })}`),
  items: (id: number) => api.get<ServiceInvoice[]>(`/booking/si-batches/${id}/items`),
  resend: (id: number) => api.post<SiBatch>(`/booking/si-batches/${id}/resend`),
  supplementary: (companyId: number, date: string) =>
    api.post<SiBatch[]>(`/booking/si-batches/supplementary${toQuery({ companyId, date })}`),
  pdf: (id: number): Promise<DownloadedFile> => api.getFile(`/booking/si-batches/${id}/pdf`),
  manifest: (id: number): Promise<DownloadedFile> =>
    api.getFile(`/booking/si-batches/${id}/manifest`),
};

/** The batch of a day in words: Daily, or Supplementary 2, 3... */
export const batchKind = (batch: Pick<SiBatch, 'sequenceNo'>) =>
  batch.sequenceNo > 1 ? `Supplementary ${String(batch.sequenceNo)}` : 'Daily';

/** Whether a batch can be sent again: only a failed one. */
export const canResend = (batch: Pick<SiBatch, 'status'>) => batch.status === 'FAILED';

/** The first day of the default period: thirty days before a date (yyyy-mm-dd). */
export function periodStart(to: string): string {
  const d = new Date(`${to}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - 30);
  return d.toISOString().slice(0, 10);
}
