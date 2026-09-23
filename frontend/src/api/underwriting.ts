import { api, toQuery } from './client';
import type { PageResponse, RecordStatus } from './types';

/** Underwriting API: products, quotations, policies, endorsements, open covers. */

export type UprBasis = 'DAYS_365' | 'TWENTY_FOURTHS' | 'EIGHTHS';
export type SourceType = 'DIRECT' | 'AGENT' | 'BROKER';
export type BusinessType = 'DIRECT' | 'DIRECT_WITH_COINSURANCE';
export type PolicyStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'CANCELLED';
export type EndorsementType = 'ADDITIONAL' | 'REFUND' | 'RENEWAL' | 'CANCELLATION' | 'NIL';
export type QuotationStatus =
  'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'CONVERTED' | 'EXPIRED';

export interface ProductInput {
  companyId: number;
  code: string;
  name: string;
  businessLine: string;
  defaultCommissionRate: number;
  uprBasis: UprBasis;
  dstRate: number;
  vatRate: number;
  lgtRate: number;
  fstRate: number;
  premiumTaxRate: number;
  policyFee: number;
  openCoverAllowed: boolean;
}

export interface Product extends ProductInput {
  id: number;
  recordStatus: RecordStatus;
  createdBy: string;
  updatedBy?: string;
  authorizedBy?: string;
}

export interface MarineDetails {
  vesselName?: string;
  voyageFrom?: string;
  voyageTo?: string;
  sailDate?: string;
  blNo?: string;
  blDate?: string;
  lcNo?: string;
  bankName?: string;
  valuationBasis?: string;
}

export interface RiskInput extends MarineDetails {
  /** Client-side row key for editing (ignored by the server). */
  key?: string;
  description: string;
  sumInsured: number;
  rate?: number;
  premium?: number;
  occupation?: string;
  accumulationZone?: string;
}

export interface Risk {
  lineNo: number;
  description: string;
  sumInsured: number;
  rate: number;
  premium: number;
  occupation?: string;
  accumulationZone?: string;
  marine?: MarineDetails;
}

export interface Premium {
  sumInsured: number;
  ourSumInsured: number;
  grossPremium: number;
  discountAmount: number;
  loadingAmount: number;
  netPremium: number;
  ourGrossPremium: number;
  ourDiscount: number;
  ourLoading: number;
  ourNetPremium: number;
  coinsurerPremium: number;
  billedPremium: number;
  dst: number;
  vat: number;
  lgt: number;
  fst: number;
  premiumTax: number;
  policyFee: number;
  taxesAndCharges: number;
  totalDue: number;
  commissionRate: number;
  commission: number;
  withholdingRate: number;
  withholdingTax: number;
  netCommission: number;
}

export interface DocumentStatus {
  status: PolicyStatus;
  createdBy: string;
  createdAt?: string;
  submittedBy?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  approvalDate?: string;
  rejectionReason?: string;
  debitNoteNo?: string;
  creditNoteNo?: string;
  premiumBatchNo?: string;
  commissionBatchNo?: string;
  exchangeRate?: number;
}

export interface PolicyInput {
  companyId: number;
  branchId: number;
  productId: number;
  customerCode: string;
  insuredName: string;
  sourceType: SourceType;
  intermediaryCode?: string;
  issueDate: string;
  periodFrom: string;
  periodTo: string;
  currency: string;
  businessType: BusinessType;
  sharePct: number;
  coinsurerCode?: string;
  coinsuranceLeader: boolean;
  discountRate?: number;
  loadingRate?: number;
  commissionRate?: number;
  risks: RiskInput[];
}

