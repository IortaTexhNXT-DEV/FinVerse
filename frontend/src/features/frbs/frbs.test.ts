import { describe, expect, it } from 'vitest';
import type { PackEntry, Schedule, ScheduleValues, ServiceFeeLine } from './api';
import {
  emptySchedule,
  entryLink,
  groupByFamily,
  groupPack,
  packFormats,
  periodOf,
  quickParams,
  runnerErrors,
  scheduleErrors,
  scheduleParams,
} from './schedules';
import {
  lineActions,
  periodErrors,
  ruleErrors,
  tabOf,
  tagProgress,
  totalsByCurrency,
} from './serviceFee';

const line = (
  over: Partial<ServiceFeeLine> & { fee?: number; currency?: string },
): ServiceFeeLine => ({
  id: 1,
  lineNo: 1,
  segment: 'CBG',
  salesUnit: 'U1',
  payeeCode: 'U1',
  payeeName: 'Unit',
  status: 'SENT',
  amounts: {
    currency: over.currency ?? 'PHP',
    rate: 2.5,
    invoiceCount: 2,
    commission: 1000,
    wtax: 100,
    base: 900,
    fee: over.fee ?? 22.5,
  },
  payout: { sendCount: 1 },
  tags: {},
  ...over,
});

const schedule = (code: string, family: ScheduleValues['family']): Schedule => ({
  code,
  values: { ...emptySchedule(), name: code, family, accountSelector: '1210' },
  wordOutput: false,
  updatedAt: '2026-09-01T00:00:00Z',
});

const entry = (over: Partial<PackEntry>): PackEntry => ({
  groupCode: 'SCH',
  groupName: 'IV. Schedules',
  reportCode: 'FRBS-GAP',
  title: 'GAP',
  wordRequested: false,
  available: true,
  ...over,
});

describe('service fee', () => {
  it('reads the stage tab of the URL', () => {
    expect(tabOf('APPROVED')).toBe('APPROVED');
    expect(tabOf('NOPE')).toBe('ALL');
    expect(tabOf(null)).toBe('ALL');
  });

  it('validates the period of a run', () => {
    expect(periodErrors('', '', '2026-09-25')).toEqual({
      from: 'Enter the first day of the period',
      to: 'Enter the last day of the period',
    });
    expect(periodErrors('2026-09-01', '2026-09-30', '2026-09-25').to).toContain('after today');
    expect(periodErrors('2026-09-20', '2026-09-10', '2026-09-25').to).toContain('before it starts');
    expect(periodErrors('2026-09-01', '2026-09-25', '2026-09-25')).toEqual({});
  });

  it('offers the tags a line allows', () => {
    expect(lineActions(line({ status: 'SENT' }), 'APPROVED')).toEqual(['release']);
    expect(lineActions(line({ status: 'RELEASED' }), 'RELEASED')).toEqual(['liquidate']);
    expect(lineActions(line({ status: 'RETURNED' }), 'APPROVED')).toEqual(['resend']);
    expect(lineActions(line({ status: 'RETURNED' }), 'RELEASED')).toEqual([]);
    expect(lineActions(line({ status: 'LIQUIDATED' }), 'LIQUIDATED')).toEqual([]);
  });

  it('totals the lines per currency and counts the tags', () => {
    const lines = [
      line({ status: 'RELEASED' }),
      line({ id: 2, status: 'LIQUIDATED', currency: 'USD', fee: 10 }),
      line({ id: 3, status: 'SENT' }),
      line({ id: 4, status: 'COMPUTED', fee: 0 }),
    ];
    const totals = totalsByCurrency(lines);
    expect(totals.find((t) => t.currency === 'PHP')?.fee).toBeCloseTo(45);
    expect(totals.find((t) => t.currency === 'USD')?.count).toBe(2);
    expect(tagProgress(lines)).toEqual({ paid: 3, released: 2, liquidated: 1 });
  });

  it('validates a rate', () => {
    expect(
      ruleErrors({
        segment: '',
        marketSegments: [],
        rate: 0,
        netOfWtax: true,
        effectiveFrom: '2026-01-01',
        effectiveTo: '2025-12-31',
        active: true,
      }),
    ).toEqual({
      segment: 'Select the service-fee segment',
      marketSegments: 'List the market segments covered',
      rate: 'Enter a rate above 0 and up to 100',
      effectiveTo: 'Ends before it starts',
    });
    expect(
      ruleErrors({
        segment: 'CBG',
        marketSegments: ['CBG'],
        rate: 2.5,
        netOfWtax: true,
        effectiveFrom: '2026-01-01',
        active: true,
      }),
    ).toEqual({});
  });
});

