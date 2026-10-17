import { api, toQuery } from './client';
import type { CandidateRow } from './renewalTypes';
import type { PageResponse } from './types';

/**
 * Renewal and Processing dashboards and the Annual Renewal Budget (BDOI Renewal FRS FRRN.002.02,
 * FRRN.003, FRRN.042).
 */
export interface DashboardFilters {
  from?: string;
  to?: string;
  segment?: string;
  officer?: string;
}

export interface DashboardCell {
  key: string;
  metric: string;
  count: number;
  premium: number;
  commission: number;
}

export interface DashboardCount {
  key: string;
  metric: string;
  count: number;
}

export interface ProductionRow {
  measure: string;
  label: string;
  actual: number;
  budget: number;
  percentVsBudget: number | null;
  variance: number;
  growth: number | null;
  adjustments: number;
}

export interface PipelineRow {
  category: string;
  cells: DashboardCell[];
  total: DashboardCell;
}

export interface ClosingRow {
  category: string;
  inProcess: number;
  posted: number;
  booked: number;
  total: number;
  ratio: number | null;
}

export interface DealRow {
  accountNo: string | null;
  renewalRef: string;
  assured: string | null;
  riskCode: string | null;
  businessType: string;
  team: string | null;
  premium: number;
  commission: number;
}

export interface PersistencyRow {
  key: string;
  label: string;
  renewable: number;
  renewed: number;
  countRatio: number | null;
  premiumRatio: number | null;
  commissionRatio: number | null;
}

export interface InsurerRow {
  group: string;
  totalRenewAsIs: number;
  approved: number;
  pending: number;
  returned: number;
  approvalRatio: number | null;
}

export interface RenewalDashboard {
  filter: { from: string; to: string; segment: string | null; officer: string | null };
  production: ProductionRow[];
  pipeline: { rows: PipelineRow[]; total: PipelineRow };
  ageing: DashboardCell[];
  closing: ClosingRow[];
  topDeals: DealRow[];
  topOptions: number[];
  persistency: PersistencyRow[];
  productMix: DashboardCell[];
  insurer: InsurerRow[];
  insurerPending: DashboardCount[];
  cards: DashboardCount[];
  workload: DashboardCount[];
}

export interface DrillColumn {
  key: string;
  label: string;
  kind: 'TEXT' | 'DATE' | 'AMOUNT' | 'NUMBER';
}

export interface DrillRow {
  ref: string;
  businessType: string;
  arn: string | null;
  invoiceNo: string | null;
  expiringInvoiceNo: string | null;
  assured: string | null;
  productLine: string | null;
  riskCode: string | null;
  premium: number;
  commission: number;
  status: string | null;
  assignedUser: string | null;
  insurerDisposition: string | null;
  insurerRemarks: string | null;
  expiryDate: string | null;
  extra: Record<string, string | number | null>;
}

export interface Drill {
  metric: string;
  columns: DrillColumn[];
  rows: DrillRow[];
}

export interface ProcessingCard {
  key: string;
  label: string;
  count: number;
  newBusiness: number;
  renewal: number;
}

export interface ProcessingDashboard {
  filter: { from: string; to: string };
  cards: ProcessingCard[];
  workload: Record<string, number>;
}

export interface ProcessingRow {
  arn: string;
  renewalRef: string | null;
  businessType: string;
  branch: string | null;
  department: string | null;
  unitHead: string | null;
  accountOfficer: string | null;
  assured: string | null;
  insurer: string | null;
  riskCode: string | null;
  insuranceLine: string | null;
  inceptionDate: string | null;
  expiryDate: string | null;
  datePosted: string | null;
  placementDate: string | null;
  bookingDate: string | null;
  processor: string | null;
  sumInsured: number;
  premium: number;
  commission: number;
  policyNumber: string | null;
  segment: string | null;
  policyStatus: string | null;
  ageing: number | null;
  placementAgeing: number | null;
  tatStatus: string | null;
  placementIssue: string | null;
  resolutionDate: string | null;
  policyReceivedDate: string | null;
  transmittalStatus: string | null;
  transmittalDate: string | null;
  deliveryAgeing: number | null;
  reasonForReject: string | null;
  returnedDate: string | null;
  returnReason: string | null;
}

