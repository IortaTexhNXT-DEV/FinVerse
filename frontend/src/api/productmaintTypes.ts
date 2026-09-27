/** Types of the Product Maintenance package request API (mirror the backend DTOs). */
export type RequestType = 'NEW' | 'AMEND' | 'UPDATE' | 'RENEW' | 'RETIRE' | 'REACTIVATE';
export type RequestScope = 'GENERIC' | 'CLIENT_SPECIFIC';
export type RequestStage =
  | 'DRAFT'
  | 'FOR_MKT_APPROVAL'
  | 'FOR_TSU_REVIEW'
  | 'FOR_TSU_APPROVAL'
  | 'NEGOTIATION'
  | 'TERMS_REVIEW'
  | 'FOR_MKT_REVIEW'
  | 'REQUIREMENTS_PREP'
  | 'FOR_MANCOM'
  | 'WITH_MBS'
  | 'FOR_VALIDATION'
  | 'RELEASED'
  | 'RETIRED'
  | 'NOT_PROCEEDED'
  | 'VOIDED';

export interface TermsSection {
  heading?: string;
  text?: string;
}

export interface CoverageTerm {
  coverageCode: string;
  included: boolean;
  optional: boolean;
  limitAmount?: number;
  subLimit?: number;
  deductibleAmount?: number;
  deductiblePercent?: number;
  deductibleText?: string;
  clauseCodes: string[];
  remarks?: string;
}

export interface Scheme {
  defaultRate?: number;
  minimumPremium?: number;
  commissionRate?: number;
  maxSumInsured?: number;
  ratingBasisNote?: string;
}

export interface PackageDates {
  effectiveFrom?: string;
  packageStartDate?: string;
  packageEndDate?: string;
  anniversaryDate?: string;
}

export interface InsurerLine {
  insurerCode: string;
  role?: string;
  sharePercent?: number;
  rate?: number;
  minimumPremium?: number;
  terms: CoverageTerm[];
}

export interface PackageTerms {
  sections: TermsSection[];
  coverages: CoverageTerm[];
  scheme: Scheme;
  dates: PackageDates;
  insurers: InsurerLine[];
}

export interface Milestones {
  submittedBy?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  recommendedBy?: string;
  recommendedAt?: string;
  tsuApprovedBy?: string;
  tsuApprovedAt?: string;
  termsFinalBy?: string;
  termsFinalAt?: string;
  requirementsBy?: string;
  requirementsAt?: string;
  setupBy?: string;
  setupAt?: string;
}

export interface PackageRequest {
  id: number;
  companyId: number;
  requestNo: string;
  requestType: RequestType;
  scope: RequestScope;
  title: string;
  clientId?: number;
  clientCode?: string;
  clientName?: string;
  lineCode: string;
  coverTypeCode?: string;
  productCode?: string;
  baseVersionNo?: number;
  marketSegments: string[];
  reason: string;
  reasonNote?: string;
  negotiationRequired: boolean;
  recommendation?: string;
  requestedTerms: PackageTerms;
  proposedTerms?: PackageTerms;
  chosenInsurers: string[];
  packageEndDate?: string;
  schemeRate?: number;
  status: RequestStage;
  milestones: Milestones;
  resultingVersionNo?: number;
  releasedAt?: string;
  createdBy: string;
  createdAt: string;
}

export interface RequestListItem {
  id: number;
  requestNo: string;
  requestType: RequestType;
  scope: RequestScope;
  title: string;
  clientName?: string;
  lineCode: string;
  productCode?: string;
  resultingVersionNo?: number;
  packageEndDate?: string;
  status: RequestStage;
  stageEnteredAt?: string;
  dueAt?: string;
  assignee?: string;
  createdBy: string;
  createdAt: string;
}

export interface RequestInput {
  companyId?: number;
  type: RequestType;
  scope: RequestScope;
  title: string;
  clientId?: number;
  lineCode: string;
  coverTypeCode?: string;
  productCode?: string;
  marketSegments: string[];
  reason: string;
  reasonNote?: string;
  negotiationRequired?: boolean;
  terms: PackageTerms;
}

export interface RequestSearch {
  text?: string;
  stage?: RequestStage[];
  type?: RequestType[];
  scope?: RequestScope;
  mine?: boolean;
  expiringWithin?: number;
}

