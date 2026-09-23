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
  ],
  warning: [
    'PENDING_AUTHORIZATION',
    'PENDING_APPROVAL',
    'CLOSING',
    'REOPENED',
    'RUNNING',
    'UNMATCHED',
  ],
  neutral: ['DRAFT', 'FUTURE', 'INACTIVE', 'CANCELLED', 'UNKNOWN'],
  danger: [
    'REJECTED',
    'REVERSED',
    'CLOSED',
    'FROZEN',
    'LOCKED',
    'FAILED',
    'DOWN',
    'OUT_OF_SERVICE',
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
