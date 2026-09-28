import { humanize } from '@/utils/format';

/** Colour tones of the status pills (BDO status pill colours). */

export type Tone = 'success' | 'warning' | 'info' | 'neutral' | 'danger';

/**
 * Colour tone per status. Statuses only share a colour: the badge always shows the status's own
 * label, so callers pass the real status (LOCKED, SUCCEEDED, UP…) and never a look-alike.
 */
const TONE_GROUPS: Record<Tone, string[]> = {
  success: [
    'ACTIVE',
    'POSTED',
    'OPEN',
    'APPROVED',
    'PAID',
    'RECONCILED',
    'MATCHED',
    'SUCCEEDED',
    'UP',
    // broking
    'SENT',
    'VALID',
    'COMMITTED',
    'COMPLETED',
    'CONFIRMED',
    'VERIFIED',
    'ACCEPTED',
    'CONVERTED',
    'POLICY_ISSUED',
    'BOOKED',
    'KYC_VERIFIED',
    // product maintenance
    'RELEASED',
    'SIGNED',
    'CURRENT',
    'ACCEPTED_AS_REQUESTED',
    // collections
    'TAKEN',
    'PUBLISHED',
    // collections plans and escalations
    'RESOLVED',
    'KEPT',
    // report batch items (FRBS 2.4.5)
    'OK',
    // disbursement
    'IN_VOUCHER',
    'NEGOTIATED',
    'DEBITED',
    'CREDITED',
    // collections unapplied payments
    'APPLIED',
    // accounting reports: service fee and certificates received
    'LIQUIDATED',
    'RECORDED',
    // sanction screening (runs, cleared matches)
    'SUCCESS',
    'FALSE_POSITIVE',
    // sanction screening cases: SLA on time, STR filed with the AMLC
    'ON_TIME',
    'FILED',
  ],
  warning: [
    'ON_HOLD',
    // sanction screening: a match not yet decided; cases waiting for a decision
    'POTENTIAL',
    'DUE_SOON',
    'UNIT_HEAD_APPROVAL',
    'COMPLIANCE_REVIEW',
    'AML_COMMITTEE',
    'PENDING_AUTHORIZATION',
    'PENDING_APPROVAL',
    'CLOSING',
    'REOPENED',
    'RUNNING',
    'UNMATCHED',
    // broking: waiting for a review, an approval or a decision
    'QUEUED',
    'VALIDATED',
    'PROSPECT',
    'PENDING',
    // employee benefits: waiting for a sign-off, an approval or a validation
    'FOR_SIGNOFF',
    'THRESHOLD_APPROVAL',
    'READY_TO_PRESENT',
    'UPLOADED',
    'LAPSED',
    'FOR_REVIEW',
    'FOR_MKT_APPROVAL',
    'QS_FOR_APPROVAL',
    'PS_FOR_APPROVAL',
    'KYC_REVIEW',
    // product maintenance: approvals, reviews and sign-off
    'FOR_TSU_REVIEW',
    'FOR_TSU_APPROVAL',
    'TERMS_REVIEW',
    'FOR_MKT_REVIEW',
    'FOR_MANCOM',
    'FOR_VALIDATION',
    'FOR_APPROVAL',
    'APPROVED_WITH_CHANGES',
    'COUNTER_PROPOSAL',
    // collections
    'UNPAID',
    // collections plans and escalations
    'WITH_TL',
    'WITH_UH',
    'RETURNED',
    'DUE',
    'PARTIAL',
    'PARTIALLY_KEPT',
    // disbursement: waiting for the checker
    'FOR_AUTHORIZATION',
    'FOR_DEACTIVATION',
    'FOR_REACTIVATION',
    'FOR_APPROVAL_1',
    'FOR_APPROVAL_2',
    // collections unapplied payments
    'UNAPPLIED',
    'FOR_REVERSAL',
    // remittance deductions
    'FOR_CONFIRMATION',
  ],
  // BDO style guide: blue pills for records moving through processing ("Account for Placement").
  info: [
    // sanction screening cases in process
    'INVESTIGATION',
    'STR_PREPARATION',
    'STR_EXTRACTION',
    'SUBMITTED',
    'AWAITING_PAYMENT',
    'READY_FOR_PLACEMENT',
    'PLACED',
    'SENT_TO_CLIENT',
    'WITH_TSU',
    'QS_PREPARATION',
    'QS_SENT',
    'TERMS_RECEIVED',
    'PS_RELEASED',
    'REQUESTED',
    'GENERATED',
    'SCHEDULED',
    'READY',
    // product maintenance: work in progress
    'NEGOTIATION',
    'REQUIREMENTS_PREP',
    'WITH_MBS',
    'PREPARATION',
    // collections
    'CREDIT',
    'PARTIALLY_PAID',
    // collections plans and escalations
    'IN_ACTION',
    'NOT_DUE',
    // disbursement: in process
    'IN_PROCESS',
    'RECEIVED',
    'PRINTED',
    'EMAILED',
    'EXTRACTED',
    // collections unapplied payments
    'DEFERRED',
    'MONITORING',
    // accounting reports: computed service fee, schedule layout to confirm
    'COMPUTED',
    'TO_CONFIRM',
    // employee benefits: cycle stages in marketing and placement
    'RA_SENT',
    'REQUIREMENTS',
    'INCUMBENT_TERMS',
    'FRANCHISE',
    'PROPOSALS',
    'COMPARATIVE',
    'REVISION',
    'WITH_CLIENT',
    'IN_PLACEMENT',
  ],
  neutral: [
    'DRAFT',
    // platform: job run not started (another instance ran it), event delivered in-process
    'SKIPPED_LOCKED',
    'LOCAL',
    'DISCARDED',
    'RETRIED',
    'FUTURE',
    'INACTIVE',
    'CANCELLED',
    'UNKNOWN',
    'NOT_STARTED',
    // product maintenance
    'SUPERSEDED',
    'RETIRED',
    // collections
    'EXCLUDED_CANCELLED',
    'NOT_APPLICABLE',
    // disbursement
    'NOT_POSTED',
    'EXHAUSTED',
    // remittance: settled by deductions, no payment request
    'NOT_REQUIRED',
  ],
  danger: [
    // employee benefits: cycles and programmes lost or not renewed
    'CLOSED_LOST',
    'NOT_RENEWED',
    'LOST',
    // sanction screening: a confirmed match, a case past its SLA
    'TRUE_MATCH',
    'BREACHED',
    'REJECTED',
    'REVERSED',
    'CLOSED',
    'FROZEN',
    'LOCKED',
    'FAILED',
    'DOWN',
    'OUT_OF_SERVICE',
    // broking
    'INVALID',
    'VOIDED',
    'NOT_PROCEEDED',
    'RETURNED_TO_MARKETING',
    'RETURNED_BY_INSURER',
    'PLACEMENT_CANCELLED',
    'EXPIRED',
    // product maintenance
    'DECLINED',
    'NO_RESPONSE',
    // collections plans and escalations
    'OVERDUE',
    'BROKEN',
    // disbursement
    'NO_PAYEE',
    'STALE',
    'REVERSAL_FAILED',
  ],
};

