import { api } from '@/api/client';
import type { PageResponse, RecordStatus } from '@/api/types';
import type { ItemResult } from '../plans/api';
import { toQuery } from '../plans/api';

/** Escalations API client (BRCLXN.049/050): /api/v1/collections/escalations and rules. */

export type EscalationStage =
  'RAISED' | 'WITH_TL' | 'WITH_UH' | 'IN_ACTION' | 'RETURNED' | 'RESOLVED';
export type TargetLevel = 'TL' | 'UH' | 'SECTION_HEAD' | 'USER';
export type Basis =
  | 'AGING_FROM_BOOKING'
  | 'AGING_FROM_INCEPTION'
  | 'NO_COMMITMENT_BY_DAY'
  | 'BROKEN_PROMISES_COUNT'
  | 'INSTALLMENT_OVERDUE_DAYS'
  | 'AMOUNT_OVER';

export interface EscalationItem {
  invoiceNo: string;
  policyNo?: string;
  balance: number;
  agingDays: number;
}

export interface Escalation {
  id: number;
  escalationNo: string;
  kind: 'AUTO' | 'MANUAL';
  ruleCode?: string;
  arn: string;
  clientCode: string;
  assuredName: string;
  targetLevel: TargetLevel;
  targetUsername?: string;
  reasonCode: string;
  remarks?: string;
  totalBalance: number;
  currency: string;
  slaHours: number;
  status: EscalationStage;
  stageSince: string;
  overdue: boolean;
  bulkRef?: string;
  resolvedAt?: string;
  resolution?: string;
  raisedBy: string;
  createdAt: string;
  items: EscalationItem[];
}

export interface EscalateInput {
  companyId: number;
  invoiceNos: string[];
  targetLevel: TargetLevel;
  targetUsername?: string;
  reasonCode: string;
  remarks?: string;
}

export interface Rule {
  id: number;
  code: string;
  name: string;
  basis: Basis;
  threshold: number;
  segment?: string;
  salesUnit?: string;
  productLine?: string;
  amountFrom?: number;
  amountTo?: number;
  targetLevel: TargetLevel;
  targetUsername?: string;
  reasonCode: string;
  slaHours: number;
  notifyTarget: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
  recordStatus: RecordStatus;
  maker: string;
  authorizedBy?: string;
}

export type RuleInput = Omit<Rule, 'id' | 'recordStatus' | 'maker' | 'authorizedBy'> & {
  companyId: number;
};

export interface RuleMatch {
  invoiceNo: string;
  arn: string;
  assuredName: string;
  bookingDate: string;
  inceptionDate: string;
  balance: number;
  currency: string;
}

const BASE = '/collections/escalations';
const RULES = '/collections/escalation-rules';

export const escalationsApi = {
  escalations: (companyId: number, stage: readonly EscalationStage[], q: string, page: number) =>
    api.get<PageResponse<Escalation>>(`${BASE}${toQuery({ companyId, stage, q, page, size: 20 })}`),
  escalation: (id: number) => api.get<Escalation>(`${BASE}/${id}`),
  act: (id: number, action: string, body: { reasonCode?: string; comment?: string }) =>
    api.post<Escalation>(`${BASE}/${id}/actions/${action}`, body),
  escalate: (input: EscalateInput) => api.post<ItemResult[]>('/collections/bulk/escalate', input),
  rules: (companyId: number) => api.get<Rule[]>(`${RULES}${toQuery({ companyId })}`),
  createRule: (input: RuleInput) => api.post<Rule>(RULES, input),
  updateRule: (id: number, input: RuleInput) => api.put<Rule>(`${RULES}/${id}`, input),
  authorizeRule: (id: number) => api.post<Rule>(`${RULES}/${id}/authorize`),
  deactivateRule: (id: number) => api.post<Rule>(`${RULES}/${id}/deactivate`),
  matches: (id: number) => api.get<RuleMatch[]>(`${RULES}/${id}/matches`),
};
