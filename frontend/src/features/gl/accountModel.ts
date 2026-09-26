import type { GlAccount } from '@/api/gl';
import type { FrbsAccount, FrbsAccountRequest } from './glPlatformApi';
import { today } from '@/utils/format';

/** Empty account for the create form. */
export function blankAccount(companyId: number): FrbsAccountRequest {
  return {
    companyId,
    code: '',
    name: '',
    accountClass: 'ASSET',
    level: 'SUB',
    subLedgerType: 'NONE',
    controlAccount: false,
    allowManualPosting: true,
    costCenterRequired: false,
    businessLineRequired: false,
    revaluationRequired: false,
    reconcilable: false,
    interBranch: false,
    openedOn: today(),
    allowedCurrencies: [],
    allowedBranchIds: [],
    allowedRoleCodes: [],
    negativeBalancePolicy: 'ALLOW',
  };
}

/** Maps an account to its editable request shape. */
export function toRequest(a: GlAccount | FrbsAccount): FrbsAccountRequest {
  return {
    companyId: a.companyId,
    code: a.code,
    name: a.name,
    shortName: a.shortName,
    accountClass: a.accountClass,
    level: a.level,
    parentCode: a.parentCode,
    categoryCode: a.categoryCode,
    controlAccount: a.controlAccount,
    subLedgerType: a.subLedgerType,
    allowManualPosting: a.allowManualPosting,
    costCenterRequired: a.costCenterRequired,
    businessLineRequired: a.businessLineRequired,
    revaluationRequired: a.revaluationRequired,
    reconcilable: a.reconcilable,
    interBranch: a.interBranch,
    contraAccountCode: a.contraAccountCode,
    reportGroup: a.reportGroup,
    openedOn: a.openedOn,
    allowedCurrencies: a.allowedCurrencies,
    allowedBranchIds: a.allowedBranchIds,
    allowedRoleCodes: a.allowedRoleCodes,
    negativeBalancePolicy: 'negativeBalancePolicy' in a ? a.negativeBalancePolicy : 'ALLOW',
  };
}
