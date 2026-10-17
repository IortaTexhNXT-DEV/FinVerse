import type { DepreciationMethod, FixedAssetInput } from '@/api/assets';
import { check } from './runSummary';
import type { Rule } from './runSummary';

const CENTS = 100;
const PERCENT = 100;
const DOUBLE = 2;
const TAG_PATTERN = /^[A-Z0-9\-/]+$/;

/** Rounds to centavos (the server rounds half-even; half-up is close enough for previews). */
export function roundMoney(value: number): number {
  return Math.round(value * CENTS) / CENTS;
}

/** Residual value from a category's residual percentage. */
export function residualValue(cost: number, residualPercent: number): number {
  return roundMoney((cost * residualPercent) / PERCENT);
}

/**
 * First month's depreciation, mirroring the server:
 * straight line = (cost - residual) / life; double-declining = cost x 2 / life, capped at the
 * depreciable amount.
 */
export function monthlyDepreciation(
  cost: number,
  residualPercent: number,
  method: DepreciationMethod,
  lifeMonths: number,
): number {
  if (cost <= 0 || lifeMonths <= 0) {
    return 0;
  }
  const depreciable = cost - residualValue(cost, residualPercent);
  if (lifeMonths === 1) {
    return roundMoney(depreciable);
  }
  const base = method === 'STRAIGHT_LINE' ? depreciable : cost * DOUBLE;
  return roundMoney(Math.min(base / lifeMonths, depreciable));
}

/** Gain (positive) or loss (negative) on disposal. */
export function disposalResult(proceeds: number, netBookValue: number): number {
  return roundMoney(proceeds - netBookValue);
}

/** Current accounting period (YYYY-MM) of a date. */
export function periodOf(isoDate: string): string {
  return isoDate.slice(0, 7);
}

const ASSET_RULES: readonly Rule<Partial<FixedAssetInput>>[] = [
  [
    'tagNo',
    (f) => !TAG_PATTERN.test(f.tagNo ?? ''),
    'Use capital letters, digits, dashes and slashes',
  ],
  ['description', (f) => !f.description, 'Description is required'],
  ['categoryId', (f) => !f.categoryId, 'Select a category'],
  ['costCenter', (f) => !f.costCenter, 'Cost centre is required'],
  ['acquisitionCost', (f) => (f.acquisitionCost ?? 0) <= 0, 'Cost must be positive'],
  ['acquisitionDate', (f) => !f.acquisitionDate, 'Acquisition date is required'],
  [
    'capitalizationDate',
    (f) => f.takeOn === true && !f.capitalizationDate,
    'Take-on date is required',
  ],
  [
    'settlementAccount',
    (f) => f.takeOn !== true && !f.settlementAccount,
    'Settlement account is required',
  ],
];

/** Client-side checks mirroring the API; returns messages by field. */
export function validateAsset(form: Partial<FixedAssetInput>): Record<string, string> {
  return check(form, ASSET_RULES);
}
