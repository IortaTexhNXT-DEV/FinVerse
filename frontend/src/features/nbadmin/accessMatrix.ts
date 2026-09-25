import type { AccessMatrix, ActionClass } from '@/api/nbadmin';

/** Area of permissions that have no action class (finance and platform administration). */
export const UNCLASSIFIED = 'OTHER';

const AREA_LABELS: Record<string, string> = {
  PRODUCT_MAINTENANCE: 'Product Maintenance',
  PACKAGE_REQUEST: 'Package Requests',
  INCENTIVES: 'Incentive Criteria',
  MASTER_DATA: 'Master Data',
  NON_PACKAGE: 'Non-Package / TSU',
  PRODUCTION_RECON: 'Production Reconciliation',
  BROKING_ADMIN: 'Broking Administration',
  [UNCLASSIFIED]: 'Other (not classified)',
};

export const ACTION_LABELS: Record<ActionClass, string> = {
  VIEW: 'View only',
  CREATE: 'Create',
  AMEND: 'Amend',
  APPROVE: 'Approve / Validate',
};

/** Display name of a functional area code (PMADD05), e.g. CLIENTS -> "Clients". */
export function areaLabel(area: string): string {
  const known = AREA_LABELS[area];
  if (known !== undefined) {
    return known;
  }
  return area
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export interface AreaGroup {
  area: string;
  permissions: string[];
}

/**
 * Permissions grouped by functional area in the order the areas first appear, unclassified
 * permissions last (permission picker of role-permission change requests).
 */
export function groupByArea(rows: AccessMatrix['permissions']): AreaGroup[] {
  const groups = new Map<string, string[]>();
  for (const row of rows) {
    const area = row.area ?? UNCLASSIFIED;
    groups.set(area, [...(groups.get(area) ?? []), row.permission]);
  }
  const ordered = [...groups.entries()].map(([area, permissions]) => ({ area, permissions }));
  return [
    ...ordered.filter((g) => g.area !== UNCLASSIFIED),
    ...ordered.filter((g) => g.area === UNCLASSIFIED),
  ];
}

/** Action classes of a permission as text, e.g. "Create, Amend"; empty when not classified. */
export function actionText(actions: readonly ActionClass[]): string {
  return actions.map((a) => ACTION_LABELS[a]).join(', ');
}
