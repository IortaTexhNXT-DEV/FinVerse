import { describe, expect, it } from 'vitest';
import { percent } from './migrationCodes';

describe('migration percent', () => {
  it('shows a rate with two decimals at most', () => {
    expect(percent(33.3333)).toBe('33.33%');
    expect(percent(0)).toBe('0%');
    expect(percent(12.5)).toBe('12.5%');
    expect(percent(undefined)).toBe('');
  });
});
