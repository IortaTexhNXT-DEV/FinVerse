import type { IssuanceCounts, IssuanceRow, IssuanceTab, Review } from '@/api/issuance';
import { humanize } from '@/utils/format';

/** Issuance Workbench tabs. */
export const ISSUANCE_TABS: readonly { id: IssuanceTab; label: string }[] = [
  { id: 'AWAITING_POLICY', label: 'Placed – Awaiting Policy' },
  { id: 'REVIEW', label: 'Policy Received – Review Extraction' },
  { id: 'READY_TO_DISPATCH', label: 'Ready to Dispatch' },
  { id: 'IA_TO_GENERATE', label: 'IA to Generate' },
];

/** The tiles of the Issuance Workbench, one per tab. */
export function issuanceTiles(
  counts: IssuanceCounts | undefined,
): { tab: IssuanceTab; label: string; value: number; alert: boolean }[] {
  const values: Record<IssuanceTab, number> = {
    AWAITING_POLICY: counts?.awaitingPolicy ?? 0,
    REVIEW: counts?.toReview ?? 0,
    READY_TO_DISPATCH: counts?.readyToDispatch ?? 0,
    IA_TO_GENERATE: counts?.adviceToGenerate ?? 0,
  };
  return ISSUANCE_TABS.map((t) => ({
    tab: t.id,
    label: t.label,
    value: values[t.id],
    alert: t.id === 'REVIEW',
  }));
}

/**
 * Policy numbers proposed for confirmation: one field per policy year (BRNB.112), filled with the
 * extracted numbers, else the numbers already on the account.
 */
export function proposedNumbers(review: Review): string[] {
  const years = Math.max(review.account.termYears, 1);
  const source =
    review.epolicy.extractedPolicyNumbers.length > 0
      ? review.epolicy.extractedPolicyNumbers
      : review.account.policyNumbers;
  return Array.from({ length: years }, (_, i) => source[i] ?? '');
}

/** Error of the policy number fields, or undefined when every year has a distinct number. */
export function numbersError(numbers: string[]): string | undefined {
  const clean = numbers.map((n) => n.trim());
  if (clean.some((n) => n === '')) {
    return 'Enter one policy number per policy year.';
  }
  if (new Set(clean).size !== clean.length) {
    return 'The policy numbers must be different.';
  }
  return undefined;
}

/** Splits an address list typed by the user. */
export function addresses(text: string): string[] {
  return text
    .split(/[,;\s]+/)
    .map((a) => a.trim())
    .filter((a) => a.length > 0);
}

/** Key of a workbench row: the e-policy on the e-policy tabs, else the account. */
export function rowKeyOf(row: IssuanceRow): string {
  return row.epolicyId === undefined ? row.arn : String(row.epolicyId);
}

const MATCH_TEXTS: Record<string, string> = {
  MANUAL: 'the account chosen at upload',
  POLICY_NUMBER: 'the policy number',
  FILE_NAME: 'the ARN in the file name',
  CONTENT: 'the ARN printed in the document',
};

/** How an e-policy was matched to its account, as a phrase ("matched by the policy number"). */
export function matchText(method: string | null | undefined): string {
  if (!method) {
    return '—';
  }
  return MATCH_TEXTS[method] ?? humanize(method).toLowerCase();
}
