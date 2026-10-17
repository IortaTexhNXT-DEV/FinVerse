import { api, toQuery } from './client';
import type { DownloadedFile } from './client';

/**
 * Case management of the Customer Servicing Facility (FRS BRD-9 FR-CSF-050 to 061): work queues,
 * the case record and its actions, the Case Dashboard and the Case Routing setup.
 */

export type CaseQueue =
  'MINE' | 'RETURNED' | 'CSR_ACTION' | 'TEAM' | 'UNIT' | 'OPEN' | 'CLOSED' | 'ALL';

export type CaseAction =
  | 'RESOLVE'
  | 'RETURN'
  | 'CSR_ACTION'
  | 'REENDORSE'
  | 'CLOSE'
  | 'REASSIGN'
  | 'ESCALATE'
  | 'CHANGE_CATEGORY'
  | 'NOTE'
  | 'UPLOAD'
  | 'FOLLOW_UP';

export interface Coded {
  code: string;
  label: string;
}

export interface CaseSla {
  ageDays: number;
  status: 'WITHIN_TAT' | 'APPROACHING' | 'BREACHED';
  label: string;
}

export interface CaseRow {
  id: number;
  caseNo: string;
  createdAt: string;
  client: string | null;
  clientId: number | null;
  arn: string | null;
  policyNo: string | null;
  caseType: Coded;
  tatDays: number;
  nature: Coded;
  fulfilmentUnit: Coded;
  marketSegment: Coded;
  priority: Coded;
  owner: string;
  agent: string;
  status: string;
  statusLabel: string;
  statusGroup: string;
  dueDate: string;
  sla: CaseSla;
  escalated: { reason: Coded; at: string } | null;
  subject: string;
}

export interface LinkedCase {
  id: number;
  caseNo: string;
  relation: 'ORIGINAL' | 'FOLLOW_UP';
  statusLabel: string;
  reason: string | null;
}

export interface CaseRecord {
  row: CaseRow;
  companyId: number;
  channel: Coded;
  productType: Coded;
  description: string;
  callerContact: string | null;
  returnReason: Coded | null;
  instruction: string | null;
  resolution: string | null;
  closureReason: Coded | null;
  closedAt: string | null;
  closedBy: string | null;
  linked: LinkedCase[];
  actions: CaseAction[];
}

export interface CaseNote {
  note: string;
  author: string;
  createdAt: string;
}

export interface CaseHistory {
  at: string;
  user: string;
  action: string;
  summary: string;
  from: string | null;
  to: string | null;
}

export interface CaseDocument {
  id: number;
  fileName: string;
  documentType: string | null;
  sizeBytes: number;
  uploadedBy: string;
  uploadedAt: string;
}

export interface LogCaseBody {
  clientId?: number;
  accountId?: number;
  callerName?: string;
  callerContact?: string;
  channel: string;
  caseType: string;
  nature: string;
  productType: string;
  marketSegment: string;
  fulfilmentUnit: string;
  priority: string;
  subject: string;
  description: string;
  resolveNow: boolean;
  resolution?: string;
  closureReason?: string;
}

export interface CaseCount {
  code: string;
  label: string;
  count: number;
}

export interface CaseDashboard {
  created: number;
  closed: number;
  open: number;
  averageAgeClosed: number | null;
  slaCompliance: number | null;
  breached: number;
  notBreached: number;
  escalated: number;
  byStatus: CaseCount[];
  byType: CaseCount[];
  byNature: CaseCount[];
  byUnit: CaseCount[];
  ageing: CaseCount[];
  openCases: CaseRow[];
}

export interface DashboardFilters {
  from?: string;
  to?: string;
  caseType?: string;
  priority?: string;
  status?: string;
  marketSegment?: string;
  fulfilmentUnit?: string;
}

export interface TurnaroundTime {
  caseType: string;
  tatDays: number;
  autoEscalate: boolean;
}

export interface CaseRoute {
  id: number;
  caseType: string;
  marketSegment: string;
  nature: string;
  fulfilmentUnit: string;
  escalationGroup: string | null;
  active: boolean;
}

export interface UnitMember {
  id: number;
  unitCode: string;
  username: string;
}

const B = '/csf/cases';
const S = '/csf/case-setup';
const co = (companyId: number) => `?companyId=${String(companyId)}`;
const one = (id: number, path = '') => `${B}/${String(id)}${path}`;

export const csfCaseApi = {
  queue: (companyId: number, queue: CaseQueue) =>
    api.get<CaseRow[]>(`${B}${toQuery({ companyId, queue })}`),
  ofClient: (companyId: number, clientId: number) =>
    api.get<CaseRow[]>(`${B}/clients/${String(clientId)}${co(companyId)}`),
  propose: (companyId: number, caseType: string, marketSegment: string, nature: string) =>
    api.get<{ fulfilmentUnit: string | null }>(
      `${B}/proposal${toQuery({ companyId, caseType, marketSegment, nature })}`,
    ),
  log: (companyId: number, body: LogCaseBody) => api.post<CaseRecord>(`${B}${co(companyId)}`, body),
  get: (companyId: number, id: number) => api.get<CaseRecord>(`${one(id)}${co(companyId)}`),
  notes: (companyId: number, id: number) =>
    api.get<CaseNote[]>(`${one(id, '/notes')}${co(companyId)}`),
  addNote: (companyId: number, id: number, text: string) =>
    api.post<CaseNote[]>(`${one(id, '/notes')}${co(companyId)}`, { text }),
  history: (companyId: number, id: number) =>
    api.get<CaseHistory[]>(`${one(id, '/history')}${co(companyId)}`),
  documents: (companyId: number, id: number) =>
    api.get<CaseDocument[]>(`${one(id, '/documents')}${co(companyId)}`),
  upload: (companyId: number, id: number, documentType: string, file: File) => {
    const form = new FormData();
    form.append('documentType', documentType);
    form.append('file', file);
    return api.upload<CaseDocument>(`${one(id, '/documents')}${co(companyId)}`, form);
  },
  download: (companyId: number, id: number, documentId: number): Promise<DownloadedFile> => {
    const path = one(id, '/documents/' + String(documentId) + '/content');
    return api.getFile(path + co(companyId));
  },
  act: (companyId: number, id: number, path: string, body: Record<string, unknown>) =>
    api.post<CaseRecord>(one(id, '/' + path) + co(companyId), body),
  dashboard: (companyId: number, filters: DashboardFilters) =>
    api.get<CaseDashboard>(`${B}/dashboard${toQuery({ companyId, ...filters })}`),
  turnaroundTimes: () => api.get<TurnaroundTime[]>(`${S}/turnaround-times`),
  saveTurnaroundTime: (body: TurnaroundTime) =>
    api.put<TurnaroundTime[]>(`${S}/turnaround-times`, body),
  routes: () => api.get<CaseRoute[]>(`${S}/routes`),
  saveRoute: (body: Omit<CaseRoute, 'id'>, id?: number) =>
    id === undefined
      ? api.post<CaseRoute[]>(`${S}/routes`, body)
      : api.put<CaseRoute[]>(`${S}/routes/${String(id)}`, body),
  members: () => api.get<UnitMember[]>(`${S}/members`),
  addMember: (unitCode: string, username: string) =>
    api.post<UnitMember[]>(`${S}/members`, { unitCode, username }),
  removeMember: (id: number) => api.delete(`${S}/members/${String(id)}`),
};
