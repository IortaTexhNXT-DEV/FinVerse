import { describe, expect, it } from 'vitest';
import {
  SCHEDULE_NOTE,
  dayOfYear,
  missingRateNotice,
  openItemDocument,
  periodOption,
  yearOption,
} from './closeTexts';

describe('closing texts', () => {
  it('shows the status of a year or a period in words', () => {
    expect(periodOption('2026-09', 'OPEN')).not.toContain('OPEN');
    expect(periodOption('2026-09', 'OPEN')).toMatch(/^2026-09 · \w/);
    expect(yearOption(2026, 'OPEN')).not.toContain('OPEN');
  });

  it('names the side of an open item and the missing rate in words', () => {
    expect(openItemDocument('CS-HO-2026-000010', 'CREDIT')).toBe('CS-HO-2026-000010 (Credit)');
    expect(missingRateNotice(['USD'])).toBe('No month-end rate for USD: posting is blocked.');
  });
});

describe('year-end deadline', () => {
  it('reads as dd-MMM', () => {
    expect(dayOfYear('04-15')).toBe('15-Apr');
    expect(dayOfYear(undefined)).toBe('—');
  });
});

describe('scheduled close note', () => {
  it('names the alert in words, not by its code', () => {
    expect(SCHEDULE_NOTE).not.toMatch(/[A-Z]_[A-Z]/);
  });
});
