import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';
import type { RiskProfileRow, ScreeningMatch } from '../matches/api';

/**
 * Screening cases API client (BRD-10, SNSRP-303, 401-405, 501-502, 601, 701-704):
 * /api/v1/screening/cases, its tabs and actions, the client's cases and the high-risk clients.
 */

export type CaseStage =
  | 'NEW'
  | 'INVESTIGATION'
  | 'RETURNED'
  | 'UNIT_HEAD_APPROVAL'
  | 'COMPLIANCE_REVIEW'
  | 'AML_COMMITTEE'
  | 'STR_PREPARATION'
  | 'STR_EXTRACTION'
  | 'CLOSED';

export type SlaState = 'NONE' | 'ON_TIME' | 'DUE_SOON' | 'BREACHED';

export type CaseTab = 'MY' | 'TEAM' | 'APPROVAL' | 'COMMITTEE' | 'STR' | 'CLOSED' | 'ALL';

/** The actions the user may take on a case (backend CaseActions). */
export type CaseAction =
  | 'REVIEW_EDIT'
  | 'UPLOAD'
  | 'MATCH_DECIDE'
  | 'SUBMIT'
  | 'RESUBMIT'
  | 'RISK_TAG'
  | 'DECIDE'
  | 'OUTCOME'
  | 'VOTE'
  | 'STR_EDIT'
  | 'FILING'
  | 'REOPEN'
  | 'REASSIGN';

export interface CaseRow {
  id: number;
  caseNo: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  caseType: string;
  riskCategory?: string | null;
  stage: CaseStage;
  status: 'OPEN' | 'CLOSED';
  assignee?: string | null;
  marketingUnit?: string | null;
  unitHead?: string | null;
  disposition?: string | null;
  activePolicy: boolean;
  createdAt: string;
  dueAt?: string | null;
  slaState: SlaState;
}

export interface CaseDetail {
  row: CaseRow;
  clientType: string;
  triggerCode: string;
  triggerReference?: string | null;
  templateType: string;
  accountOfficer?: string | null;
  investigator?: string | null;
  returnedFrom?: CaseStage | null;
  returnedBy?: string | null;
  roundNo: number;
  recommendation?: string | null;
  strRequired: boolean;
  committeeDecision?: string | null;
  committeeDecidedAt?: string | null;
  committeeRound: number;
  stageEnteredAt: string;
  reminderLeadHours?: number | null;
  breached: boolean;
  escalatedTo?: string | null;
  versions: (number | null)[];
  actions: CaseAction[];
}

export interface CaseEvent {
  id: number;
  event: string;
  roundNo: number;
  fromStage?: string | null;
  toStage?: string | null;
  fromValue?: string | null;
  toValue?: string | null;
  reasonCode?: string | null;
  remarks?: string | null;
  actor: string;
  occurredAt: string;
}

export interface TemplateField {
  code: string;
  section: string;
  label: string;
  dataType: 'TEXT' | 'LONG_TEXT' | 'NUMBER' | 'AMOUNT' | 'DATE' | 'LOV' | 'CHECKBOX' | 'ATTACHMENT';
  lovType?: string | null;
  mandatory: boolean;
  help?: string | null;
}

export interface CaseReview {
  templateVersionId: number;
  templateType: string;
  name: string;
  status: 'DRAFT' | 'SUBMITTED';
  submittedBy?: string | null;
  submittedAt?: string | null;
  fields: TemplateField[];
  values: Record<string, string>;
}

export interface CaseDocument {
  id: number;
  attachmentId: number;
  formType: string;
  documentType: string;
  dateReceived: string;
  source: string;
  sequenceNo: number;
  nominatedName: string;
  kycRegistered: boolean;
  createdAt: string;
  createdBy: string;
}

export interface CommitteeVote {
  id: number;
  roundNo: number;
  member: string;
  decision: string;
  remarks: string;
  votedAt: string;
}

export interface CaseTiles {
  openByStage: Partial<Record<CaseStage, number>>;
  dueToday: number;
  breached: number;
  potentialMatches: number;
  lastRunNo?: string | null;
  lastRunStatus?: string | null;
  lastRunAt?: string | null;
}

export interface HighRiskClient {
  clientCode: string;
  clientName: string;
  clientType: string;
  riskCategory?: string | null;
  riskRating?: string | null;
  tags?: string | null;
  taggedOn?: string | null;
  source?: string | null;
  openCase: string;
  activePolicy: string;
  unit?: string | null;
}

