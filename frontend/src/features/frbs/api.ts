import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Accounting reports API (BRD-5 FRBS 2.10, 3.2.0): the BDOI report pack, the account schedule
 * definitions and their commentary, the service-fee runs with their lines and invoices, and the
 * service-fee rules and recipients.
 */

export const RUN_ENTITY = 'FrbsServiceFeeRun';

export type RunStage =
  'COMPUTED' | 'FOR_APPROVAL' | 'APPROVED' | 'RELEASED' | 'LIQUIDATED' | 'CANCELLED';

export type LineStatus = 'COMPUTED' | 'SENT' | 'RETURNED' | 'RELEASED' | 'LIQUIDATED' | 'CANCELLED';

export interface PackEntry {
  groupCode: string;
  groupName: string;
  reportCode: string;
  scheduleCode?: string;
  title: string;
  sourceRef?: string;
  wordRequested: boolean;
  available: boolean;
}

export type Measure =
  | 'OPENING'
  | 'DEBITS'
  | 'CREDITS'
  | 'MOVEMENT'
  | 'CLOSING'
  | 'COMPARATIVE'
  | 'VARIANCE'
  | 'VARIANCE_PCT';

export type ScheduleFamily = 'GARD' | 'SUBSIDIARIES' | 'SCHEDULE' | 'AGING' | 'OTHER';
export type Grouping =
  'ACCOUNT' | 'PARTY' | 'DOCUMENT' | 'COST_CENTER' | 'BRANCH' | 'BUSINESS_LINE';
export type Comparative = 'NONE' | 'PREVIOUS_MONTH' | 'PREVIOUS_YEAR';

export interface ScheduleColumn {
  seq?: number;
  measure: Measure;
  label: string;
}

export interface ScheduleValues {
  name: string;
  family: ScheduleFamily;
  sourceRef?: string;
  description?: string;
  selectorKind: 'ACCOUNT_PREFIX' | 'REPORT_GROUP';
  accountSelector: string;
  grouping: Grouping;
  currency?: string;
  side: 'DEBIT' | 'CREDIT';
  basis: 'BALANCE' | 'MOVEMENT';
  ageingSlots?: string;
  comparative: Comparative;
  commentary: boolean;
  boardDocument: boolean;
  layoutStatus: 'TO_CONFIRM' | 'CONFIRMED';
  active: boolean;
  columns: ScheduleColumn[];
}

export interface Schedule {
  code: string;
  values: ScheduleValues;
  wordOutput: boolean;
  updatedAt: string;
}

export interface ScheduleComment {
  rowKey: string;
  period: string;
  text: string;
  by: string;
  at: string;
}

export interface ServiceFeeRun {
  id: number;
  runNo: string;
  companyId: number;
  periodFrom: string;
  periodTo: string;
  stage: RunStage;
  invoiceCount: number;
  feeTotal: number;
  computedAt: string;
  createdBy: string;
  submittedBy?: string;
  approvedBy?: string;
  approvedAt?: string;
}

export interface ServiceFeeLine {
  id: number;
  lineNo: number;
  segment: string;
  salesUnit: string;
  payeeCode: string;
  payeeName: string;
  costCenter?: string;
  status: LineStatus;
  amounts: {
    currency: string;
    rate: number;
    invoiceCount: number;
    commission: number;
    wtax: number;
    base: number;
    fee: number;
  };
  payout: {
    accrualBatchNo?: string;
    requestNo?: string;
    gatewayStatus?: string;
    dvNo?: string;
    message?: string;
    sendCount: number;
  };
  tags: {
    releasedOn?: string;
    releasedBy?: string;
    liquidatedOn?: string;
    liquidatedBy?: string;
    liquidationReportId?: string;
    remarks?: string;
  };
}

export interface ServiceFeeInvoice {
  lineId: number;
  invoiceNo: string;
  rootInvoiceNo?: string;
  clientCode: string;
  assuredName: string;
  insurerCode: string;
  marketSegment?: string;
  paidOn: string;
  commission: number;
  wtax: number;
  base: number;
  fee: number;
}

export interface ServiceFeeRule {
  id: number;
  segment: string;
  marketSegments: string[];
  rate: number;
  base: string;
  netOfWtax: boolean;
  effectiveFrom: string;
  effectiveTo?: string;
  active: boolean;
  description?: string;
}

export type RuleBody = Omit<ServiceFeeRule, 'id' | 'base'>;

