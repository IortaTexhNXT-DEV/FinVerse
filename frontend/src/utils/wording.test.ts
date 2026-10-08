import { workflowNoun } from '@/api/workflow';
import {
  formatMoney,
  formatMoneyInText,
  nounFor,
  pluralOf,
  sentenceCase,
  splitTrailingAmount,
} from './wording';

describe('wording', () => {
  it('puts a noun in the right number', () => {
    expect(nounFor(1, 'Claim')).toBe('Claim');
    expect(nounFor(0, 'Claim')).toBe('Claims');
    expect(nounFor(2, 'Service fee run')).toBe('Service fee runs');
    expect(nounFor(3, 'Entry', 'Entries')).toBe('Entries');
    expect(pluralOf('Policy')).toBe('Policies');
    expect(pluralOf('Process')).toBe('Processes');
    expect(pluralOf('Account funding')).toBe('Account funding');
    expect(pluralOf('CWT certificate (2307)')).toBe('CWT certificates (2307)');
    expect(pluralOf('EB SOA')).toBe('EB SOA');
  });

  it('writes Title Case labels in sentence case with acronyms kept', () => {
    expect(sentenceCase('Service Fee Run')).toBe('Service fee run');
    expect(sentenceCase('EB Statement of Account')).toBe('EB statement of account');
    expect(sentenceCase('ACSL Correction Entry')).toBe('ACSL correction entry');
  });

  it('names workflow items in the right number, never by code', () => {
    expect(workflowNoun('BCL_CLAIM', 1)).toBe('Claim');
    expect(workflowNoun('BCL_CLAIM', 2)).toBe('Claims');
    expect(workflowNoun('FRBS_SERVICE_FEE', 1)).toBe('Service fee run');
    expect(workflowNoun('FRBS_SERVICE_FEE', 5)).toBe('Service fee runs');
    expect(workflowNoun('DISB_FUNDING', 1)).toBe('Account funding');
    expect(workflowNoun('OPS_DP_BILLING', 2)).toBe('Direct payment billing');
  });

  it('formats amounts written in texts with thousands separators and two decimals', () => {
    expect(formatMoney('PHP', 2500)).toBe('PHP 2,500.00');
    expect(formatMoney(undefined, '1234.5')).toBe('1,234.50');
    expect(formatMoneyInText('Funding PHP 2500.00: payroll')).toBe('Funding PHP 2,500.00: payroll');
    expect(formatMoneyInText('Refund USD 1,200')).toBe('Refund USD 1,200.00');
    // A form number is not an amount.
    expect(formatMoneyInText('BIR 2307 for June')).toBe('BIR 2307 for June');
  });

  it('splits the amount off the end of a description', () => {
    expect(splitTrailingAmount('Maria Clara Santos PHP 500.00')).toEqual({
      text: 'Maria Clara Santos',
      currency: 'PHP',
      amount: 500,
    });
    expect(splitTrailingAmount('Mega Traders Inc. – PHP 2500.00')).toEqual({
      text: 'Mega Traders Inc.',
      currency: 'PHP',
      amount: 2500,
    });
    expect(splitTrailingAmount('Bayside Builders Co.')).toEqual({ text: 'Bayside Builders Co.' });
    expect(splitTrailingAmount('Funding PHP 500.00: payroll')).toEqual({
      text: 'Funding PHP 500.00: payroll',
    });
    expect(splitTrailingAmount('PHP 500.00')).toEqual({ text: 'PHP 500.00' });
  });
});
