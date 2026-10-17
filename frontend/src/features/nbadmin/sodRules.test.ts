import { describe, expect, it } from 'vitest';
import type { SodRule } from '@/api/nbadmin';
import { riskFlagLabel } from './riskFlags';
import { EMPTY_SOD_RULE, pendingText, RULE_KINDS, ruleErrors } from './sodRules';

const RULE: SodRule = {
  id: 1,
  ruleCode: 'SOD-000001',
  profileA: 'UAM_REQUESTOR',
  profileAName: 'User Access Requestor',
  profileB: 'UAM_APPROVER',
  profileBName: 'User Access Approver',
  description: 'Requester and approver of the same request',
  status: 'PENDING_AUTHORIZATION',
  pendingAction: 'CREATE',
  maker: 'badmin',
  createdAt: '2026-09-27T01:00:00Z',
};

describe('separation-of-duties rules', () => {
  it('says what waits for authorisation', () => {
    expect(pendingText(RULE)).toBe('New rule to authorise');
    expect(pendingText({ ...RULE, status: 'ACTIVE', pendingAction: 'DEACTIVATE' })).toBe(
      'Deactivation to authorise',
    );
    expect(pendingText({ ...RULE, status: 'ACTIVE', pendingAction: 'NONE' })).toBe('');
  });

  it('needs two different profiles and a reason', () => {
    expect(ruleErrors(EMPTY_SOD_RULE)).toEqual({
      profileA: 'Select the first group profile',
      profileB: 'Select the second group profile',
      description: 'Enter why the two profiles are not held together',
    });
    expect(ruleErrors({ profileA: 'A', profileB: 'A', description: 'x' })).toEqual({
      profileB: 'Choose two different group profiles',
    });
    expect(ruleErrors({ profileA: 'A', profileB: 'B', description: ' why ' })).toEqual({});
  });
});

describe('risk flags', () => {
  it('are shown in words', () => {
    expect(riskFlagLabel('OUTSIDE_HOURS')).toBe('Outside working hours');
    expect(riskFlagLabel('PRIVILEGE_INCREASE')).toBe('Privilege increase');
    expect(riskFlagLabel('NEW_FLAG')).toBe('new flag');
  });
});

describe('permission combination rules', () => {
  it('names the permissions in the checks of a rule of two permissions', () => {
    expect(
      ruleErrors({
        kind: 'PERMISSIONS',
        profileA: 'ACCESS_REQUEST',
        profileB: 'ACCESS_REQUEST',
        description: 'Requester and approver',
      }),
    ).toEqual({ profileB: 'Choose two different permissions' });
    expect(ruleErrors({ ...EMPTY_SOD_RULE, kind: 'PERMISSIONS' }).profileA).toBe(
      'Select the first permission',
    );
    expect(RULE_KINDS.map((k) => k.value)).toEqual(['PROFILES', 'PERMISSIONS']);
  });
});
