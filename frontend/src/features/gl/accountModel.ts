import type { GlAccount, GlAccountRequest } from '@/api/gl';
import { today } from '@/utils/format';

/** Empty account for the create form. */
export function blankAccount(companyId: number): GlAccountRequest {
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
  };
}

/** Maps an account to its editable request shape. */
export function toRequest(a: GlAccount): GlAccountRequest {
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
  };
}
