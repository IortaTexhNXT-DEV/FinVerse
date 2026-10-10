import { api, toQuery } from './client';

/** Hold cover requests per insurer and the insurer allocation of a renewal (FRRN.014.03, FRRN.036). */
export interface HoldCoverRequestView {
  requestNo: string;
  kind: string;
  insurerCode: string;
  share: number | null;
  days: number;
  start: string;
  end: string;
  status: string;
  channel: string | null;
  batchNo: string | null;
  insurerRef: string | null;
  remarks: string | null;
  respondedAt: string | null;
  requestedBy: string;
  requestedAt: string;
}

export interface InsurerShare {
  insurerCode: string;
  percent: number;
  amount: number | null;
  premium: number | null;
}

const c = (ref: string) => `/renewal/candidates/${encodeURIComponent(ref)}`;

export const renewalHoldCoverRequestsApi = {
  list: (companyId: number, ref: string) =>
    api.get<{ defaultDays: number; requests: HoldCoverRequestView[] }>(
      `${c(ref)}/hold-cover-requests${toQuery({ companyId })}`,
    ),
  request: (companyId: number, ref: string, days?: number) =>
    api.post<HoldCoverRequestView[]>(`${c(ref)}/hold-cover-requests${toQuery({ companyId })}`, {
      days,
    }),
  extend: (companyId: number, ref: string, days: number) =>
    api.post<HoldCoverRequestView[]>(
      `${c(ref)}/hold-cover-requests/extension${toQuery({ companyId })}`,
      { days },
    ),
  respond: (
    companyId: number,
    ref: string,
    requestNo: string,
    body: { approved: boolean; end?: string; reference?: string; remarks?: string },
  ) =>
    api.post<HoldCoverRequestView>(
      `${c(ref)}/hold-cover-requests/${encodeURIComponent(requestNo)}/response${toQuery({ companyId })}`,
      body,
    ),
  insurers: (companyId: number, ref: string) =>
    api.get<InsurerShare[]>(`${c(ref)}/insurers${toQuery({ companyId })}`),
  allocate: (
    companyId: number,
    ref: string,
    shares: { insurerCode: string; percent?: number; amount?: number }[],
  ) => api.put<InsurerShare[]>(`${c(ref)}/insurers${toQuery({ companyId })}`, shares),
};

/** The kinds of request as users read them. */
export const HOLD_COVER_KINDS: Record<string, string> = {
  REQUEST: 'Request',
  EXTENSION: 'Extension',
  CBG_BATCH: 'CBG request file',
  MONTHLY_EXTENSION: 'Monthly extension',
};
