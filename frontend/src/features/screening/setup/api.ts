import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Compliance Setup API client (BRD-10, SNSRP-101-109, 201-204): versioned screening configuration
 * (/api/v1/screening/config) and watchlists, sources and ingestion runs
 * (/api/v1/screening/watchlist).
 */

export type ConfigType =
  | 'MATCH_CRITERIA'
  | 'RISK_RULES'
  | 'APPROVAL_MATRIX'
  | 'ASSIGNMENT_MATRIX'
  | 'SLA_MATRIX'
  | 'VALIDATION_RULES'
  | 'TEMPLATE'
  | 'STR_LAYOUT';

export type ConfigStatus = 'DRAFT' | 'PENDING' | 'ACTIVE' | 'SUPERSEDED' | 'REJECTED';

export interface ConfigVersion {
  id: number;
  companyId: number;
  type: ConfigType;
  scope?: string;
  versionNo: number;
  label: string;
  status: ConfigStatus;
  effectiveFrom: string;
  changeNote?: string;
  baseVersionId?: number;
  createdBy: string;
  createdAt: string;
  submittedBy?: string;
  submittedAt?: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionReason?: string;
}

/** A configuration row: the fields of one rule, keyed as in the backend records. */
export type ConfigRow = Record<string, unknown>;

export interface TemplateContent {
  templateType: string;
  name: string;
  fields: ConfigRow[];
}

export interface LayoutContent {
  format: string;
  delimiter?: string;
  encoding: string;
  columns: ConfigRow[];
}

export interface ConfigContent {
  matchRules: ConfigRow[];
  riskCategories: ConfigRow[];
  riskRules: ConfigRow[];
  routes: ConfigRow[];
  assignmentRules: ConfigRow[];
  slaRules: ConfigRow[];
  validationRules: ConfigRow[];
  template?: TemplateContent | null;
  layout?: LayoutContent | null;
}

export interface ConfigChange {
  item: string;
  attribute: string;
  before?: string | null;
  after?: string | null;
}

export interface ConfigVersionDetail {
  version: ConfigVersion;
  content: ConfigContent;
  changes: ConfigChange[];
}

export type EntryStatus = 'DRAFT' | 'PENDING' | 'ACTIVE' | 'INACTIVE';
export type ChangeStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Alias {
  name: string;
  type: 'AKA' | 'FKA' | 'SPELLING';
}

export interface EntryValues {
  listType?: string;
  entityType?: 'INDIVIDUAL' | 'ENTITY';
  primaryName?: string;
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  nationality?: string;
  idNumbers?: string;
  listedOn?: string;
  delistedOn?: string;
  aliases: Alias[];
}

export interface WatchlistEntry {
  id: number;
  sourceCode: string;
  externalRef: string;
  listType: string;
  entityType: 'INDIVIDUAL' | 'ENTITY';
  primaryName: string;
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  nationality?: string;
  idNumbers?: string;
  listedOn?: string;
  delistedOn?: string;
  status: EntryStatus;
  effectiveFrom?: string;
  entryVersion: number;
  remarks?: string;
}

export interface WatchlistChange {
  id: number;
  entryId: number;
  externalRef: string;
  primaryName: string;
  runId?: number;
  changeType: 'ADD' | 'UPDATE' | 'DEACTIVATE';
  before?: EntryValues | null;
  after: EntryValues;
  makerRemarks: string;
  status: ChangeStatus;
  createdBy: string;
  createdAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionRemarks?: string;
}

export interface EntryDetail {
  entry: WatchlistEntry;
  aliases: Alias[];
  history: WatchlistChange[];
}

export interface EntryRequest extends EntryValues {
  sourceCode?: string;
  remarks: string;
}

export interface ListSource {
  id: number;
  code: string;
  name: string;
  listType: string;
  transport: 'FILE' | 'API' | 'MANUAL';
  schedule?: string;
  fileLayout?: string;
  fullFile: boolean;
  active: boolean;
}

export type RunStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL' | 'FAILED';

