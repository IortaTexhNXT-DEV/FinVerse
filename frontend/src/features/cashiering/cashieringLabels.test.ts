import { describe, expect, it } from 'vitest';
import { modeLabel, receiptActionLabel, receiptSourceLabel } from './cashieringLabels';

describe('Cashiering labels', () => {
  it('names modes, sources and receipt transactions in words', () => {
    expect(modeLabel('ADA')).toBe('Auto-debit Arrangement (ADA)');
    expect(modeLabel('NON_CASH')).toBe('Non-cash (settlement)');
    expect(receiptSourceLabel('OTC')).toBe('Over the Counter');
    expect(receiptActionLabel('CANCEL')).toBe('Cancellation');
    expect(modeLabel(undefined)).toBe('');
  });
});
