/** The class, tier and sub-ledger of an account read in words in the form and the list, never as codes. */
import { describe, expect, it } from 'vitest';
import form from './AccountForm.tsx?raw';
import list from './ChartOfAccountsPage.tsx?raw';

describe('account texts', () => {
  it('offers the class, tier and sub-ledger in words', () => {
    expect(form).not.toMatch(/<option key=\{[cls]\}>\{[cls]\}<\/option>/);
    expect(form.match(/accountWord\(/g)?.length).toBe(3);
    expect(list).toContain('accountWord(a.level)');
  });
});
