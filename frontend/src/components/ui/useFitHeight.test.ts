import { describe, expect, it } from 'vitest';
import { fittedHeight } from './useFitHeight';

describe('fittedHeight', () => {
  it('leaves a list whose rows fit uncapped', () => {
    expect(fittedHeight({ wrap: 400, table: 400, overflow: -50, min: 240 })).toBeNull();
  });

  it('caps a long list to the room left in the window', () => {
    expect(fittedHeight({ wrap: 2000, table: 2000, overflow: 1500, min: 240 })).toBe(500);
  });

  it('leaves an uncapped list uncapped when the room left is under the least height', () => {
    expect(fittedHeight({ wrap: 2000, table: 2000, overflow: 1900, min: 240 })).toBeNull();
  });

  it('keeps a capped list at the least height instead of releasing it (no flicker)', () => {
    expect(fittedHeight({ wrap: 270, table: 2000, overflow: 60, min: 240 })).toBe(240);
    expect(fittedHeight({ wrap: 240, table: 2000, overflow: 30, min: 240 })).toBe(240);
  });
});
