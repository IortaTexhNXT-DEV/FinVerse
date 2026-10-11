import type { TrueUp } from '@/api/migrationGl';
import { migrationGlApi } from '@/api/migrationGl';
import type { MigAction } from '../common/ActionConfirm';

/** The actions a user may take on a true-up in its current status. */
export function trueUpActions(t: TrueUp, can: (p: string) => boolean): MigAction[] {
  const prepare = can('MIG_TRUEUP_PREPARE');
  const approve = can('MIG_TRUEUP_APPROVE');
  const record = t.reference;
  const actions: MigAction[] = [];
  if (t.status === 'PREPARED' && prepare) {
    actions.push({
      title: 'Submit for Approval',
      record,
      effect: 'The Head of Comptrollership approves or returns the adjustment.',
      confirmLabel: 'Submit',
      reason: 'optional',
      done: 'Adjustment submitted',
      run: (note) => migrationGlApi.submit(t.reference, note),
    });
  }
  if (t.status === 'FOR_APPROVAL' && approve) {
    actions.push(
      {
        title: 'Approve Adjustment',
        record,
        effect:
          'The adjustment batch may then be loaded; its journals post into the opening period.',
        confirmLabel: 'Approve',
        reason: 'optional',
        done: 'Adjustment approved',
        run: (note) => migrationGlApi.decide(t.reference, true, note),
      },
      {
        title: 'Return Adjustment',
        record,
        effect: 'The adjustment goes back to the preparer.',
        confirmLabel: 'Return',
        reason: 'required',
        destructive: true,
        done: 'Adjustment returned',
        run: (note) => migrationGlApi.decide(t.reference, false, note),
      },
    );
  }
  if ((t.status === 'POSTED' || t.status === 'RECONCILED') && (prepare || approve)) {
    actions.push({
      title: 'Reconcile Adjustment',
      record,
      effect:
        'Compares the adjustment lines with the legacy trial balances and checks that Migration Clearing is zero.',
      confirmLabel: 'Reconcile',
      done: 'Reconciliation run',
      run: () => migrationGlApi.reconcile(t.reference),
    });
  }
  if (t.status === 'RECONCILED' && approve) {
    actions.push({
      title: 'Sign Adjustment',
      record,
      effect: 'Signs the reconciled adjustment as the Head of Comptrollership.',
      confirmLabel: 'Sign',
      done: 'Adjustment signed',
      run: () => migrationGlApi.sign(t.reference),
    });
  }
  return actions;
}
