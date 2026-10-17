import { describe, expect, it } from 'vitest';
import { isAnyBand } from '../common/presentation';

describe('the sum insured band of a package mapping', () => {
  it('reads Any when no bound is given, whether missing or empty', () => {
    expect(isAnyBand(undefined, undefined)).toBe(true);
    expect(isAnyBand(null, null)).toBe(true);
    expect(isAnyBand(100000, undefined)).toBe(false);
    expect(isAnyBand(null, 500000)).toBe(false);
  });
});
