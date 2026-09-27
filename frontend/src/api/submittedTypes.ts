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

export interface ReviewView {
  id: number;
  reviewNo: number;
  reviewDate: string;
  reviewer: string;
  adequacy: string;
  findings: string[];
  remarks: string | null;
  sentTo: string | null;
  sentAt: string | null;
}

export interface Approval {
  status: string;
  currentLevel: number;
  totalLevels: number;
  submittedAt: string | null;
  preparedBy: string;
  preparedAt: string;
  returnReason: string | null;
  attachmentId: number | null;
  levels: { level: number; permission: string; approverUsername: string | null; signatoryTitle: string }[];
  signatures: {
    level: number;
    signer: string;
    signerName: string;
    position: string;
    signedAt: string;
    method: string;
    hash: string;
  }[];
}

export interface IaafView {
  id: number;
  iaafNo: string;
  policyId: number;
  sbmNo: string;
  assuredName: string;
  segment: string;
  sumInsured: number | null;
  sentTo: string | null;
  sentAt: string | null;
  approval: Approval;
  reviews: ReviewView[];
  links: { policyId: number; sbmNo: string | null; relation: string }[];
}

export interface TorView {
  id: number;
  torNo: string;
  policyId: number | null;
  arn: string | null;
  sbmNo: string | null;
  assuredName: string | null;
  breaches: string;
  proposedTerms: string;
  aoUsername: string;
  approvedAt: string | null;
  releasedAt: string | null;
  approval: Approval;
}

export interface RenewalRow {
  id: number;
  policyId: number;
  sbmNo: string;
  assuredName: string;
  segment: string;
  expiringInsurer: string | null;
  expiryDate: string;
  sumInsured: number | null;
  policyStatus: string;
  handoffStatus: string;
  manual: boolean;
  insurerAssigned: string | null;
  raTemplate: string;
  renewalRef: string | null;
  arn: string | null;
  holdCoverOn: string | null;
  insurerAcceptedOn: string | null;
  reassignCount: number;
  outcome: string | null;
  declineReason: string | null;
  handedOffAt: string;
  message: string | null;
  handlerUsername: string | null;
  aoUsername: string | null;
}

export interface LetterView {
  id: number;
  letterNo: string;
  policyId: number;
  sbmNo: string | null;
  letterType: string;
  channel: string;
  status: string;
  recipient: string | null;
  templateCode: string;
  templateVersion: number | null;
  storedFileId: number | null;
  printBatchId: number | null;
  error: string | null;
  sentAt: string | null;
}

export interface PrintBatchView {
  id: number;
  batchNo: string;
  source: string;
  letterType: string;
  batchDate: string;
  letterCount: number;
  mergedFileId: number | null;
  controlFileId: number | null;
  handedTo: string | null;
  handedAt: string | null;
}

export interface FeeView {
  id: number;
  feeNo: string;
  policyId: number | null;
  pnNo: string | null;
  locationRef: string | null;
  amount: number;
  currency: string;
  billingDate: string;
  status: string;
  unappliedRef: string | null;
  channel: string | null;
  ticketRef: string | null;
  ticketMessage: string | null;
  orNo: string | null;
  taggedBy: string | null;
  taggedAt: string | null;
  appliedAt: string | null;
  cancelReason: string | null;
  bulkJobNo: string | null;
}

export interface AmbiguousView {
  unappliedRef: string;
  paymentDate: string;
  amount: number;
  currency: string;
  reference: string | null;
  channel: string | null;
  fees: FeeView[];
}

export interface NoTouchBatch {
  id: number;
  batchNo: string;
  insurerCode: string;
  period: string;
  status: string;
  lineCount: number;
  grossFee: number | null;
  vat: number | null;
  wtax: number | null;
  exportedFileId: number | null;
  statementFileId: number | null;
  siNo: string | null;
  journalBatchNo: string | null;
  returnedAt: string | null;
  billedAt: string | null;
}

export interface NoTouchLine {
  policyId: number;
  sbmNo: string;
  pnNo: string | null;
  assuredName: string;
  policyNo: string | null;
  plateNo: string | null;
  sumInsured: number | null;
  basicPremium: number | null;
  grossFee: number | null;
  vat: number | null;
  wtax: number | null;
}

export interface IntakeRunView {
  id: number;
  runNo: string;
  sourceCode: string;
  bulkJobNo: string | null;
  fileName: string | null;
  received: number;
  created: number;
  updated: number;
  duplicate: number;
  failed: number;
  status: string;
  startedAt: string;
  finishedAt: string | null;
  processingRunNo: string | null;
}

export interface HomeCounts {
  iaafForApproval: number;
  iaafApproved: number;
  torForApproval: number;
  renewalsPending: number;
  lettersFailed: number;
  feesBilled: number;
  feesTagged: number;
}

export interface SetupControl {
  recordStatus: string;
  maker: string;
  authorizedBy: string | null;
  authorizedAt: string | null;
}

export interface LimitRule {
  insurerCode: string;
  segment: string | null;
  line: string | null;
  maxSumInsured: number | null;
  maxVehicleAge: number | null;
  attribute: string | null;
  attributeLimit: string | null;
  description: string | null;
}

export interface InsurerRule {
  segment: string;
  vehicleType: string | null;
  occupancy: string | null;
  insurerCode: string;
  priority: number;
  excludeExpiring: boolean;
  description: string | null;
}

export interface LetterRule {
  letterType: string;
  segment: string | null;
  bucket: string | null;
  status: string | null;
  daysFromExpiry: number;
  channel: string;
  templateCode: string;
  description: string | null;
}

export interface MatrixRow {
  document: string;
  segment: string | null;
  tsiFrom: number;
  tsiTo: number | null;
  level: number;
  permission: string;
  approverUsername: string | null;
  signatoryTitle: string;
}

export interface Controlled<T> {
  id: number;
  control: SetupControl;
  limits?: T;
  row?: T;
}

export interface SourceView {
  id: number;
  code: string;
  name: string;
  segment: string | null;
  businessType: string | null;
  bulkHandler: string | null;
  format: string;
  mandatoryFields: string | null;
  active: boolean;
}

export interface StatusMapView {
  legacyStatus: string;
  status: string;
  bucket: string | null;
}

export interface ScopeView {
  username: string;
  segments: string[];
  ownRecordsOnly: boolean;
}
