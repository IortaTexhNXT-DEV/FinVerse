import {
  amountError,
  authorizationBlock,
  claimFormErrors,
  dateErrors,
  emptyClaimForm,
  flagLabels,
  policyYearLabel,
  toAmount,
} from './recordLogic';

const TODAY = '2026-09-26';

function valid() {
  return {
    ...emptyClaimForm(TODAY),
    arn: 'ARN-2026-000501',
    lossDate: '2026-09-25',
    lossNature: 'MOTOR_OWN_DAMAGE',
    claimType: 'MOTOR_OWN_DAMAGE',
    lossDescription: 'Rear-ended',
  };
}

describe('Record Claim rules', () => {
  it('accepts a complete form', () => {
    expect(claimFormErrors(valid(), TODAY)).toEqual({});
  });

  it('asks for the cover, nature, type and description with the FRS messages', () => {
    const errors = claimFormErrors(emptyClaimForm(TODAY), TODAY);
    expect(errors.arn).toBe('Select the cover of the claim');
    expect(errors.lossNature).toBe('Select the nature of loss');
    expect(errors.claimType).toBe('Select the claim type');
    expect(errors.lossDescription).toBe('Enter the loss description');
    expect(errors.lossDate).toBe('Enter a loss date that is not in the future');
  });

  it('checks the dates', () => {
    expect(dateErrors('2026-09-27', TODAY, TODAY).lossDate).toBeDefined();
    expect(dateErrors('2026-03-10', '2026-03-09', TODAY).reportedDate).toBe(
      'The reported date must be between the loss date and today',
    );
    expect(dateErrors('2026-03-10', '2026-09-27', TODAY).reportedDate).toBeDefined();
    expect(dateErrors('2026-03-10', '2026-03-10', TODAY)).toEqual({});
  });

  it('needs a code for an event name and positive amounts', () => {
    const errors = claimFormErrors(
      { ...valid(), catastropheEvent: 'Typhoon Kristine', claimAmount: '-1' },
      TODAY,
    );
    expect(errors.catastropheEvent).toBe('Select the catastrophe code of the event');
    expect(errors.claimAmount).toBe('Enter an amount of 0 or more');
    expect(toAmount('1,250.50')).toBe(1250.5);
    expect(toAmount(' ')).toBeUndefined();
    expect(amountError('abc')).toBeDefined();
    expect(amountError('0')).toBeUndefined();
  });

  it('needs an insurer claim number on an insurer-reported claim', () => {
    const form = { ...valid(), source: 'INSURER_REPORTED' as const };
    expect(claimFormErrors(form, TODAY).insurerClaimNos).toBeDefined();
    expect(claimFormErrors({ ...form, insurerClaimNos: ['C-1'] }, TODAY)).toEqual({});
  });

  it('allows the authorization code on paid or permitted direct-payment covers only', () => {
    expect(authorizationBlock('PAID', 'CONFIRM', false)).toBeUndefined();
    expect(authorizationBlock('DIRECT_PAYMENT', 'CONFIRM', false)).toBeUndefined();
    expect(authorizationBlock('DIRECT_PAYMENT', 'BLOCK', false)).toBeDefined();
    expect(authorizationBlock('UNPAID', 'ALLOW', false)).toBeDefined();
    expect(authorizationBlock('PAID', 'CONFIRM', true)).toBeDefined();
  });

  it('lists the summary flags in order', () => {
    expect(
      flagLabels({
        unpaidPremium: true,
        awaitingPremiumRemittance: false,
        newerCoverVersion: true,
        multiLocation: true,
        multiInsurer: false,
        catastrophe: true,
        claimantOverridden: false,
      }),
    ).toEqual(['Unpaid premium', 'Newer cover version', 'Multi-location', 'CAT']);
    expect(policyYearLabel(1)).toBe('Year 1');
    expect(policyYearLabel(2, '2027-01-01', '2028-01-01')).toBe(
      'Year 2 (2027-01-01 to 2028-01-01)',
    );
  });
});
