import type { CatalogueEntry } from '@/api/reports';

/** Category key of the New Business reports. */
export const NEW_BUSINESS = 'NEW_BUSINESS';

/** Process step of each NB report, in the order the steps happen. */
const STEPS: { title: string; codes: string[] }[] = [
  { title: 'Accounts and Workflow', codes: ['NB-ACC-STATUS', 'NB-STAGE-OUTCOME'] },
  { title: 'Placement', codes: ['NB-PLC-UPDATE', 'NB-PLC-SUMMARY'] },
  { title: 'Billing and Payments', codes: ['NB-CLPC-BILLING', 'NB-PAY-MATCH'] },
  { title: 'Issuance', codes: ['NB-DISPATCH'] },
  { title: 'Booking and Production', codes: ['NB-BOOKED-REG', 'NB-SI-REG', 'NB-PRODUCTION'] },
];

const OTHER = 'Other New Business Reports';

/** The NB reports of a catalogue grouped by process step (steps without a report are left out). */
export function groupReports(
  catalogue: readonly CatalogueEntry[],
): { title: string; reports: CatalogueEntry[] }[] {
  const nb = catalogue.filter((e) => e.category === NEW_BUSINESS);
  const known = new Set(STEPS.flatMap((s) => s.codes));
  const groups = STEPS.map((s) => ({
    title: s.title,
    reports: s.codes.flatMap((code) => nb.filter((e) => e.code === code)),
  }));
  groups.push({ title: OTHER, reports: nb.filter((e) => !known.has(e.code)) });
  return groups.filter((g) => g.reports.length > 0);
}
