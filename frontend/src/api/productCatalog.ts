import { api, toQuery } from './client';
import type { Authorizable } from './catalog';

/**
 * Product Maintenance catalog data (BRD-3; PRODUCT_MAINTENANCE_DESIGN section 10): package
 * versions and the validation checkpoint, coverages and clauses, incentive criteria and rate-scheme
 * exceptions.
 */

export type InsurerRole = 'LEAD' | 'PARTICIPANT' | 'PANEL';
export type ProductLifecycle = 'ACTIVE' | 'EXPIRED' | 'RETIRED';
export type VersionStatus = 'DRAFT' | 'FOR_VALIDATION' | 'RELEASED' | 'SUPERSEDED' | 'EXPIRED';
export type IncentiveValueBasis = 'RATE' | 'FIXED_AMOUNT' | 'RULE';

export interface VersionSummary {
  productCode: string;
  productName: string;
  versionNo: number;
  status: VersionStatus;
  effectiveFrom: string;
  effectiveTo?: string;
  packageEndDate?: string;
  defaultRate?: number;
  minimumPremium: number;
  sourceRequestNo?: string;
  changeSummary?: string;
  maker: string;
  submittedBy?: string;
  submittedAt?: string;
  validatedBy?: string;
  validatedAt?: string;
}

export interface SchemeTerms {
  defaultRate?: number;
  minimumPremium: number;
  defaultCommissionRate: number;
  maxSumInsured?: number;
  ratingBasisNote?: string;
  manualRateAllowed: boolean;
}

export interface VersionCoverage {
  coverageCode: string;
  included: boolean;
  optional: boolean;
  limitAmount?: number;
  subLimit?: number;
  deductibleAmount?: number;
  deductiblePercent?: number;
  deductibleText?: string;
  sortOrder: number;
}

export interface VersionInsurer {
  insurerCode: string;
  role: InsurerRole;
  sharePercent?: number;
  rate?: number;
  minimumPremium?: number;
  defaultBranchCode?: string;
}

export interface VersionInsurerTerm {
  insurerCode: string;
  coverageCode: string;
  included: boolean;
  limitAmount?: number;
  subLimit?: number;
  deductibleAmount?: number;
  deductiblePercent?: number;
  deductibleText?: string;
  /** Comma-separated clause codes. */
  clauseCodes?: string;
  remarks?: string;
}

export interface VersionDetail {
  summary: VersionSummary;
  lineCode: string;
  coverTypeCode?: string;
  packageStartDate?: string;
  anniversaryDate?: string;
  scheme: SchemeTerms;
  coverages: VersionCoverage[];
  insurers: VersionInsurer[];
  insurerTerms: VersionInsurerTerm[];
  mancomSignoffRef?: string;
  validationChecklist?: string;
  testPremium?: number;
  returnedReason?: string;
}

interface Deductible {
  amount?: number;
  percent?: number;
  text?: string;
}

/** Body of a draft version (PUT). */
export interface VersionContentInput {
  companyId: number;
  rateScheme: {
    defaultRate?: number;
    minimumPremium: number;
    defaultCommissionRate: number;
    maxSumInsured?: number;
    ratingBasisNote?: string;
  };
  dates: {
    effectiveFrom: string;
    packageStartDate?: string;
    packageEndDate?: string;
    anniversaryDate?: string;
  };
  coverages: {
    coverageCode: string;
    included: boolean;
    optional: boolean;
    limitAmount?: number;
    subLimit?: number;
    deductible?: Deductible;
    sortOrder: number;
  }[];
  insurers: VersionInsurer[];
  insurerTerms: {
    insurerCode: string;
    coverageCode: string;
    included: boolean;
    limitAmount?: number;
    deductible?: Deductible;
    clauseCodes: string[];
    remarks?: string;
  }[];
  changeSummary?: string;
  mancomSignoffRef?: string;
}

export interface Coverage extends Authorizable {
  lineCode: string;
  code: string;
  name: string;
  kind: string;
  basic: boolean;
  sortOrder: number;
}

export interface CoverageInput {
  lineCode: string;
  code: string;
  name: string;
  kind: string;
  basic: boolean;
  sortOrder: number;
}

