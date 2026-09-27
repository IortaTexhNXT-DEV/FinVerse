/**
 * Types of the Renewal processing, insurer, letters, LAMD, extraction, Setup and Home API (BRD-6).
 */

export interface Terms {
  currency: string | null;
  netPremium: number | null;
  charges: number | null;
  grossPremium: number | null;
  sumInsured: number | null;
  commissionRate: number | null;
  commission: number | null;
}

export interface Computations {
  renewalArn: string | null;
  expiring: Terms;
  renewal: Terms;
}

export interface InsurerBatchView {
  batchNo: string;
  insurerCode: string;
  expiryFrom: string;
  expiryTo: string;
  status: string;
  lineCount: number;
  recipients: string | null;
  sentAt: string | null;
  sentBy: string | null;
  replyDue: string | null;
  attachmentId: number | null;
}

export interface InsurerBatchDetail {
  batch: InsurerBatchView;
  headers: string[];
  lines: { renewalRef: string | null; responded: boolean; columns: (string | null)[] }[];
}

export interface InsurerResponseView {
  response: string;
  insurerRef: string | null;
  revisedPremium: number | null;
  revisedSumInsured: number | null;
  revisedRate: number | null;
  terms: string | null;
  receivedOn: string;
  source: string;
  match: string;
  latestValid: boolean;
  late: boolean;
  jobNo: string | null;
  remarks: string | null;
  by: string;
  at: string;
}

export interface LetterView {
  letterNo: string;
  type: string;
  notice: string | null;
  status: string;
  source: string;
  templateCode: string | null;
  recipients: string | null;
  failure: string | null;
  lateConfirmedBy: string | null;
  generatedAt: string;
  sentAt: string | null;
  attachmentId: number | null;
}

export interface AcceptanceView {
  method: string;
  attachmentId: number | null;
  reference: string | null;
  acceptedOn: string;
  source: string;
  financialImpactAck: boolean;
  remarks: string | null;
  by: string;
  at: string;
}

export interface FollowupView {
  channel: string;
  outcome: string;
  remarks: string;
  nextActionDate: string | null;
  by: string;
  at: string;
}

export interface LamdReportView {
  reportNo: string;
  type: string;
  period: string;
  jobNo: string;
  lines: number;
  matched: number;
  by: string;
  at: string;
}

export interface LamdLineView {
  rowNo: number;
  pnNo: string;
  status: string;
  statusDate: string | null;
  borrower: string | null;
  match: string;
  renewalRef: string | null;
  routing: string | null;
  message: string | null;
}

export interface ExtractionRunView {
  runNo: string;
  trigger: string;
  expiryFrom: string | null;
  expiryTo: string | null;
  requestedBy: string;
  counts: { read: number; created: number; existing: number; skipped: number; urgent: number };
  status: string;
  message: string | null;
  startedAt: string;
  endedAt: string | null;
}

export interface Approval {
  recordStatus: string;
  maker: string | null;
  authorizedBy: string | null;
  authorizedAt: string | null;
}

export interface RiskCodeView {
  id: number;
  riskCode: string;
  lineCode: string | null;
  reason: string;
  effectiveFrom: string;
  effectiveTo: string | null;
  approval: Approval;
}

export interface CheckSettingView {
  checkCode: string;
  checkName: string;
  active: boolean;
  severity: string;
  parameters: string | null;
  approval: Approval;
}

export interface VersionView<R> {
  id: number;
  versionNo: number;
  status: string;
  effectiveFrom: string;
  description: string | null;
  maker: string | null;
  submittedAt: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  decisionRemarks: string | null;
  rules: R[];
}

export interface BucketRuleData {
  priority: number;
  checkCode: string | null;
  severity: string | null;
  outcome: string;
  bucket: string;
}

export interface DecisionRuleData {
  priority: number;
  criteria: {
    segment: string | null;
    lineCode: string | null;
    productCode: string | null;
    mortgaged: boolean | null;
    bucket: string | null;
    claims: string | null;
    endorsement: string | null;
    payment: string | null;
    daysFrom: number | null;
    daysTo: number | null;
  };
  outcome: string;
  automation: string;
  letterHint: string | null;
}

export interface PackageMapData {
  legacyPackageCode: string;
  legacyPackageVersion: string | null;
  riskCode: string | null;
  insurerCode: string | null;
  siFrom: number | null;
  siTo: number | null;
  productCode: string | null;
  productVersionNo: number | null;
  remarks: string | null;
}

export interface PackageMapView {
  id: number;
  data: PackageMapData;
  action: string;
  mapVersion: number;
  source: string;
  approval: Approval;
}

export interface PackageChoiceView {
  id: number;
  candidateId: number;
  legacyPackage: string;
  productCode: string;
  productVersionNo: number;
  reason: string;
  status: string;
  maker: string;
  createdAt: string;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionRemarks: string | null;
}

export interface HomeCount {
  code: string;
  label: string;
  count: number;
}

export interface RenewalHome {
  stages: HomeCount[];
  buckets: HomeCount[];
  tiles: {
    due30: number;
    due60: number;
    due90: number;
    due140: number;
    atRisk: number;
    urgent: number;
    returned: number;
    nrns: number;
    insurerOverdue: number;
    lettersFailed: number;
  };
  workload: { username: string; role: string; count: number }[];
}

export interface CorrectionView {
  id: number;
  legacyRef: string;
  raDate: string;
  raRef: string | null;
  jobNo: string | null;
  rowNo: number | null;
  correction: string;
  status: string;
  preparedBy: string;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionRemarks: string | null;
}
