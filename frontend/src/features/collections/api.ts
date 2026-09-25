import { api } from '@/api/client';
import type { PageResponse } from '@/api/types';

/** Collections core API client (BRCLXN.001-029, 043-057): /api/v1/collections. */

export type ItemStatus = 'OPEN' | 'COMPLETED' | 'EXCLUDED_CANCELLED' | 'CREDIT';

export interface CollectionItem {
  id: number;
  invoiceNo: string;
  arn: string;
  policyNo?: string;
  policyYear: number;
  clientCode: string;
  assuredName: string;
  insurerCode: string;
  segment?: string;
  salesUnit?: string;
  unitHead?: string;
  aoUsername?: string;
  currency: string;
  bookingDate: string;
  inceptionDate: string;
  expiryDate: string;
  dpFlag: boolean;
  cwtFlag: boolean;
  invoiceCategory: string;
  grossPremium: number;
  netOutstanding: number;
  outstandingPr2307: number;
  agingDays: number;
  agingBracket?: string;
  paymentStatus: string;
  status: ItemStatus;
  listedOn: string;
  completedOn?: string;
  currentHandler?: string;
  dispositionCode?: string;
  category?: string;
  taggingOwner?: string;
  lastEffortAt?: string;
  lastEffortCode?: string;
  remarks?: string;
  promiseStatus?: string;
  escalationLevel?: string;
  installmentOverdue: boolean;
  lastRefreshedAt: string;
}

export interface LedgerFacts {
  kind: string;
  rootInvoiceNo?: string;
  accountId?: number;
  paymentStatus: string;
  remittanceStatus: string;
  hold: boolean;
  pendingNegativeAdjustment: boolean;
  writtenOff: boolean;
  cancelled: boolean;
  lockOwner?: string;
  lockReason?: string;
}

export interface BalanceLine {
  component: string;
  booked: number;
  adjusted: number;
  applied: number;
  writtenOff: number;
  balance: number;
}

export interface EditLock {
  editingBy?: string;
  since?: string;
  mine: boolean;
}

export interface Account {
  item: CollectionItem;
  ledger?: LedgerFacts;
  breakdown: BalanceLine[];
  lock: EditLock;
}

export interface PaymentLine {
  date: string;
  type: 'APPLIED' | 'UNAPPLIED';
  sourceModule: string;
  reference: string;
  arNo?: string;
  orNo?: string;
  amount: number;
}

export interface Payments {
  booked: number;
  paid: number;
  outstanding: number;
  lines: PaymentLine[];
}

export interface Share {
  insurerCode: string;
  sharePct: number;
  lead: boolean;
}

export interface FamilyLine {
  invoiceNo: string;
  kind: string;
  endorsementNo?: string;
  bookingDate: string;
  grossPremium: number;
  premiumBalance: number;
  paymentStatus: string;
}

export interface Policy {
  invoiceNo: string;
  policyNo?: string;
  policyYear: number;
  arn: string;
  accountId?: number;
  productLine?: string;
  riskCode?: string;
  bookingDate: string;
  inceptionDate: string;
  expiryDate: string;
  firstReceiptDate?: string;
  shares: Share[];
  family: FamilyLine[];
}

export interface Assignment {
  id: number;
  handler: string;
  previousHandler?: string;
  kind: 'PERMANENT' | 'TEMPORARY' | 'RULE' | 'REVERT';
  validFrom: string;
  validTo?: string;
  reason: string;
  assignedBy: string;
  endedAt?: string;
  bulkRef?: string;
  createdAt: string;
}

export interface FieldChange {
  id: number;
  entity: string;
  field: string;
  oldValue?: string;
  newValue?: string;
  username: string;
  changedAt: string;
  sourceIp?: string;
  bulkRef?: string;
}

export interface TimelineEntry {
  at: string;
  kind: string;
  title: string;
  detail?: string;
  by?: string;
  amount?: number;
}

export interface Disposition {
  id: number;
  code: string;
  category?: string;
  taggingOwner?: string;
  opsAction: string;
  remarks?: string;
  effectiveOn: string;
  details?: string;
  source: string;
  outboxId?: number;
  supersededBy?: number;
  bulkRef?: string;
  createdAt: string;
  createdBy: string;
}

