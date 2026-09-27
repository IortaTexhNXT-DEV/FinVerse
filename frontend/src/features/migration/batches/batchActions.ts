import { migrationApi } from '@/api/migration';
import type { Batch } from '@/api/migration';
import type { MigAction } from '../common/ActionConfirm';

/** A button of the batch page. */
export interface BatchButton {
  id: string;
  label: string;
  primary?: boolean;
  action: MigAction;
}

const LOADED = ['LOADED', 'LOADED_WITH_REJECTS', 'RECONCILED'];

function validation(b: Batch, can: (p: string) => boolean): BatchButton[] {
  const out: BatchButton[] = [];
  if (b.status === 'PLANNED' && can('MIG_LOAD_RUN')) {
    out.push({
      id: 'validate',
      label: 'Validate',
      primary: true,
      action: {
        title: `Validate batch ${b.batchNo}`,
        record: b.batchNo,
        effect:
          'The rows are mapped with the approved code maps and checked against the data-quality rules.',
        confirmLabel: 'Validate',
        done: 'Batch validated',
        run: () => migrationApi.validate(b.batchNo),
      },
    });
  }
  if (b.status === 'VALIDATED' && can('MIG_DQ_RESOLVE')) {
    out.push({
      id: 'g3',
      label: 'Sign Validation',
      action: {
        title: `Sign the validation of ${b.batchNo}`,
        record: b.batchNo,
        effect:
          'Gate G3: the error rate is within the limit, no mandatory code is unmapped and no client pair waits for review.',
        confirmLabel: 'Sign',
        reason: 'optional',
        done: 'Validation signed',
        run: (comment) => migrationApi.signValidation(b.batchNo, { approve: true, comment }),
      },
    });
  }
  return out;
}

function load(b: Batch, can: (p: string) => boolean, user: string | undefined): BatchButton[] {
  const out: BatchButton[] = [];
  if (b.status === 'VALIDATED' && can('MIG_LOAD_APPROVE') && b.validatedBy !== user) {
    out.push({
      id: 'g4',
      label: 'Approve Load',
      primary: true,
      action: {
        title: `Approve the load of ${b.batchNo}`,
        record: b.batchNo,
        effect: 'Gate G4: the batch may be loaded into BIBS by an operator other than you.',
        confirmLabel: 'Approve Load',
        reason: 'optional',
        done: 'Load approved',
        run: (comment) => migrationApi.approveLoad(b.batchNo, comment),
      },
    });
  }
  if (b.status === 'APPROVED' && can('MIG_LOAD_RUN') && b.loadApprovedBy !== user) {
    out.push({
      id: 'load',
      label: 'Load',
      primary: true,
      action: {
        title: `Load batch ${b.batchNo}`,
        record: b.batchNo,
        effect:
          'The valid rows are loaded through the BIBS services, without notifications or outbound events, and the batch is reconciled.',
        confirmLabel: 'Load',
        done: 'Batch loaded',
        run: () => migrationApi.load(b.batchNo),
      },
    });
  }
  if (LOADED.includes(b.status) && can('MIG_LOAD_RUN')) {
    out.push({
      id: 'reconcile',
      label: 'Reconcile',
      action: {
        title: `Reconcile batch ${b.batchNo}`,
        record: b.batchNo,
        effect:
          'Counts, amounts, hash totals and fields are compared again between source, staging and BIBS.',
        confirmLabel: 'Reconcile',
        done: 'Batch reconciled',
        run: () => migrationApi.reconcile(b.batchNo),
      },
    });
    if (b.counts.rejected > 0) {
      out.push({
        id: 'rerun',
        label: 'Rerun Rejects',
        action: {
          title: `Rerun the rejected rows of ${b.batchNo}`,
          record: b.batchNo,
          effect: 'A new batch is planned with the rejected rows for validation and load.',
          confirmLabel: 'Rerun',
          done: 'Rerun batch planned',
          run: () => migrationApi.rerun(b.batchNo),
        },
      });
    }
  }
  return out;
}

