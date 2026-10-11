import type { LegacyBatch, LegacyBatchApi } from '@/api/legacyBatches';
import { cashBatchApi, dpprBatchApi } from '@/api/legacyBatches';

/** What a legacy batch screen shows and which permissions act on it. */
export interface BatchScreen {
  id: 'income' | 'pr2307' | 'dppr';
  section: string;
  title: string;
  description: string;
  api: LegacyBatchApi;
  request: string;
  approve: string;
  finalApprove?: string;
  detailBase: string;
  listPath: string;
  askCurrency: boolean;
  approvalFlow: string;
}

export const INCOME_SCREEN: BatchScreen = {
  id: 'income',
  section: 'Cashiering',
  title: 'Unapplied to Income',
  description:
    'Old unapplied payments, new and legacy, reclassified to other income - unclaimed collections.',
  api: cashBatchApi('INCOME_RECLASS'),
  request: 'CASH_UPP_INCOME_REQUEST',
  approve: 'CASH_DISPOSITION_APPROVE',
  finalApprove: 'CASH_UPP_INCOME_APPROVE',
  detailBase: '/cashiering/legacy-batches',
  listPath: '/cashiering/unapplied-income',
  askCurrency: true,
  approvalFlow:
    'The Cashiering team lead approves first, then top management; the lines post after the second approval.',
};

export const PR2307_SCREEN: BatchScreen = {
  id: 'pr2307',
  section: 'Cashiering',
  title: 'Legacy PR 2307 Reversal',
  description: 'PR 2307 balances of legacy invoices settled against the amount due to the insurer.',
  api: cashBatchApi('PR2307_REVERSAL'),
  request: 'LEGACY_REVERSAL_REQUEST',
  approve: 'LEGACY_REVERSAL_APPROVE',
  detailBase: '/cashiering/legacy-batches',
  listPath: '/cashiering/legacy-pr2307',
  askCurrency: true,
  approvalFlow:
    'The Cashiering team lead approves; any premium receivable needed is first moved to PR 2307.',
};

export const DPPR_SCREEN: BatchScreen = {
  id: 'dppr',
  section: 'Commission',
  title: 'DP PR Legacy Reversal',
  description:
    'Legacy invoices paid directly to the insurer: their open premium receivable and due to insurer are reversed.',
  api: dpprBatchApi,
  request: 'LEGACY_REVERSAL_REQUEST',
  approve: 'LEGACY_REVERSAL_APPROVE',
  detailBase: '/commission/dppr-batches',
  listPath: '/commission/dppr-batches',
  askCurrency: false,
  approvalFlow: 'The Commission team lead approves; each invoice then posts on its own.',
};

/** The screen of a Cashiering batch, from its kind. */
export function cashScreenOf(batch: LegacyBatch | undefined): BatchScreen {
  return batch?.kind === 'PR2307_REVERSAL' ? PR2307_SCREEN : INCOME_SCREEN;
}

/** The permission that approves a batch in its current status, undefined when none does. */
export function approverPermission(screen: BatchScreen, batch: LegacyBatch): string | undefined {
  if (batch.status === 'FOR_TOP_MANAGEMENT') {
    return screen.finalApprove;
  }
  return batch.status === 'FOR_APPROVAL' ? screen.approve : undefined;
}

/** Who approved a batch, in order. */
export function approvers(
  batch: LegacyBatch,
  name: (login: string) => string = (login) => login,
): string {
  return [batch.firstApprovedBy, batch.finalApprovedBy ?? batch.approvedBy]
    .filter((v): v is string => Boolean(v))
    .map(name)
    .join(' / ');
}
