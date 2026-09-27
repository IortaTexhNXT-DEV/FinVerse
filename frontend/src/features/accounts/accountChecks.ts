import type { AccountCheck } from '@/api/accounts';
import { fieldErrorLines } from '@/utils/fieldErrors';
import { humanize } from '@/utils/format';

/** Lines telling what still blocks the submission of an account (TSU routing excluded). */
export function checkLines(check: AccountCheck): string[] {
  const lines = fieldErrorLines(check.fieldErrors);
  check.missingDocuments.forEach((d) => lines.push(`Attach the ${humanize(d)}.`));
  if (!check.premiumRated) {
    lines.push('The premium could not be rated yet (sum insured, period and rates).');
  }
  return lines;
}
