import { api, toQuery } from '@/api/client';
import type { YearEndClose } from '@/api/closing';

/** Scheduled month-end close (FRBS 2.6.0). */
export interface CloseSchedule {
  id: number;
  periodId: number;
  periodName: string;
  scheduledAt: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  executedAt?: string;
  result?: string;
  scheduledBy: string;
}

/** Cut-off of the broking books of a period (FRBS 3.4.0). */
export interface BooksCutoff {
  periodId: number;
  periodName: string;
  module: string;
  locked: boolean;
  lockedBy?: string;
  lockedAt?: string;
  unlockedBy?: string;
  unlockedAt?: string;
  note?: string;
}

/** Close settings (AQ04). */
export interface CloseSettings {
  closeOnlyPreviousMonth: string;
  brokingCloseTime: string;
  yearEndDeadline: string;
}

/** Year-end close with the post-close verification (FRBS 2.7.1). */
export type VerifiedYearEndClose = YearEndClose & {
  nominalBalance?: number;
  tbDifference?: number;
  verified?: boolean;
  verifiedAt?: string;
};

/** FRBS close controls: scheduled close, broking books cut-off, year-end verification. */
export const closeControlsApi = {
  settings: () => api.get<CloseSettings>('/closing/settings'),
  schedules: (companyId: number) =>
    api.get<CloseSchedule[]>(`/closing/close-schedules${toQuery({ companyId })}`),
  proposal: (companyId: number, periodId: number) =>
    api.get<{ scheduledAt: string }>(
      `/closing/close-schedules/proposal${toQuery({ companyId, periodId })}`,
    ),
  schedule: (companyId: number, periodId: number, scheduledAt?: string) =>
    api.post<CloseSchedule>('/closing/close-schedules', { companyId, periodId, scheduledAt }),
  cancel: (id: number, reason: string) =>
    api.post<CloseSchedule>(`/closing/close-schedules/${id}/cancel`, { reason }),
  closeNow: (companyId: number, periodId: number) =>
    api.post<CloseSchedule>('/closing/close-now', { companyId, periodId }),
  brokingBooks: (companyId: number) =>
    api.get<BooksCutoff[]>(`/closing/broking-books${toQuery({ companyId })}`),
  pending: (companyId: number, periodId: number) =>
    api.get<string[]>(`/closing/broking-books/pending${toQuery({ companyId, periodId })}`),
  closeBooks: (companyId: number, periodId: number) =>
    api.post<BooksCutoff>('/closing/broking-books/close', { companyId, periodId }),
  reopenBooks: (companyId: number, periodId: number, reason: string) =>
    api.post<BooksCutoff>('/closing/broking-books/reopen', { companyId, periodId, reason }),
  verifyYear: (companyId: number, fiscalYearId: number) =>
    api.post<VerifiedYearEndClose>('/closing/year-end/verify', { companyId, fiscalYearId }),
};
