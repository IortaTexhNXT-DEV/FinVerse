import type { AuditEntry, AuditQuery } from './admin';
import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** The Product Matrix attributes of a product (BDOI FRS FRPM.003.01, FRPM.003.02, Annex A). */
export interface ProductMatrixInput {
  /** Package description shown on the Product Matrix. */
  description?: string;
  /** Incentive Eligible (default No) and its amount or commission rate. */
  incentiveEligible?: boolean;
  incentiveAmount?: number;
  incentiveRate?: number;
  /** Policy Type and Insured's Name of the Product Matrix record. */
  policyType?: string;
  insuredName?: string;
  /** The approver told of the record (optional). */
  approver?: string;
}

/** KPIs of the Product Maintenance dashboard (BDOI FRS FRPM.001.01), in screen order. */
export type PmKpi =
  'INCOMING' | 'IN_PROGRESS' | 'FOR_APPROVAL' | 'EXPIRING' | 'ISSUED' | 'DEACTIVATION';

/** Filters of the dashboard. */
export interface PmDashboardFilters {
  from?: string;
  to?: string;
  tsuOfficer?: string;
  lineCode?: string;
  /** true packages, false non-package quotation requests, undefined both. */
  packaged?: boolean;
}

export interface PmDashboardCounts {
  incoming: number;
  inProgress: number;
  forApproval: number;
  expiring: number;
  issued: number;
  deactivation: number;
}

export interface PmDashboard {
  counts: PmDashboardCounts;
  kpis: { code: PmKpi; label: string }[];
}

/** A drill-down row: the record behind a KPI. */
export interface PmDrillRow {
  kind: 'PACKAGE' | 'QUOTATION' | 'DEACTIVATION' | 'EXPIRY';
  id: number | null;
  requestNo: string;
  requestType: string;
  lineCode: string | null;
  productLine: string | null;
  requestedBy: string | null;
  assignee: string | null;
  status: string;
  submittedAt: string | null;
  agingDays: number | null;
  expiryDate: string | null;
}

export type MatrixTab = 'ACTIVE' | 'EXPIRING' | 'EXPIRED';

export interface MatrixFilters {
  tab: MatrixTab;
  text?: string;
  lineCode?: string;
  packaged?: boolean;
  sort?: string;
  direction?: 'asc' | 'desc';
}

/** A row of the Product Matrix (BDOI FRS FRPM.003.01). */
export interface MatrixRow {
  productCode: string;
  lineCode: string;
  lineOfInsurance: string;
  subLine: string | null;
  packageName: string;
  description: string | null;
  productType: string;
  insurers: string | null;
  status: string;
  effectiveDate: string | null;
  expiryDate: string | null;
  daysLeft: number | null;
  lastUpdatedBy: string | null;
  lastUpdatedAt: string | null;
  versionNo: number | null;
  deactivationId: number | null;
  deactivationNo: string | null;
}

export interface MatrixView {
  page: PageResponse<MatrixRow>;
  noticeDays: number;
}

export interface QuotationFilters {
  text?: string;
  lineCode?: string;
  status?: string;
  tsuUser?: string;
  open?: boolean;
  sort?: string;
  direction?: 'asc' | 'desc';
}

/** A row of the Quotation Request List (BDOI FRS FRPM.002.02 and FRPM.005.01). */
export interface QuotationRow {
  id: number;
  requestNo: string;
  requestDate: string;
  referenceNo: string | null;
  businessType: string;
  assuredName: string;
  lineCode: string;
  productLine: string | null;
  accountOfficer: string | null;
  tsuUser: string | null;
  submittedAt: string | null;
  effectiveDate: string | null;
  expiryDate: string | null;
  statusCode: string;
  status: string;
  agingDays: number | null;
}

export type DeactivationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

/** A package deactivation request (BDOI FRS FRPM.003.04 to FRPM.003.07). */
export interface Deactivation {
  id: number;
  requestNo: string;
  productCode: string;
  versionNo: number | null;
  packageName: string;
  effectiveDate: string;
  reason: string;
  remarks: string | null;
  approver: string;
  status: DeactivationStatus;
  statusLabel: string;
  packageExpiryDate: string | null;
  decisionRemarks: string | null;
  decidedBy: string | null;
  decidedAt: string | null;
  expiryDate: string | null;
  requestedBy: string;
  requestedAt: string;
}

export interface DeactivationInput {
  companyId: number;
  productCode: string;
  effectiveDate: string;
  reason: string;
  remarks?: string;
  approver: string;
}

