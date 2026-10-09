import { detailsErrors, detailsOf, emptyDetails, toDetails } from './requestDetails';

describe('Annex E fields of a package request', () => {
  it('sends nothing when every field is blank', () => {
    expect(toDetails(emptyDetails())).toBeUndefined();
  });

  it('round-trips the saved fields', () => {
    const saved = {
      businessOrigin: 'INSURER_OFFER',
      estimatedPolicies: 25,
      estimatedPremium: 6_000_000,
      incentiveEligible: true,
      incentiveRate: 2.5,
    };
    const form = detailsOf(saved);
    expect(form.estimatedPolicies).toBe('25');
    expect(toDetails(form)).toEqual(saved);
  });

  it("checks BDOI's least policies and premium and the incentive", () => {
    const form = {
      ...emptyDetails(),
      estimatedPolicies: '10',
      estimatedPremium: '4000000',
      incentiveEligible: true,
    };
    expect(detailsErrors(form)).toEqual({
      estimatedPolicies: 'Enter at least 20 policies',
      estimatedPremium: 'Enter at least 5,000,000.00',
      incentiveAmount: 'Enter the incentive amount or the incentive commission rate',
    });
    expect(detailsErrors({ ...emptyDetails(), incentiveRate: '120' })).toEqual({
      incentiveRate: 'The incentive commission rate must be above 0% and at most 100%',
    });
  });
});
