import { api, toQuery } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Production Reconciliation API (PRCID.001-039): cycles per insurer and production month,
 * extracts of the production register, insurer feedback uploads, reconciliation items with their
 * feedback, the unbooked repository and the extract schedules.
 */

export const CYCLE_ENTITY = 'ReconCycle';

export type ReconStage = 'EXTRACTED' | 'SENT_TO_INSURER' | 'RECONCILING' | 'CLOSED';
export type ReconStatus =
  | 'MATCHED'
  | 'MATCHED_WITH_DISCREPANCY'
  | 'BDOI_ONLY'
  | 'UNMATCHED_PREBOOKED'
  | 'UNMATCHED_NO_BOOKING';
export type Bucket = 'ALL' | 'MATCHED' | 'DISCREPANCY' | 'BDOI_ONLY' | 'INSURER_ONLY';
export type UnbookedStatus = 'OPEN' | 'PREBOOKED' | 'BOOKED' | 'CLOSED';
export type Frequency = 'MONTHLY' | 'WEEKLY';
export type UploadStatus = 'RECEIVED' | 'PROCESSED' | 'PARTIAL' | 'FAILED' | 'DUPLICATE_BLOCKED';
export type IncentiveOutcome = 'ELIGIBLE' | 'LATE' | 'NOT_REMITTED' | 'NO_INSURER_DATA' | 'NO_RULE';

export interface BucketCounts {
  matched: number;
  discrepancy: number;
  bdoiOnly: number;
  insurerOnly: number;
  total: number;
}

export interface ReconCycle {
  id: number;
  cycleNo: string;
  insurerCode: string;
  productionMonth: string;
  stage: ReconStage;
  closed: boolean;
  sentAt?: string;
  lastUploadAt?: string;
  lastMatchedAt?: string;
  closedAt?: string;
  counts: BucketCounts;
}

export interface ReconSide {
  policyNo?: string;
  referenceNo?: string;
  pnNo?: string;
  periodFrom?: string;
  periodTo?: string;
  assuredName?: string;
  commission?: number;
  basicPremium?: number;
  grossPremium?: number;
}

export interface ReconFeedback {
  companyConcerned?: string;
  instruction?: string;
  insurerFeedback?: string;
  marketingFeedback?: string;
  disposition?: string;
  forClosure: boolean;
}

export interface ReconItem {
  id: number;
  cycleId: number;
  status: ReconStatus;
  invoiceNo?: string;
  arn?: string;
  bookingDate?: string;
  aoUsername?: string;
  salesUnit?: string;
  segment?: string;
  productLine?: string;
  bdoi?: ReconSide;
  insurer?: ReconSide;
  insurerIncentive?: number;
  insurerRemarks?: string;
  inOriginalExtract: boolean;
  prebookedArn?: string;
  discrepancies: string[];
  matchMethod?: 'AUTO' | 'MANUAL';
  matchedAt?: string;
  matchedBy?: string;
  feedback?: ReconFeedback;
  unbookedStatus?: UnbookedStatus;
}

export interface ReconExtract {
  id: number;
  cycleId: number;
  extractNo: string;
  trigger: 'SCHEDULED' | 'MANUAL';
  bookingFrom: string;
  bookingTo: string;
  fileName: string;
  fileId?: number;
  rowCount: number;
  newCount: number;
  createdAt: string;
  createdBy: string;
  sentAt?: string;
  sentBy?: string;
  recipients?: string;
}

export interface ExtractLine {
  lineNo: number;
  invoiceNo: string;
  kind: string;
  bookingDate: string;
  policyNo?: string;
  assuredName?: string;
  aoUsername?: string;
  basicPremium: number;
  grossCommission: number;
  grossPremium: number;
  amountPaid?: number;
  datePaid?: string;
  remittanceStatus?: string;
  estimated: boolean;
}

export interface ReconUpload {
  id: number;
  cycleId?: number;
  insurerCode?: string;
  productionMonth?: string;
  fileName: string;
  attemptNo: number;
  status: UploadStatus;
  runNo?: string;
  rowsRead: number;
  rowsAccepted: number;
  rowsFailed: number;
  message?: string;
  createdAt: string;
  createdBy: string;
}

export interface UploadResult {
  runNo: string;
  runStatus: string;
  runMessage?: string;
  attempts: ReconUpload[];
}

export interface ReconSchedule {
  id: number;
  insurerCode: string;
  frequency: Frequency;
  runDay: number;
  nextRunDate: string;
  autoSend: boolean;
  recipients?: string;
  active: boolean;
  lastRunAt?: string;
  lastExtractNo?: string;
}

export interface ScheduleInput {
  companyId: number;
  insurerCode: string;
  frequency: Frequency;
  runDay: number;
  autoSend: boolean;
  recipients: string;
  active: boolean;
}

