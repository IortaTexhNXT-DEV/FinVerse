import { describe, expect, it } from 'vitest';
import type { IdentityEvent } from '@/api/identity';
import {
  EMPTY_ACCOUNT,
  missingAccountFields,
  outcomeStatus,
  SIMULATED_EVENTS,
} from './identitySync';

const event = (status: IdentityEvent['status']): IdentityEvent => ({
  id: 1,
  receivedAt: '2026-10-09T01:00:00Z',
  source: 'UIDM_ISC',
  sourceLabel: 'UIDM-ISC',
  eventType: 'JOINER',
  status,
  attempts: 1,
  reprocessable: status === 'REFUSED' || status === 'FAILED',
});

describe('identity synchronisation helpers', () => {
  it('shows a refused or failed event as needing review', () => {
    expect(outcomeStatus(event('APPLIED'))).toBe('COMPLETED');
    expect(outcomeStatus(event('NO_CHANGE'))).toBe('CLOSED');
    expect(outcomeStatus(event('REFUSED'))).toBe('REJECTED');
    expect(outcomeStatus(event('FAILED'))).toBe('FAILED');
  });

  it('names every field still needed for a simulator account', () => {
    expect(missingAccountFields(EMPTY_ACCOUNT)).toEqual([
      'Windows ID',
      'User ID',
      'E-mail',
      'First name',
      'Last name',
    ]);
    expect(
      missingAccountFields({
        ...EMPTY_ACCOUNT,
        windowsId: 'DOMAIN\\bsantos',
        userId: 'a013000301',
        email: 'b@x.ph',
        firstName: 'Bea',
        lastName: 'Santos',
      }),
    ).toEqual([]);
  });

  it('offers the joiner, mover, leaver, rehire and status events', () => {
    expect(SIMULATED_EVENTS.map((e) => e.type)).toEqual([
      'JOINER',
      'MOVER',
      'LEAVER',
      'REHIRE',
      'STATUS',
    ]);
  });
});
