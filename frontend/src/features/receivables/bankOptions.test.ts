import { describe, expect, it } from 'vitest';
import { bankOptions } from './useReceivablesLookups';

describe('bank account options', () => {
  it('names the currency once', () => {
    const [current, savings] = bankOptions([
      { code: '1111', name: 'Cash in Bank - BDO Current Account (PHP)', currency: 'PHP' },
      { code: '1112', name: 'Cash in Bank - BPI Savings', currency: 'PHP' },
    ]);
    expect(current?.label).toBe('1111 - Cash in Bank - BDO Current Account (PHP)');
    expect(savings?.label).toBe('1112 - Cash in Bank - BPI Savings (PHP)');
  });
});

describe('reconciliation statement title', () => {
  it('names the bank account with the shared label', async () => {
    const view = (await import('./BrsView.tsx?raw')).default;
    expect(view).toContain('bankOptions([data.bank])');
  });
});