export interface ServiceFeeRecipient {
  id: number;
  salesUnit: string;
  payeeCode: string;
  payeeName: string;
  costCenter?: string;
  active: boolean;
}

export type RecipientBody = Omit<ServiceFeeRecipient, 'id'>;

export type StageCounts = Partial<Record<RunStage, number>>;

const FEE = '/frbs/service-fee';
const SCHEDULES = '/finreport/schedules';

export const frbsApi = {
  pack: () => api.get<PackEntry[]>('/frbs/report-pack'),

  schedules: (activeOnly = false) => api.get<Schedule[]>(`${SCHEDULES}${toQuery({ activeOnly })}`),
  schedule: (code: string) => api.get<Schedule>(`${SCHEDULES}/${encodeURIComponent(code)}`),
  createSchedule: (code: string, values: ScheduleValues) =>
    api.post<Schedule>(SCHEDULES, { ...values, code }),
  updateSchedule: (code: string, values: ScheduleValues) =>
    api.put<Schedule>(`${SCHEDULES}/${encodeURIComponent(code)}`, { ...values, code }),
  comments: (code: string, companyId: number, period: string) =>
    api.get<ScheduleComment[]>(
      `${SCHEDULES}/${encodeURIComponent(code)}/comments${toQuery({ companyId, period })}`,
    ),
  comment: (
    code: string,
    body: { companyId: number; period: string; rowKey: string; text: string },
  ) =>
    api.put<ScheduleComment | undefined>(`${SCHEDULES}/${encodeURIComponent(code)}/comments`, body),

  runs: (companyId: number, filter: { stage?: RunStage; q?: string; page?: number }) =>
    api.get<PageResponse<ServiceFeeRun>>(
      `${FEE}/runs${toQuery({ companyId, stage: filter.stage, q: filter.q, page: filter.page, size: 20 })}`,
    ),
  counts: (companyId: number) =>
    api.get<StageCounts>(`${FEE}/runs/counts${toQuery({ companyId })}`),
  run: (id: number) => api.get<ServiceFeeRun>(`${FEE}/runs/${String(id)}`),
  lines: (id: number) => api.get<ServiceFeeLine[]>(`${FEE}/runs/${String(id)}/lines`),
  invoices: (id: number) => api.get<ServiceFeeInvoice[]>(`${FEE}/runs/${String(id)}/invoices`),
  compute: (companyId: number, from: string, to: string) =>
    api.post<ServiceFeeRun>(`${FEE}/runs${toQuery({ companyId })}`, { from, to }),
  recompute: (id: number) => api.post<ServiceFeeRun>(`${FEE}/runs/${String(id)}/recompute`),
  submit: (id: number, comment?: string) =>
    api.post<ServiceFeeRun>(`${FEE}/runs/${String(id)}/submit`, { comment }),
  approve: (id: number, comment?: string) =>
    api.post<ServiceFeeRun>(`${FEE}/runs/${String(id)}/approve`, { comment }),
  resend: (lineId: number) => api.post<ServiceFeeLine>(`${FEE}/lines/${String(lineId)}/resend`),
  release: (lineId: number, releasedOn: string) =>
    api.post<ServiceFeeLine>(`${FEE}/lines/${String(lineId)}/release`, { releasedOn }),
  liquidate: (lineId: number, liquidatedOn: string, remarks: string, file: File) => {
    const form = new FormData();
    form.append('liquidatedOn', liquidatedOn);
    form.append('remarks', remarks);
    form.append('file', file);
    return api.upload<ServiceFeeLine>(`${FEE}/lines/${String(lineId)}/liquidate`, form);
  },

  rules: () => api.get<ServiceFeeRule[]>(`${FEE}/rules`),
  createRule: (body: RuleBody) => api.post<ServiceFeeRule>(`${FEE}/rules`, body),
  updateRule: (id: number, body: RuleBody) =>
    api.put<ServiceFeeRule>(`${FEE}/rules/${String(id)}`, body),
  recipients: (companyId: number) =>
    api.get<ServiceFeeRecipient[]>(`${FEE}/recipients${toQuery({ companyId })}`),
  createRecipient: (companyId: number, body: RecipientBody) =>
    api.post<ServiceFeeRecipient>(`${FEE}/recipients${toQuery({ companyId })}`, body),
  updateRecipient: (id: number, body: RecipientBody) =>
    api.put<ServiceFeeRecipient>(`${FEE}/recipients/${String(id)}`, body),
};