function gates(b: Batch, can: (p: string) => boolean): BatchButton[] {
  const out: BatchButton[] = [];
  if (LOADED.includes(b.status) && can('MIG_RECON_SIGNOFF')) {
    out.push({
      id: 'g5',
      label: 'Sign Reconciliation',
      primary: true,
      action: {
        title: `Sign the reconciliation of ${b.batchNo}`,
        record: b.batchNo,
        effect: 'Gate G5: every break is explained and approved.',
        confirmLabel: 'Sign',
        reason: 'optional',
        done: 'Reconciliation signed',
        run: (comment) => migrationApi.signReconciliation(b.batchNo, { approve: true, comment }),
      },
    });
  }
  if (b.status === 'RECONCILED' && can('MIG_SIGNOFF')) {
    for (const role of [
      { code: 'DATA_OWNER', label: 'Accept as Data Owner' },
      { code: 'DATA_MIGRATION_LEAD', label: 'Accept as Migration Lead' },
    ]) {
      out.push({
        id: `g6-${role.code}`,
        label: role.label,
        action: {
          title: `Accept object ${b.objectCode} (batch ${b.batchNo})`,
          record: b.batchNo,
          effect:
            'Gate G6: with the acceptance of the data owner and of the Data Migration Lead the object is accepted and its staging data is purged after the retention days.',
          confirmLabel: 'Accept',
          reason: 'optional',
          done: 'Acceptance signed',
          run: (comment) =>
            migrationApi.signAcceptance(b.batchNo, { approve: true, role: role.code, comment }),
        },
      });
    }
  }
  return out;
}

function rollback(b: Batch, can: (p: string) => boolean, user: string | undefined): BatchButton[] {
  const out: BatchButton[] = [];
  if ([...LOADED, 'SIGNED_OFF', 'FAILED'].includes(b.status) && can('MIG_ROLLBACK_REQUEST')) {
    out.push({
      id: 'rollback',
      label: 'Request Rollback',
      action: {
        title: `Request the rollback of ${b.batchNo}`,
        record: b.batchNo,
        effect:
          'After approval, the records loaded by this batch are undone through the BIBS services.',
        confirmLabel: 'Request Rollback',
        reason: 'required',
        destructive: true,
        done: 'Rollback requested',
        run: (reason) => migrationApi.requestRollback(b.batchNo, reason),
      },
    });
  }
  if (
    b.status === 'ROLLBACK_REQUESTED' &&
    can('MIG_ROLLBACK_APPROVE') &&
    b.rollbackRequestedBy !== user
  ) {
    out.push(
      {
        id: 'rollback-approve',
        label: 'Approve Rollback',
        primary: true,
        action: {
          title: `Approve the rollback of ${b.batchNo}`,
          record: b.batchNo,
          effect: 'The records loaded by this batch are undone now.',
          confirmLabel: 'Roll Back',
          reason: 'optional',
          destructive: true,
          done: 'Batch rolled back',
          run: (comment) => migrationApi.approveRollback(b.batchNo, comment),
        },
      },
      {
        id: 'rollback-reject',
        label: 'Reject Rollback',
        action: {
          title: `Reject the rollback of ${b.batchNo}`,
          record: b.batchNo,
          effect: 'The batch stays loaded.',
          confirmLabel: 'Reject',
          reason: 'required',
          done: 'Rollback rejected',
          run: (reason) => migrationApi.rejectRollback(b.batchNo, reason),
        },
      },
    );
  }
  return out;
}

/**
 * The actions a user may take on a batch in its status: validate, sign the validation (G3), approve
 * the load (G4, not the validator), load (not the approver), reconcile, rerun the rejected rows,
 * sign the reconciliation (G5), accept (G6) and the rollback request and decision (not the
 * requester).
 */
export function batchActions(
  b: Batch,
  can: (p: string) => boolean,
  user: string | undefined,
): BatchButton[] {
  return [
    ...validation(b, can),
    ...load(b, can, user),
    ...gates(b, can),
    ...rollback(b, can, user),
  ];
}
