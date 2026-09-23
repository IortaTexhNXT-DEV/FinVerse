import type { EventType, Rule, RuleInput, RuleLine, Simulation } from '@/api/accounting';
import { formatAmount } from '@/utils/format';

/** Prefix of account placeholders resolved from the event (e.g. "@BANK"). */
export const ROLE_PREFIX = '@';

/** Amount components an event type supplies. */
export function components(eventType: EventType | undefined): string[] {
  if (eventType === undefined) {
    return [];
  }
  return eventType.amountComponents
    .split(',')
    .map((c) => c.trim())
    .filter((c) => c !== '');
}

/** Empty rule line. */
export function emptyRuleLine(side: RuleLine['side'], component = ''): RuleLine {
  return { side, accountCode: '', amountComponent: component, partyLine: false };
}

/** New rule for an event type: one debit and one credit line on its first component. */
export function newRule(
  companyId: number,
  eventType: EventType | undefined,
  today: string,
): RuleInput {
  const first = components(eventType)[0] ?? '';
  return {
    companyId,
    eventType: eventType?.code ?? '',
    name: eventType === undefined ? '' : `${eventType.name} rule`,
    priority: 100,
    effectiveFrom: today,
    lines: [emptyRuleLine('DEBIT', first), emptyRuleLine('CREDIT', first)],
  };
}

/** Editable copy of an existing rule. */
export function toRuleInput(rule: Rule): RuleInput {
  return {
    companyId: rule.companyId,
    eventType: rule.eventType,
    name: rule.name,
    businessLine: rule.businessLine,
    currency: rule.currency,
    priority: rule.priority,
    effectiveFrom: rule.effectiveFrom,
    effectiveTo: rule.effectiveTo,
    lines: rule.lines.map((l) => ({ ...l })),
  };
}

function lineErrors(line: RuleLine, index: number, allowed: string[]): string[] {
  const errors: string[] = [];
  const label = `Line ${index + 1}`;
  if (line.accountCode.trim() === '' || line.accountCode.trim() === ROLE_PREFIX) {
    errors.push(`${label}: enter a GL account code or an @ROLE`);
  }
  if (!allowed.includes(line.amountComponent)) {
    errors.push(`${label}: choose an amount component of the event`);
  }
  return errors;
}

/**
 * Validates a rule before saving: header fields, at least one debit and one credit line, and each
 * line with an account (or role) and a component the event type supplies.
 */
export function validateRule(rule: RuleInput, eventType: EventType | undefined): string[] {
  const errors: string[] = [];
  if (eventType === undefined) {
    errors.push('Select an event type');
  }
  if (rule.name.trim() === '') {
    errors.push('Rule name is required');
  }
  if (
    rule.effectiveTo !== undefined &&
    rule.effectiveTo !== '' &&
    rule.effectiveTo < rule.effectiveFrom
  ) {
    errors.push('Effective to must not be before effective from');
  }
  const sides = new Set(rule.lines.map((l) => l.side));
  if (!sides.has('DEBIT') || !sides.has('CREDIT')) {
    errors.push('A rule needs at least one debit and one credit line');
  }
  const allowed = components(eventType);
  rule.lines.forEach((line, i) => errors.push(...lineErrors(line, i, allowed)));
  return errors;
}

/** Account roles ("@BANK" → "BANK") the event must supply for this rule. */
export function roles(lines: RuleLine[]): string[] {
  const names = lines
    .map((l) => l.accountCode.trim())
    .filter((code) => code.startsWith(ROLE_PREFIX) && code.length > 1)
    .map((code) => code.slice(1));
  return [...new Set(names)];
}

/**
 * Warning for a simulated journal the server reports as unbalanced (sample amounts that do not add
 * up): the real posting would be rejected. Undefined when balanced.
 */
export function unbalancedWarning(sim: Simulation): string | undefined {
  if (sim.balanced) {
    return undefined;
  }
  return (
    `Preview is unbalanced by ${formatAmount(Math.abs(sim.difference))} — posting would be ` +
    `rejected (debits ${formatAmount(sim.totalDebit)}, credits ${formatAmount(sim.totalCredit)}).`
  );
}

/** Numeric amounts of the components that were filled in (blank or invalid entries are skipped). */
export function toAmounts(
  comps: string[],
  entered: Record<string, string>,
): Record<string, number> {
  const out: Record<string, number> = {};
  comps.forEach((c) => {
    const text = entered[c]?.trim() ?? '';
    const value = Number(text);
    if (text !== '' && !Number.isNaN(value)) {
      out[c] = value;
    }
  });
  return out;
}

export interface SimulationHeader {
  valueDate: string;
  currency: string;
  businessLine: string;
  partyCode: string;
}

/** Simulation header with blanks removed and the base currency as default currency. */
export function optionalHeader(header: SimulationHeader, baseCurrency = 'PHP') {
  return {
    valueDate: header.valueDate,
    currency: header.currency.trim() === '' ? baseCurrency : header.currency.trim().toUpperCase(),
    businessLine: header.businessLine.trim() === '' ? undefined : header.businessLine.trim(),
    partyCode: header.partyCode.trim() === '' ? undefined : header.partyCode.trim(),
  };
}

/** Parses the "COMPONENT=amount, ..." text of the event register. */
export function parseAmounts(text: string): Record<string, number> {
  const out: Record<string, number> = {};
  text
    .split(',')
    .map((part) => part.split('='))
    .filter((kv) => kv.length === 2)
    .forEach(([key, value]) => {
      const n = Number(value);
      if (key !== undefined && value !== undefined && !Number.isNaN(n)) {
        out[key.trim()] = n;
      }
    });
  return out;
}
