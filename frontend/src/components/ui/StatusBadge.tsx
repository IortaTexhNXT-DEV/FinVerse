import { humanize } from '@/utils/format';

type Tone = 'success' | 'warning' | 'neutral' | 'danger';

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
  ],
  warning: [
    'PENDING_AUTHORIZATION',
    'PENDING_APPROVAL',
    'CLOSING',
    'REOPENED',
    'RUNNING',
    'UNMATCHED',
    // broking
    'QUEUED',
    'VALIDATED',
    'PROSPECT',
    'PENDING',
    'SUBMITTED',
    'FOR_REVIEW',
    'FOR_MKT_APPROVAL',
    'QS_FOR_APPROVAL',
    'PS_FOR_APPROVAL',
    'AWAITING_PAYMENT',
    'READY_FOR_PLACEMENT',
    'PLACED',
    'SENT_TO_CLIENT',
    'WITH_TSU',
    'QS_PREPARATION',
    'QS_SENT',
    'TERMS_RECEIVED',
    'PS_RELEASED',
    'KYC_REVIEW',
  ],
  neutral: ['DRAFT', 'FUTURE', 'INACTIVE', 'CANCELLED', 'UNKNOWN', 'NOT_STARTED'],
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
  ],
};

const TONES = new Map<string, Tone>(
  (Object.entries(TONE_GROUPS) as [Tone, string[]][]).flatMap(([tone, statuses]) =>
    statuses.map((status) => [status, tone] as const),
  ),
);

/** Colour-coded status pill; unknown statuses render in the neutral info tone. */
export function StatusBadge({ status }: Readonly<{ status: string }>) {
  const tone = TONES.get(status) ?? '';
  return <span className={`badge ${tone}`}>{humanize(status)}</span>;
}
