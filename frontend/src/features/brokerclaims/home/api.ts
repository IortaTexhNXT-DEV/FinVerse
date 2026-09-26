import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/** Claims home, worklist, reassignment and loss experience: /api/v1/broker-claims. */

export interface HomeTile {
  key: string;
  label: string;
  value: number;
  alert: boolean;
  link: string;
}

export interface StatusCount {
  phase: string;
  statusCode?: string;
  statusLabel?: string;
  claims: number;
}

export interface BucketCount {
  bucket: string;
  claims: number;
}

export interface ClaimsHome {
  tiles: HomeTile[];
  byStatus: StatusCount[];
  ageing: BucketCount[];
}

export type WorklistTab = 'MINE' | 'OPEN' | 'TEMP_CLOSED' | 'CLOSED' | 'FOLLOW_UPS_DUE' | 'ALL';
export type WorklistFlag = 'OVERDUE' | 'UNPAID_PREMIUM' | 'AWAITING_REMITTANCE';

export interface WorklistRow {
  id: number;
  claimNo: string;
  cover: {
    arn: string;
    policyYear: number;
    policyNo?: string;
    assuredName?: string;
    claimantName?: string;
    insurerClaimNos?: string;
  };
  handling: {
    handler: string;
    unitCode?: string;
    statusCode?: string;
    statusLabel?: string;
    phase: string;
    closureKind?: string;
  };
  dates: {
    reportedDate: string;
    lossDate: string;
    nextFollowUpDate?: string;
    ageThisStage: number;
    ageOverall: number;
    followUpOverdue: boolean;
  };
  nextActionPlan?: string;
  premiumStatus?: string;
  currency: string;
  claimAmount?: number;
}

export interface WorklistQuery {
  tab: WorklistTab;
  flag?: WorklistFlag;
  status?: string;
  q?: string;
}

export interface Assignee {
  username: string;
  unitCode?: string;
  team?: string;
}

export interface ClaimExperience {
  arn: string;
  policyYear?: number;
  claimCount: number;
  openCount: number;
  paid: number;
  outstanding: number;
  total: number;
  statuses: Record<string, number>;
}

export const claimsHomeApi = {
  home: (companyId: number) => api.get<ClaimsHome>(`/broker-claims/home${toQuery({ companyId })}`),
  worklist: (companyId: number, query: WorklistQuery, page: number) =>
    api.get<PageResponse<WorklistRow>>(
      `/broker-claims/worklist${toQuery({ companyId, ...query, page, size: 20 })}`,
    ),
  assignees: () => api.get<Assignee[]>('/broker-claims/assignees'),
  reassign: (companyId: number, claimIds: number[], handler: string, comment?: string) =>
    api.post<{ moved: number }>(`/broker-claims/reassign${toQuery({ companyId })}`, {
      claimIds,
      handler,
      comment,
    }),
  experience: (arn: string, policyYear?: number) =>
    api.get<ClaimExperience>(`/broker-claims/experience${toQuery({ arn, policyYear })}`),
};
