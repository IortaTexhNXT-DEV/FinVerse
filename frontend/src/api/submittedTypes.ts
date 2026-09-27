/** Types of the Submitted Policies API (BRD-12; docs/architecture/SUBMITTED_POLICIES_DESIGN.md). */

export type MasterlistTab =
  | 'ALL'
  | 'FOR_VALIDATION'
  | 'CLASSIFIED'
  | 'FOR_RENEWAL'
  | 'MANUAL_DISPOSITION'
  | 'NON_RENEWAL'
  | 'FALLOUT';

export interface MasterlistFilters {
  tab?: MasterlistTab;
  q?: string;
  segment?: string;
  businessType?: string;
  bucket?: string;
  expiryMonth?: string;
  insurer?: string;
  handler?: string;
  conversionStatus?: string;
  migrated?: boolean;
}

export interface PolicyRow {
  id: number;
  sbmNo: string;
  segment: string;
  businessType: 'NB' | 'RB';
  pnNo: string | null;
  policyNo: string | null;
  assuredName: string;
  insurerCode: string | null;
  inceptionDate: string | null;
  expiryDate: string;
  sumInsured: number | null;
  currency: string;
  classification: string | null;
  bucket: string | null;
  renewalTag: string | null;
  status: string;
  handlerUsername: string | null;
  conversionStatus: string | null;
  flags: string[];
  falloutReason: string | null;
}

export interface SbmLoan {
  pnNo: string | null;
  loanApplicationNo: string | null;
  cif: string | null;
  valueDate: string | null;
  maturityDate: string | null;
  referringBranch: string | null;
  originatingUnit: string | null;
  borrowerName: string | null;
}

export interface SbmAssured {
  assuredName: string;
  mailingAddress: string | null;
  telephone: string | null;
  mobile: string | null;
  email: string | null;
  bankCounterpartEmail: string | null;
}

export interface SbmTerms {
  insurerCode: string | null;
  policyNo: string | null;
  inceptionDate: string | null;
  expiryDate: string;
  coverageDays: number | null;
  sumInsured: number | null;
  totalPremium: number | null;
  currency: string;
}

export interface SbmRisk {
  unitDescription: string | null;
  serialNo: string | null;
  motorNo: string | null;
  colour: string | null;
  plateNo: string | null;
  vehicleType: string | null;
  vehicleYear: number | null;
  propertyLocation: string | null;
  occupancy: string | null;
  mortgagee: string | null;
}

export interface SbmMarks {
  ffy: boolean;
  employeeAccount: boolean;
  noTouch: boolean;
}

export interface PolicyData {
  segment: string;
  businessType: 'NB' | 'RB';
  loan: SbmLoan;
  assured: SbmAssured;
  terms: SbmTerms;
  risk: SbmRisk;
  marks: SbmMarks;
}

export interface Tracking {
  handlerUsername: string | null;
  aoUsername: string | null;
  conversionStatus: string | null;
  opportunityTag: string | null;
  remarks: string | null;
}

export interface PolicyDetail {
  row: PolicyRow;
  data: PolicyData;
  outcome: {
    loanStatus: string | null;
    amortised: boolean;
    bucketReason: string | null;
    raTemplate: string | null;
    renewalTagSource: string | null;
    renewalTagReason: string | null;
    renewalTaggedBy: string | null;
    renewalTaggedAt: string | null;
    lastRunNo: string | null;
    adequacyStatus: string | null;
    statusReason: string | null;
  };
  tracking: Tracking;
  origin: {
    sourceCode: string;
    dateReceived: string;
    migrated: boolean;
    legacyRef: string | null;
    hasDocuments: boolean;
    createdBy: string;
    createdAt: string;
  };
  renewal: {
    renewalRef: string | null;
    arn: string | null;
    bookedInvoiceNo: string | null;
    bookedOn: string | null;
  };
}

export interface HistoryRow {
  field: string;
  oldValue: string | null;
  newValue: string | null;
  source: string;
  reference: string | null;
  by: string;
  at: string;
}

export interface ExtractedValue {
  value: string | null;
  confidence: number | null;
}

export interface ExtractionView {
  id: number;
  extractionNo: string;
  policyId: number | null;
  segment: string;
  businessType: 'NB' | 'RB';
  attachmentId: number | null;
  fileName: string;
  status: string;
  readable: boolean;
  note: string | null;
  fields: Record<string, ExtractedValue>;
  proposed: PolicyData;
  current: PolicyData | null;
  decidedBy: string | null;
  decidedAt: string | null;
  rejectReason: string | null;
  createdAt: string;
}

export interface RunView {
  id: number;
  runNo: string;
  trigger: string;
  scope: string;
  startedAt: string;
  finishedAt: string | null;
  startedBy: string;
  total: number;
  passed: number;
  bucketed: number;
  fallout: number;
  overridden: number;
  breaches: number;
}

export interface ResultView {
  id: number;
  runId: number;
  policyId: number;
  sbmNo: string;
  assuredName: string;
  step: string;
  outcome: string;
  bucket: string | null;
  reasonCode: string | null;
  ruleName: string | null;
  ruleSetCode: string | null;
  ruleSetVersion: number | null;
  message: string | null;
  at: string;
}

export interface LimitCheckView {
  id: number;
  runId: number;
  attribute: string;
  limit: string;
  value: string;
  breached: boolean;
  at: string;
}

export interface RuleCondition {
  field: string;
  operator: string;
  value: string | null;
}

export interface RuleOutcome {
  bucket: string | null;
  tag: string | null;
  classification: string | null;
  raTemplate: string | null;
  flag: string | null;
}

export interface RuleView {
  id: number;
  priority: number;
  name: string;
  conditions: RuleCondition[];
  outcome: RuleOutcome;
  reasonCode: string | null;
  stop: boolean;
  active: boolean;
}

export interface RuleSetView {
  id: number;
  code: string;
  step: string;
  segment: string | null;
  businessType: string | null;
  versionNo: number;
  status: string;
  effectiveFrom: string;
  description: string | null;
  maker: string;
  submittedAt: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  decisionRemarks: string | null;
  rules: RuleView[];
}
