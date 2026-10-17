import type { DrillRow } from '@/api/renewalDashboard';
import type { SortState } from '@/components/ui/DataTable';

/** Business types of the pipeline and the closing ratio. */
export const CATEGORY_LABELS: Record<string, string> = {
  NEW: 'New',
  ORGANIC: 'Organic',
  SUBMITTED: 'Submitted',
  RENEWAL: 'Renewal',
  DEFERRED: 'Deferred',
  ALL: 'Total',
};

/** Stages of the pipeline. */
export const STAGE_LABELS: Record<string, string> = {
  FOR_SURVEY: 'For Survey',
  FOR_PROPOSAL: 'For Proposal',
  FOR_CONFIRM: 'For Confirm',
  FOR_BOOKING: 'For Booking',
  FOR_DELIVERY: 'For Delivery',
  TOTAL: 'Total',
};

/** Ageing buckets of the outstanding renewal accounts. */
export const AGEING_LABELS: Record<string, string> = {
  PRIOR: 'Prior Month',
  CURRENT: 'Current Month',
  PLUS1: '+1 Month',
  PLUS2: '+2 Months',
  TOTAL: 'Total',
};

/** The KPI cards of the Renewal dashboard. */
export const CARD_LABELS: Record<string, string> = {
  TOTAL_EXPIRING: 'Total Expiring',
  FOR_DISPOSITION: 'For Disposition',
  FOR_RENEWAL: 'For Renewal',
  FOR_QUOTATION_PROPOSAL: 'For Quotation / Proposal',
  NOT_FOR_RENEWAL: 'Not for Renewal',
  RENEWED: 'Renewed',
  UNRENEWED: 'Unrenewed',
  RETURNED: 'Returned Accounts',
  CANCELLED: 'Cancelled',
  ADJUSTMENTS: 'Adjustments',
  BOUNCED_CHECK: 'Bounced Check',
  UNAPPLIED_PAYMENT: 'Unapplied Payment',
  REFUND: 'Refund',
  EXPIRING_HOLD_COVER: 'Expiring Hold Cover',
  EXPIRING_30: 'Expiring within 30 days',
  EXPIRING_60: 'Expiring within 60 days',
  EXPIRING_90: 'Expiring within 90 days',
  EXPIRING_140: 'Expiring within 140 days',
};

/** A percentage with two decimals, or a dash when it cannot be computed. */
export function percent(value: number | null | undefined): string {
  return value === null || value === undefined ? '-' : `${value.toFixed(2)}%`;
}

/** The default period: January of this year to December of next year. */
export function defaultPeriod(today = new Date()): { from: string; to: string } {
  const year = today.getFullYear();
  return { from: `${year}-01-01`, to: `${year + 1}-12-31` };
}

/** The label of a persistency column: Current, -n Month or the year to date. */
export function persistencyLabel(key: string, label: string): string {
  if (key === 'M0') {
    return `Current (${label})`;
  }
  return key === 'YTD' ? label : `-${key.substring(1)} Month (${label})`;
}

/** A value of a drill row as text, for search and sort. */
export function cellText(row: DrillRow, key: string): string {
  const own = (row as unknown as Record<string, unknown>)[key];
  const value = own ?? row.extra[key];
  if (typeof value === 'string') {
    return value;
  }
  return typeof value === 'number' ? String(value) : '';
}

/** Rows sorted by a column, numbers by value and text alphabetically. */
export function sortRows(rows: DrillRow[], sort: SortState | undefined): DrillRow[] {
  if (sort === undefined) {
    return rows;
  }
  const dir = sort.direction === 'asc' ? 1 : -1;
  return [...rows].sort((a, b) => {
    const x = cellText(a, sort.key);
    const y = cellText(b, sort.key);
    const nx = Number(x);
    const ny = Number(y);
    if (x !== '' && y !== '' && !Number.isNaN(nx) && !Number.isNaN(ny)) {
      return (nx - ny) * dir;
    }
    return x.localeCompare(y) * dir;
  });
}

/** Rows whose text matches every word of the search, across all columns. */
export function searchRows(rows: DrillRow[], search: string): DrillRow[] {
  const words = search
    .toLowerCase()
    .split(/\s+/)
    .filter((w) => w !== '');
  if (words.length === 0) {
    return rows;
  }
  const keys = [
    'ref',
    'invoiceNo',
    'expiringInvoiceNo',
    'assured',
    'productLine',
    'riskCode',
    'status',
  ];
  return rows.filter((r) => {
    const all = [
      ...keys.map((k) => cellText(r, k)),
      r.assignedUser ?? '',
      r.insurerDisposition ?? '',
      r.insurerRemarks ?? '',
      ...Object.keys(r.extra).map((k) => cellText(r, k)),
    ]
      .join(' ')
      .toLowerCase();
    return words.every((w) => all.includes(w));
  });
}
