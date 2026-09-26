import { describe, expect, it } from 'vitest';
import type { DispositionRule } from './api';
import {
  agingBars,
  agingSegments,
  bracketStart,
  cleanDetails,
  detailFields,
  dispositionErrors,
  filtersFromSearch,
  handoffText,
  reportTitle,
  tabFromSearch,
  worklistQuery,
} from './collectionsLogic';

const rule = (opsAction: DispositionRule['opsAction']): DispositionRule => ({
  code: 'X',
  label: 'X',
  opsAction,
  allowedRoles: [],
});

describe('worklist query', () => {
  it('builds the status, search and filters', () => {
    const q = worklistQuery('OPEN', ' BI-1 ', { segment: 'CBG', mine: true, amountTo: ' ' });
    expect(q).toBe('status=OPEN&q=BI-1&segment=CBG&mine=true');
  });

  it('lists every status on the All tab and skips false flags', () => {
    const q = worklistQuery('ALL', '', { unassigned: false });
    expect(q.match(/status=/g)).toHaveLength(4);
    expect(q).not.toContain('unassigned');
  });

  it('reads the tile links', () => {
    const s = new URLSearchParams('mine=true&unassigned=true&disposition=DP_RETURNED&tab=CREDIT');
    expect(filtersFromSearch(s)).toEqual({
      mine: true,
      unassigned: true,
      disposition: 'DP_RETURNED',
    });
    expect(tabFromSearch(s)).toBe('CREDIT');
    expect(tabFromSearch(new URLSearchParams('tab=NOPE'))).toBe('OPEN');
    expect(filtersFromSearch(new URLSearchParams(''))).toEqual({});
  });
});

describe('disposition details', () => {
  it('asks the pick-up and 2307 details only', () => {
    expect(detailFields('CHECK_PICKUP').map((f) => f.key)).toContain('pickupDate');
    expect(detailFields('CWT2307_REVERSAL').map((f) => f.key)).toContain('certificateNo');
    expect(detailFields('DP_REVERSAL')).toEqual([]);
    expect(detailFields(undefined)).toEqual([]);
  });

  it('explains the hand-off', () => {
    expect(handoffText(rule('CHECK_PICKUP'))).toContain('pick-up');
    expect(handoffText(rule('DP_REVERSAL'))).toContain('Commission');
    expect(handoffText(rule('CWT2307_REVERSAL'))).toContain('Cashiering');
    expect(handoffText(rule('CANCEL_REQUEST'))).toContain('Adjustment');
    expect(handoffText(rule('NONE'))).toBeUndefined();
  });

  it('validates the fields', () => {
    expect(dispositionErrors('', undefined, {}, '2026-09-25')).toEqual({
      code: 'Choose a disposition',
    });
    const pickup = dispositionErrors(
      'P',
      rule('CHECK_PICKUP'),
      { pickupDate: '2026-09-01' },
      '2026-09-25',
    );
    expect(pickup.pickupDate).toContain('past');
    expect(pickup.pickupAddress).toBeDefined();
    expect(pickup.amount).toBeDefined();
    expect(
      dispositionErrors('C', rule('CWT2307_REVERSAL'), { path: 'certificate' }, '2026-09-25'),
    ).toEqual({ certificateNo: 'A certificate tag needs the certificate number' });
    expect(dispositionErrors('C', rule('CWT2307_REVERSAL'), { path: 'X' }, '2026-09-25')).toEqual({
      path: 'Enter CASH or CERTIFICATE',
    });
    expect(dispositionErrors('N', rule('NONE'), {}, '2026-09-25')).toEqual({});
  });

  it('keeps the filled details only', () => {
    expect(
      cleanDetails(rule('CWT2307_REVERSAL'), { path: ' cash ', certificateNo: '', other: 'x' }),
    ).toEqual({ path: 'CASH' });
  });
});

describe('aging chart', () => {
  const cells = [
    { segment: 'CBG', bracket: '31-45', items: 2, netOutstanding: 300 },
    { segment: 'CBG', bracket: '0-30', items: 1, netOutstanding: 600 },
    { segment: 'CORPORATE', bracket: '121+', items: 1, netOutstanding: 150 },
    { bracket: '0-30', items: 1, netOutstanding: 100 },
  ];

  it('orders the brackets and scales the bars', () => {
    const all = agingBars(cells, '');
    expect(all.map((b) => b.bracket)).toEqual(['0-30', '31-45', '121+']);
    expect(all[0]).toEqual({ bracket: '0-30', items: 2, amount: 700, share: 1 });
    expect(agingBars(cells, 'CBG')).toHaveLength(2);
    expect(agingBars([], '')).toEqual([]);
  });

  it('lists the segments', () => {
    expect(agingSegments(cells)).toEqual(['CBG', 'CORPORATE', 'Unclassified']);
    expect(bracketStart(undefined)).toBe(Number.MAX_SAFE_INTEGER);
  });

  it('names the reports', () => {
    expect(reportTitle('CLX-DP-FOR-REVERSAL')).toBe('DP PR for Reversal');
    expect(reportTitle('OTHER')).toBe('OTHER');
  });
});