export interface BudgetMonth {
  monthNo: number;
  newAmount: number;
  renewalAmount: number;
  organicAmount: number;
}

export interface BudgetHeads {
  unitHead: string | null;
  sectionHead: string | null;
  teamHead: string | null;
  teamLead: string | null;
}

export interface BudgetView {
  id: number;
  fiscalYear: number;
  measure: string;
  segment: string;
  region: string | null;
  team: string | null;
  subTeam: string | null;
  heads: BudgetHeads;
  accountOfficer: string | null;
  months: BudgetMonth[];
  totals: { newTotal: number; renewalTotal: number; organicTotal: number; grandTotal: number };
  createdBy: string;
  createdAt: string;
  updatedBy: string;
  updatedAt: string;
}

export interface BudgetBody {
  fiscalYear: number;
  measure: string;
  segment: string;
  region?: string;
  team?: string;
  subTeam?: string;
  heads: BudgetHeads;
  accountOfficer?: string;
  months: BudgetMonth[];
}

export interface BudgetHistory {
  field: string;
  previous: number | null;
  updated: number | null;
  modifiedBy: string;
  modifiedAt: string;
}

export interface AuditEntry {
  at: string | null;
  module: string;
  ref: string;
  client: string | null;
  actionType: string;
  description: string | null;
  oldValue: string | null;
  newValue: string | null;
  performedBy: string | null;
  remarks: string | null;
}

export interface AuditFilters {
  from?: string;
  to?: string;
  actionType?: string;
  ref?: string;
  user?: string;
}

const D = '/renewal/dashboard';
const P = '/renewal/processing-dashboard';
const B = '/renewal/budgets';

export const renewalDashboardApi = {
  dashboard: (companyId: number, f: DashboardFilters, top?: number) =>
    api.get<RenewalDashboard>(`${D}${toQuery({ companyId, ...f, top })}`),
  drill: (companyId: number, f: DashboardFilters, metric: string, top?: number) =>
    api.get<Drill>(`${D}/drill${toQuery({ companyId, ...f, metric, top })}`),
  processing: (companyId: number, f: DashboardFilters & { businessType?: string }) =>
    api.get<ProcessingDashboard>(`${P}${toQuery({ companyId, ...f })}`),
  processingDrill: (
    companyId: number,
    f: DashboardFilters & { businessType?: string },
    card: string,
    tab?: string,
  ) => api.get<ProcessingRow[]>(`${P}/drill${toQuery({ companyId, ...f, card, tab })}`),
  assignProcessor: (companyId: number, arns: string[], processor: string) =>
    api.post<number>(`${P}/assign${toQuery({ companyId })}`, { arns, processor }),
  budgets: (companyId: number, fiscalYear: number) =>
    api.get<BudgetView[]>(`${B}${toQuery({ companyId, fiscalYear })}`),
  saveBudget: (companyId: number, body: BudgetBody) =>
    api.post<BudgetView>(`${B}${toQuery({ companyId })}`, body),
  accounts: (
    companyId: number,
    q: { tab: string; q?: string; sort?: string; expiryFrom?: string; expiryTo?: string },
    page: number,
    size = 500,
  ) =>
    api.get<PageResponse<CandidateRow>>(
      `/renewal/candidates${toQuery({ companyId, ...q, page, size })}`,
    ),
  auditLogs: (companyId: number, f: AuditFilters) =>
    api.get<AuditEntry[]>(`/renewal/audit-logs${toQuery({ companyId, ...f })}`),
  exportAuditLogs: (companyId: number, f: AuditFilters, format: 'CSV' | 'XLSX') =>
    api.getFile(`/renewal/audit-logs/export${toQuery({ companyId, ...f, format })}`),
  panels: () => api.get<{ thirdBucket: string }>('/renewal/candidates/panels'),
  budgetHistory: (companyId: number, id: number) =>
    api.get<BudgetHistory[]>(`${B}/${id}/history${toQuery({ companyId })}`),
};

/** The Report Centre link that exports and prints the accounts of a figure. */
export function exportLink(report: string, params: Record<string, string | number | undefined>) {
  const query = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&');
  const path = `/reports/${report}`;
  return query === '' ? path : `${path}?${query}`;
}
