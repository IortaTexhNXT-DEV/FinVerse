import type { RowAction } from '@/components/ui/RowActions';
import type { Bank, CheckBook, PayeeAccount, PayeeRequest } from './types';

/**
 * The row action menus of the Disbursement lists (screen standard: the actions on a record of a
 * list are one menu at the end of the row, never buttons in the row; a destructive action comes
 * last, in red, and asks for confirmation).
 */

/** Where a payee request comes from, in words. */
export function payeeRequestSource(source: PayeeRequest['source']): string {
  const labels: Record<PayeeRequest['source'], string> = {
    RRF: 'Refund request',
    DISBURSEMENT: 'Disbursement',
    NO_MATCH: 'No match',
  };
  return labels[source];
}

/** Create Payee from the request, or Close it without a payee (last, in red, confirmed). */
export function payeeRequestActions(
  request: Pick<PayeeRequest, 'status'>,
  mayMaintain: boolean,
  on: { create: () => void; close: () => unknown },
): RowAction[] {
  if (!mayMaintain || request.status !== 'OPEN') {
    return [];
  }
  return [
    { label: 'Create Payee', onSelect: on.create },
    {
      label: 'Close',
      danger: true,
      confirm: {
        title: 'Close Payee Request',
        effect: 'The request is closed without creating a payee.',
        destructive: true,
      },
      onSelect: on.close,
    },
  ];
}

/** Deactivate an active bank account of a payee (red, confirmed). */
export function payeeAccountActions(
  account: Pick<PayeeAccount, 'active'>,
  mayMaintain: boolean,
  on: { deactivate: () => unknown },
): RowAction[] {
  if (!mayMaintain || !account.active) {
    return [];
  }
  return [
    {
      label: 'Deactivate',
      danger: true,
      confirm: {
        title: 'Deactivate Bank Account',
        effect: "The payee's bank account can no longer be used for payments.",
        destructive: true,
      },
      onSelect: on.deactivate,
    },
  ];
}

/**
 * The actions of a BDOIR bank account: Add Series and the status change for the team leader
 * (Deactivate last, in red), Authorise for the approver while a change waits.
 */
export function bankActions(
  bank: Pick<Bank, 'code' | 'status' | 'requestedStatus' | 'recordStatus'>,
  may: { review: boolean; approve: boolean },
  on: { addSeries: () => void; changeStatus: () => unknown; authorize: () => unknown },
): RowAction[] {
  const actions: RowAction[] = [];
  if (may.review) {
    actions.push({ label: 'Add Series', onSelect: on.addSeries });
  }
  if (may.approve && bank.recordStatus === 'PENDING_AUTHORIZATION') {
    actions.push({
      label: 'Authorise',
      confirm: { title: 'Authorise Bank Account', effect: 'The bank account change takes effect.' },
      onSelect: on.authorize,
    });
  }
  if (may.review && bank.requestedStatus === undefined) {
    const deactivate = bank.status === 'ACTIVE';
    actions.push({
      label: deactivate ? 'Deactivate' : 'Activate',
      danger: deactivate,
      confirm: {
        title: `${deactivate ? 'Deactivate' : 'Activate'} Bank ${bank.code}`,
        effect: 'The status change is sent for authorisation.',
        destructive: deactivate,
      },
      onSelect: on.changeStatus,
    });
  }
  return actions;
}

/** Edit a check series, only before its first check is printed. */
export function seriesActions(
  book: Pick<CheckBook, 'status' | 'nextNo' | 'firstNo'>,
  canEdit: boolean,
  on: { edit: () => void },
): RowAction[] {
  if (!canEdit || book.status !== 'ACTIVE' || book.nextNo !== book.firstNo) {
    return [];
  }
  return [{ label: 'Edit', onSelect: on.edit }];
}