export type RoundStatus = 'PREPARATION' | 'FOR_APPROVAL' | 'SENT' | 'CLOSED';

export interface InsurerResponse {
  id: number;
  roundId: number;
  insurerCode: string;
  insurerName: string;
  outcome: string;
  revision: number;
  rate?: number;
  minimumPremium?: number;
  coverages: CoverageTerm[];
  conditions?: string;
  validUntil?: string;
  remarks?: string;
  documentId?: number;
  respondedAt?: string;
  sends: number;
}

export interface NegotiationRound {
  id: number;
  roundNo: number;
  status: RoundStatus;
  qsNo?: string;
  qsTemplate?: string;
  qsNotes?: string;
  replyDue?: string;
  preparedBy?: string;
  approvedBy?: string;
  sentAt?: string;
  locked: boolean;
  insurers: string[];
  responses: InsurerResponse[];
}

export interface ResponseInput {
  outcome: string;
  rate?: number;
  minimumPremium?: number;
  coverages: CoverageTerm[];
  conditions?: string;
  validUntil?: string;
  remarks?: string;
}

export interface ResponseHistory {
  responseId: number;
  revision: number;
  outcome: string;
  rate?: number;
  minimumPremium?: number;
  conditions?: string;
  remarks?: string;
  changedBy: string;
  changedAt: string;
}

export interface ComparativeRow {
  insurerCode: string;
  insurerName: string;
  outcome: string;
  rate?: number;
  minimumPremium?: number;
  coverages?: string;
  deductibles?: string;
  conditions?: string;
  validUntil?: string;
  remarks?: string;
  lowest: boolean;
}

export interface Comparative {
  roundNo: number;
  fields: string[];
  headers: string[];
  rows: ComparativeRow[];
}

export interface ComparativeOutput {
  id: number;
  roundNo: number;
  kind: 'MASTER' | 'CLIENT';
  parentOutputId?: number;
  current: boolean;
  title: string;
  fields: string[];
  insurers: string[];
  templateVersion: string;
  attachmentId?: number;
  sha256: string;
  generatedBy: string;
  generatedAt: string;
}

export interface InsurerChoice {
  insurerCode: string;
  role?: string;
  sharePercent?: number;
}

export interface Signoff {
  id: number;
  decision: 'SIGNED' | 'RETURNED';
  reference: string;
  signedBy: string;
  signedAt: string;
  comment?: string;
  signedSheetAttachmentId?: number;
}

export interface Requirements {
  missing: string[];
  signoffs: Signoff[];
}

export interface DocumentCheck {
  documentType: string;
  attached: boolean;
}

export interface Advisory {
  id: number;
  requestId?: number;
  productCode: string;
  versionNo?: number;
  type: 'PACKAGE_READY' | 'PACKAGE_UPDATED' | 'RENEWAL' | 'RETIREMENT';
  status: 'DRAFT' | 'SENT';
  groups: string[];
  emailTo: string[];
  subject: string;
  body: string;
  templateVersion?: string;
  documents: DocumentCheck[];
  sentBy?: string;
  sentAt?: string;
  createdAt: string;
}

export interface AdvisoryInput {
  groups: string[];
  emailTo: string[];
  subject: string;
  body: string;
}

export interface ExpiryRow {
  productCode: string;
  productName: string;
  versionNo: number;
  packageEndDate: string;
  anniversaryDate?: string;
  daysLeft: number;
  renewalRequestId?: number;
  renewalRequestNo?: string;
  renewalStage?: RequestStage;
}

export interface RenewalResult {
  productCode: string;
  requestId: number;
  requestNo: string;
  created: boolean;
}

export interface StageCount {
  stage: RequestStage;
  total: number;
  overdue: number;
  dueSoon: number;
}

export interface HomeCounts {
  stages: StageCount[];
  expiring: Record<string, number>;
  advisoriesPending: number;
  outputsThisWeek: number;
}

export interface Prefill {
  productCode: string;
  versionNo: number;
  versionStatus: string;
  terms: PackageTerms;
}

export interface SetupInput {
  productCode?: string;
  productName?: string;
  changeSummary?: string;
  comment?: string;
}
