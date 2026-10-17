import type { ChargeBasis, ChargeVatTreatment, OtherChargeInput } from '@/api/otherCharges';
import { formatAmount, formatRate } from '@/utils/format';

/** Bases of an other charge with their labels. */
export const CHARGE_BASES: { value: ChargeBasis; label: string }[] = [
  { value: 'AMOUNT', label: 'Fixed amount per policy' },
  { value: 'RATE', label: '% of the net premium' },
];

/** VAT treatments of an other charge with their labels. */
export const CHARGE_VAT_TREATMENTS: { value: ChargeVatTreatment; label: string }[] = [
  { value: 'VATABLE', label: 'With VAT' },
  { value: 'EXEMPT', label: 'VAT-exempt' },
  { value: 'ZERO_RATED', label: 'Zero-rated' },
];

/** The value of a charge as the list shows it: an amount or a percentage. */
export function chargeValueLabel(basis: ChargeBasis, value: number): string {
  return basis === 'RATE' ? `${formatRate(value)} % of the net premium` : formatAmount(value);
}

/** The label of a VAT treatment. */
export function chargeVatLabel(code: ChargeVatTreatment): string {
  return CHARGE_VAT_TREATMENTS.find((t) => t.value === code)?.label ?? code;
}

/**
 * The problems of a new charge row, by field.
 *
 * @param f form values
 * @returns messages by field; empty when the row can be saved
 */
export function chargeProblems(f: Partial<OtherChargeInput>): Partial<Record<string, string>> {
  const problems: Partial<Record<string, string>> = {};
  if (!/^[A-Z0-9_]+$/.test(f.chargeCode ?? '')) {
    problems.chargeCode = 'Capital letters, digits and underscores';
  }
  if (!f.name?.trim()) {
    problems.name = 'Enter the name of the charge';
  }
  const value = valueProblem(f);
  if (value) {
    problems.value = value;
  }
  if (!f.glAccountCode?.trim()) {
    problems.glAccountCode = 'Enter the GL account of the charge';
  }
  Object.assign(problems, dateProblems(f));
  return problems;
}

function valueProblem(f: Partial<OtherChargeInput>): string | null {
  if (f.value === undefined || f.value < 0) {
    return 'Enter an amount or rate of zero or more';
  }
  return f.basis === 'RATE' && f.value > 100 ? 'A rate is a percentage between 0 and 100' : null;
}

function dateProblems(f: Partial<OtherChargeInput>): Partial<Record<string, string>> {
  if (!f.effectiveFrom) {
    return { effectiveFrom: 'Enter the effective-from date' };
  }
  return f.effectiveTo && f.effectiveTo < f.effectiveFrom
    ? { effectiveTo: 'The effective-to date is before the effective-from date' }
    : {};
}
