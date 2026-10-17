import { expiryPhase, minutesLeft } from './sessionExpiry';

const END = '2026-09-24T18:00:00Z';
const at = (iso: string) => Date.parse(iso);

describe('absolute session end', () => {
  it('warns the configured minutes before the end and expires at the end', () => {
    expect(expiryPhase(END, at('2026-09-24T17:00:00Z'), 30)).toBe('none');
    expect(expiryPhase(END, at('2026-09-24T17:30:00Z'), 30)).toBe('warning');
    expect(expiryPhase(END, at('2026-09-24T17:59:59Z'), 30)).toBe('warning');
    expect(expiryPhase(END, at('2026-09-24T18:00:00Z'), 30)).toBe('expired');
  });

  it('ignores a missing or unreadable expiry', () => {
    expect(expiryPhase(null, 0, 30)).toBe('none');
    expect(expiryPhase(undefined, 0, 30)).toBe('none');
    expect(expiryPhase('soon', 0, 30)).toBe('none');
  });

  it('counts the minutes left', () => {
    expect(minutesLeft(END, at('2026-09-24T17:30:00Z'))).toBe(30);
    expect(minutesLeft(END, at('2026-09-24T17:59:30Z'))).toBe(1);
    expect(minutesLeft(END, at('2026-09-24T18:10:00Z'))).toBe(1);
  });
});