export interface Effort {
  id: number;
  code: string;
  at: string;
  channel?: string;
  contactPerson?: string;
  remarks?: string;
  bulkRef?: string;
  createdBy: string;
}

export interface Handoff {
  id: number;
  feedCode: string;
  key: string;
  status: 'PENDING' | 'TAKEN' | 'CANCELLED';
  fields: Record<string, string>;
  createdAt: string;
  takenAt?: string;
}

export interface GroupTotal {
  key: string;
  name?: string;
  items: number;
  netOutstanding: number;
}

export interface ClientView {
  clientCode: string;
  name?: string;
  items: CollectionItem[];
  openOutstanding: number;
  openPr2307: number;
}

export interface Tile {
  key: string;
  label: string;
  value: number;
  link: string;
  alert: boolean;
}

export interface AgingCell {
  segment?: string;
  bracket?: string;
  items: number;
  netOutstanding: number;
}

export interface Home {
  tiles: Tile[];
  aging: AgingCell[];
}

export interface Criteria {
  segment?: string;
  salesUnit?: string;
  clientCode?: string;
  amountFrom?: number;
  amountTo?: number;
  agingFrom?: number;
  agingTo?: number;
  handler?: string;
}

export interface Rule {
  id: number;
  priority: number;
  name: string;
  criteria: Criteria;
  handler: string;
  active: boolean;
  updatedBy: string;
}

export interface RuleInput {
  priority: number;
  name: string;
  criteria: Criteria;
  handler: string;
}

export interface ReassignInput {
  invoiceNos?: string[];
  criteria?: Criteria;
  handler?: string;
  kind?: 'PERMANENT' | 'TEMPORARY';
  validTo?: string;
  reason?: string;
}

export interface ScheduledFile {
  id: number;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ON_REQUEST';
  reportCode: string;
  periodKey: string;
  periodFrom: string;
  periodTo: string;
  scope: string;
  reportRunId?: number;
  rowCount: number;
  availableFrom?: string;
  available: boolean;
  status: 'PUBLISHED' | 'FAILED';
  message?: string;
  createdAt: string;
  createdBy: string;
}

export interface Parameter {
  key: string;
  value: string;
  valueType: string;
  description: string;
  minValue?: number;
  maxValue?: number;
  updatedBy: string;
}

export interface DispositionValue {
  typeCode: string;
  code: string;
  label: string;
  effectiveTo?: string;
  attributes: { attribute: string; value: string }[];
}

export interface Unit {
  code: string;
  name: string;
  level: string;
  parentCode?: string;
  headUsername?: string;
}

export interface Setup {
  parameters: Parameter[];
  dispositions: DispositionValue[];
  units: Unit[];
}

export interface DispositionRule {
  code: string;
  label: string;
  category?: string;
  taggingOwner?: string;
  opsAction: 'NONE' | 'CWT2307_REVERSAL' | 'DP_REVERSAL' | 'CHECK_PICKUP' | 'CANCEL_REQUEST';
  allowedRoles: string[];
}

export interface DispositionInput {
  invoiceNos: string[];
  code: string;
  remarks?: string;
  effectiveOn?: string;
  details?: Record<string, string>;
}

export interface EffortInput {
  invoiceNos: string[];
  code: string;
  at?: string;
  channel?: string;
  contactPerson?: string;
  remarks?: string;
}

const base = '/collections';
const item = (no: string) => `${base}/items/${encodeURIComponent(no)}`;
const co = (companyId: number) => `companyId=${companyId}`;