export interface IngestionRun {
  id: number;
  runNo: string;
  sourceCode: string;
  trigger: 'SCHEDULED' | 'MANUAL_UPLOAD' | 'API';
  fileName?: string;
  fileAttachmentId?: number;
  received: number;
  added: number;
  updated: number;
  delisted: number;
  unchanged: number;
  failed: number;
  pendingApproval: boolean;
  status: RunStatus;
  error?: string;
  startedAt: string;
  endedAt?: string;
  createdBy: string;
}

export interface RunDetail {
  run: IngestionRun;
  errors: { lineNo: number; rawRecord?: string; reason: string; digested: boolean }[];
  pendingChanges: number;
}

const CONFIG = '/screening/config';
const LISTS = '/screening/watchlist';

export const screeningSetupApi = {
  versions: (companyId: number, type: ConfigType) =>
    api.get<ConfigVersion[]>(`${CONFIG}/versions${toQuery({ companyId, type })}`),
  version: (id: number) => api.get<ConfigVersionDetail>(`${CONFIG}/versions/${id}`),
  newDraft: (companyId: number, type: ConfigType, scope?: string) =>
    api.post<ConfigVersionDetail>(`${CONFIG}/drafts`, { companyId, type, scope }),
  saveDraft: (
    id: number,
    body: { effectiveFrom: string; changeNote?: string; content: Partial<ConfigContent> },
  ) => api.put<ConfigVersionDetail>(`${CONFIG}/versions/${id}`, body),
  submit: (id: number) => api.post<ConfigVersionDetail>(`${CONFIG}/versions/${id}/submit`),
  withdraw: (id: number) => api.post<ConfigVersion>(`${CONFIG}/versions/${id}/withdraw`),
  approve: (id: number) => api.post<ConfigVersion>(`${CONFIG}/versions/${id}/approve`),
  reject: (id: number, remarks: string) =>
    api.post<ConfigVersion>(`${CONFIG}/versions/${id}/reject`, { remarks }),

  sources: () => api.get<ListSource[]>(`${LISTS}/sources`),
  updateSource: (code: string, source: ListSource) =>
    api.put<ListSource>(`${LISTS}/sources/${code}`, source),
  template: () => api.getFile(`${LISTS}/template`),
  upload: (code: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<IngestionRun>(`${LISTS}/sources/${code}/upload`, form);
  },
  runs: (params: { source?: string; page: number; size?: number }) =>
    api.get<PageResponse<IngestionRun>>(`${LISTS}/runs${toQuery(params)}`),
  run: (id: number) => api.get<RunDetail>(`${LISTS}/runs/${id}`),
  approveRun: (id: number) => api.post<{ approved: number }>(`${LISTS}/runs/${id}/approve`),

  entries: (params: {
    source?: string;
    status?: EntryStatus | '';
    search?: string;
    page: number;
    size?: number;
  }) => api.get<PageResponse<WatchlistEntry>>(`${LISTS}/entries${toQuery(params)}`),
  entry: (id: number) => api.get<EntryDetail>(`${LISTS}/entries/${id}`),
  addEntry: (request: EntryRequest) => api.post<WatchlistChange>(`${LISTS}/entries`, request),
  changeEntry: (id: number, request: EntryRequest) =>
    api.put<WatchlistChange>(`${LISTS}/entries/${id}`, request),
  deactivateEntry: (id: number, remarks: string) =>
    api.post<WatchlistChange>(`${LISTS}/entries/${id}/deactivate`, { remarks }),
  changes: (status: ChangeStatus, page: number) =>
    api.get<PageResponse<WatchlistChange>>(`${LISTS}/changes${toQuery({ status, page })}`),
  change: (id: number) => api.get<WatchlistChange>(`${LISTS}/changes/${id}`),
  approveChange: (id: number, remarks?: string) =>
    api.post<WatchlistChange>(`${LISTS}/changes/${id}/approve`, { remarks }),
  rejectChange: (id: number, remarks: string) =>
    api.post<WatchlistChange>(`${LISTS}/changes/${id}/reject`, { remarks }),
};
