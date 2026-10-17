import type { ClaimProgress } from './api';
import { progressFlags, statusActions, validateFollowUp, validateSettlement } from './statusLogic';

const TL = new Set([
  'BCL_STATUS_UPDATE',
  'BCL_SETTLEMENT_UPDATE',
  'BCL_FOLLOW_UP_OVERRIDE',
  'BCL_ADJUSTER_ASSIGN',
  'BCL_ACTION_PLAN',
]);

describe('claim status logic', () => {
  it('offers the actions of the rights on an open claim and only Reopen when closed', () => {
    expect(statusActions('IN_PROGRESS', (p) => TL.has(p))).toEqual([
      'changeStatus',
      'settle',
      'followUp',
      'adjuster',
      'actionPlan',
    ]);
    expect(statusActions('NEW', (p) => p === 'BCL_STATUS_UPDATE')).toEqual(['changeStatus']);
    expect(statusActions('CLOSED', (p) => TL.has(p))).toEqual([]);
    expect(statusActions('CLOSED', (p) => p === 'BCL_REOPEN')).toEqual(['reopen']);
  });

  it('flags remittance, override and temporary closure', () => {
    const progress = {
      status: { awaitingPremiumRemittance: true, closureKind: 'TEMPORARY' },
      followUp: { overridden: true },
    } as unknown as ClaimProgress;
    expect(progressFlags(progress)).toEqual([
      'Awaiting premium remittance',
      'Follow-up overridden',
      'Temporary closure',
    ]);
  });

  it('validates the settlement and the follow-up override', () => {
    expect(
      validateSettlement(
        { typeCode: '', amount: '-1', dateSettled: '2026-10-01', remark: '' },
        '2026-09-26',
      ),
    ).toEqual({
      typeCode: 'Select the type of settlement',
      amount: 'Enter an amount of zero or more',
      dateSettled: 'The date settled cannot be in the future',
    });
    expect(
      validateSettlement(
        { typeCode: 'SETTLED', amount: '85000', dateSettled: '2026-09-25', remark: '' },
        '2026-09-26',
      ),
    ).toEqual({});
    expect(validateFollowUp('2026-09-25', '', '2026-09-26')).toEqual({
      date: 'The follow-up date cannot be before today',
      reasonCode: 'Enter the reason for the change',
    });
    expect(validateFollowUp('', 'OTHERS', '2026-09-26')).toEqual({
      date: 'Enter the next follow-up date',
    });
    expect(validateFollowUp('2026-09-26', 'OTHERS', '2026-09-26')).toEqual({});
  });
});