export interface CaseSearchParams {
  companyId: number;
  tab: CaseTab;
  q?: string;
  stage?: string;
  caseType?: string;
  riskCategory?: string;
  marketingUnit?: string;
  unitHead?: string;
  disposition?: string;
  assignee?: string;
  sla?: string;
  createdFrom?: string;
  createdTo?: string;
  page: number;
}

/** A case after an action, with the warnings of non-blocking validation rules. */
export interface CaseOutcome {
  screeningCase: CaseDetail;
  warnings: string[];
}

export interface StepRequest {
  disposition?: string;
  reasonCode?: string;
  remarks?: string;
}

export interface DocumentUpload {
  file: File;
  formType: string;
  documentType: string;
  dateReceived: string;
  source: string;
}

const BASE = '/screening';
const CASES = `${BASE}/cases`;

export const casesApi = {
  search: (params: CaseSearchParams) =>
    api.get<PageResponse<CaseRow>>(`${CASES}${toQuery({ ...params })}`),
  tiles: (companyId: number) => api.get<CaseTiles>(`${CASES}/tiles${toQuery({ companyId })}`),
  get: (id: number) => api.get<CaseDetail>(`${CASES}/${id}`),
  timeline: (id: number) => api.get<CaseEvent[]>(`${CASES}/${id}/timeline`),
  matches: (id: number) => api.get<ScreeningMatch[]>(`${CASES}/${id}/matches`),
  votes: (id: number) => api.get<CommitteeVote[]>(`${CASES}/${id}/votes`),
  review: (id: number) => api.get<CaseReview | undefined>(`${CASES}/${id}/review`),
  saveReview: (id: number, values: Record<string, string>) =>
    api.put<CaseReview>(`${CASES}/${id}/review`, { values }),
  documents: (id: number) => api.get<CaseDocument[]>(`${CASES}/${id}/documents`),
  upload: (id: number, doc: DocumentUpload) => {
    const form = new FormData();
    form.append('file', doc.file);
    form.append('formType', doc.formType);
    form.append('documentType', doc.documentType);
    form.append('dateReceived', doc.dateReceived);
    form.append('source', doc.source);
    return api.upload<CaseDocument>(`${CASES}/${id}/documents`, form);
  },
  submit: (
    id: number,
    request: { disposition: string; recommendation: string; strRequired: boolean },
  ) => api.post<CaseOutcome>(`${CASES}/${id}/submit`, request),
  resubmit: (id: number, request: { response: string; strRequired: boolean }) =>
    api.post<CaseOutcome>(`${CASES}/${id}/resubmit`, request),
  decide: (id: number, request: StepRequest) =>
    api.post<CaseOutcome>(`${CASES}/${id}/decision`, request),
  outcome: (id: number, request: StepRequest) =>
    api.post<CaseOutcome>(`${CASES}/${id}/outcome`, request),
  reopen: (id: number, request: StepRequest) =>
    api.post<CaseOutcome>(`${CASES}/${id}/reopen`, request),
  vote: (id: number, request: { decision: string; remarks: string }) =>
    api.post<CommitteeVote>(`${CASES}/${id}/votes`, request),
  eligible: (id: number) => api.get<string[]>(`${CASES}/${id}/eligible-assignees`),
  reassign: (id: number, request: { assignee: string; reasonCode: string; comment?: string }) =>
    api.post<CaseOutcome>(`${CASES}/${id}/reassign`, request),
  confirmMatch: (id: number, matchId: number, remarks: string) =>
    api.post<ScreeningMatch>(`${CASES}/${id}/matches/${matchId}/confirm`, { remarks }),
  clearMatch: (id: number, matchId: number, justification: string) =>
    api.post<ScreeningMatch>(`${CASES}/${id}/matches/${matchId}/false-positive`, {
      justification,
    }),
  riskTag: (
    id: number,
    request: {
      riskRating: string;
      addTags?: string[];
      removeTags?: string[];
      justification: string;
    },
  ) => api.post<RiskProfileRow>(`${CASES}/${id}/risk-tag`, request),
  clientCases: (clientId: number) => api.get<CaseRow[]>(`${BASE}/clients/${clientId}/cases`),
  highRisk: (params: {
    companyId: number;
    riskCategory?: string;
    marketingUnit?: string;
    clientType?: string;
  }) => api.get<HighRiskClient[]>(`${BASE}/high-risk-clients${toQuery({ ...params })}`),
};
