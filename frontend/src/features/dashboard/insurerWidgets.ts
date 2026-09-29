import { MODULE_OFF } from '@/navigation/productModules';

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

/** Dashboard widgets that belong to a product module (the others are platform widgets). */
export const WIDGET_MODULES = {
  premium: 'UNDERWRITING',
  claims: 'INSURER_CLAIMS',
  payables: 'PAYABLES',
  budget: 'BUDGET',
} as const;

/** Whether a widget of a product module is shown: its module is in use in this deployment. */
export function showsWidget(
  widget: keyof typeof WIDGET_MODULES,
  can: (permission: string) => boolean,
): boolean {
  return !can(`${MODULE_OFF}${WIDGET_MODULES[widget]}`);
}