export interface Policy {
  id: number;
  companyId: number;
  branchId: number;
  policyNo: string;
  productId: number;
  productCode: string;
  productName: string;
  businessLine: string;
  customerCode: string;
  customerName: string;
  insuredName: string;
  sourceType: SourceType;
  intermediaryCode?: string;
  intermediaryName?: string;
  issueDate: string;
  periodFrom: string;
  periodTo: string;
  currency: string;
  uwYear: number;
  businessType: BusinessType;
  sharePct: number;
  coinsurerCode?: string;
  coinsuranceLeader: boolean;
  discountRate: number;
  loadingRate: number;
  openCoverId?: number;
  openCoverNo?: string;
  quotationId?: number;
  cancelledOn?: string;
  document: DocumentStatus;
  premium: Premium;
  risks: Risk[];
}

export interface PolicyFilters {
  companyId: number;
  branchId?: number;
  status?: string;
  productId?: number;
  customerCode?: string;
  q?: string;
  fromDate?: string;
  toDate?: string;
  openCoverId?: number;
  page?: number;
  size?: number;
}

export interface EndorsementInput {
  type: EndorsementType;
  issueDate: string;
  effectiveDate: string;
  description: string;
  grossPremium?: number;
  sumInsuredChange?: number;
  newPeriodFrom?: string;
  newPeriodTo?: string;
}

export interface Endorsement {
  id: number;
  policyId: number;
  policyNo: string;
  endorsementNo: number;
  documentNo: string;
  type: EndorsementType;
  issueDate: string;
  effectiveDate: string;
  newPeriodFrom?: string;
  newPeriodTo?: string;
  description: string;
  currency: string;
  document: DocumentStatus;
  premium: Premium;
}

export interface IterationInput {
  sumInsured: number;
  grossPremium: number;
  discount?: number;
  loading?: number;
  charges?: number;
  remarks?: string;
}

export interface Iteration extends IterationInput {
  iterationNo: number;
  netPremium: number;
  ourNetPremium: number;
  brokerage: number;
  createdBy: string;
  createdAt: string;
}

export interface QuotationInput {
  companyId: number;
  branchId: number;
  productId: number;
  customerCode: string;
  insuredName: string;
  sourceType: SourceType;
  intermediaryCode?: string;
  issueDate: string;
  validityDays: number;
  periodFrom: string;
  periodTo: string;
  currency: string;
  sharePct: number;
  commissionRate?: number;
  iteration?: IterationInput;
}

export interface Quotation {
  id: number;
  companyId: number;
  branchId: number;
  quotationNo: string;
  productId: number;
  productCode: string;
  productName: string;
  businessLine: string;
  customerCode: string;
  customerName: string;
  insuredName: string;
  sourceType: SourceType;
  intermediaryCode?: string;
  intermediaryName?: string;
  issueDate: string;
  validityDays: number;
  expiryDate: string;
  periodFrom: string;
  periodTo: string;
  currency: string;
  sharePct: number;
  commissionRate: number;
  status: QuotationStatus;
  currentIteration: number;
  createdBy: string;
  submittedBy?: string;
  decidedBy?: string;
  decisionReason?: string;
  convertedPolicyId?: number;
  iterations: Iteration[];
}

export interface ConvertInput {
  issueDate?: string;
  coinsurerCode?: string;
  coinsuranceLeader: boolean;
  riskDescription?: string;
}

export interface OpenCoverInput {
  companyId: number;
  branchId: number;
  productId: number;
  customerCode: string;
  insuredName: string;
  periodFrom: string;
  periodTo: string;
  currency: string;
  limitPerShipment: number;
  annualLimit: number;
  rate: number;
  cargoDescription?: string;
}

export interface OpenCover extends OpenCoverInput {
  id: number;
  openCoverNo: string;
  productCode: string;
  customerName: string;
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}

export interface CertificateInput {
  issueDate: string;
  transitDays?: number;
  sourceType?: SourceType;
  intermediaryCode?: string;
  shipment: RiskInput;
}

const BASE = '/underwriting';

