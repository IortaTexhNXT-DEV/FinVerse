/**
 * The Data Migration and Legacy Inquiry screens take dates with the one date picker of BIBS (dd-MMM-yyyy), never the
 * browser's own date field, which shows the date in the format of the computer (mm/dd/yyyy).
 */
import { describe, expect, it } from 'vitest';

const sources = import.meta.glob<string>(
  [
    '../**/*.tsx',
    '../../legacy-inquiry/*.tsx',
    '!../**/*.test.tsx',
    '!../../legacy-inquiry/*.test.tsx',
  ],
  {
    query: '?raw',
    import: 'default',
    eager: true,
  },
);

describe('migration dates', () => {
  it('uses the dd-MMM-yyyy date picker on every migration and legacy inquiry screen', () => {
    const native = Object.entries(sources)
      .filter(([, source]) => source.includes('type="date"'))
      .map(([path]) => path);
    expect(Object.keys(sources).length).toBeGreaterThan(10);
    expect(native).toEqual([]);
  });
});
