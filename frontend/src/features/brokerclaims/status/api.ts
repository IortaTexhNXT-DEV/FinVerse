import { api, toQuery } from '@/api/client';

/** Claim status actions and history (BRCLM.005/010-021/027/035): /api/v1/broker-claims/{id}. */

export type ClaimPhase = 'NEW' | 'IN_PROGRESS' | 'TEMP_CLOSED' | 'CLOSED';

export interface ClaimProgress {
  claimId: number;
  claimNo: string;
  handler: string;
  status: {
    code?: string;
    label: string;
    phase: ClaimPhase;
    since?: string;
    closureKind?: 'TEMPORARY' | 'PERMANENT';
    awaitingPremiumRemittance: boolean;
  };
  ages: { thisStage: number; overall: number };
  settlement: {
    typeCode?: string;
    typeLabel: string;
    amount?: number;
    dateSettled?: string;
    closedOn?: string;
  };
  followUp: {
    nextFollowUpDate?: string;
    overridden: boolean;
    nextActionPlan?: string;
    adjusterCode?: string;
    adjusterName?: string;
  };
}

export interface StatusOption {
  code: string;
  label: string;
  phase: ClaimPhase;
}

export interface Stamp {
  by: string;
  at: string;
  remark?: string;
}

export interface StatusChange {
  fromStatus?: string;
  fromLabel?: string;
  fromPhase?: ClaimPhase;
  toStatus: string;
  toLabel: string;
  toPhase: ClaimPhase;
  stamp: Stamp;
  daysInPrevious?: number;
}

export interface FieldChange {
  field: string;
  oldValue?: string;
  newValue?: string;
  stamp: Stamp;
}

export interface ClaimHistory {
  statusChanges: StatusChange[];
  fieldChanges: FieldChange[];
}

export interface SettlementInput {
  typeCode: string;
  amount?: number;
  dateSettled?: string;
  remark?: string;
}

const base = (id: number, companyId: number, path: string) =>
  `/broker-claims/${id}/${path}${toQuery({ companyId })}`;

export const claimStatusApi = {
  progress: (id: number, companyId: number) =>
    api.get<ClaimProgress>(base(id, companyId, 'progress')),
  history: (id: number, companyId: number) => api.get<ClaimHistory>(base(id, companyId, 'history')),
  allowedStatuses: (id: number, companyId: number) =>
    api.get<StatusOption[]>(base(id, companyId, 'allowed-statuses')),
  changeStatus: (id: number, companyId: number, statusCode: string, remark?: string) =>
    api.post<unknown>(base(id, companyId, 'status'), { statusCode, remark }),
  settle: (id: number, companyId: number, input: SettlementInput) =>
    api.post<unknown>(base(id, companyId, 'settlement'), input),
  reopen: (id: number, companyId: number, reasonCode: string, remark?: string) =>
    api.post<unknown>(base(id, companyId, 'reopen'), { reasonCode, remark }),
  overrideFollowUp: (id: number, companyId: number, date: string, reasonCode: string) =>
    api.post<unknown>(base(id, companyId, 'follow-up'), { date, reasonCode }),
  planNextAction: (id: number, companyId: number, text: string) =>
    api.put<unknown>(base(id, companyId, 'action-plan'), { text }),
  assignAdjuster: (id: number, companyId: number, adjusterCode: string, remark?: string) =>
    api.put<unknown>(base(id, companyId, 'adjuster'), { adjusterCode, remark }),
};
