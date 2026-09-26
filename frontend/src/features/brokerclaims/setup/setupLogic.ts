import type { SettlementAttributesInput, StatusAttributesInput, ValueAttributes } from './api';

/** Phases a status may belong to (CLOSED is reached only through a settlement type). */
export const STATUS_PHASES = [
  { code: 'NEW', label: 'Newly filed' },
  { code: 'IN_PROGRESS', label: 'In progress' },
  { code: 'TEMP_CLOSED', label: 'Temporarily closed' },
] as const;

/** Parties a claim waits on (spec 6.1). */
export const WAITING_ON = [
  { code: 'INSURER', label: 'Insurer' },
  { code: 'CLAIMANT', label: 'Claimant' },
  { code: 'ASSURED', label: 'Assured' },
  { code: 'ADJUSTER', label: 'Adjuster' },
  { code: 'BDOI', label: 'BDOI' },
] as const;

/** Outcomes of a settlement type (spec 6.2). */
export const OUTCOMES = [
  { code: 'SETTLED', label: 'Settled' },
  { code: 'CLOSED_WITHOUT_PAYMENT', label: 'Closed without payment' },
] as const;

/** The status attribute form of a value (current attributes, else the pending proposal). */
export function statusForm(value: ValueAttributes): StatusAttributesInput {
  const a = { ...value.attributes, ...value.pending };
  return {
    phase: a.phase ?? '',
    waitingOn: a.waiting_on ?? '',
    followUpDays: a.follow_up_days ?? '',
    awaitingPremiumRemittance: a.awaiting_premium_remittance === 'true',
  };
}

/** The settlement attribute form of a value. */
export function settlementForm(value: ValueAttributes): SettlementAttributesInput {
  const a = { ...value.attributes, ...value.pending };
  return {
    outcome: a.outcome ?? '',
    closesClaim: a.closes_claim === 'true',
    requiresSettlementAmount: a.requires_settlement_amount === 'true',
  };
}

/** Field errors of the status attributes (FR-CL-040). */
export function validateStatusForm(
  form: StatusAttributesInput,
): Partial<Record<keyof StatusAttributesInput, string>> {
  const errors: Partial<Record<keyof StatusAttributesInput, string>> = {};
  if (form.phase === '') {
    errors.phase = 'Set the phase of the status';
  }
  if (form.waitingOn === '') {
    errors.waitingOn = 'Select the party the claim waits on';
  }
  const days = form.followUpDays.trim();
  if (days !== '' && (!/^\d{1,3}$/.test(days) || Number(days) < 1 || Number(days) > 365)) {
    errors.followUpDays = 'Enter a whole number of days';
  }
  return errors;
}

/** Field errors of the settlement attributes (FR-CL-043). */
export function validateSettlementForm(
  form: SettlementAttributesInput,
): Partial<Record<keyof SettlementAttributesInput, string>> {
  return form.outcome === '' ? { outcome: 'Set the outcome of the settlement type' } : {};
}

/** Short text of the attributes of a value, e.g. "IN_PROGRESS · waits on INSURER · 7 days". */
export function describeStatus(attributes: Record<string, string | null>): string {
  const parts: string[] = [];
  if (attributes.phase) {
    parts.push(attributes.phase);
  }
  if (attributes.waiting_on) {
    parts.push(`waits on ${attributes.waiting_on}`);
  }
  if (attributes.follow_up_days) {
    parts.push(`${attributes.follow_up_days} days`);
  }
  if (attributes.awaiting_premium_remittance === 'true') {
    parts.push('awaiting premium remittance');
  }
  return parts.join(' · ');
}

/** Short text of the attributes of a settlement type. */
export function describeSettlement(attributes: Record<string, string | null>): string {
  const parts: string[] = [];
  if (attributes.outcome) {
    parts.push(attributes.outcome);
  }
  if (attributes.closes_claim !== undefined) {
    parts.push(attributes.closes_claim === 'true' ? 'closes the claim' : 'keeps the claim open');
  }
  if (attributes.requires_settlement_amount === 'true') {
    parts.push('amount and date required');
  }
  return parts.join(' · ');
}
