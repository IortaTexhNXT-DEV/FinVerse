import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Screening matches, runs and client risk profile API client (BRD-10, SNSRP-301, 302, 304, 602):
 * /api/v1/screening/matches, /runs and /clients/{id}/matches | risk-profile | screen.
 */

export type MatchStatus = 'POTENTIAL' | 'TRUE_MATCH' | 'FALSE_POSITIVE';

export type ScreeningTrigger =
  | 'CLIENT_REGISTERED'
  | 'CLIENT_CHANGED'
  | 'ACCOUNT_SUBMITTED'
  | 'LIST_CHANGE'
  | 'PERIODIC'
  | 'MANUAL';

export interface ScreeningMatch {
  id: number;
  runId: number;
  clientId: number;
  clientCode: string;
  clientName: string;
  entryId: number;
  entryVersion: number;
  entryName: string;
  sourceCode: string;
  listType: string;
  subjectType: 'INDIVIDUAL' | 'ENTITY';
  score: number;
  algorithm: 'EXACT' | 'PHONETIC' | 'FUZZY';
  matchedFields: string[];
  caseThreshold: boolean;
  status: MatchStatus;
  caseId?: number | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionRemarks?: string | null;
  createdAt: string;
}

/** One side of the side-by-side comparison (client or list entry). */
export interface MatchSide {
  name: string;
  reference: string;
  type: string;
  status: string;
  birthDate?: string;
  nationality?: string;
  ids?: string;
  aliases: string[];
  list?: string | null;
  entryVersion?: number | null;
}

export interface MatchDetail {
  match: ScreeningMatch;
  client: MatchSide;
  entry?: MatchSide | null;
}

export interface ScreeningRun {
  id: number;
  runNo: string;
  trigger: ScreeningTrigger;
  reference?: string;
  scope: string;
  fullRescreen: boolean;
  matchVersionId: number;
  riskVersionId?: number;
  clientsScreened: number;
  entriesScreened: number;
  matches: number;
  riskChanges: number;
  casesOpened: number;
  status: 'RUNNING' | 'SUCCESS' | 'FAILED';
  error?: string;
  startedAt: string;
  endedAt?: string;
  createdBy: string;
}

export interface RiskProfileRow {
  id: number;
  clientId: number;
  clientCode: string;
  categoryCode?: string;
  previousRating?: string;
  riskRating?: string;
  tagsAdded: string;
  tagsRemoved: string;
  activeTags: string;
  source: 'RULE' | 'MANUAL';
  riskVersionId?: number;
  ruleId?: number;
  matchId?: number;
  runId?: number;
  justification?: string;
  evidence?: string;
  evidenceCaseId?: number;
  kycReviewDue?: string;
  effectiveAt: string;
  by: string;
}

export interface MatchSearch {
  companyId: number;
  status?: MatchStatus | '';
  listType?: string;
  minScore?: string;
  maxScore?: string;
  q?: string;
  uncased?: boolean;
  page: number;
  size?: number;
}

export interface FalsePositiveRequest {
  justification: string;
  evidenceAttachmentIds?: number[];
  riskRating?: string;
  addTags?: string[];
  removeTags?: string[];
  caseId?: number;
}

export interface ManualRiskRequest {
  riskRating: string;
  addTags?: string[];
  removeTags?: string[];
  justification: string;
  evidenceAttachmentIds: number[];
  matchId?: number;
  caseId?: number;
  reference?: string;
}

export interface OpenedCase {
  caseId: number;
  caseNo: string;
  joined: boolean;
}

const BASE = '/screening';

export const screeningMatchesApi = {
  matches: (params: MatchSearch) =>
    api.get<PageResponse<ScreeningMatch>>(`${BASE}/matches${toQuery({ ...params })}`),
  match: (id: number) => api.get<MatchDetail>(`${BASE}/matches/${id}`),
  falsePositive: (id: number, request: FalsePositiveRequest) =>
    api.post<ScreeningMatch>(`${BASE}/matches/${id}/false-positive`, request),
  openCase: (id: number) => api.post<OpenedCase>(`${BASE}/matches/${id}/open-case`),
  clientMatches: (clientId: number) =>
    api.get<ScreeningMatch[]>(`${BASE}/clients/${clientId}/matches`),
  screen: (clientId: number) =>
    api.post<ScreeningRun | undefined>(`${BASE}/clients/${clientId}/screen`),
  riskProfile: (clientId: number) =>
    api.get<RiskProfileRow[]>(`${BASE}/clients/${clientId}/risk-profile`),
  updateRiskProfile: (clientId: number, request: ManualRiskRequest) =>
    api.post<RiskProfileRow>(`${BASE}/clients/${clientId}/risk-profile`, request),
  runs: (params: { companyId: number; trigger?: ScreeningTrigger | ''; page: number }) =>
    api.get<PageResponse<ScreeningRun>>(`${BASE}/runs${toQuery({ ...params })}`),
  run: (id: number) => api.get<ScreeningRun>(`${BASE}/runs/${id}`),
  runMatches: (id: number) => api.get<ScreeningMatch[]>(`${BASE}/runs/${id}/matches`),
};
