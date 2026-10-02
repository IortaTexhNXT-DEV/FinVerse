import { describe, expect, it } from 'vitest';
import { scopeText } from './ruleScope';

describe('rule scope', () => {
  it('names every product, a line and a product in words', () => {
    const names: Record<string, string> = { PROPERTY: 'Property', MTR12: 'Motor Comprehensive' };
    const nameOf = (code: string) => names[code] ?? '';
    expect(scopeText('ALL', '', nameOf)).toBe('All products');
    expect(scopeText('LINE', 'PROPERTY', nameOf)).toBe('Line Property');
    expect(scopeText('PRODUCT', 'MTR12', nameOf)).toBe('Product Motor Comprehensive');
  });
});
