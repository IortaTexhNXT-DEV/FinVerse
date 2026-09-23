import { humanize } from '@/utils/format';

const TONES: Record<string, string> = {
  ACTIVE: 'success',
  POSTED: 'success',
  OPEN: 'success',
  APPROVED: 'success',
  PAID: 'success',
  RECONCILED: 'success',
  PENDING_AUTHORIZATION: 'warning',
  PENDING_APPROVAL: 'warning',
  CLOSING: 'warning',
  REOPENED: 'warning',
  DRAFT: 'neutral',
  FUTURE: 'neutral',
  INACTIVE: 'neutral',
  CANCELLED: 'neutral',
  REJECTED: 'danger',
  REVERSED: 'danger',
  CLOSED: 'danger',
  FROZEN: 'danger',
};

/** Colour-coded status pill; unknown statuses render in the neutral info tone. */
export function StatusBadge({ status }: Readonly<{ status: string }>) {
  const tone = TONES[status] ?? '';
  return <span className={`badge ${tone}`}>{humanize(status)}</span>;
}
