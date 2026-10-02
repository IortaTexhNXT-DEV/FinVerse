/** Types of the Renewal API (BRD-6; docs/architecture/RENEWAL_DESIGN.md sections 11 and 12). */

export type RenewalTab =
  | 'ALL'
  | 'EXTRACTED'
  | 'UNASSIGNED'
  | 'FOR_RENEWAL'
  | 'FOR_QUOTATION'
  | 'FOR_PROPOSAL'
  | 'NOT_FOR_RENEWAL'
  | 'LOST_BUSINESS'
  | 'EXCEPTIONS'
  | 'REVIEW'
  | 'TRANSFER_PENDING'
  | 'FOR_PROCESSING'
  | 'IN_PROCESSING'
  | 'WITH_INSURER'
  | 'INSURER_RESPONDED'
  | 'RETURNED'
  | 'RA_READY'
  | 'RA_GENERATED'
  | 'RA_SENT'
  | 'LETTER_PENDING'
  | 'NRNS'
  | 'NB_PATH'
  | 'CLOSED';

export type Disposition =
  'FOR_RENEWAL' | 'NOT_FOR_RENEWAL' | 'FOR_QUOTATION' | 'FOR_PROPOSAL' | 'LOST_BUSINESS';

/** Criteria of a renewal list; the multi-select criteria are "A,B" or "!A,B" (all except). */
export interface RenewalFilters {
  tab?: RenewalTab;
  q?: string;
  expiryFrom?: string;
  expiryTo?: string;
  unitHead?: string;
  origin?: string;
  accountType?: string;
  region?: string;
  department?: string;
  branch?: string;
  riskCode?: string;
  segment?: string;
  officer?: string;
  bucket?: string;
  disposition?: string;
  stage?: string;
  insurer?: string;
  mine?: boolean;
  assignedPo?: string;
  returned?: boolean;
  nrns?: boolean;
  urgent?: boolean;
  kycDue?: boolean;
  dueWithin?: number;
  disposed?: boolean;
}

export interface PolicyColumns {
  source: string;
  sourceRef: string | null;
  expiringInvoiceNo: string | null;
  expiringArn: string | null;
  policyNo: string | null;
  coverNo: string | null;
  versionNo: number | null;
  productCode: string | null;
  productName: string | null;
  lineCode: string | null;
  insurerCode: string | null;
  inception: string | null;
  pnNos: string | null;
  mortgaged: boolean;
  mortgageeBank: string | null;
}

export interface PartyColumns {
  clientId: number | null;
  clientCode: string | null;
  clientName: string;
  assuredName: string | null;
  segment: string | null;
  businessOrigin: string | null;
  accountType: string | null;
  branchCode: string | null;
  regionCode: string | null;
  departmentCode: string | null;
  salesUnit: string | null;
  ownerUnit: string | null;
  unitHead: string | null;
  accountOfficer: string | null;
  assignedAo: string | null;
  assignedPo: string | null;
}

export interface MoneyColumns {
  currency: string | null;
  basicPremium: number | null;
  grossPremium: number | null;
  sumInsured: number | null;
  premiumRate: number | null;
  commissionRate: number | null;
  outstanding: number | null;
  claimCount: number | null;
  claimStatus: string | null;
}

export interface FlagChips {
  urgent: boolean;
  returned: boolean;
  transferred: boolean;
  endorsed: boolean;
  claims: boolean;
  outstanding: boolean;
  kycDue: boolean;
  kycFlaggedAt: string | null;
  nrns: boolean;
  stp: boolean;
  locked: boolean;
  nfrSent: boolean;
}

export interface CandidateRow {
  renewalRef: string;
  stage: string;
  stageLabel: string;
  bucket: string | null;
  disposition: Disposition | null;
  reason: string | null;
  remarks: string | null;
  policy: PolicyColumns;
  parties: PartyColumns;
  money: MoneyColumns;
  flags: FlagChips;
  expiry: string;
  daysToExpiry: number;
  /** Names of the insurer and the owner unit, read by the server for every Renewal user. */
  names?: { insurer: string | null; ownerUnit: string | null; product?: string | null };
}

export interface Lifecycle {
  id: number;
  policyYear: number | null;
  initiatedBy: string | null;
  initiatedAt: string | null;
  evaluatedAt: string | null;
  bucketRuleVersion: number | null;
  proposal: {
    disposition: string | null;
    automation: string | null;
    matrixVersion: number | null;
    ruleId: number | null;
  };
  raNotice: string;
  path: string;
  links: {
    renewalArn: string | null;
    quotationRef: string | null;
    proposalRef: string | null;
    renewedInvoiceNo: string | null;
    closedAs: string | null;
    closedAt: string | null;
  };
  legacyPackage: {
    legacyCode: string | null;
    legacyVersion: string | null;
    productCode: string | null;
    versionNo: number | null;
  };
}

export interface CandidateDetail {
  row: CandidateRow;
  lifecycle: Lifecycle;
  blocking: string[];
  historyViewed: boolean;
}

export interface CheckResultView {
  checkCode: string;
  checkName: string;
  outcome: string;
  severity: string;
  message: string;
  detail: string | null;
}

export interface ChecksView {
  results: CheckResultView[];
  runs: {
    id: number;
    trigger: string;
    runAt: string;
    bucketBefore: string | null;
    bucketAfter: string;
    ruleSetVersion: number | null;
    failed: number;
    runBy: string;
  }[];
  buckets: {
    from: string | null;
    to: string;
    ruleSetVersion: number | null;
    ruleId: number | null;
    cause: string;
    remarks: string | null;
    by: string;
    at: string;
  }[];
  endorsements: {
    reference: string;
    source: string | null;
    /** The status of the endorsement when it was linked; absent when not known. */
    status?: string | null;
    linkedAt: string;
  }[];
}

export interface HistoryView {
  dispositions: {
    code: string;
    reason: string | null;
    remarks: string | null;
    newInvoiceNo: string | null;
    source: string;
    matrixVersion: number | null;
    ruleId: number | null;
    superseded: boolean;
    by: string;
    at: string;
  }[];
  assignments: {
    role: string;
    username: string;
    previous: string | null;
    reason: string | null;
    by: string;
    at: string;
  }[];
  overrides: {
    kind: string;
    checkCode: string | null;
    from: string | null;
    to: string | null;
    reason: string;
    remarks: string;
    active: boolean;
    by: string;
    at: string;
  }[];
}

export interface AccountHistory {
  priorRenewals: {
    renewalRef: string;
    expiringArn: string | null;
    renewalArn: string | null;
    expiry: string;
    outcome: string;
  }[];
  endorsements: {
    invoiceNo: string;
    kind: string;
    endorsementNo: string | null;
    bookedOn: string | null;
    grossPremium: number | null;
  }[];
  payments: {
    invoiceNo: string;
    valueDate: string | null;
    type: string;
    component: string | null;
    amount: number | null;
    orNo: string | null;
  }[];
  claims: { connected: boolean; count: number; open: number };
}

export interface BatchOutcome {
  done: string[];
  refused: Record<string, string>;
}

export interface Officer {
  username: string;
  fullName: string;
  unit?: string;
}

export interface RemarkView {
  text: string;
  stage: string;
  by: string;
  at: string;
}

export interface TransferView {
  id: number;
  renewalRef: string;
  clientName: string;
  expiry: string;
  fromUnit: string | null;
  toUnit: string;
  reasonCode: string | null;
  remarks: string;
  status: string;
  requestedBy: string;
  requestedAt: string;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionRemarks: string | null;
}

export type * from './renewalSetupTypes';
