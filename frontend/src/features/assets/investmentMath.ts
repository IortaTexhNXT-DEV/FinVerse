import type { DayCount, HoldingInput } from '@/api/investments';
import { check } from './runSummary';
import type { Rule } from './runSummary';

const CENTS = 100;
const PERCENT = 100;
const MS_PER_DAY = 86_400_000;
const DAYS_PER_MONTH = 30;
const MONTHS_PER_YEAR = 12;
const YEAR_DAYS: Record<DayCount, number> = { ACT_365: 365, THIRTY_360: 360 };
const BUCKET_LIMITS = [30, 90, 180, 365, 1095, 1826];
const BUCKET_LABELS = [
  'Due / overdue',
  'Within 1 month',
  '1 - 3 months',
  '3 - 6 months',
  '6 - 12 months',
  '1 - 3 years',
  '3 - 5 years',
  'Over 5 years',
  'No maturity',
];

function parts(iso: string): [number, number, number] {
  const [y, m, d] = iso.split('-').map(Number);
  return [y ?? 0, m ?? 0, d ?? 0];
}

function utc(iso: string): number {
  const [y, m, d] = parts(iso);
  return Date.UTC(y, m - 1, d);
}

/** Actual calendar days between two ISO dates. */
export function actualDays(from: string, to: string): number {
  return Math.round((utc(to) - utc(from)) / MS_PER_DAY);
}

/**
 * Days under a convention, mirroring the server: Actual/365 counts calendar days; 30E/360 treats
 * day 31 as day 30 and every month as 30 days.
 */
export function dayCountDays(convention: DayCount, from: string, to: string): number {
  if (convention === 'ACT_365') {
    return actualDays(from, to);
  }
  const [y1, m1, d1] = parts(from);
  const [y2, m2, d2] = parts(to);
  const months = (y2 - y1) * MONTHS_PER_YEAR + m2 - m1;
  return months * DAYS_PER_MONTH + Math.min(d2, DAYS_PER_MONTH) - Math.min(d1, DAYS_PER_MONTH);
}

/** Coupon interest for (from, to]: face x rate x days / year days, in centavos. */
export function couponInterest(
  face: number,
  ratePercent: number,
  convention: DayCount,
  from: string,
  to: string,
): number {
  const days = dayCountDays(convention, from, to);
  if (days <= 0 || ratePercent <= 0) {
    return 0;
  }
  const interest = (face * ratePercent * days) / (PERCENT * YEAR_DAYS[convention]);
  return Math.round(interest * CENTS) / CENTS;
}

/** Discount (positive) or premium (negative) to be amortized to maturity. */
export function discountOrPremium(face: number, price: number): number {
  return Math.round((face - price) * CENTS) / CENTS;
}

/**
 * Realized gain (positive) or loss on a sale or maturity, as the server computes it:
 * proceeds + final tax + recycled FVOCI reserve - carrying amount - accrued interest.
 */
export function realizedGain(
  proceeds: number,
  finalTax: number,
  carrying: number,
  accrued: number,
  fvociReserve = 0,
): number {
  return Math.round((proceeds + finalTax + fvociReserve - carrying - accrued) * CENTS) / CENTS;
}

/** Remaining-term bucket label (maturity profile) of a holding on a date. */
export function maturityBucket(asOf: string, maturity: string | undefined): string {
  if (maturity === undefined) {
    return BUCKET_LABELS[BUCKET_LABELS.length - 1] ?? '';
  }
  const days = actualDays(asOf, maturity);
  if (days <= 0) {
    return BUCKET_LABELS[0] ?? '';
  }
  const index = BUCKET_LIMITS.findIndex((limit) => days <= limit);
  return BUCKET_LABELS[index === -1 ? BUCKET_LIMITS.length + 1 : index + 1] ?? '';
}

type HoldingForm = Partial<HoldingInput>;

function badMaturity(f: HoldingForm): boolean {
  return f.instrumentType !== 'EQUITY' && (f.maturityDate ?? '') <= (f.settlementDate ?? '');
}

const HOLDING_RULES: readonly Rule<HoldingForm>[] = [
  ['portfolioId', (f) => !f.portfolioId, 'Select a portfolio'],
  ['branchId', (f) => !f.branchId, 'Select the branch'],
  ['description', (f) => !f.description, 'Description is required'],
  ['issuerCode', (f) => !f.issuerCode, 'Select the issuer or bank'],
  ['faceValue', (f) => (f.faceValue ?? 0) <= 0, 'Face value must be positive'],
  ['purchasePrice', (f) => (f.purchasePrice ?? 0) <= 0, 'Price must be positive'],
  ['settlementDate', (f) => !f.settlementDate, 'Settlement date is required'],
  ['maturityDate', badMaturity, 'Maturity must be after settlement'],
  ['bankAccount', (f) => !f.bankAccount, 'Settlement bank account is required'],
  [
    'takeOnDate',
    (f) => f.takeOn === true && (f.takeOnDate ?? '') <= (f.settlementDate ?? ''),
    'Take-on date must be after settlement',
  ],
];

/** Client-side checks mirroring the API; returns messages by field. */
export function validateHolding(form: HoldingForm): Record<string, string> {
  return check(form, HOLDING_RULES);
}
