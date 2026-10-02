import type { RiskInput } from '@/api/underwriting';

/**
 * Client-side premium arithmetic for instant feedback while typing. The server's premium preview
 * (and approval) remains authoritative; these functions mirror its rules.
 */

const CENTS = 100;

export function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * CENTS) / CENTS;
}

export function pct(amount: number, ratePct: number | undefined): number {
  return round2((amount * (ratePct ?? 0)) / 100);
}

/** Premium of a risk: entered premium, else sum insured × rate %. */
export function riskPremium(risk: Pick<RiskInput, 'sumInsured' | 'rate' | 'premium'>): number {
  if (risk.premium !== undefined && Number.isFinite(risk.premium)) {
    return round2(risk.premium);
  }
  return pct(risk.sumInsured, risk.rate);
}

export interface RiskTotals {
  sumInsured: number;
  grossPremium: number;
}

export function riskTotals(
  risks: Pick<RiskInput, 'sumInsured' | 'rate' | 'premium'>[],
): RiskTotals {
  return risks.reduce<RiskTotals>(
    (acc, r) => ({
      sumInsured: round2(acc.sumInsured + (Number.isFinite(r.sumInsured) ? r.sumInsured : 0)),
      grossPremium: round2(acc.grossPremium + riskPremium(r)),
    }),
    { sumInsured: 0, grossPremium: 0 },
  );
}

export interface EstimateInput {
  grossPremium: number;
  discountRate?: number;
  loadingRate?: number;
  sharePct: number;
  leader: boolean;
  taxRatePct: number;
  policyFee: number;
  commissionRate: number;
}

export interface Estimate {
  netPremium: number;
  ourNetPremium: number;
  billedPremium: number;
  taxes: number;
  totalDue: number;
  commission: number;
}

/**
 * Estimates the debit note: net = gross + loading − discount, our share, taxes on our net (all
 * tax rates added together), policy fee, commission on our net.
 */
export function estimate(input: EstimateInput): Estimate {
  const gross = round2(input.grossPremium);
  const discount = pct(gross, input.discountRate);
  const loading = pct(gross, input.loadingRate);
  const netPremium = round2(gross + loading - discount);
  const ourNetPremium = round2(
    pct(gross, input.sharePct) + pct(loading, input.sharePct) - pct(discount, input.sharePct),
  );
  const billedPremium = input.leader ? netPremium : ourNetPremium;
  const taxes = pct(ourNetPremium, input.taxRatePct);
  return {
    netPremium,
    ourNetPremium,
    billedPremium,
    taxes,
    totalDue: round2(billedPremium + taxes + input.policyFee),
    commission: pct(ourNetPremium, input.commissionRate),
  };
}

const DAY_MS = 86_400_000;

function days(fromIso: string, toIso: string): number {
  return Math.round((Date.parse(toIso) - Date.parse(fromIso)) / DAY_MS);
}

/**
 * Pro-rata (1/365) return premium on cancellation: gross × unexpired days / period days, where
 * unexpired days are counted from the effective date to the period end inclusive.
 */
export function cancellationReturn(
  grossPremium: number,
  periodFrom: string,
  periodTo: string,
  effectiveDate: string,
): number {
  const total = days(periodFrom, periodTo) + 1;
  const unexpired = Math.max(0, days(effectiveDate, periodTo) + 1);
  if (total <= 0) {
    return 0;
  }
  return round2((grossPremium * unexpired) / total);
}

/** Last valid day of a quotation. */
export function expiryDate(issueDate: string, validityDays: number): string {
  return new Date(Date.parse(issueDate) + validityDays * DAY_MS).toISOString().slice(0, 10);
}

/** Default period end: one year less a day after the start. */
export function oneYearFrom(startIso: string): string {
  const d = new Date(`${startIso}T00:00:00Z`);
  d.setUTCFullYear(d.getUTCFullYear() + 1);
  d.setUTCDate(d.getUTCDate() - 1);
  return d.toISOString().slice(0, 10);
}
