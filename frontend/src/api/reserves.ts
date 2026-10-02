import { api, toQuery } from './client';

export type IbnrMethod = 'RATE' | 'CHAIN_LADDER';
export type TriangleBasis = 'PAID' | 'INCURRED';
export type DevelopmentPeriod = 'YEAR' | 'QUARTER';
export type RunStatus = 'PREVIEW' | 'PENDING_APPROVAL' | 'APPROVED' | 'POSTED' | 'CANCELLED';

export interface ReserveParameterInput {
  companyId: number;
  businessLine: string;
  effectiveFrom: string;
  ibnrMethod: IbnrMethod;
  ibnrRate: number;
  triangleBasis: TriangleBasis;
  developmentPeriod: DevelopmentPeriod;
  accidentPeriods: number;
  mfadPct: number;
  ulaePct: number;
  expectedLossRatio: number;
  treatyCommissionPct: number;
  facCommissionPct: number;
  remarks?: string;
}

export interface ReserveParameter extends ReserveParameterInput {
  id: number;
  recordStatus: string;
  maker: string;
  authorizedBy?: string;
  authorizedAt?: string;
}

export interface TakafulSettingInput {
  companyId: number;
  enabled: boolean;
  productCodes?: string;
  participantSharePct: number;
  taxPct: number;
  costCenter?: string;
}

export interface TakafulSetting extends TakafulSettingInput {
  configured: boolean;
  recordStatus?: string;
  maker?: string;
  authorizedBy?: string;
}

export interface ValuationRun {
  id: number;
  companyId: number;
  valuationDate: string;
  periodName: string;
  status: RunStatus;
  baseCurrency: string;
  previousRunId?: number;
  calculatedAt: string;
  remarks?: string;
  preparedBy: string;
  submittedBy?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  rejectionReason?: string;
  postedBy?: string;
  postedAt?: string;
  journalCount: number;
  cancelledBy?: string;
  cancelledAt?: string;
  cancelReason?: string;
  cancelDate?: string;
}

export interface ReserveTotal {
  type: string;
  label: string;
  gross: number;
  ri: number;
  net: number;
}

export interface ReserveLine {
  type: string;
  branchId: number;
  branchCode: string;
  businessLine: string;
  productCode: string;
  sourceType: string;
  gross: number;
  ri: number;
  net: number;
  base?: number;
  rate?: number;
  method?: string;
}

export interface ReserveMovement {
  branchCode: string;
  businessLine: string;
  eventType: string;
  component: string;
  amount: number;
}

export interface ValuationRunDetail {
  run: ValuationRun;
  totals: ReserveTotal[];
  lines: ReserveLine[];
  movements: ReserveMovement[];
}

export interface UprDetail {
  policyId: number;
  documentNo: string;
  kind: string;
  branchId: number;
  businessLine: string;
  productCode: string;
  basis: string;
  coverFrom: string;
  coverTo: string;
  approvalDate: string;
  totalUnits: number;
  earnedUnits: number;
  unearnedUnits: number;
  premium: number;
  commission: number;
  cededPremium: number;
  upr: number;
  riUpr: number;
  dac: number;
  ucr: number;
}

export interface TakafulItem {
  policyNo: string;
  insuredName: string;
  businessLine: string;
  productCode: string;
  expiryDate: string;
  gross: number;
  discount: number;
  loading: number;
  commission: number;
  claims: number;
  applicable: number;
  retakaful: number;
  tax: number;
  payable: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface SummaryRow {
  reserve: string;
  businessLine: string;
  gross: number;
  ri: number;
  net: number;
  previousGross: number;
  previousRi: number;
  previousNet: number;
}

export interface ReserveSummary {
  currentRunId?: number;
  currentDate?: string;
  currentStatus?: RunStatus;
  previousRunId?: number;
  previousDate?: string;
  rows: SummaryRow[];
}

export interface AccidentRow {
  accidentPeriod: string;
  latest: number;
  cumulativeFactor: number;
  ultimate: number;
  incurred: number;
  ibnr: number;
}

export interface TriangleData {
  businessLine: string;
  basis: TriangleBasis;
  period: DevelopmentPeriod;
  paid: number[][];
  incurred: number[][];
  factors: number[];
  rows: AccidentRow[];
  ibnr: number;
}

export interface TriangleQuery {
  companyId: number;
  businessLine: string;
  asOf: string;
  basis?: TriangleBasis;
  period?: DevelopmentPeriod;
  accidentPeriods?: number;
}

const BASE = '/reserves';
const RUNS = `${BASE}/runs`;

export const reservesApi = {
  parameters: (companyId: number) =>
    api.get<ReserveParameter[]>(`${BASE}/parameters${toQuery({ companyId })}`),
  createParameter: (body: ReserveParameterInput) =>
    api.post<ReserveParameter>(`${BASE}/parameters`, body),
  updateParameter: (id: number, body: ReserveParameterInput) =>
    api.put<ReserveParameter>(`${BASE}/parameters/${id}`, body),
  authorizeParameter: (id: number) =>
    api.post<ReserveParameter>(`${BASE}/parameters/${id}/authorize`),
  deactivateParameter: (id: number) =>
    api.post<ReserveParameter>(`${BASE}/parameters/${id}/deactivate`),
  takaful: (companyId: number) =>
    api.get<TakafulSetting>(`${BASE}/takaful-setting${toQuery({ companyId })}`),
  saveTakaful: (body: TakafulSettingInput) =>
    api.put<TakafulSetting>(`${BASE}/takaful-setting`, body),
  authorizeTakaful: (companyId: number) =>
    api.post<TakafulSetting>(`${BASE}/takaful-setting/authorize${toQuery({ companyId })}`),
  runs: (companyId: number) => api.get<ValuationRun[]>(`${RUNS}${toQuery({ companyId })}`),
  run: (id: number) => api.get<ValuationRunDetail>(`${RUNS}/${id}`),
  createRun: (companyId: number, valuationDate: string) =>
    api.post<ValuationRunDetail>(RUNS, { companyId, valuationDate }),
  recalculate: (id: number) => api.post<ValuationRunDetail>(`${RUNS}/${id}/recalculate`),
  submit: (id: number) => api.post<ValuationRun>(`${RUNS}/${id}/submit`),
  approve: (id: number) => api.post<ValuationRun>(`${RUNS}/${id}/approve`),
  reject: (id: number, reason: string) =>
    api.post<ValuationRun>(`${RUNS}/${id}/reject`, { reason }),
  post: (id: number) => api.post<ValuationRun>(`${RUNS}/${id}/post`),
  cancel: (id: number, reason: string) =>
    api.post<ValuationRun>(`${RUNS}/${id}/cancel`, { reason }),
  upr: (id: number, businessLine: string | undefined, page: number) =>
    api.get<Page<UprDetail>>(`${RUNS}/${id}/upr${toQuery({ businessLine, page, size: 50 })}`),
  takafulLines: (id: number) => api.get<TakafulItem[]>(`${RUNS}/${id}/takaful`),
  summary: (companyId: number, asOf: string) =>
    api.get<ReserveSummary>(`${BASE}/summary${toQuery({ companyId, asOf })}`),
  triangles: (q: TriangleQuery) => api.get<TriangleData>(`${BASE}/triangles${toQuery({ ...q })}`),
};