export interface ReconSettings {
  tolerance: number;
  keys: string[];
}

export interface EarlyIncentiveLine {
  invoiceNo: string;
  assuredName?: string;
  segment?: string;
  productLine?: string;
  inceptionDate?: string;
  remittedOn?: string;
  days?: number;
  ratePercent?: number;
  expected?: number;
  insurerIncentive?: number;
  outcome: IncentiveOutcome;
}

export interface ItemFilter {
  bucket: Bucket;
  q?: string;
  ao?: string;
  unit?: string;
  segment?: string;
  productLine?: string;
  page: number;
}

const BASE = '/prodrecon';
const PAGE = 20;
const enc = encodeURIComponent;

export const prodreconApi = {
  cycles: (
    companyId: number,
    filter: { insurer?: string; month?: string; stage?: string },
    page: number,
  ) =>
    api.get<PageResponse<ReconCycle>>(
      `${BASE}/cycles${toQuery({ companyId, ...filter, page, size: PAGE })}`,
    ),
  cycle: (id: number) => api.get<ReconCycle>(`${BASE}/cycles/${String(id)}`),
  items: (id: number, filter: ItemFilter) =>
    api.get<PageResponse<ReconItem>>(
      `${BASE}/cycles/${String(id)}/items${toQuery({ ...filter, size: PAGE })}`,
    ),
  automatch: (id: number) => api.post<ReconCycle>(`${BASE}/cycles/${String(id)}/automatch`),
  close: (id: number, comment: string) =>
    api.post<ReconCycle>(`${BASE}/cycles/${String(id)}/close`, { comment }),
  earlyIncentive: (id: number) =>
    api.get<EarlyIncentiveLine[]>(`${BASE}/cycles/${String(id)}/early-incentive`),
  cycleExtracts: (id: number) => api.get<ReconExtract[]>(`${BASE}/cycles/${String(id)}/extracts`),
  feedback: (id: number, feedback: ReconFeedback) =>
    api.put<ReconItem>(`${BASE}/items/${String(id)}/feedback`, feedback),
  bulkFeedback: (
    ids: number[],
    companyConcerned: string,
    disposition: string,
    forClosure: boolean,
  ) =>
    api.post<ReconItem[]>(`${BASE}/items/feedback`, {
      ids,
      companyConcerned,
      disposition,
      forClosure,
    }),
  pair: (id: number, insurerItemId: number) =>
    api.post<ReconItem>(`${BASE}/items/${String(id)}/pair`, { insurerItemId }),
  split: (id: number) => api.post<ReconItem>(`${BASE}/items/${String(id)}/split`),
  unbooked: (
    companyId: number,
    filter: { insurer?: string; status?: UnbookedStatus; q?: string },
    page: number,
  ) =>
    api.get<PageResponse<ReconItem>>(
      `${BASE}/unbooked${toQuery({ companyId, ...filter, page, size: PAGE })}`,
    ),
  extract: (companyId: number, insurerCode: string, from: string, to: string) =>
    api.post<ReconExtract>(`${BASE}/extracts`, { companyId, insurerCode, from, to }),
  extracts: (companyId: number, insurer: string | undefined, page: number) =>
    api.get<PageResponse<ReconExtract>>(
      `${BASE}/extracts${toQuery({ companyId, insurer, page, size: PAGE })}`,
    ),
  lines: (id: number, page: number) =>
    api.get<PageResponse<ExtractLine>>(
      `${BASE}/extracts/${String(id)}/lines${toQuery({ page, size: 50 })}`,
    ),
  extractFile: (id: number): Promise<DownloadedFile> =>
    api.getFile(`${BASE}/extracts/${String(id)}/file`),
  send: (id: number, to: string[], cc: string[]) =>
    api.post<ReconExtract>(`${BASE}/extracts/${String(id)}/send`, { to, cc }),
  upload: (companyId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<UploadResult>(`${BASE}/uploads${toQuery({ companyId })}`, form);
  },
  uploads: (companyId: number, insurer: string | undefined, page: number) =>
    api.get<PageResponse<ReconUpload>>(
      `${BASE}/uploads${toQuery({ companyId, insurer, page, size: PAGE })}`,
    ),
  schedules: (companyId: number) =>
    api.get<ReconSchedule[]>(`${BASE}/schedules${toQuery({ companyId })}`),
  createSchedule: (input: ScheduleInput) => api.post<ReconSchedule>(`${BASE}/schedules`, input),
  updateSchedule: (id: number, input: ScheduleInput) =>
    api.put<ReconSchedule>(`${BASE}/schedules/${enc(String(id))}`, input),
  settings: () => api.get<ReconSettings>(`${BASE}/settings`),
};