export const collectionsApi = {
  home: (companyId: number) => api.get<Home>(`${base}/home?${co(companyId)}`),
  worklist: (companyId: number, query: string, page: number, size = 20) =>
    api.get<PageResponse<CollectionItem>>(
      `${base}/worklist?${co(companyId)}&${query}&page=${page}&size=${size}`,
    ),
  totals: (companyId: number, query: string, groupBy: 'CLIENT' | 'ARN') =>
    api.get<GroupTotal[]>(`${base}/worklist/totals?${co(companyId)}&${query}&groupBy=${groupBy}`),
  account: (no: string) => api.get<Account>(item(no)),
  payments: (no: string) => api.get<Payments>(`${item(no)}/payments`),
  policy: (no: string) => api.get<Policy>(`${item(no)}/policy`),
  assignments: (no: string) => api.get<Assignment[]>(`${item(no)}/assignments`),
  history: (no: string, page: number) =>
    api.get<PageResponse<FieldChange>>(`${item(no)}/history?page=${page}`),
  timeline: (no: string) => api.get<TimelineEntry[]>(`${item(no)}/timeline`),
  dispositions: (no: string) => api.get<Disposition[]>(`${item(no)}/dispositions`),
  efforts: (no: string) => api.get<Effort[]>(`${item(no)}/efforts`),
  handoffs: (no: string) => api.get<Handoff[]>(`${item(no)}/handoffs`),
  lock: (no: string) => api.post<EditLock>(`${item(no)}/lock`),
  unlock: (no: string) => api.delete(`${item(no)}/lock`),
  refreshOne: (no: string) => api.post<CollectionItem>(`${item(no)}/refresh`),
  details: (companyId: number, no: string, remarks: string, category: string) =>
    api.put<CollectionItem>(`${item(no)}/details?${co(companyId)}`, { remarks, category }),
  dispose: (companyId: number, input: DispositionInput) =>
    api.post<Disposition[]>(`${base}/dispositions?${co(companyId)}`, input),
  effort: (companyId: number, input: EffortInput) =>
    api.post<Effort[]>(`${base}/efforts?${co(companyId)}`, input),
  client: (companyId: number, clientCode: string) =>
    api.get<ClientView>(`${base}/clients/${encodeURIComponent(clientCode)}?${co(companyId)}`),
  dispositionRules: () => api.get<DispositionRule[]>(`${base}/disposition-rules`),
  handlers: () => api.get<string[]>(`${base}/handlers`),
  rules: (companyId: number) => api.get<Rule[]>(`${base}/assignment-rules?${co(companyId)}`),
  createRule: (companyId: number, input: RuleInput) =>
    api.post<Rule>(`${base}/assignment-rules?${co(companyId)}`, input),
  updateRule: (id: number, input: RuleInput) =>
    api.put<Rule>(`${base}/assignment-rules/${id}`, input),
  activateRule: (id: number, active: boolean) =>
    api.post<Rule>(`${base}/assignment-rules/${id}/active`, { active }),
  preview: (companyId: number, input: ReassignInput) =>
    api.post<{ total: number; items: CollectionItem[] }>(
      `${base}/reassignments/preview?${co(companyId)}`,
      input,
    ),
  reassign: (companyId: number, input: ReassignInput) =>
    api.post<{ bulkRef?: string; moved: number }>(`${base}/reassignments?${co(companyId)}`, input),
  files: (companyId: number, frequency: string, page: number) => {
    const filter = frequency === '' ? '' : '&frequency=' + frequency;
    return api.get<PageResponse<ScheduledFile>>(
      `${base}/files?${co(companyId)}${filter}&page=${page}`,
    );
  },
  generate: (companyId: number, frequency: string, date: string) =>
    api.post<ScheduledFile[]>(`${base}/files/generate?${co(companyId)}`, { frequency, date }),
  export: (companyId: number, segment: string, salesUnit: string) =>
    api.post<ScheduledFile>(`${base}/exports?${co(companyId)}`, { segment, salesUnit }),
  downloadFile: (reportRunId: number) => api.getFile(`/reports/runs/${reportRunId}/file`),
  refresh: (companyId: number) => api.post<{ message: string }>(`${base}/refresh?${co(companyId)}`),
  setup: (companyId: number) => api.get<Setup>(`${base}/setup?${co(companyId)}`),
  updateParameter: (companyId: number, key: string, value: string) =>
    api.put<Parameter>(`${base}/setup/parameters/${key}?${co(companyId)}`, { value }),
  setAttribute: (
    companyId: number,
    input: { typeCode: string; code: string; attribute: string; value: string },
  ) => api.put<undefined>(`${base}/setup/lov-attributes?${co(companyId)}`, input),
  setUnitHead: (companyId: number, unitCode: string, username: string) =>
    api.put<Unit>(`${base}/setup/unit-heads/${unitCode}?${co(companyId)}`, { username }),
};
