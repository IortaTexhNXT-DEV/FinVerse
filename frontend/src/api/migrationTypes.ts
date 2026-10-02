/** Types of the Data Migration console (BRD-13), mirroring the backend DTOs of `migration`. */

export type MigrationClass = 'MIGRATE' | 'CARRY_FORWARD' | 'ARCHIVE' | 'EXCLUDED' | 'CONDITIONAL';

export interface DataObject {
  code: string;
  name: string;
  category: string;
  sourceSystems: string[];
  target: string;
  businessOwner?: string;
  dataSteward?: string;
  ownerTitle?: string;
  stewardTitle?: string;
  day1Need: boolean;
  day1Note?: string;
  complianceNeed: boolean;
  complianceNote?: string;
  archivalOption: boolean;
  archivalNote?: string;
  dataTrust: string;
  proposedClass: MigrationClass;
  decidedClass?: MigrationClass;
  conditionText?: string;
  conditionMet: boolean;
  dependsOn: string[];
  loadOrder: number;
  financial: boolean;
  rationale: string;
  status: string;
  decidedBy?: string;
  decidedAt?: string;
}

export interface Decision {
  decisionNo: string;
  objectCode: string;
  proposedClass: MigrationClass;
  conditionText?: string;
  conditionMet: boolean;
  criteria: string;
  rationale: string;
  status: string;
  submittedBy: string;
  submittedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  returnReason?: string;
}

export interface MapSet {
  code: string;
  name: string;
  targetDomain: string;
  targetKind: string;
  ownerTitle?: string;
  stewardTitle?: string;
  usedBy?: string;
  approvedVersion?: number;
  openVersionId?: number;
  openStatus?: string;
}

export interface MapVersion {
  id: number;
  setCode: string;
  versionNo: number;
  status: string;
  comment?: string;
  returnReason?: string;
  submittedBy?: string;
  submittedAt?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdBy: string;
}

export type EntryAction = 'MAP' | 'DEFAULT' | 'REJECT' | 'CREATE';

export interface MapEntry {
  id: number;
  sourceSystem: string;
  legacyCode: string;
  legacyDescription?: string;
  qualifier?: string;
  qualifierValue?: string;
  action: EntryAction;
  targetCode?: string;
  remarks?: string;
}

export type MapEntryInput = Omit<MapEntry, 'id'>;

export interface MapDiff {
  added: MapEntry[];
  changed: MapEntry[];
  removed: MapEntry[];
}

export interface UnmappedCode {
  setCode: string;
  sourceSystem: string;
  legacyCode: string;
  rows: number;
  sampleKeys: string;
  error: boolean;
}

export interface Layout {
  id: number;
  code: string;
  versionNo: number;
  objectCode: string;
  title: string;
  keyColumns: string;
  hashRule: string;
  hashColumns?: string;
  amountColumns?: string;
  status: string;
  frozenBy?: string;
  frozenAt?: string;
}

export interface LayoutColumn {
  seq: number;
  name: string;
  description: string;
  dataType: string;
  length?: string;
  mandatory: string;
  allowedValues?: string;
  mapSet?: string;
  format?: string;
  example?: string;
  target?: string;
  validation?: string;
}

export interface Rule {
  code: string;
  layoutScope: string;
  columns: string;
  kind: string;
  description: string;
  severity: string;
  message: string;
  fixedBy?: string;
  active: boolean;
}

export interface MaskingRule {
  id: number;
  layoutCode: string;
  columnName: string;
  rule: string;
  active: boolean;
}

export interface Extract {
  extractNo: string;
  objectCode: string;
  layoutCode: string;
  layoutVersion: number;
  sourceSystem: string;
  asOf: string;
  sequenceNo: number;
  mode: string;
  fileName: string;
  controlFileName?: string;
  dataFileId?: number;
  controlFileId?: number;
  sha256: string;
  declaredRows?: number;
  parsedRows: number;
  stagedRows: number;
  hashTotal?: string;
  masked: boolean;
  status: string;
  rejectCode?: string;
  rejectMessage?: string;
  /** Every failed intake check of a rejected extract with its reason. */
  rejectChecks?: { check: string; reason: string }[];
  receivedBy: string;
  receivedAt: string;
  purgedAt?: string;
}

