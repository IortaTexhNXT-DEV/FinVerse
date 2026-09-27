import { describe, expect, it } from 'vitest';
import type { Rule } from './api';
import { describeRule, ruleFormErrors, ruleFormOf, ruleInputOf, stagesOf } from './labels';

const RULE: Rule = {
  id: 1,
  code: 'CLX-AGING-45',
  name: 'Aging',
  basis: 'AGING_FROM_BOOKING',
  threshold: 45,
  targetLevel: 'TL',
  reasonCode: 'AGING',
  slaHours: 48,
  notifyTarget: true,
  effectiveFrom: '2026-01-01',
  recordStatus: 'ACTIVE',
  maker: 'badmin',
};

describe('escalation labels', () => {
  it('maps tabs to the workflow stages', () => {
    expect(stagesOf('TL')).toEqual(['WITH_TL', 'RAISED']);
    expect(stagesOf('RESOLVED')).toEqual(['RESOLVED']);
  });

  it('describes a rule', () => {
    expect(describeRule(RULE)).toBe('Days since booking ≥ 45 → Team Lead');
    expect(describeRule({ ...RULE, targetLevel: 'USER', targetUsername: 'mkttl' })).toContain(
      'mkttl',
    );
  });

  it('builds the form of a new or existing rule and its request', () => {
    const blank = ruleFormOf(undefined, '2026-09-25');
    expect(blank.threshold).toBe('45');
    expect(blank.effectiveFrom).toBe('2026-09-25');
    const form = ruleFormOf(RULE, '2026-09-25');
    expect(form.code).toBe('CLX-AGING-45');
    const input = ruleInputOf({ ...form, segment: ' CBG ', targetUsername: ' ' }, 7);
    expect(input).toMatchObject({ companyId: 7, segment: 'CBG', threshold: 45, slaHours: 48 });
    expect(input.targetUsername).toBeUndefined();
    expect(input.effectiveTo).toBeUndefined();
  });

  it('checks the rule form', () => {
    const errors = ruleFormErrors(
      {
        ...ruleFormOf(undefined, '2026-09-25'),
        threshold: '0',
        targetLevel: 'USER',
        slaHours: '0',
        effectiveTo: '2026-01-01',
      },
      true,
    );
    expect(new Set(Object.keys(errors))).toEqual(
      new Set([
        'code',
        'effectiveTo',
        'name',
        'reasonCode',
        'slaHours',
        'targetUsername',
        'threshold',
      ]),
    );
    expect(ruleFormErrors({ ...ruleFormOf(RULE, '2026-09-25'), effectiveFrom: '' }, false)).toEqual(
      {
        effectiveFrom: 'Enter the first day',
      },
    );
    expect(ruleFormErrors(ruleFormOf(RULE, '2026-09-25'), false)).toEqual({});
  });
});
