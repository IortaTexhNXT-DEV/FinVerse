import { describe, expect, it } from 'vitest';
import { purposeLabel } from './messagePurpose';

describe('purposeLabel', () => {
  it('names what an e-mail was sent for, never the purpose code in words', () => {
    expect(purposeLabel('CLX_SOA')).toBe('Statement of Account');
    expect(purposeLabel('PASSWORD')).toBe('Password');
    expect(purposeLabel('CLX_SOA')).not.toMatch(/Clx/);
    expect(purposeLabel('QUOTATION')).toBe('Quotation');
  });
});