export interface DeactivationFilters {
  status?: DeactivationStatus;
  text?: string;
  from?: string;
  to?: string;
}

export interface DeactivationSettings {
  /** EFFECTIVE_DATED (deactivation request) or RETIRE_REQUEST (retirement package request). */
  route: string;
  approvers: string[];
}

/** Entity type of deactivation requests (attachments, audit). */
export const DEACTIVATION_ENTITY = 'PackageDeactivation';

const base = '/product-maintenance';
const deact = `${base}/deactivations`;

/** The Product Maintenance workspace: dashboard, Product Matrix, quotation requests. */
export const pmWorkspaceApi = {
  dashboard: (companyId: number, f: PmDashboardFilters) =>
    api.get<PmDashboard>(`${base}/dashboard${toQuery({ companyId, ...f })}`),
  drillDown: (companyId: number, f: PmDashboardFilters, kpi: PmKpi, page = 0, size = 20) =>
    api.get<PageResponse<PmDrillRow>>(
      `${base}/dashboard/rows${toQuery({ companyId, ...f, kpi, page, size })}`,
    ),
  officers: () => api.get<string[]>(`${base}/dashboard/officers`),
  matrix: (f: MatrixFilters, page = 0, size = 20) =>
    api.get<MatrixView>(`${base}/matrix${toQuery({ ...f, page, size })}`),
  quotationRequests: (companyId: number, f: QuotationFilters, page = 0, size = 20) =>
    api.get<PageResponse<QuotationRow>>(
      `${base}/quotation-requests${toQuery({ companyId, ...f, page, size })}`,
    ),
};

/** The Product Maintenance audit logs (BDOI FRS FRPM.021.01 and FRPM.021.02). */
export const pmAuditApi = {
  search: (params: AuditQuery, size = 50) =>
    api.get<PageResponse<AuditEntry>>(`${base}/audit-logs${toQuery({ ...params, size })}`),
};

/** Export parameters of the audit logs report PM-AUDIT. */
export function auditExportParams(f: {
  from: string;
  to: string;
  username?: string;
  entityId?: string;
  action?: string;
}): Record<string, string> {
  const params: Record<string, string> = { fromDate: f.from, toDate: f.to };
  if (f.username) params.username = f.username;
  if (f.entityId) params.reference = f.entityId;
  if (f.action) params.action = f.action;
  return params;
}

/** Package deactivation requests. */
export const deactivationApi = {
  settings: () => api.get<DeactivationSettings>(`${deact}/settings`),
  list: (companyId: number, f: DeactivationFilters, page = 0, size = 20) =>
    api.get<PageResponse<Deactivation>>(`${deact}${toQuery({ companyId, ...f, page, size })}`),
  get: (id: number) => api.get<Deactivation>(`${deact}/${id}`),
  submit: (input: DeactivationInput) => api.post<Deactivation>(deact, input),
  approve: (id: number, remarks?: string) =>
    api.post<Deactivation>(`${deact}/${id}/approve`, { remarks }),
  reject: (id: number, remarks: string) =>
    api.post<Deactivation>(`${deact}/${id}/reject`, { remarks }),
  cancel: (id: number) => api.post<Deactivation>(`${deact}/${id}/cancel`),
  reassign: (id: number, approver: string) =>
    api.post<Deactivation>(`${deact}/${id}/reassign`, { approver }),
};

/** Export parameters of the dashboard report PM-DASHBOARD. */
export function dashboardExportParams(
  companyId: number,
  f: PmDashboardFilters,
  kpi?: PmKpi,
): Record<string, string> {
  const params: Record<string, string> = { companyId: String(companyId), kpi: kpi ?? 'ALL' };
  if (f.from) params.from = f.from;
  if (f.to) params.to = f.to;
  if (f.tsuOfficer) params.tsuOfficer = f.tsuOfficer;
  if (f.lineCode) params.lineCode = f.lineCode;
  params.packageType = packageType(f.packaged);
  return params;
}

/** Export parameters of the Product Matrix report PM-MATRIX. */
export function matrixExportParams(companyId: number, f: MatrixFilters): Record<string, string> {
  const params: Record<string, string> = { companyId: String(companyId), tab: f.tab };
  if (f.text) params.text = f.text;
  if (f.lineCode) params.lineCode = f.lineCode;
  params.packageType = packageType(f.packaged);
  return params;
}

function packageType(packaged: boolean | undefined): string {
  if (packaged === undefined) return 'ALL';
  return packaged ? 'PACKAGE' : 'NON_PACKAGE';
}
