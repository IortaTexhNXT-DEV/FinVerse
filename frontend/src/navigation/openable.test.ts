import { describe, expect, it } from 'vitest';
import { canOpenLink, screenOf } from './openable';

const only =
  (...granted: string[]) =>
  (permission: string) =>
    granted.includes(permission);

describe('canOpenLink', () => {
  it('finds no screen for a link no route answers', () => {
    expect(screenOf('/no-such-page/1')).toBeUndefined();
    expect(canOpenLink('/no-such-page/1', () => true)).toBe(false);
  });

  it('opens a TOR link for a TOR approver, also the earlier link with the id', () => {
    expect(canOpenLink('/submitted/tors', only('TOR_APPROVE'))).toBe(true);
    expect(canOpenLink('/submitted/tor/1', only('TOR_APPROVE'))).toBe(true);
  });

  it('refuses a link to a screen the user may not open', () => {
    expect(canOpenLink('/submitted/tors', only('WORK_VIEW'))).toBe(false);
  });
});
