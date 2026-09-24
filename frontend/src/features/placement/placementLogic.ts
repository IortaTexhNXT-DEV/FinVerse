import type {
  MatchStatus,
  ReportLine,
  WorkbenchCounts,
  WorkbenchRow,
  WorkbenchTab,
} from '@/api/placement';

/** Route of the booking workbench that receives accounts sent "For Booking" (booking module). */
export const BOOKING_WORKBENCH = '/booking';

/** Workbench tabs in the BDOI Placement & Booking order. */
export const WORKBENCH_TABS: readonly { id: WorkbenchTab; label: string }[] = [
  { id: 'FOR_PLACEMENT', label: 'For Placement & Booking' },
  { id: 'BOOKED', label: 'Booked Account' },
  { id: 'AWAITING_PAYMENT', label: 'Awaiting Payment' },
  { id: 'RETURNED', label: 'Returned by Insurer' },
  { id: 'HOLD_COVER_EXPIRING', label: 'Hold Cover Expiring' },
  { id: 'CANCELLED', label: 'Cancelled Placement' },
];

/** A workbench tile: a figure that opens its tab. */
export interface Tile {
  label: string;
  value: number;
  tab: WorkbenchTab;
  alert?: boolean;
}

/** The five tiles of the Placement Workbench. */
export function tilesOf(counts: WorkbenchCounts | undefined): Tile[] {
  const c = counts ?? {
    awaitingPayment: 0,
    readyForPlacement: 0,
    placed: 0,
    returnedByInsurer: 0,
    holdCoverExpiring: 0,
    placementCancelled: 0,
    policyIssued: 0,
    booked: 0,
  };
  return [
    { label: 'Awaiting payment', value: c.awaitingPayment, tab: 'AWAITING_PAYMENT' },
    { label: 'Ready for placement', value: c.readyForPlacement, tab: 'FOR_PLACEMENT' },
    { label: 'Placed', value: c.placed, tab: 'FOR_PLACEMENT' },
    { label: 'Returned by insurer', value: c.returnedByInsurer, tab: 'RETURNED', alert: true },
    {
      label: 'Hold cover expiring',
      value: c.holdCoverExpiring,
      tab: 'HOLD_COVER_EXPIRING',
      alert: true,
    },
  ];
}

/** Bulk actions of the workbench. */
export type BulkAction = 'PLACE' | 'SEND' | 'BOOK' | 'CANCEL' | 'REACTIVATE';

const TAB_ACTIONS: Record<WorkbenchTab, BulkAction[]> = {
  FOR_PLACEMENT: ['PLACE', 'SEND', 'BOOK', 'CANCEL'],
  BOOKED: [],
  AWAITING_PAYMENT: [],
  RETURNED: ['CANCEL'],
  HOLD_COVER_EXPIRING: [],
  CANCELLED: ['REACTIVATE'],
};

/** The bulk actions offered on a tab. */
export function actionsOf(tab: WorkbenchTab): BulkAction[] {
  return TAB_ACTIONS[tab];
}

const ELIGIBLE: Record<BulkAction, (row: WorkbenchRow) => boolean> = {
  PLACE: (r) => r.status === 'READY_FOR_PLACEMENT',
  SEND: (r) => r.slipStatus === 'GENERATED',
  BOOK: (r) => r.status === 'POLICY_ISSUED',
  CANCEL: (r) => ['READY_FOR_PLACEMENT', 'PLACED', 'RETURNED_BY_INSURER'].includes(r.status),
  REACTIVATE: (r) => r.status === 'PLACEMENT_CANCELLED',
};

/**
 * The selected rows an action applies to, and whether the button is enabled: at least one row is
 * selected and every selected row is eligible.
 */
export function selectionFor(
  action: BulkAction,
  rows: WorkbenchRow[],
  selected: string[],
): { arns: string[]; enabled: boolean } {
  const chosen = rows.filter((r) => selected.includes(r.arn));
  return {
    arns: chosen.map((r) => r.arn),
    enabled: chosen.length > 0 && chosen.every(ELIGIBLE[action]),
  };
}

/** Link to the placement record of an account. */
export function placementLink(arn: string): string {
  return `/placement/accounts/${arn}`;
}

/** The route that hands accounts to the booking workbench. */
export function bookingLink(arns: string[]): string {
  return `${BOOKING_WORKBENCH}?arns=${encodeURIComponent(arns.join(','))}`;
}

/** Review tabs of a payment report, with the lines of each. */
export const REPORT_TABS: readonly { id: MatchStatus; label: string }[] = [
  { id: 'MATCHED', label: 'Matched' },
  { id: 'UNPAID', label: 'Unpaid' },
  { id: 'UNMATCHED', label: 'Unmatched' },
  { id: 'AMBIGUOUS', label: 'Ambiguous' },
];

/** Lines of a payment report with one match status. */
export function linesOf(lines: ReportLine[], status: MatchStatus): ReportLine[] {
  return lines.filter((l) => l.matchStatus === status);
}

/** Candidate ARNs of an ambiguous line. */
export function candidatesOf(line: ReportLine): string[] {
  return (line.candidates ?? '')
    .split(',')
    .map((c) => c.trim())
    .filter((c) => c.length > 0);
}
