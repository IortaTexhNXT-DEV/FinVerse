import { businessText } from './businessText';

describe('businessText', () => {
  it.each([
    [
      'Days from request to completion of endorsement requests (ADJID.021; layout to confirm, OQ42)',
      'Days from request to completion of endorsement requests',
    ],
    [
      'Incentive runs ending in the period (CMRID.003/005/006)',
      'Incentive runs ending in the period',
    ],
    [
      'Payments received before booking (CSHID.020, Annex II #14, draft)',
      'Payments received before booking',
    ],
    [
      'Commission ORs with their invoices (CSHID.023 Annex II #2).',
      'Commission ORs with their invoices.',
    ],
    ['Clients due by the date (BRNB.110).', 'Clients due by the date.'],
    ['A match is not raised again (SNSRP-302, 304).', 'A match is not raised again.'],
    ['Open collection items (BRCLXN.001-012, 045)', 'Open collection items'],
    [
      'Versions with the change summary (BRPM.006/007, PMADD06)',
      'Versions with the change summary',
    ],
  ])('removes the references of "%s"', (text, expected) => {
    expect(businessText(text)).toBe(expected);
  });

  it('keeps ordinary parentheses', () => {
    expect(businessText('Journal (draft) for approval')).toBe('Journal (draft) for approval');
    expect(businessText('Write-off (debit) or credit (overpayment)')).toBe(
      'Write-off (debit) or credit (overpayment)',
    );
  });
});
