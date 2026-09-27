/** Types of the Submitted Policies work API: reviews, IAAF, TOR, renewal, letters, fees, setup. */

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
  levels: {
    level: number;
    permission: string;
    approverUsername: string | null;
    signatoryTitle: string;
  }[];
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
