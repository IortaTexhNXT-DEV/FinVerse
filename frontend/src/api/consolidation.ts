import { api, toQuery } from './client';

export interface Relationship {
  id: number;
  companyAId: number;
  aDueFromAccount: string;
  aDueToAccount: string;
  companyBId: number;
  bDueFromAccount: string;
  bDueToAccount: string;
  active: boolean;
}

export type IcTransactionType = 'CHARGE' | 'SETTLEMENT';

export interface IcTransactionInput {
  type: IcTransactionType;
  creditorCompanyId: number;
  debtorCompanyId: number;
  valueDate: string;
  currency: string;
  amount: number;
  creditorAccount: string;
  debtorAccount: string;
  narration: string;
  costCenter?: string;
}

export interface IcTransaction extends IcTransactionInput {
  id: number;
  icReference: string;
  creditorBatchNo: string;
  debtorBatchNo: string;
  createdBy: string;
}

export interface ReconciliationLine {
  relationshipId: number;
  creditorCompanyId: number;
  debtorCompanyId: number;
  dueFromAccount: string;
  dueToAccount: string;
  currency: string;
  dueFromFc: number;
  dueToFc: number;
  differenceFc: number;
  dueFromBase: number;
  dueToBase: number;
  matched: boolean;
}

export interface GroupMember {
  companyId: number;
  ownershipPct: number;
  investmentAccount?: string;
  equityAccounts: string[];
}

export interface ConsolidationGroup {
  id: number;
  code: string;
  name: string;
  parentCompanyId: number;
  currency: string;
  ctaAccount: string;
  nciAccount: string;
  goodwillAccount: string;
  active: boolean;
  members: GroupMember[];
}

export type RunStatus = 'DRAFT' | 'FINAL' | 'CANCELLED';

export interface TbLine {
  accountCode: string;
  accountName: string;
  accountClass: string;
  aggregated: number;
  eliminations: number;
  consolidated: number;
}

export interface RunLine {
  lineNo: number;
  type: 'TRANSLATED' | 'CTA' | 'ELIMINATION';
  ruleCode?: string;
  companyId?: number;
  accountCode: string;
  accountName: string;
  localAmount: number;
  rate: number;
  amount: number;
  description?: string;
}

export interface ConsolidationRun {
  id: number;
  groupId: number;
  runNo: string;
  asOfDate: string;
  currency: string;
  status: RunStatus;
  totalDebit: number;
  totalCredit: number;
  createdBy: string;
  finalizedBy?: string;
  trialBalance?: TbLine[];
  lines?: RunLine[];
}

export const intercompanyApi = {
  relationships: (companyId?: number) =>
    api.get<Relationship[]>(`/intercompany/relationships${toQuery({ companyId })}`),
  createRelationship: (body: Omit<Relationship, 'id' | 'active'>) =>
    api.post<Relationship>('/intercompany/relationships', body),
  setActive: (id: number, active: boolean) =>
    api.post<Relationship>(`/intercompany/relationships/${id}/active${toQuery({ active })}`),
  transactions: (companyId: number) =>
    api.get<IcTransaction[]>(`/intercompany/transactions${toQuery({ companyId })}`),
  post: (body: IcTransactionInput) => api.post<IcTransaction>('/intercompany/transactions', body),
  reconciliation: (companyId: number, asOf: string) =>
    api.get<ReconciliationLine[]>(`/intercompany/reconciliation${toQuery({ companyId, asOf })}`),
};

export const consolidationApi = {
  groups: () => api.get<ConsolidationGroup[]>('/consolidation/groups'),
  createGroup: (body: Omit<ConsolidationGroup, 'id'>) =>
    api.post<ConsolidationGroup>('/consolidation/groups', body),
  runs: (groupId: number) => api.get<ConsolidationRun[]>(`/consolidation/groups/${groupId}/runs`),
  run: (groupId: number, asOf: string) =>
    api.post<ConsolidationRun>(`/consolidation/groups/${groupId}/runs${toQuery({ asOf })}`),
  get: (runId: number) => api.get<ConsolidationRun>(`/consolidation/runs/${runId}`),
  finalize: (runId: number) => api.post<ConsolidationRun>(`/consolidation/runs/${runId}/finalize`),
};
