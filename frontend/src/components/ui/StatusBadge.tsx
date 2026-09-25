import { humanize } from '@/utils/format';

type Tone = 'success' | 'warning' | 'info' | 'neutral' | 'danger';

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
  ],
  warning: [
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
  ],
  // BDO style guide: blue pills for records moving through processing ("Account for Placement").
  info: [
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
  ],
  danger: [
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
    'RETURNED',
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
 * Colour-coded, outlined status pill (BDO): green approved or done, yellow waiting for review or
 * approval, blue in process, red exception or rejected; unknown statuses render in the neutral
 * Header Blue tone.
 */
export function StatusBadge({ status }: Readonly<{ status: string }>) {
  const tone = TONES.get(status) ?? '';
  return <span className={`badge ${tone}`}>{humanize(status)}</span>;
}
