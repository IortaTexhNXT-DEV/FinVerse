import type { LineView } from '@/api/eb';
import type { ConfirmationInput, Proposal } from '@/api/ebMarket';

/** The premium a proposal offers on a benefit line. */
export function premiumOn(p: Proposal, benefitLine: string): number {
  return p.lines
    .filter((l) => l.benefitLine === benefitLine)
    .reduce((sum, l) => sum + l.annualPremium, 0);
}

/** The errors of a confirmation; empty when it may be recorded. */
export function confirmationErrors(
  input: ConfirmationInput,
  lines: LineView[],
  file: File | undefined,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (input.channel === '') {
    errors.channel = 'Select how the client confirmed';
  }
  if (!file) {
    errors.file = "Attach the client's confirmation";
  }
  const missing = lines.find((l) => !input.choices.some((c) => c.lineNo === l.lineNo));
  if (missing) {
    errors.choices = `Select the chosen proposal of line ${String(missing.lineNo)} (${missing.benefitLine})`;
  }
  return errors;
}