export interface Clause extends Authorizable {
  code: string;
  kind: string;
  lineCode?: string;
  title: string;
  wording: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface ClauseInput {
  code: string;
  kind: string;
  lineCode?: string;
  title: string;
  wording: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface IncentiveScope {
  productCode: string;
  coverTypeCode?: string;
  marketSegment?: string;
  sourceChannel?: string;
  insurerCode?: string;
}

export interface IncentiveInput {
  companyId: number;
  code: string;
  name: string;
  incentiveType: string;
  valueBasis: IncentiveValueBasis;
  value?: number;
  ruleParams?: string;
  description?: string;
  products: IncentiveScope[];
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface IncentiveCriteria extends Authorizable {
  companyId: number;
  code: string;
  name: string;
  incentiveType: string;
  valueBasis: IncentiveValueBasis;
  value?: number;
  ruleParams?: string;
  description?: string;
  products: IncentiveScope[];
  effectiveFrom: string;
  effectiveTo?: string;
  successorOf?: number;
}

export interface RateExceptionInput {
  productCode: string;
  requestedVersionNo?: number;
  requestedRate?: number;
  transactionRef: string;
  reason: string;
  validUntil?: string;
}

export interface RateException extends Authorizable {
  referenceNo: string;
  productCode: string;
  purpose: string;
  requestedVersionNo?: number;
  requestedRate?: number;
  transactionRef: string;
  reason: string;
  validUntil: string;
  requestedBy: string;
  requestedAt: string;
}

const base = '/catalog';
const version = (code: string, no: number) => `${base}/products/${code}/versions/${no}`;

export const productCatalogApi = {
  versions: (code: string) => api.get<VersionSummary[]>(`${base}/products/${code}/versions`),
  version: (code: string, no: number) => api.get<VersionDetail>(version(code, no)),
  newVersion: (code: string, companyId: number, changeSummary: string) =>
    api.post<VersionDetail>(`${base}/products/${code}/versions`, { companyId, changeSummary }),
  saveVersion: (code: string, no: number, input: VersionContentInput) =>
    api.put<VersionDetail>(version(code, no), input),
  submitVersion: (code: string, no: number) =>
    api.post<VersionDetail>(`${version(code, no)}/submit`, {}),
  validateVersion: (code: string, no: number, checklist: string[]) =>
    api.post<VersionDetail>(`${version(code, no)}/validate`, { checklist }),
  returnVersion: (code: string, no: number, reason: string) =>
    api.post<VersionDetail>(`${version(code, no)}/return`, { reason }),
  validationQueue: () => api.get<VersionSummary[]>(`${base}/validation-queue`),

  coverages: (line?: string) => api.get<Coverage[]>(`${base}/coverages${toQuery({ line })}`),
  createCoverage: (input: CoverageInput) => api.post<Coverage>(`${base}/coverages`, input),
  updateCoverage: (id: number, input: CoverageInput) =>
    api.put<Coverage>(`${base}/coverages/${id}`, input),
  clauses: () => api.get<Clause[]>(`${base}/clauses`),
  createClause: (input: ClauseInput) => api.post<Clause>(`${base}/clauses`, input),
  updateClause: (id: number, input: ClauseInput) => api.put<Clause>(`${base}/clauses/${id}`, input),

  incentives: (companyId: number) =>
    api.get<IncentiveCriteria[]>(`${base}/incentive-criteria${toQuery({ companyId })}`),
  incentiveHistory: (id: number) =>
    api.get<IncentiveCriteria[]>(`${base}/incentive-criteria/${id}/history`),
  createIncentive: (input: IncentiveInput) =>
    api.post<IncentiveCriteria>(`${base}/incentive-criteria`, input),
  updateIncentive: (id: number, input: IncentiveInput) =>
    api.put<IncentiveCriteria>(`${base}/incentive-criteria/${id}`, input),
  deactivateIncentive: (id: number, lastDay?: string) =>
    api.post<IncentiveCriteria>(`${base}/incentive-criteria/${id}/deactivate`, { lastDay }),

  rateExceptions: (transactionRef?: string) =>
    api.get<RateException[]>(`${base}/rate-scheme-exceptions${toQuery({ transactionRef })}`),
  requestRateException: (input: RateExceptionInput) =>
    api.post<RateException>(`${base}/rate-scheme-exceptions`, input),
};
