import type { PmDashboardCounts, PmDrillRow, PmKpi } from '@/api/pmWorkspace';
import type { ComboOption } from '@/components/ui/comboOptions';

/** The KPIs of BDOI's FRS FRPM.001.01 with the count each one shows. */
export const KPI_COUNTS: Readonly<Record<PmKpi, keyof PmDashboardCounts>> = {
  INCOMING: 'incoming',
  IN_PROGRESS: 'inProgress',
  FOR_APPROVAL: 'forApproval',
  EXPIRING: 'expiring',
  ISSUED: 'issued',
  DEACTIVATION: 'deactivation',
};

/** What each KPI counts, in BDOI's words (tile tooltip). */
export const KPI_HINTS: Readonly<Record<PmKpi, string>> = {
  INCOMING:
    'Package requests and quotation requests for non-package products received in the period',
  IN_PROGRESS: 'Requests currently being worked on by TSU',
  FOR_APPROVAL: 'Requests pending approval',
  EXPIRING: 'Packages approaching their expiry date',
  ISSUED: 'Proposals generated and released to Marketing in the period',
  DEACTIVATION: 'Package deactivation requests awaiting approval',
};

/** The Package Type filter. */
export const PACKAGE_TYPES: readonly ComboOption[] = [
  { value: 'PACKAGE', label: 'Package' },
  { value: 'NON_PACKAGE', label: 'Non-Package' },
];

/**
 * The Package Type filter value as the API flag.
 *
 * @param value PACKAGE, NON_PACKAGE or empty
 * @returns true, false or undefined
 */
export function packagedFlag(value: string): boolean | undefined {
  if (value === 'PACKAGE') return true;
  if (value === 'NON_PACKAGE') return false;
  return undefined;
}

/**
 * The screen of the record behind a drill-down row.
 *
 * @param row drill-down row
 * @returns route
 */
export function drillLink(row: PmDrillRow): string {
  switch (row.kind) {
    case 'PACKAGE':
      return `/product-maintenance/requests/${row.id}`;
    case 'QUOTATION':
      return `/proposals/${row.id}`;
    case 'DEACTIVATION':
      return `/product-maintenance/deactivations?open=${row.id}`;
    default:
      return `/catalog/products/${row.requestNo}`;
  }
}

/**
 * Aging in words: days in the current stage, or the days left for an expiring package.
 *
 * @param row drill-down row
 * @returns text
 */
export function agingText(row: PmDrillRow): string {
  if (row.agingDays === null) {
    return '';
  }
  return row.agingDays === 1 ? '1 day' : `${row.agingDays} days`;
}
