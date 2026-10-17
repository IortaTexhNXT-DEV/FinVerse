import { describe, expect, it } from 'vitest';
import { UNAPPLIED_TYPE_LABELS, filtersFromSearch, pnAndLoan } from './unappliedInquiryApi';

describe('Unapplied payments list (FRS.CSH.06.01.03)', () => {
  it('opens the list of the type chosen on the dashboard and ignores an unknown type', () => {
    expect(filtersFromSearch('?type=UNBOOKED').type).toBe('UNBOOKED');
    expect(filtersFromSearch('?type=SOMETHING').type).toBe('');
    expect(Object.keys(UNAPPLIED_TYPE_LABELS)).toHaveLength(6);
  });

  it('shows the PN and the loan application numbers in one column', () => {
    expect(pnAndLoan({ account: { pnNo: 'PN-1', loanApplicationNo: 'LA-9' } })).toBe('PN-1 / LA-9');
    expect(pnAndLoan({ account: {} })).toBe('');
  });
});
