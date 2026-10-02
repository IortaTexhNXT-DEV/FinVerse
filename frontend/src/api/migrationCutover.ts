import { api, toQuery } from './client';

/**
 * Cutover and run-off of the data migration (BRD-13): the cutover plans (mock runs, dress
 * rehearsal, production cut-over) with their runbook tasks, the go / no-go criteria and decisions,
 * the monthly run-off of the legacy in-force headers and the legacy decommissioning checklists.
 */

export type PlanKind = 'MOCK' | 'DRESS_REHEARSAL' | 'PRODUCTION';
export type TaskStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE' | 'BLOCKED' | 'SKIPPED';

export interface CutoverPlan {
  planNo: string;
  name: string;
  kind: PlanKind;
  mockNo?: number;
  environment: string;
  goLiveDate: string;
  freezeStart?: string;
  freezeEnd?: string;
  status: string;
}

export interface CutoverTask {
  seq: number;
  phase: string;
  task: string;
  ownerRole: string;
  objectCode?: string;
  dependsOn?: string;
  plannedStart?: string;
  plannedEnd?: string;
  actualStart?: string;
  actualEnd?: string;
  status: TaskStatus;
  remarks?: string;
  updatedBy?: string;
}

export interface GonogoCriterion {
  criterionNo: number;
  name: string;
  threshold: string;
  manual: boolean;
  measuredValue?: string;
  met?: boolean;
  note?: string;
  measuredBy?: string;
  measuredAt?: string;
}

export interface GonogoDecision {
  decision: 'GO' | 'NO_GO';
  comment?: string;
  criteriaMet: number;
  criteriaTotal: number;
  decidedBy: string;
  decidedAt: string;
}

export interface PlanDetail {
  plan: CutoverPlan;
  tasks: CutoverTask[];
  criteria: GonogoCriterion[];
  decisions: GonogoDecision[];
}

export interface PlanInput {
  name: string;
  kind: PlanKind;
  mockNo?: number;
  environment: string;
  goLiveDate: string;
}

export interface RunoffCohort {
  snapshotDate: string;
  expiryMonth: string;
  sourceSystem: string;
  headersInForce: number;
  premiumInForce: number;
  renewed: number;
  notRenewed: number;
  lapsed: number;
  stillOpen: number;
}

export type DecommissionStatus = 'OPEN' | 'MET' | 'SIGNED' | 'NOT_APPLICABLE';

export interface DecommissionItem {
  id: number;
  systemCode: string;
  milestone: 'SYSTEM' | 'CONTEXT';
  criterion: string;
  name: string;
  description: string;
  evidence?: string;
  status: DecommissionStatus;
  signedBy?: string;
  signedAt?: string;
}

const BASE = '/migration';

export const cutoverApi = {
  plans: (companyId: number) =>
    api.get<CutoverPlan[]>(`${BASE}/cutover/plans${toQuery({ companyId })}`),
  plan: (planNo: string) => api.get<PlanDetail>(`${BASE}/cutover/plans/${planNo}`),
  create: (companyId: number, input: PlanInput) =>
    api.post<CutoverPlan>(`${BASE}/cutover/plans${toQuery({ companyId })}`, input),
  progress: (planNo: string, seq: number, status: TaskStatus, note: string) =>
    api.post<CutoverTask>(`${BASE}/cutover/plans/${planNo}/tasks/${String(seq)}`, {
      status,
      note,
    }),
  measure: (planNo: string) =>
    api.post<GonogoCriterion[]>(`${BASE}/cutover/plans/${planNo}/measure`),
  record: (planNo: string, no: number, met: boolean, note: string) =>
    api.post<GonogoCriterion>(`${BASE}/cutover/plans/${planNo}/criteria/${String(no)}`, {
      met,
      note,
    }),
  decide: (planNo: string, go: boolean, comment: string) =>
    api.post<GonogoDecision>(`${BASE}/cutover/plans/${planNo}/decision`, { go, comment }),
  runbook: (planNo: string) => api.getFile(`${BASE}/cutover/plans/${planNo}/runbook`),
  runoff: (companyId: number) => api.get<RunoffCohort[]>(`${BASE}/runoff${toQuery({ companyId })}`),
  snapshot: (companyId: number, date: string) =>
    api.post<RunoffCohort[]>(`${BASE}/runoff/snapshot${toQuery({ companyId, date })}`),
  checklists: (companyId: number) =>
    api.get<DecommissionItem[]>(`${BASE}/decommission${toQuery({ companyId })}`),
  open: (companyId: number, system: string) =>
    api.post<DecommissionItem[]>(`${BASE}/decommission/systems/${system}${toQuery({ companyId })}`),
  update: (id: number, status: DecommissionStatus, evidence: string) =>
    api.post<DecommissionItem>(`${BASE}/decommission/items/${String(id)}`, { status, evidence }),
};
