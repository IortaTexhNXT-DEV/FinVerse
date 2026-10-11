/** An item of the Cashiering dashboard (FRS.CSH.01.03.02): count and amount per currency. */
export interface DashboardItem {
  code: string;
  label: string;
  count: number;
  amounts: Record<string, number>;
  link: string;
}

/** A group of the Cashiering dashboard. */
export interface DashboardGroup {
  code: string;
  label: string;
  items: DashboardItem[];
}

/** The permissions that see each group: an item the user cannot act on is not shown. */
const GROUP_PERMISSIONS: Record<string, readonly string[]> = {
  ISSUANCE: ['CASH_APPROVE'],
  CANCELLATION: ['CASH_APPROVE'],
  REINSTATEMENT: ['CASH_APPROVE'],
  RETURNED: ['CASH_RECEIPT', 'CASH_CANCEL', 'CASH_REINSTATE'],
  UNAPPLIED: ['CASH_DISPOSITION', 'CASH_DISPOSITION_APPROVE', 'CASH_APPLY'],
  FILES: ['CASH_UPLOAD'],
};

/** The groups of the user's role. */
export function visibleGroups(
  groups: readonly DashboardGroup[],
  can: (permission: string) => boolean,
): DashboardGroup[] {
  return groups.filter((g) => (GROUP_PERMISSIONS[g.code] ?? []).some((p) => can(p)));
}

/** The currencies of the amounts shown, the base currency first. */
export function currenciesOf(groups: readonly DashboardGroup[], base: string): string[] {
  const found = new Set<string>([base]);
  groups.forEach((g) =>
    g.items.forEach((i) => Object.keys(i.amounts).forEach((c) => found.add(c))),
  );
  return [...found].filter((c) => c !== '');
}

/** The main navigation of the dashboard (FRS.CSH.01.03.03). */
export const NAVIGATION: readonly { label: string; to: string; permissions: readonly string[] }[] =
  [
    { label: 'Create AR/OR', to: '/cashiering/records/new?kind=AR', permissions: ['CASH_RECEIPT'] },
    {
      label: 'Post - Issuance',
      to: '/cashiering/posting?kind=CREATION',
      permissions: ['CASH_APPROVE', 'CASH_RECEIPT'],
    },
    { label: 'Cancel AR/OR', to: '/cashiering/receipts', permissions: ['CASH_CANCEL'] },
    {
      label: 'Post - Cancellation',
      to: '/cashiering/posting?kind=CANCELLATION',
      permissions: ['CASH_APPROVE', 'CASH_CANCEL'],
    },
    { label: 'Reinstate AR/OR', to: '/cashiering/receipts', permissions: ['CASH_REINSTATE'] },
    {
      label: 'Post - Reinstatement',
      to: '/cashiering/posting?kind=REINSTATEMENT',
      permissions: ['CASH_APPROVE', 'CASH_REINSTATE'],
    },
    { label: 'Batch Print AR/OR Facility', to: '/cashiering/print', permissions: ['CASH_PRINT'] },
    {
      label: 'Unapplied Payments Facility',
      to: '/cashiering/unapplied',
      permissions: ['CASH_DISPOSITION', 'CASH_APPROVE', 'CASH_APPLY'],
    },
    {
      label: 'Automatic Payment Processing Uploading Facility',
      to: '/cashiering/uploads',
      permissions: ['CASH_UPLOAD'],
    },
    {
      label: 'Reports Generation',
      to: '/reports',
      permissions: ['REPORT_VIEW', 'OPS_REPORT_VIEW'],
    },
  ];
