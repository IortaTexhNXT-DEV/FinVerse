import type { RowAction } from '@/components/ui/RowActions';
import type { CloseSchedule } from './closeControlsApi';

/**
 * The row menu of a scheduled month-end close: the GL Team Lead withdraws a close that has not run
 * yet, after a confirmation (screen standard: row actions in one menu, destructive last in red).
 */
export function scheduleRowActions(
  s: Pick<CloseSchedule, 'status' | 'periodName'>,
  mayWithdraw: boolean,
  withdraw: () => unknown,
): RowAction[] {
  return mayWithdraw && s.status === 'SCHEDULED'
    ? [
        {
          label: 'Withdraw',
          danger: true,
          onSelect: () => withdraw(),
          confirm: {
            title: `Withdraw the Close of ${s.periodName}`,
            record: s.periodName,
            effect: 'No close runs for the period until a new close is scheduled.',
            confirmLabel: 'Withdraw',
            destructive: true,
          },
        },
      ]
    : [];
}

/** The note under the closes: when the scheduled jobs run, in words. */
export const CLOSE_JOBS_NOTE =
  'Due closes are run every 15 minutes; the broking books are cut off automatically on the last day of the month.';
