import type { ValueAttributes } from './api';
import {
  describeSettlement,
  describeStatus,
  settlementForm,
  statusForm,
  validateSettlementForm,
  validateStatusForm,
} from './setupLogic';

const value = (attributes: Record<string, string>, pending: Record<string, string | null> = {}) =>
  ({ code: 'X', label: 'X', valueStatus: 'ACTIVE', attributes, pending }) as ValueAttributes;

describe('Claims Setup logic', () => {
  it('fills the forms from the attributes, the pending proposal first', () => {
    expect(
      statusForm(value({ phase: 'IN_PROGRESS', waiting_on: 'INSURER' }, { follow_up_days: '5' })),
    ).toEqual({
      phase: 'IN_PROGRESS',
      waitingOn: 'INSURER',
      followUpDays: '5',
      awaitingPremiumRemittance: false,
    });
    expect(statusForm(value({ phase: 'NEW' }, { follow_up_days: null })).followUpDays).toBe('');
    expect(settlementForm(value({ outcome: 'SETTLED', closes_claim: 'true' }))).toEqual({
      outcome: 'SETTLED',
      closesClaim: true,
      requiresSettlementAmount: false,
    });
  });

  it('validates phase, waiting party, follow-up days and outcome', () => {
    expect(
      validateStatusForm({
        phase: '',
        waitingOn: '',
        followUpDays: '366',
        awaitingPremiumRemittance: false,
      }),
    ).toEqual({
      phase: 'Set the phase of the status',
      waitingOn: 'Select the party the claim waits on',
      followUpDays: 'Enter a whole number of days',
    });
    expect(
      validateStatusForm({
        phase: 'NEW',
        waitingOn: 'BDOI',
        followUpDays: '2.5',
        awaitingPremiumRemittance: false,
      }),
    ).toHaveProperty('followUpDays');
    expect(
      validateStatusForm({
        phase: 'NEW',
        waitingOn: 'BDOI',
        followUpDays: '365',
        awaitingPremiumRemittance: false,
      }),
    ).toEqual({});
    expect(
      validateSettlementForm({ outcome: '', closesClaim: true, requiresSettlementAmount: true }),
    ).toEqual({
      outcome: 'Set the outcome of the settlement type',
    });
  });

  it('describes the attributes', () => {
    expect(
      describeStatus({
        phase: 'IN_PROGRESS',
        waiting_on: 'BDOI',
        follow_up_days: '5',
        awaiting_premium_remittance: 'true',
      }),
    ).toBe('IN_PROGRESS · waits on BDOI · 5 days · awaiting premium remittance');
    expect(
      describeSettlement({
        outcome: 'SETTLED',
        closes_claim: 'false',
        requires_settlement_amount: 'true',
      }),
    ).toBe('SETTLED · keeps the claim open · amount and date required');
    expect(describeSettlement({})).toBe('');
  });
});
