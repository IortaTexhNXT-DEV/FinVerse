/** Breadcrumb of every Data Migration screen. */
export const MIG_SECTION = 'Data Migration';

const LABELS: Record<string, string> = {
  MIGRATE: 'Migrate',
  CARRY_FORWARD: 'Carry forward',
  ARCHIVE: 'Archive',
  EXCLUDED: 'Excluded',
  CONDITIONAL: 'Conditional',
  PROPOSED: 'Proposed',
  FOR_DECISION: 'For decision',
  DECIDED: 'Decided',
  RETURNED: 'Returned',
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  APPROVED: 'Approved',
  SUPERSEDED: 'Superseded',
  FROZEN: 'Frozen',
  RETIRED: 'Retired',
  RECEIVED: 'Received',
  CHECKED: 'Checked',
  STAGED: 'Staged',
  REJECTED: 'Rejected',
  PURGED: 'Purged',
  PLANNED: 'Planned',
  VALIDATING: 'Validating',
  VALIDATED: 'Validated',
  LOADING: 'Loading',
  LOADED: 'Loaded',
  LOADED_WITH_REJECTS: 'Loaded with rejects',
  FAILED: 'Failed',
  RECONCILED: 'Reconciled',
  SIGNED_OFF: 'Accepted',
  ROLLBACK_REQUESTED: 'Rollback requested',
  ROLLED_BACK: 'Rolled back',
  VALID: 'Valid',
  WARNING: 'Warning',
  INVALID: 'Invalid',
  SKIPPED: 'Skipped',
  WAIVED: 'Waived',
  MATCHED: 'Matched',
  BREAK: 'Break',
  EXPLAINED: 'Explained',
  BREAKS: 'Breaks',
  SIGNED: 'Signed',
  OPEN: 'Open',
  FIXED_AT_SOURCE: 'Fixed at source',
  MAPPED: 'Mapped',
  AUTO_MERGE: 'Merged automatically',
  REVIEW: 'To review',
  MERGE: 'Merged',
  KEEP_SEPARATE: 'Kept separate',
  PREPARED: 'Prepared',
  DATA_OWNER: 'Data owner',
  DATA_MIGRATION_LEAD: 'Data Migration Lead',
  DATA_STEWARD: 'Data Steward',
  MIGRATION_RECON_APPROVER: 'Reconciliation approver',
};

/**
 * Readable label of a migration code ("LOADED_WITH_REJECTS" to "Loaded with rejects").
 *
 * @param code code
 * @returns label
 */
export function migLabel(code: string | undefined | null): string {
  if (code === undefined || code === null || code === '') {
    return '';
  }
  const known = LABELS[code];
  if (known !== undefined) {
    return known;
  }
  const text = code.replaceAll('_', ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** The gates of an object and batch, in order. */
export const GATES = [
  { id: 'G1', label: 'Decision' },
  { id: 'G2', label: 'Mapping' },
  { id: 'G3', label: 'Validation' },
  { id: 'G4', label: 'Load approval' },
  { id: 'G5', label: 'Reconciliation' },
  { id: 'G6', label: 'Acceptance' },
  { id: 'G7', label: 'Go-live' },
] as const;

/** Reasons of a waiver or exclusion (list MIG_WAIVER_REASON). */
export const WAIVER_REASONS = [
  { code: 'NOT_NEEDED', label: 'Record not needed in BIBS' },
  { code: 'MANUAL_ENTRY', label: 'Entered manually in BIBS after go-live' },
];

/** Reasons of a reconciliation break (list MIG_BREAK_REASON). */
export const BREAK_REASONS = [
  { code: 'TIMING', label: 'Timing difference between extract and ledger' },
  { code: 'ROUNDING', label: 'Rounding in the legacy extract' },
  { code: 'EXCLUDED_ROWS', label: 'Rows excluded by the data owner' },
  { code: 'SOURCE_ERROR', label: 'Error in the legacy source, corrected by manual entry' },
  { code: 'MAPPING', label: 'Code map difference' },
  { code: 'OTHER', label: 'Other (see explanation)' },
];

/** Steps of a batch shown on the batch timeline. */
export const BATCH_STEPS = [
  { id: 'PLANNED', label: 'Planned' },
  { id: 'VALIDATED', label: 'Validated' },
  { id: 'APPROVED', label: 'Load approved' },
  { id: 'LOADED', label: 'Loaded' },
  { id: 'RECONCILED', label: 'Reconciled' },
  { id: 'SIGNED_OFF', label: 'Accepted' },
] as const;

const ORDER = [
  'PLANNED',
  'VALIDATING',
  'VALIDATED',
  'APPROVED',
  'LOADING',
  'LOADED',
  'LOADED_WITH_REJECTS',
  'RECONCILED',
  'SIGNED_OFF',
];

/**
 * Whether a batch in `status` has passed the timeline step `step`.
 *
 * @param status batch status
 * @param step step
 * @returns true when done
 */
export function stepDone(status: string, step: string): boolean {
  const s = status === 'LOADED_WITH_REJECTS' ? 'LOADED' : status;
  const at = ORDER.indexOf(s);
  const wanted = ORDER.indexOf(step);
  return at >= 0 && wanted >= 0 && at >= wanted;
}

/**
 * A percentage for display, empty when unknown.
 *
 * @param value percent
 * @returns text
 */
export function percent(value: number | undefined): string {
  return value === undefined ? '' : `${String(value)}%`;
}

/**
 * "Yes" or "No" with an optional note in brackets.
 *
 * @param value flag
 * @param note note
 * @returns text
 */
export function yesNo(value: boolean, note?: string): string {
  const answer = value ? 'Yes' : 'No';
  return note ? answer + ' (' + note + ')' : answer;
}

/**
 * A name followed by a detail in brackets when there is one.
 *
 * @param main main text
 * @param detail detail
 * @returns text
 */
export function withDetail(main: string, detail: string | undefined): string {
  return detail ? main + ' (' + detail + ')' : main;
}
