/** Types of the Employee Benefits servicing and set-up API (see `api/ebService.ts`). */

export interface ThresholdRuleInput {
  benefitLine?: string;
  measure: 'TSI' | 'ANNUAL_PREMIUM' | '';
  amount: number | '';
  currency?: string;
  approverPermission?: string;
  approvalLevel: number;
  effectiveFrom: string;
  effectiveTo?: string;
  description?: string;
}

export interface ThresholdRule {
  id: number;
  benefitLine?: string | null;
  measure: 'TSI' | 'ANNUAL_PREMIUM';
  amount: number;
  currency: string;
  approverPermission: string;
  approvalLevel: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  description?: string | null;
  recordStatus: string;
  maker: string;
  authorizedBy?: string | null;
  authorizedAt?: string | null;
}

export interface RequiredDocumentInput {
  processType: string;
  benefitLine?: string;
  documentType: string;
  mandatory: boolean;
}

export interface RequiredDocument {
  id: number;
  processType: string;
  benefitLine?: string | null;
  documentType: string;
  mandatory: boolean;
  recordStatus: string;
  maker: string;
  authorizedBy?: string | null;
}

export interface RosterVersion {
  id: number;
  policyYear: number;
  versionNo: number;
  sourceRef: string;
  status: 'STAGED' | 'ACCEPTED' | 'REJECTED' | 'SUPERSEDED';
  headcount: number;
  loadedBy: string;
  loadedAt: string;
  decidedBy?: string | null;
  decidedAt?: string | null;
  rejectReason?: string | null;
}

export interface Member {
  id: number;
  employeeNo: string;
  lastName: string;
  firstName: string;
  birthDate: string;
  gender?: string | null;
  civilStatus?: string | null;
  planCode: string;
  dependants: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  status: 'ACTIVE' | 'DELETED';
}

export interface RosterDifferences {
  headcount: number;
  currentHeadcount: number;
  added: number;
  removed: number;
  planChanges: number;
}

export type MemberAction = 'ADD' | 'DELETE' | 'CHANGE_PLAN' | 'CHANGE_DATA';

export interface MemberChangeLineInput {
  action: MemberAction;
  employeeNo: string;
  member?: {
    lastName?: string;
    firstName?: string;
    birthDate?: string;
    gender?: string;
    civilStatus?: string;
    planCode?: string;
    dependants?: number;
  };
  effectiveDate: string;
}

export interface MemberChangeInput {
  lineNo: number;
  policyYear?: number;
  source: 'AO' | 'CLIENT';
  financial: boolean;
  description?: string;
  lines: MemberChangeLineInput[];
}

export interface MemberChange {
  id: number;
  changeNo: string;
  programmeId: number;
  programmeNo?: string | null;
  clientName?: string | null;
  lineNo: number;
  benefitLine: string;
  policyYear: number;
  source: string;
  financial: boolean;
  directBilled: boolean;
  status: string;
  description?: string | null;
  relayedAt?: string | null;
  billedOn?: string | null;
  billingRef?: string | null;
  billedAmount?: number | null;
  validatedBy?: string | null;
  endorsementRequestNo?: string | null;
  createdBy: string;
  createdAt: string;
  lines: {
    sortOrder: number;
    action: MemberAction;
    employeeNo: string;
    lastName?: string | null;
    firstName?: string | null;
    birthDate?: string | null;
    planCode?: string | null;
    effectiveDate: string;
    memberId?: number | null;
  }[];
}

export interface BillingInput {
  billedOn: string;
  reference: string;
  amount: string;
  direct: boolean;
}

export interface Soa {
  id: number;
  soaNo: string;
  programmeId: number;
  programmeNo?: string | null;
  clientName?: string | null;
  insurerCode: string;
  insurerName: string;
  insurerSoaNo: string;
  periodFrom: string;
  periodTo: string;
  amount: number;
  currency: string;
  attachmentId: number;
  status: 'RECEIVED' | 'VALIDATED' | 'RELEASED' | 'REJECTED';
  receivedOn: string;
  validatedBy?: string | null;
  releasedAt?: string | null;
  rejectReason?: string | null;
  remarks?: string | null;
  invoices: { invoiceNo: string; paymentStatus: string }[];
}

export interface SoaInput {
  insurerCode: string;
  insurerSoaNo: string;
  periodFrom: string;
  periodTo: string;
  amount: string;
  currency?: string;
  receivedOn?: string;
  remarks?: string;
}

export interface ProgrammeInvoice {
  invoiceNo: string;
  arn: string;
  insurerCode: string;
  bookingDate?: string | null;
  grossPremium: number;
  currency: string;
  paymentStatus: string;
}

export interface SoaFilters {
  status?: string;
  insurer?: string;
  programmeId?: number;
  q?: string;
}

export interface MemberChangeFilters {
  status?: string;
  programmeId?: number;
  q?: string;
}
