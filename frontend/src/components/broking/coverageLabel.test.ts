import { describe, expect, it } from 'vitest';
import { coverageLabel } from './useLabels';

describe('coverage label', () => {
  it('names a coverage of the line by its catalog name', () => {
    expect(
      coverageLabel([{ code: 'EARTHQUAKE', name: 'Earthquake Fire and Shock' }], 'EARTHQUAKE'),
    ).toBe('Earthquake Fire and Shock');
  });

  it('reads a coverage outside the catalog of the line as words, never as its code', () => {
    expect(coverageLabel([], 'RIOT_STRIKE')).toBe('Riot Strike');
    expect(coverageLabel(undefined, 'RIOT_STRIKE')).toBe('Riot Strike');
  });
});
