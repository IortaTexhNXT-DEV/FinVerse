import { describe, expect, it } from 'vitest';
import type { ReportLine, WorkbenchRow } from '@/api/placement';
import {
  actionsOf,
  bookingLink,
  candidatesOf,
  linesOf,
  selectionFor,
  tilesOf,
  WORKBENCH_TABS,
} from './placementLogic';

const row = (arn: string, status: string, slipStatus?: string): WorkbenchRow => ({
  accountId: 1,
  arn,
  clientId: 1,
  clientCode: 'CL-1',
  clientName: 'Client',
  status,
  productCode: 'PAR01',
  lineCode: 'PROPERTY',
  directPayment: false,
  paymentStatus: 'PAID',
  slipStatus,
});

const line = (
  id: number,
  matchStatus: ReportLine['matchStatus'],
  candidates?: string,
): ReportLine => ({
  id,
  rowNo: id + 1,
  reference: `R${id}`,
  paid: true,
  matchStatus,
  manuallyMatched: false,
  applied: false,
  candidates,
});

describe('placement workbench logic', () => {
  it('lists the BDOI tabs with For Placement & Booking first', () => {
    expect(WORKBENCH_TABS[0]?.label).toBe('For Placement & Booking');
    expect(WORKBENCH_TABS.map((t) => t.id)).toContain('HOLD_COVER_EXPIRING');
  });

  it('builds the five tiles, empty counts included', () => {
    expect(tilesOf(undefined).map((t) => t.value)).toEqual([0, 0, 0, 0, 0]);
    const tiles = tilesOf({
      awaitingPayment: 1,
      readyForPlacement: 2,
      placed: 3,
      returnedByInsurer: 4,
      holdCoverExpiring: 5,
      placementCancelled: 6,
      policyIssued: 7,
      booked: 8,
    });
    expect(tiles.map((t) => t.value)).toEqual([1, 2, 3, 4, 5]);
    expect(tiles[3]?.alert).toBe(true);
  });

  it('offers the bulk actions of each tab', () => {
    expect(actionsOf('FOR_PLACEMENT')).toEqual(['PLACE', 'SEND', 'BOOK', 'CANCEL']);
    expect(actionsOf('CANCELLED')).toEqual(['REACTIVATE']);
    expect(actionsOf('BOOKED')).toEqual([]);
  });

  it('enables an action only when every selected row is eligible', () => {
    const rows = [
      row('A', 'READY_FOR_PLACEMENT'),
      row('B', 'POLICY_ISSUED'),
      row('C', 'PLACED', 'GENERATED'),
    ];
    expect(selectionFor('PLACE', rows, [])).toEqual({ arns: [], enabled: false });
    expect(selectionFor('PLACE', rows, ['A'])).toEqual({ arns: ['A'], enabled: true });
    expect(selectionFor('PLACE', rows, ['A', 'B']).enabled).toBe(false);
    expect(selectionFor('BOOK', rows, ['B']).enabled).toBe(true);
    expect(selectionFor('SEND', rows, ['C']).enabled).toBe(true);
    expect(selectionFor('CANCEL', rows, ['A', 'C']).enabled).toBe(true);
    expect(selectionFor('REACTIVATE', rows, ['A']).enabled).toBe(false);
  });

  it('hands accounts to booking and reads report lines', () => {
    expect(bookingLink(['ARN-1', 'ARN-2'])).toBe('/booking?arns=ARN-1%2CARN-2');
    const lines = [line(1, 'MATCHED'), line(2, 'AMBIGUOUS', 'ARN-1, ARN-2'), line(3, 'UNMATCHED')];
    expect(linesOf(lines, 'MATCHED')).toHaveLength(1);
    expect(candidatesOf(lines[1]!)).toEqual(['ARN-1', 'ARN-2']);
    expect(candidatesOf(lines[2]!)).toEqual([]);
  });
});
