/** Types of the Employee Benefits marketing API (see `api/ebMarket.ts`). */

export interface Franchise {
  id: number;
  franchiseNo: string;
  cycleId: number;
  insurerCode: string;
  insurerName: string;
  status: string;
  decision?: string | null;
  submittedAt?: string | null;
  dueDate?: string | null;
  decidedOn?: string | null;
  decidedBy?: string | null;
  reasonCode?: string | null;
  remarks?: string | null;
  evidenceAttachmentId?: number | null;
  adviceDueDate?: string | null;
  advisedAt?: string | null;
}

export interface FranchiseDecisionInput {
  approve: boolean;
  decidedOn: string;
  reasonCode: string;
  remarks: string;
}

export interface TorItemInput {
  benefitLine: string;
  planCode?: string;
  description: string;
  requirement: string;
}

export interface TorItem extends TorItemInput {
  id: number;
  sortOrder: number;
}

export interface Tor {
  id: number;
  versionNo: number;
  status: 'DRAFT' | 'RELEASED' | 'SUPERSEDED';
  releasedAt?: string | null;
  releasedBy?: string | null;
  attachmentId?: number | null;
  items: TorItem[];
}

export interface InsurerRequest {
  id: number;
  requestNo: string;
  cycleId: number;
  insurerCode: string;
  insurerName: string;
  torVersion: number;
  sentAt: string;
  sentBy: string;
  dueDate: string;
  status: 'OPEN' | 'RESPONDED' | 'DECLINED' | 'CLOSED';
  closedReason?: string | null;
}

export interface ProposalLine {
  benefitLine: string;
  planCode: string;
  planName?: string | null;
  members?: number | null;
  premiumRate?: number | null;
  annualPremium: number;
  sumInsured?: number | null;
}

export interface ProposalItem {
  torItemId: number;
  offeredValue: string;
  deviation: boolean;
  remark?: string | null;
}

export interface ProposalFactor {
  factorCode: string;
  value?: string | null;
  rating?: number | null;
}

export interface ProposalInput {
  insurerCode: string;
  receivedOn?: string;
  validUntil?: string;
  currency?: string;
  terms?: string;
  exclusions?: string;
  lines: ProposalLine[];
  items: ProposalItem[];
  factors: ProposalFactor[];
}

export interface Proposal {
  id: number;
  proposalNo: string;
  cycleId: number;
  insurerCode: string;
  insurerName: string;
  kind: 'INCUMBENT_INDICATIVE' | 'PROPOSAL' | 'REVISED';
  versionNo: number;
  status: 'SUBMITTED' | 'VALIDATED' | 'REJECTED' | 'SUPERSEDED';
  receivedOn: string;
  validUntil?: string | null;
  currency: string;
  terms?: string | null;
  exclusions?: string | null;
  attachmentId: number;
  totalPremium: number;
  rejectReason?: string | null;
  decidedBy?: string | null;
  lines: ProposalLine[];
  items: ProposalItem[];
  factors: ProposalFactor[];
}

export interface RevisionChange {
  torItemId?: number | null;
  change: string;
}

export interface Revision {
  id: number;
  revisionNo: number;
  description?: string | null;
  relayedAt: string;
  relayedBy: string;
  dueDate: string;
  status: 'OPEN' | 'ANSWERED';
  items: RevisionChange[];
  targets: { insurerCode: string; status: string; answeredProposalId?: number | null }[];
}

export interface ComparativeSummary {
  id: number;
  comparativeNo: string;
  cycleId: number;
  versionNo: number;
  status: string;
  dueDate?: string | null;
  submittedBy?: string | null;
  presentedAt?: string | null;
  thresholdRules?: string | null;
}

export interface MatrixColumn {
  proposalId: number;
  proposalNo: string;
  insurerCode: string;
  insurerName: string;
  kind: string;
  versionNo: number;
  currency: string;
  validUntil?: string | null;
  terms?: string | null;
  exclusions?: string | null;
  totalPremium: number;
}

export interface MatrixOffer {
  annualPremium: number;
  sumInsured: number;
  plans: {
    planCode: string;
    planName?: string | null;
    members?: number | null;
    premiumRate?: number | null;
    annualPremium: number;
    sumInsured?: number | null;
  }[];
}

export interface ComparativeMatrix {
  proposals: MatrixColumn[];
  lines: {
    benefitLine: string;
    label: string;
    offers: Record<string, MatrixOffer>;
    lowestProposalId?: number | null;
    lowestPremium?: number | null;
  }[];
  items: {
    torItemId: number;
    benefitLine: string;
    planCode?: string | null;
    description: string;
    requirement: string;
    answers: Record<string, { offeredValue: string; deviation: boolean; remark?: string | null }>;
  }[];
  factors: {
    factorCode: string;
    label: string;
    ratings: Record<string, { value?: string | null; rating?: number | null }>;
  }[];
}

export interface ComparativeView {
  comparative: ComparativeSummary;
  programmeId: number;
  programmeNo: string;
  programmeName: string;
  clientName: string;
  accountOfficer: string;
  cycleNo: string;
  cycleStage: string;
  summary?: string | null;
  approverPermission?: string | null;
  attachmentId?: number | null;
  lines: {
    benefitLine: string;
    recommendedProposalId?: number | null;
    lowestPremium?: number | null;
  }[];
  matrix: ComparativeMatrix;
  decisions: {
    role: string;
    signatory: string;
    decision: string;
    remarks?: string | null;
    decidedAt: string;
  }[];
  comments: {
    id: number;
    authorKind: 'INTERNAL' | 'CLIENT';
    text: string;
    replyToId?: number | null;
    createdBy: string;
    createdAt: string;
  }[];
}

export interface ConfirmationInput {
  channel: 'EMAIL' | 'SIGNED_DOCUMENT' | '';
  confirmedOn?: string;
  remarks?: string;
  choices: { lineNo: number; proposalId: number }[];
}

export interface Confirmation {
  id: number;
  cycleId: number;
  comparativeId?: number | null;
  channel: string;
  confirmedOn: string;
  evidenceAttachmentId: number;
  status: 'ACTIVE' | 'VOIDED';
  voidReason?: string | null;
  remarks?: string | null;
  recordedBy: string;
  recordedAt: string;
  lines: {
    lineNo: number;
    benefitLine: string;
    proposalId: number;
    insurerCode: string;
    annualPremium: number;
    sumInsured?: number | null;
    accountArn?: string | null;
  }[];
}

export interface ChecklistItem {
  documentType: string;
  label: string;
  mandatory: boolean;
  present: boolean;
  attachmentIds: number[];
}

export interface SubmissionInput {
  programmeId: number;
  cycleId?: number;
  memberChangeId?: number;
  processType: string;
  insurerCode: string;
  attachmentIds: number[];
  remarks?: string;
}

export interface Submission {
  id: number;
  cycleId?: number | null;
  memberChangeId?: number | null;
  processType: string;
  insurerCode: string;
  insurerName: string;
  sentAt: string;
  sentBy: string;
  recipients: string;
  acknowledgedOn?: string | null;
  remarks?: string | null;
  documents: { attachmentId: number; documentType: string }[];
}
