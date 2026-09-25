import type {
  Answer,
  BillingStage,
  CertificateInput,
  DpItem,
  DpTag,
  IncentiveTier,
  SchemeTerms,
} from './commissionApi';

/** Work list tabs of the direct payment accounts, in the order they are worked. */
export const TAG_TABS: readonly { id: DpTag; label: string }[] = [
  { id: 'DP_FOR_CONFIRMATION', label: 'For Confirmation' },
  { id: 'DP_FOR_BILLING', label: 'For Billing' },
  { id: 'BILLED', label: 'Billed' },
  { id: 'APPROVED', label: 'Approved' },
  { id: 'REJECTED', label: 'Rejected' },
  { id: 'COLLECTED', label: 'Collected' },
  { id: 'PR_REVERSED', label: 'PR Reversed' },
  { id: 'EXCLUDED', label: 'Excluded' },
];

/** Tabs of the billings. */
export const STAGE_TABS: readonly { id: BillingStage; label: string }[] = [
  { id: 'DP_FOR_BILLING', label: 'To Send' },
  { id: 'AWAITING_INSURER', label: 'Awaiting Insurer' },
  { id: 'APPROVED', label: 'To Collect' },
  { id: 'RETURNED_TO_COLLECTION', label: 'Returned to Collection' },
  { id: 'CLOSED', label: 'Closed' },
  { id: 'CANCELLED', label: 'Cancelled' },
];

/** A tab label with its count ("For Billing (3)"). */
export function withCount(label: string, count: number | undefined): string {
  return count === undefined || count === 0 ? label : `${label} (${String(count)})`;
}

/** The failed validation rules of an account, "" when every rule passed. */
export function failedRules(item: Pick<DpItem, 'rules'>): string {
  return item.rules
    .filter((r) => !r.passed)
    .map((r) => r.message ?? r.rule)
    .join('; ');
}

/** Actions allowed on the selected accounts of a tab. */
export function bulkActions(tag: DpTag): { confirm: boolean; exclude: boolean; bill: boolean } {
  return {
    confirm: tag === 'DP_FOR_CONFIRMATION',
    exclude: tag === 'DP_FOR_CONFIRMATION' || tag === 'DP_FOR_BILLING',
    bill: tag === 'DP_FOR_BILLING',
  };
}

/** Decision of the insurer on one account while answers are recorded. */
export interface Decision {
  approved: boolean | undefined;
  reason: string;
  comment: string;
}

/** The answers to send; undefined decisions are left out. */
export function answersOf(decisions: Record<string, Decision>): Answer[] {
  return Object.entries(decisions)
    .filter(([, d]) => d.approved !== undefined)
    .map(([invoiceNo, d]) => ({
      invoiceNo,
      approved: d.approved === true,
      reason: d.approved === true ? undefined : d.reason || undefined,
      comment: d.comment || undefined,
    }));
}

/** The first problem of the recorded answers, undefined when they can be sent. */
export function answersProblem(answers: Answer[]): string | undefined {
  if (answers.length === 0) {
    return 'Record the insurer decision of at least one account';
  }
  const missing = answers.find((a) => !a.approved && a.reason === undefined);
  return missing === undefined
    ? undefined
    : `Give the insurer's reason for rejecting ${missing.invoiceNo}`;
}

function positive(value: number | undefined, zeroAllowed: boolean): boolean {
  if (value === undefined || Number.isNaN(value)) {
    return false;
  }
  return zeroAllowed ? value >= 0 : value > 0;
}

function tierValid(calculation: SchemeTerms['calculation'], t: IncentiveTier): boolean {
  return calculation === 'TARGET_TIERED'
    ? positive(t.minProduction, true) && positive(t.ratePercent, false)
    : positive(t.minBasicPremium, true) && positive(t.fixedAmount, false);
}

/** The first problem of a scheme, undefined when it can be saved (CMRID.005/006). */
export function schemeProblem(code: string, terms: SchemeTerms): string | undefined {
  if (code.trim() === '' || terms.name.trim() === '') {
    return 'Code and name are required';
  }
  if (
    terms.effectiveFrom !== undefined &&
    terms.effectiveTo !== undefined &&
    terms.effectiveTo < terms.effectiveFrom
  ) {
    return 'The scheme ends before it starts';
  }
  const bad = terms.tiers.findIndex((t) => !tierValid(terms.calculation, t));
  if (bad >= 0) {
    return terms.calculation === 'TARGET_TIERED'
      ? `Tier ${String(bad + 1)} needs a production target and a rate above zero`
      : `Tier ${String(bad + 1)} needs a minimum basic premium and a fixed amount above zero`;
  }
  if (terms.active && terms.tiers.length === 0) {
    return 'An active scheme needs at least one tier';
  }
  return undefined;
}

/** A tier described in words. */
export function describeTier(calculation: SchemeTerms['calculation'], t: IncentiveTier): string {
  if (calculation === 'TARGET_TIERED') {
    const multiplier =
      t.multiplier === undefined || t.multiplier === 1 ? '' : ` × ${String(t.multiplier)}`;
    return `From ${String(t.minProduction ?? 0)}: ${String(t.ratePercent ?? 0)}%${multiplier}`;
  }
  return `Basic premium from ${String(t.minBasicPremium ?? 0)}: ${String(t.fixedAmount ?? 0)} per policy`;
}

/** Total commission of the ORs a certificate covers. */
export function receiptsTotal(input: Pick<CertificateInput, 'receipts'>): number {
  return input.receipts.reduce((sum, r) => sum + (Number.isFinite(r.amount) ? r.amount : 0), 0);
}

/** The first problem of a certificate, undefined when it can be submitted (CMRID.015). */
export function certificateProblem(input: CertificateInput): string | undefined {
  if (input.form.trim() === '' || input.number.trim() === '') {
    return 'Form and certificate number are required';
  }
  if (input.periodFrom === '' || input.periodTo === '' || input.periodTo < input.periodFrom) {
    return 'Enter a period that ends after it starts';
  }
  if (Number.isNaN(input.taxWithheld) || input.taxWithheld <= 0) {
    return 'The tax withheld must be above zero';
  }
  if (input.receipts.length === 0) {
    return 'Tag the certificate to at least one official receipt';
  }
  return undefined;
}
