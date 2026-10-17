const CRITERIA: Record<string, string> = {
  CLIENT: 'Client',
  RISK_CODE: 'Risk code',
  PN: 'PN number',
  EXPIRING_POLICY: 'Expiring policy number',
  EXPIRING_INVOICE: 'Expiring account',
};

/** The duplicate criteria of a match in words (FRRN.006.01). */
export function criteriaText(criteria: readonly string[]): string {
  return criteria.map((c) => CRITERIA[c] ?? c).join(', ');
}
