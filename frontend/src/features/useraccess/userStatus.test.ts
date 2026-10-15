import { describe, expect, it } from 'vitest';
import type { UserProfile } from '@/api/types';
import { NO_USER_FILTER, matchesUser, statusOf } from './userStatus';
import { csvCell, toCsv } from './csv';

const user = (over: Partial<UserProfile> = {}): UserProfile => ({
  id: 1,
  username: 'a100000001',
  fullName: 'Maria Santos',
  windowsId: 'BDO\\msantos',
  enabled: true,
  locked: false,
  roles: ['MKT_AO'],
  permissions: [],
  businessUnitCode: 'MKT',
  ...over,
});

describe('user status and filters', () => {
  it('reads inactive before locked', () => {
    expect(statusOf(user())).toBe('ACTIVE');
    expect(statusOf(user({ locked: true }))).toBe('LOCKED');
    expect(statusOf(user({ enabled: false, locked: true }))).toBe('INACTIVE');
  });

  it('matches the search text and every filter', () => {
    expect(matchesUser(user(), { ...NO_USER_FILTER, text: 'msantos' })).toBe(true);
    expect(matchesUser(user(), { ...NO_USER_FILTER, text: 'reyes' })).toBe(false);
    expect(matchesUser(user(), { ...NO_USER_FILTER, profile: 'MKT_AO' })).toBe(true);
    expect(matchesUser(user(), { ...NO_USER_FILTER, profile: 'SYSADMIN' })).toBe(false);
    expect(matchesUser(user(), { ...NO_USER_FILTER, businessUnit: 'FIN' })).toBe(false);
    expect(matchesUser(user({ locked: true }), { ...NO_USER_FILTER, status: 'LOCKED' })).toBe(true);
  });
});

describe('csv', () => {
  it('quotes and neutralises cells', () => {
    expect(csvCell('a,b')).toBe('"a,b"');
    expect(csvCell('say "hi"')).toBe('"say ""hi"""');
    expect(csvCell('=SUM(A1)')).toBe("'=SUM(A1)");
    expect(csvCell(undefined)).toBe('');
    expect(toCsv(['A', 'B'], [[1, 'x']])).toBe('A,B\r\n1,x');
  });
});
