import type { Period, PeriodStatus } from '@/api/periods';

export type PeriodAction = 'open' | 'startClosing' | 'close' | 'reopen';

/** Longest reopening reason the API accepts (ReasonRequest). */
export const REASON_MAX_LENGTH = 200;

/** Status changes offered for each period status. */
export const PERIOD_ACTIONS: Record<PeriodStatus, { action: PeriodAction; label: string }[]> = {
  FUTURE: [{ action: 'open', label: 'Open' }],
  OPEN: [
    { action: 'startClosing', label: 'Start closing' },
    { action: 'close', label: 'Close' },
  ],
  CLOSING: [
    { action: 'open', label: 'Back to open' },
    { action: 'close', label: 'Close' },
  ],
  CLOSED: [{ action: 'reopen', label: 'Reopen' }],
  REOPENED: [{ action: 'close', label: 'Close again' }],
};

export interface PeriodActionText {
  title: string;
  consequence: string;
  confirmLabel: string;
  /** A reason is mandatory (reopening a closed period). */
  needsReason: boolean;
  /** The action blocks postings: confirm with the danger style. */
  danger: boolean;
}

/** Title and consequence shown in the confirmation dialog of a period status change. */
export function periodActionText(action: PeriodAction, period: Period): PeriodActionText {
  const name = period.name;
  switch (action) {
    case 'open':
      return {
        title: period.status === 'CLOSING' ? `Return ${name} to open` : `Open ${name}`,
        consequence: `${name} will accept normal postings from every module.`,
        confirmLabel: 'Open period',
        needsReason: false,
        danger: false,
      };
    case 'startClosing':
      return {
        title: `Start closing ${name}`,
        consequence: `${name} will only accept system and adjustment journals; normal vouchers dated in it will be refused.`,
        confirmLabel: 'Start closing',
        needsReason: false,
        danger: false,
      };
    case 'close':
      return {
        title: `Close ${name}`,
        consequence: `All postings to ${name} will be blocked. Reopening it later needs a reason and is recorded in the audit trail.`,
        confirmLabel: 'Close period',
        needsReason: false,
        danger: true,
      };
    case 'reopen':
      return {
        title: `Reopen ${name}`,
        consequence: `${name} will accept postings again, which changes figures already reported for it. The reason is recorded in the audit trail.`,
        confirmLabel: 'Reopen period',
        needsReason: true,
        danger: true,
      };
  }
}

/** Validation message for a reopening reason, or undefined when it is acceptable. */
export function reasonProblem(reason: string): string | undefined {
  if (reason.trim() === '') {
    return 'Enter the reason for reopening';
  }
  return reason.length > REASON_MAX_LENGTH ? `At most ${REASON_MAX_LENGTH} characters` : undefined;
}
