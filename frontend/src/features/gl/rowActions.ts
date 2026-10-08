import type { GlAccount } from '@/api/gl';
import type { Period } from '@/api/periods';
import type { RowAction } from '@/components/ui/RowActions';
import { humanize } from '@/utils/format';
import { PERIOD_ACTIONS } from './periodActions';
import type { PeriodAction } from './periodActions';

/**
 * The row menus of the General Ledger lists (screen standard: the actions on a record of a list are
 * one menu at the end of the row; a destructive action comes last, in red, and asks for
 * confirmation).
 */

/** The menu of an account of the chart: Authorize for the second checker, then Freeze/Unfreeze. */
export function accountRowActions(
  a: Pick<GlAccount, 'code' | 'name' | 'frozen' | 'recordStatus'>,
  may: { authorize: boolean; awaitsMe: boolean },
  on: { authorize: () => unknown; freeze: (reason: string) => unknown; unfreeze: () => unknown },
): RowAction[] {
  const actions: RowAction[] = [];
  if (may.authorize && may.awaitsMe) {
    actions.push({
      label: 'Authorize',
      onSelect: () => on.authorize(),
      confirm: {
        title: `Authorize Account ${a.code}`,
        record: a.name,
        effect: 'The account becomes active and can be posted to.',
      },
    });
  }
  if (may.authorize && a.recordStatus === 'ACTIVE') {
    actions.push(
      a.frozen
        ? {
            label: 'Unfreeze',
            onSelect: () => on.unfreeze(),
            confirm: {
              title: `Unfreeze Account ${a.code}`,
              record: a.name,
              effect: 'Postings to the account are allowed again.',
            },
          }
        : {
            label: 'Freeze',
            danger: true,
            onSelect: (reason) => on.freeze(reason),
            confirm: {
              title: `Freeze Account ${a.code}`,
              record: a.name,
              effect: 'Postings to the account are refused until it is unfrozen.',
              reason: 'required',
              destructive: true,
            },
          },
    );
  }
  return actions;
}

/** The class or the sub-ledger of an account in words. */
export function accountWord(code: string | null | undefined): string {
  return code ? humanize(code) : '';
}

/** The menu of a financial period: the status changes of its status, each confirmed in a dialog. */
export function periodRowActions(
  p: Pick<Period, 'status'>,
  mayManage: boolean,
  ask: (action: PeriodAction) => void,
): RowAction[] {
  if (!mayManage) {
    return [];
  }
  return PERIOD_ACTIONS[p.status].map((a) => ({
    label: a.label,
    danger: a.action === 'close',
    onSelect: () => ask(a.action),
  }));
}
