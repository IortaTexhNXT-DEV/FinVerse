/**
 * The insurer roles of the platform (codebase relevance audit D1 and R1). The premium and claims
 * widgets read the insurer account categories of the ledger ("gross written premium", "claims
 * paid and outstanding"): they are insurer KPIs, so no BDOI role sees them.
 */
export const INSURER_ROLES: ReadonlySet<string> = new Set([
  'UNDERWRITER',
  'CLAIMS_OFFICER',
  'RI_OFFICER',
]);

/** Whether the user holds an insurer role, the only roles that see the insurer KPI widgets. */
export function showsInsurerWidgets(roles: readonly string[] | undefined): boolean {
  return (roles ?? []).some((r) => INSURER_ROLES.has(r));
}