describe('account schedules and report pack', () => {
  it('groups the schedules by family in Appendix A order', () => {
    const groups = groupByFamily([
      schedule('SCH-B', 'SCHEDULE'),
      schedule('GARD-A', 'GARD'),
      schedule('SCH-A', 'SCHEDULE'),
    ]);
    expect(groups.map((g) => g.family)).toEqual(['GARD', 'SCHEDULE']);
    expect(groups[1]?.schedules.map((s) => s.code)).toEqual(['SCH-A', 'SCH-B']);
  });

  it('builds the parameters of a run and its commentary month', () => {
    expect(scheduleParams(1, 'SCH-PR-PHP', '2026-09-25', '')).toEqual({
      companyId: '1',
      schedule: 'SCH-PR-PHP',
      asOf: '2026-09-25',
    });
    expect(scheduleParams(1, 'X', '2026-09-25', '2026-09-01').fromDate).toBe('2026-09-01');
    expect(periodOf('2026-09-25')).toBe('2026-09');
    expect(runnerErrors('', '', '')).toEqual({
      code: 'Select a schedule',
      asOf: 'Enter the as-of date',
    });
    expect(runnerErrors('X', '2026-09-01', '2026-09-10').from).toContain('after');
  });

  it('validates a schedule definition', () => {
    const v = emptySchedule();
    expect(Object.keys(scheduleErrors('x', v)).sort((a, b) => a.localeCompare(b))).toEqual([
      'accountSelector',
      'code',
      'name',
    ]);
    const ok: ScheduleValues = { ...v, name: 'T', accountSelector: '1210' };
    expect(scheduleErrors('SCH-T', ok)).toEqual({});
    expect(scheduleErrors('SCH-T', { ...ok, currency: 'PESO' }).currency).toBeDefined();
    expect(
      scheduleErrors('SCH-T', { ...ok, basis: 'MOVEMENT', ageingSlots: '30' }).ageingSlots,
    ).toContain('balance');
    expect(scheduleErrors('SCH-T', { ...ok, ageingSlots: '30;x' }).ageingSlots).toContain('commas');
    expect(scheduleErrors('SCH-T', { ...ok, columns: [] }).columns).toContain('at least');
    expect(
      scheduleErrors('SCH-T', {
        ...ok,
        columns: [
          { measure: 'CLOSING', label: 'a' },
          { measure: 'CLOSING', label: 'b' },
        ],
      }).columns,
    ).toContain('once');
    expect(
      scheduleErrors('SCH-T', { ...ok, columns: [{ measure: 'VARIANCE', label: 'v' }] }).columns,
    ).toContain('comparative');
  });

  it('groups the pack, links and exports its entries', () => {
    const groups = groupPack([
      entry({ groupCode: 'EOD', groupName: 'I', reportCode: 'FIN-TB-MAIN' }),
      entry({ reportCode: 'GL-SCHEDULE', scheduleCode: 'SCH-PR-PHP' }),
      entry({}),
    ]);
    expect(groups.map((g) => g.code)).toEqual(['EOD', 'SCH']);
    expect(groups[1]?.entries).toHaveLength(2);
    expect(entryLink(entry({ reportCode: 'GL-SCHEDULE', scheduleCode: 'SCH-A' }))).toBe(
      '/frbs/schedules?code=SCH-A',
    );
    expect(entryLink(entry({}))).toBe('/reports/FRBS-GAP');
    const params = quickParams(entry({ scheduleCode: 'SCH-A' }), 3, '2026-08-15');
    expect(params).toMatchObject({
      companyId: '3',
      from: '2026-08-01',
      to: '2026-08-15',
      year: '2026',
      month: '8',
      quarter: '3',
      schedule: 'SCH-A',
    });
  });

  it('exports board schedules to Word as well as Excel and PDF (client requirement 16)', () => {
    expect(packFormats(entry({ wordRequested: true }))).toEqual(['XLSX', 'PDF', 'DOCX']);
    expect(packFormats(entry({}))).toEqual(['XLSX', 'PDF']);
  });
});