export interface BatchCounts {
  staged: number;
  valid: number;
  warning: number;
  invalid: number;
  loaded: number;
  skipped: number;
  rejected: number;
  excluded: number;
  waived: number;
}

export interface Batch {
  batchNo: string;
  objectCode: string;
  mode: string;
  parentBatchNo?: string;
  status: string;
  environmentClass: string;
  counts: BatchCounts;
  errorRate?: number;
  unmappedCount: number;
  reviewCount: number;
  mapVersions: Record<string, number>;
  extractNos: string[];
  validatedBy?: string;
  validatedAt?: string;
  loadApprovedBy?: string;
  loadApprovedAt?: string;
  loadedBy?: string;
  startedAt?: string;
  endedAt?: string;
  signedOffAt?: string;
  rollbackReason?: string;
  rollbackRequestedBy?: string;
  purgeDueOn?: string;
  purgedAt?: string;
  createdBy: string;
  createdAt: string;
}

export interface BatchLog {
  step: string;
  level: string;
  message: string;
  counts?: string;
  loggedBy: string;
  loggedAt: string;
}

export interface StageRow {
  id: number;
  layoutCode: string;
  rowNo: number;
  legacyKey: string;
  status: string;
  targetEntity?: string;
  targetCode?: string;
  message?: string;
  values: Record<string, string>;
}

export interface Issue {
  id: number;
  rowId: number;
  ruleCode: string;
  severity: string;
  field?: string;
  value?: string;
  message: string;
  resolution: string;
  waiverReason?: string;
  resolutionNote?: string;
  resolvedBy?: string;
  resolvedAt?: string;
}

export interface Signoff {
  id: number;
  objectCode: string;
  batchId?: number;
  gate: string;
  gateLabel: string;
  roleCode: string;
  username: string;
  decision: string;
  comment?: string;
  evidenceName?: string;
  evidenceFileId?: number;
  signedAt: string;
}

export interface GateCell {
  decision: string;
  signedBy: string;
  signedAt: string;
}

export interface GateRow {
  objectCode: string;
  objectName: string;
  objectClass: string;
  objectStatus: string;
  batchNo?: string;
  batchStatus?: string;
  gates: Partial<Record<string, GateCell>>;
}

export interface Resubmission {
  resubmissionNo: string;
  objectCode: string;
  status: string;
  correctedRows: number;
  preparedBy: string;
  preparedAt: string;
  decidedBy?: string;
  decidedAt?: string;
  decisionNote?: string;
}

export interface ReconLine {
  id: number;
  level: string;
  measure: string;
  currency?: string;
  sourceValue?: number;
  stagedValue?: number;
  targetValue?: number;
  difference?: number;
  tolerance?: number;
  status: string;
  detail?: string;
  breakReason?: string;
  explanation?: string;
  explainedBy?: string;
  approvedBy?: string;
}

export interface ReconRun {
  runNo: string;
  objectCode: string;
  asOf: string;
  status: string;
  breakCount: number;
  runBy: string;
  runAt: string;
  lines: ReconLine[];
}

export interface MatchPair {
  id: number;
  batchNo?: string;
  clusterNo: number;
  leftKey: string;
  rightKey?: string;
  rightClientCode?: string;
  score: number;
  matchedKeys: string;
  decision: string;
  survivorKey?: string;
  lostValues?: string;
  decidedBy?: string;
  decidedAt?: string;
}

export interface MatchSides {
  left: Record<string, string>;
  right: Record<string, string>;
}

export interface Xref {
  sourceSystem: string;
  objectCode: string;
  legacyKey: string;
  targetEntity: string;
  targetCode?: string;
  batchNo?: string;
  loadedAt: string;
  rolledBackAt?: string;
}

export interface HomeTask {
  planNo: string;
  seq: number;
  phase: string;
  task: string;
  ownerRole: string;
  plannedStart?: string;
  status: string;
}

export interface RunoffMonth {
  expiryMonth: string;
  inForce: number;
  renewed: number;
  notRenewed: number;
  stillOpen: number;
}

export interface HomeTiles {
  objectsByStatus: Record<string, number>;
  batchesByStatus: Record<string, number>;
  openBreaks: number;
  unmappedCodes: number;
  openErrors: number;
  pairsToReview: number;
  nextTasks: HomeTask[];
  runoff: RunoffMonth[];
}
