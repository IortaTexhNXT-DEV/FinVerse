/**
 * The insurer KPI widgets of the dashboard (codebase relevance audit D1): "gross written premium"
 * and "claims paid and outstanding" read the insurer account categories of the ledger. They are
 * shown only with an insurer-only permission (POLICY_VIEW or CLAIM_VIEW), which no BDOI role holds
 * (V1064), so every BDOI role sees the dashboard without them.
 */
export const INSURER_PERMISSIONS: readonly string[] = ['POLICY_VIEW', 'CLAIM_VIEW'];

/** Whether the user sees the insurer KPI widgets. */
export function showsInsurerWidgets(can: (permission: string) => boolean): boolean {
  return INSURER_PERMISSIONS.some(can);
}