const TONES = new Map<string, Tone>(
  (Object.entries(TONE_GROUPS) as [Tone, string[]][]).flatMap(([tone, statuses]) =>
    statuses.map((status) => [status, tone] as const),
  ),
);

/**
 * Agreed short forms of the long status labels, so every pill keeps one size and never wraps. The
 * full label is always in the tooltip.
 */
const SHORT_LABELS: Record<string, string> = {
  RETURNED_TO_MARKETING: 'Returned to Mktg',
  RETURNED_BY_INSURER: 'Insurer Returned',
  ACCEPTED_AS_REQUESTED: 'Accepted as Req.',
  APPROVED_WITH_CHANGES: 'Appr. with Changes',
  PENDING_AUTHORIZATION: 'Pending Auth.',
  PLACEMENT_CANCELLED: 'Plcmt Cancelled',
  EXCLUDED_CANCELLED: 'Excl. Cancelled',
  REQUIREMENTS_PREP: 'Reqts Prep',
  UNIT_HEAD_APPROVAL: 'Unit Head Appr.',
  FOR_MKT_APPROVAL: 'For Mktg Approval',
  FOR_TSU_APPROVAL: 'For TSU Approval',
  FOR_AUTHORIZATION: 'For Authorization',
  FOR_DEACTIVATION: 'For Deactivation',
  FOR_REACTIVATION: 'For Reactivation',
  STR_PREPARATION: 'STR Preparation',
  READY_FOR_PLACEMENT: 'For Placement',
  REVERSAL_FAILED: 'Reversal Failed',
  PARTIALLY_PAID: 'Partially Paid',
  PARTIALLY_KEPT: 'Partially Kept',
  SKIPPED_LOCKED: 'Skipped',
};

/**
 * Business wording of statuses whose code does not read as the label users know (the code is
 * never shown): the insurer-only line of a reconciliation, the pick-up of a check.
 */
const STATUS_LABELS: Record<string, string> = {
  UNMATCHED_NO_BOOKING: 'Insurer Only',
  FOR_PICKUP: 'For Pick-up',
  PICKED_UP: 'Picked Up',
  NON_CASH: 'Non-cash',
  NOT_APPLICABLE: 'Not Applicable',
};

/** The full label of a status: its business wording, else the humanized code. */
export function statusLabel(status: string): string {
  return STATUS_LABELS[status] ?? humanize(status);
}

/**
 * The success message of a status change, a sentence with the status as its pill reads it:
 * "DSQ-2026-000003 is now DV Assigned." (never the code or its lower-case words).
 */
export function statusMessage(record: string, status: string, action?: string): string {
  const what = action ? `${action} ${record}` : record;
  return `${what} is now ${statusLabel(status)}.`;
}

/**
 * Longest label shown in full (fits the widest pill: "Returned to Marketing", "Pending
 * Authorization"); longer labels use the short form or are cut with an ellipsis.
 */
export const BADGE_MAX_CHARS = 21;

/** Colour tone of a status code (empty for an unknown status: neutral Header Blue). */
export function statusTone(status: string): Tone | '' {
  return TONES.get(status) ?? '';
}

/** Label shown in the pill: the agreed short form of a long status, else the humanized status. */
export function statusShortLabel(status: string, label?: string): string {
  const full = label ?? statusLabel(status);
  if (full.length <= BADGE_MAX_CHARS) {
    return full;
  }
  return SHORT_LABELS[status] ?? full;
}
