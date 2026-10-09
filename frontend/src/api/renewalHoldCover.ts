import { api, toQuery } from './client';
import type { BatchOutcome } from './renewalTypes';

/** Status of a hold cover. */
export type HoldCoverStatus =
  'REQUESTED' | 'CONFIRMED' | 'DECLINED' | 'EXPIRED' | 'REASSIGNED' | 'CANCELLED';

/** The hold cover of a renewal account. */
export interface HoldCoverView {
  status: HoldCoverStatus;
  insurerCode: string;
  startDate: string;
  expiryDate: string;
  durationDays: number | null;
  expiringPolicyNo: string | null;
  remarks: string | null;
  requestedBy: string | null;
  requestedAt: string | null;
  insurerRef: string | null;
  confirmedOn: string | null;
  conditions: string | null;
  cancelReason: string | null;
}

/** The hold cover panel of a renewal: the durations offered and the current hold cover. */
export interface HoldCoverPanel {
  durations: number[];
  current: HoldCoverView | null;
}

const path = (renewalRef: string, action = '') =>
  `/renewal/candidates/${encodeURIComponent(renewalRef)}/hold-cover${action}`;
const co = (companyId: number) => toQuery({ companyId });

/** Hold cover of a renewal (FR-RN-086) and the closing letters at the effective expiry. */
export const renewalHoldCoverApi = {
  get: (companyId: number, renewalRef: string) =>
    api.get<HoldCoverPanel>(`${path(renewalRef)}${co(companyId)}`),
  request: (
    companyId: number,
    renewalRef: string,
    body: { startDate?: string; durationDays: number; remarks?: string },
  ) => api.post<HoldCoverView>(`${path(renewalRef, '/request')}${co(companyId)}`, body),
  confirm: (
    companyId: number,
    renewalRef: string,
    body: { reference: string; confirmedOn?: string; validTo: string; conditions?: string },
  ) => api.post<HoldCoverView>(`${path(renewalRef, '/confirm')}${co(companyId)}`, body),
  decline: (companyId: number, renewalRef: string, reason: string) =>
    api.post<HoldCoverView>(`${path(renewalRef, '/decline')}${co(companyId)}`, { reason }),
  cancel: (companyId: number, renewalRef: string, reason: string) =>
    api.post<HoldCoverView>(`${path(renewalRef, '/cancel')}${co(companyId)}`, { reason }),
  closingAtExpiry: (companyId: number, renewalRefs: string[]) =>
    api.post<BatchOutcome>('/renewal/letters/closing-at-expiry', { companyId, renewalRefs }),
};
