import type { EventType, Rule } from '@/api/accounting';
import {
  components,
  emptyRuleLine,
  newRule,
  optionalHeader,
  parseAmounts,
  roles,
  simulationTotals,
  toAmounts,
  toRuleInput,
  validateRule,
} from './ruleModel';

const receipt: EventType = {
  code: 'PREMIUM_RECEIPT',
  name: 'Premium collection',
  category: 'RECEIPT',
  journalType: 'RECEIPT',
  amountComponents: 'AMOUNT, CHARGES,',
  active: true,
};

describe('accounting rule model', () => {
  it('lists the amount components of an event type', () => {
    expect(components(receipt)).toEqual(['AMOUNT', 'CHARGES']);
    expect(components(undefined)).toEqual([]);
  });

  it('creates a new rule with a debit and a credit line on the first component', () => {
    const rule = newRule(1, receipt, '2026-09-01');
    expect(rule.eventType).toBe('PREMIUM_RECEIPT');
    expect(rule.name).toBe('Premium collection rule');
    expect(rule.lines.map((l) => [l.side, l.amountComponent])).toEqual([
      ['DEBIT', 'AMOUNT'],
      ['CREDIT', 'AMOUNT'],
    ]);
    expect(newRule(1, undefined, '2026-09-01').eventType).toBe('');
  });

  it('accepts a complete rule', () => {
    const rule = newRule(1, receipt, '2026-09-01');
    rule.lines = [
      { ...emptyRuleLine('DEBIT', 'AMOUNT'), accountCode: '@BANK' },
      { ...emptyRuleLine('CREDIT', 'AMOUNT'), accountCode: '1201', partyLine: true },
    ];
    expect(validateRule(rule, receipt)).toEqual([]);
  });

  it('reports every problem of an incomplete rule', () => {
    const rule = newRule(1, receipt, '2026-09-10');
    rule.name = ' ';
    rule.effectiveTo = '2026-09-01';
    rule.lines = [{ ...emptyRuleLine('DEBIT'), accountCode: '@' }];
    const errors = validateRule(rule, undefined);
    expect(errors).toContain('Select an event type');
    expect(errors).toContain('Rule name is required');
    expect(errors).toContain('Effective to must not be before effective from');
    expect(errors).toContain('A rule needs at least one debit and one credit line');
    expect(errors).toContain('Line 1: enter a GL account code or an @ROLE');
    expect(errors).toContain('Line 1: choose an amount component of the event');
  });

  it('collects the account roles used by the lines', () => {
    const lines = [
      { ...emptyRuleLine('DEBIT'), accountCode: '@BANK' },
      { ...emptyRuleLine('DEBIT'), accountCode: '@BANK' },
      { ...emptyRuleLine('CREDIT'), accountCode: '@EXPENSE' },
      { ...emptyRuleLine('CREDIT'), accountCode: '4100' },
    ];
    expect(roles(lines)).toEqual(['BANK', 'EXPENSE']);
  });

  it('copies an existing rule for editing', () => {
    const rule: Rule = {
      ...newRule(3, receipt, '2026-01-01'),
      id: 9,
      recordStatus: 'ACTIVE',
      createdBy: 'fmanager',
    };
    const input = toRuleInput(rule);
    expect(input).not.toHaveProperty('id');
    expect(input.lines).not.toBe(rule.lines);
    expect(input.lines).toEqual(rule.lines);
  });

  it('totals simulated lines and parses register amounts', () => {
    expect(
      simulationTotals([
        { accountCode: '1111', side: 'DEBIT', amount: 100.1 },
        { accountCode: '1201', side: 'CREDIT', amount: 60.05 },
        { accountCode: '4700', side: 'CREDIT', amount: 40.05 },
      ]),
    ).toEqual({ debit: 100.1, credit: 100.1 });
    expect(parseAmounts('AMOUNT=1000.00, DST=12.50, BAD, X=abc')).toEqual({
      AMOUNT: 1000,
      DST: 12.5,
    });
  });

  it('keeps only filled numeric amounts and normalises the simulation header', () => {
    expect(toAmounts(['A', 'B', 'C', 'D'], { A: '10', B: '', C: 'x' })).toEqual({ A: 10 });
    expect(
      optionalHeader({
        valueDate: '2026-09-01',
        currency: ' usd',
        businessLine: '',
        partyCode: 'C-1 ',
      }),
    ).toEqual({
      valueDate: '2026-09-01',
      currency: 'USD',
      businessLine: undefined,
      partyCode: 'C-1',
    });
    expect(
      optionalHeader(
        { valueDate: '2026-09-01', currency: '', businessLine: 'FIRE', partyCode: '' },
        'SGD',
      ).currency,
    ).toBe('SGD');
  });
});
