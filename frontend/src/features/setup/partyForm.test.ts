import {
  emptyParty,
  hasWithholding,
  isIntermediary,
  toPartyInput,
  validateParty,
} from './partyForm';

describe('party form', () => {
  it('starts a corporate client in the base currency', () => {
    expect(emptyParty(5, 'USD')).toEqual({
      companyId: 5,
      partyType: 'CORPORATE_CLIENT',
      defaultCurrency: 'USD',
      creditDays: 30,
    });
  });

  it('knows which fields apply to a party type', () => {
    expect(isIntermediary('BROKER')).toBe(true);
    expect(isIntermediary('SUPPLIER')).toBe(false);
    expect(isIntermediary(undefined)).toBe(false);
    expect(hasWithholding('GARAGE')).toBe(true);
    expect(hasWithholding('REINSURER')).toBe(false);
  });

  it('accepts a valid party', () => {
    expect(
      validateParty({
        ...emptyParty(1),
        code: 'S-0009',
        name: 'Supplier',
        email: 'ap@supplier.ph',
        withholdingTaxRate: 2,
      }),
    ).toEqual({});
  });

  it('reports field errors', () => {
    const errors = validateParty({
      code: 'bad code',
      name: '',
      defaultCurrency: 'peso',
      creditDays: 400,
      email: 'nobody@',
      commissionRate: 120,
      withholdingTaxRate: -1,
    });
    expect(Object.keys(errors).sort((a, b) => a.localeCompare(b))).toEqual([
      'code',
      'commissionRate',
      'creditDays',
      'defaultCurrency',
      'email',
      'name',
      'withholdingTaxRate',
    ]);
    expect(validateParty({}).code).toBe('Code is required');
  });

  it('builds the API request and drops fields that do not apply', () => {
    const input = toPartyInput({
      ...emptyParty(1),
      partyType: 'SUPPLIER',
      code: ' S-1 ',
      name: ' Office Supplies ',
      taxId: ' ',
      commissionRate: 10,
      withholdingTaxRate: 2,
      licenceNo: 'L-1',
    });
    expect(input.code).toBe('S-1');
    expect(input.name).toBe('Office Supplies');
    expect(input.taxId).toBeUndefined();
    expect(input.commissionRate).toBeUndefined();
    expect(input.licenceNo).toBeUndefined();
    expect(input.withholdingTaxRate).toBe(2);
    const agent = toPartyInput({ partyType: 'AGENT', commissionRate: 15, licenceNo: 'IC-1' });
    expect(agent.commissionRate).toBe(15);
    expect(agent.licenceNo).toBe('IC-1');
    expect(agent.companyId).toBe(0);
  });
});
