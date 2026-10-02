/** Types of the Employee Benefits API (see `api/eb.ts`). */

export type ProgrammeTab =
  'RENEWAL_DUE' | 'IN_PROGRESS' | 'WITH_CLIENT' | 'IN_PLACEMENT' | 'PLACED' | 'LOST' | 'ALL';

export type BusinessType = 'NEW_BUSINESS' | 'RENEWAL';
export type Funding = 'EMPLOYER' | 'VOLUNTARY';
export type ContactRole = 'HR_HEAD' | 'HR_OFFICER' | 'FINANCE';
export type FeedbackChannel = 'AO' | 'EMAIL' | 'PHONE' | 'MEETING' | 'LETTER';
export type DocumentSource = 'AO' | 'PROCESSING' | 'CLIENT' | 'INSURER' | 'SYSTEM';
export type Responsible = 'INSURER' | 'CLIENT' | 'BROKER';
export type ItemAction = 'RECEIVE' | 'RELEASE' | 'CLOSE';

export interface CycleRef {
  id: number;
  cycleNo: string;
  businessType: BusinessType;
  stage: string;
  policyYear: number;
  raSentAt?: string | null;
}

export interface ProgrammeRow {
  id: number;
  programmeNo: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  name: string;
  teamCode: string;
  funding: Funding;
  accountOfficer: string;
  status: string;
  renewalEligible: boolean;
  lines?: string | null;
  nextExpiry?: string | null;
  cycle?: CycleRef | null;
}

export interface LineView {
  lineNo: number;
  benefitLine: string;
  productCode?: string | null;
  incumbentInsurer?: string | null;
  currentPolicyNo?: string | null;
  currentArn?: string | null;
  periodFrom?: string | null;
  periodTo?: string | null;
  headcount?: number | null;
  active: boolean;
}

export interface ContactView {
  id: number;
  name: string;
  email: string;
  mobile?: string | null;
  role: ContactRole;
  receivesRa: boolean;
  receivesSoa: boolean;
  active: boolean;
}

export interface AdviceView {
  sentAt: string;
  sentBy: string;
  manual: boolean;
  expiryDate: string;
  recipients: string[];
  remindersSent: number;
  lastReminderAt?: string | null;
  feedbackAt?: string | null;
  attachmentId?: number | null;
}

export interface CycleView {
  id: number;
  cycleNo: string;
  businessType: BusinessType;
  policyYear: number;
  targetInception?: string | null;
  stage: string;
  remarketing: boolean;
  outcome?: string | null;
  outcomeReason?: string | null;
  closedAt?: string | null;
  accountArns: string[];
  renewalAdvice?: AdviceView | null;
  borStatus: string;
}

export interface ProgrammeView {
  id: number;
  programmeNo: string;
  client: { id: number; code: string; name: string };
  name: string;
  teamCode: string;
  funding: Funding;
  accountOfficer: string;
  salesUnit?: string | null;
  renewalEligible: boolean;
  status: string;
  createdAt: string;
  createdBy: string;
  lines: LineView[];
  contacts: ContactView[];
  cycles: CycleView[];
  currentCycleId?: number | null;
}

export interface ProfileInput {
  name: string;
  teamCode: string;
  funding: Funding | '';
  accountOfficer?: string;
  salesUnit?: string;
  renewalEligible: boolean;
}

export interface LineInput {
  benefitLine: string;
  productCode?: string;
  incumbentInsurer?: string;
  currentPolicyNo?: string;
  currentArn?: string;
  periodFrom?: string;
  periodTo?: string;
  headcount?: number;
}

export interface ContactInput {
  name: string;
  email: string;
  mobile?: string;
  role: ContactRole | '';
  receivesRa: boolean;
  receivesSoa: boolean;
}

export interface NewProgrammeInput {
  clientId?: number;
  profile: ProfileInput;
  lines: LineInput[];
  contacts: ContactInput[];
}

export interface SendRaResult {
  programmeId: number;
  programmeNo?: string | null;
  sent: boolean;
  cycleNo?: string | null;
  message?: string | null;
}

export interface FeedbackItem {
  id: number;
  cycleId: number;
  channel: FeedbackChannel;
  receivedOn: string;
  text?: string | null;
  fileCount: number;
  recordedBy: string;
  recordedAt: string;
}

export interface DocumentView {
  id: number;
  cycleId?: number | null;
  cycleNo?: string | null;
  documentType: string;
  documentTypeLabel: string;
  processType: string;
  processLabel: string;
  versionNo: number;
  status: string;
  source: DocumentSource;
  attachmentId: number;
  fileName: string;
  sizeBytes: number;
  uploadedBy: string;
  uploadedAt: string;
}

export interface BorVersion {
  id: number;
  cycleId: number;
  versionNo: number;
  attachmentId: number;
  status: string;
  signedBySignatory?: boolean | null;
  notBlank?: boolean | null;
  clientNameMatches?: boolean | null;
  validFrom?: string | null;
  validTo?: string | null;
  uploadedBy: string;
  uploadedAt: string;
  decidedBy?: string | null;
  decidedAt?: string | null;
  rejectReason?: string | null;
}

export interface BorChecklist {
  signedBySignatory: boolean;
  notBlank: boolean;
  clientNameMatches: boolean;
  validFrom: string;
  validTo: string;
}

export interface CycleAccount {
  id: number;
  arn: string;
  cycleId: number;
  cycleNo: string;
  productCode?: string | null;
  insurerCode?: string | null;
  periodFrom?: string | null;
  periodTo?: string | null;
  businessType: BusinessType;
  renewalOfRef?: string | null;
  status: string;
  grossPremium?: number | null;
  currency?: string | null;
}

export interface ActivityRow {
  id: number;
  cycleNo?: string | null;
  activity: string;
  reference?: string | null;
  receivedAt: string;
  releasedAt?: string | null;
  actor: string;
  remarks?: string | null;
}

export interface ItemRow {
  id: number;
  programmeId: number;
  programmeNo: string;
  clientName: string;
  cycleId?: number | null;
  itemType: string;
  subject: string;
  memberRef?: string | null;
  memberChangeRef?: string | null;
  accountArn?: string | null;
  responsible: Responsible;
  partyCode?: string | null;
  recipientEmail?: string | null;
  status: string;
  dueDate: string;
  daysPastDue: number;
  followUpsSent: number;
  lastFollowUpAt?: string | null;
  escalatedAt?: string | null;
  receivedOn?: string | null;
  releasedOn?: string | null;
  closedOn?: string | null;
  remarks?: string | null;
}

export interface ItemInput {
  programmeId?: number;
  cycleId?: number;
  itemType?: string;
  subject: string;
  memberRef?: string;
  memberChangeRef?: string;
  accountArn?: string;
  responsible: Responsible | '';
  partyCode?: string;
  recipientEmail?: string;
  dueDate: string;
  remarks?: string;
}

export interface ItemFilters {
  programmeId?: number;
  member?: string;
  type?: string;
  responsible?: string;
  status?: string;
  overdue?: boolean;
  q?: string;
}

export interface ProgrammeFilters {
  tab: ProgrammeTab;
  stage?: string;
  ao?: string;
  team?: string;
  q?: string;
}

export type CycleStep = 'start' | 'stay-with-incumbent' | 'remarket';