export const underwritingApi = {
  products: (companyId: number) => api.get<Product[]>(`${BASE}/products${toQuery({ companyId })}`),
  createProduct: (body: ProductInput) => api.post<Product>(`${BASE}/products`, body),
  updateProduct: (id: number, body: ProductInput) =>
    api.put<Product>(`${BASE}/products/${id}`, body),
  authorizeProduct: (id: number) => api.post<Product>(`${BASE}/products/${id}/authorize`),

  policies: (f: PolicyFilters) =>
    api.get<PageResponse<Policy>>(`${BASE}/policies${toQuery({ ...f })}`),
  policy: (id: number) => api.get<Policy>(`${BASE}/policies/${id}`),
  preview: (body: PolicyInput) => api.post<Premium>(`${BASE}/policies/preview`, body),
  createPolicy: (body: PolicyInput) => api.post<Policy>(`${BASE}/policies`, body),
  updatePolicy: (id: number, body: PolicyInput) => api.put<Policy>(`${BASE}/policies/${id}`, body),
  submitPolicy: (id: number) => api.post<Policy>(`${BASE}/policies/${id}/submit`),
  discardPolicy: (id: number) => api.post<Policy>(`${BASE}/policies/${id}/discard`),
  approvePolicy: (id: number, accountingDate?: string) =>
    api.post<Policy>(`${BASE}/policies/${id}/approve`, { accountingDate }),
  rejectPolicy: (id: number, reason: string) =>
    api.post<Policy>(`${BASE}/policies/${id}/reject`, { reason }),

  endorsements: (policyId: number) =>
    api.get<Endorsement[]>(`${BASE}/policies/${policyId}/endorsements`),
  createEndorsement: (policyId: number, body: EndorsementInput) =>
    api.post<Endorsement>(`${BASE}/policies/${policyId}/endorsements`, body),
  submitEndorsement: (id: number) => api.post<Endorsement>(`${BASE}/endorsements/${id}/submit`),
  discardEndorsement: (id: number) => api.post<Endorsement>(`${BASE}/endorsements/${id}/discard`),
  approveEndorsement: (id: number, accountingDate?: string) =>
    api.post<Endorsement>(`${BASE}/endorsements/${id}/approve`, { accountingDate }),
  rejectEndorsement: (id: number, reason: string) =>
    api.post<Endorsement>(`${BASE}/endorsements/${id}/reject`, { reason }),

  quotations: (companyId: number, status?: string) =>
    api.get<Quotation[]>(`${BASE}/quotations${toQuery({ companyId, status })}`),
  quotation: (id: number) => api.get<Quotation>(`${BASE}/quotations/${id}`),
  createQuotation: (body: QuotationInput) => api.post<Quotation>(`${BASE}/quotations`, body),
  iterateQuotation: (id: number, body: IterationInput) =>
    api.post<Quotation>(`${BASE}/quotations/${id}/iterations`, body),
  submitQuotation: (id: number) => api.post<Quotation>(`${BASE}/quotations/${id}/submit`),
  approveQuotation: (id: number) => api.post<Quotation>(`${BASE}/quotations/${id}/approve`),
  rejectQuotation: (id: number, reason: string) =>
    api.post<Quotation>(`${BASE}/quotations/${id}/reject`, { reason }),
  convertQuotation: (id: number, body: ConvertInput) =>
    api.post<Policy>(`${BASE}/quotations/${id}/convert`, body),

  openCovers: (companyId: number) =>
    api.get<OpenCover[]>(`${BASE}/open-covers${toQuery({ companyId })}`),
  openCover: (id: number) => api.get<OpenCover>(`${BASE}/open-covers/${id}`),
  createOpenCover: (body: OpenCoverInput) => api.post<OpenCover>(`${BASE}/open-covers`, body),
  authorizeOpenCover: (id: number) => api.post<OpenCover>(`${BASE}/open-covers/${id}/authorize`),
  certificates: (id: number) => api.get<Policy[]>(`${BASE}/open-covers/${id}/certificates`),
  declareShipment: (id: number, body: CertificateInput) =>
    api.post<Policy>(`${BASE}/open-covers/${id}/certificates`, body),
};
