import type { ReserveLine, SummaryRow } from '@/api/reserves';
import {
  changePct,
  formatFactor,
  monthEnd,
  reserveByLine,
  reserveLabel,
  runActions,
  summarizeLines,
  totalsByReserve,
} from './reserveMath';

const row = (reserve: string, line: string, gross: number, ri: number, prev = 0): SummaryRow => ({
  reserve,
  businessLine: line,
  gross,
  ri,
  net: gross - ri,
  previousGross: prev,
  previousRi: 0,
  previousNet: prev,
});

const line = (type: string, businessLine: string, gross: number, ri: number): ReserveLine => ({
  type,
  branchId: 1,
  branchCode: 'HO',
  businessLine,
  productCode: 'P',
  sourceType: 'DIRECT',
  gross,
  ri,
  net: gross - ri,
});

describe('reserve math', () => {
  it('totals the summary per reserve in display order', () => {
    const totals = totalsByReserve([
      row('IBNR', 'FIRE', 50, 10),
      row('UPR', 'FIRE', 1000, 300, 900),
      row('UPR', 'MOTOR', 500, 0, 400),
      row('ZZZ', 'FIRE', 1, 0),
    ]);
    expect(totals.map((t) => t.reserve)).toEqual(['UPR', 'IBNR', 'ZZZ']);
    expect(totals[0]).toMatchObject({ gross: 1500, ri: 300, net: 1200, previousNet: 1300 });
  });

  it('lists one reserve per line of business, largest first', () => {
    const rows = [row('UPR', 'FIRE', 100, 0), row('UPR', 'MOTOR', 300, 50), row('DAC', 'PA', 9, 0)];
    expect(reserveByLine(rows, 'UPR')).toEqual([
      { line: 'MOTOR', gross: 300, net: 250, previousNet: 0 },
      { line: 'FIRE', gross: 100, net: 100, previousNet: 0 },
    ]);
  });

  it('summarises run lines per type and line', () => {
    const s = summarizeLines([
      line('IBNR', 'FIRE', 10, 1),
      line('UPR', 'MOTOR', 200, 20),
      line('UPR', 'FIRE', 100, 10),
      line('UPR', 'FIRE', 50, 5),
    ]);
    expect(s.map((x) => x.key)).toEqual(['UPR|FIRE', 'UPR|MOTOR', 'IBNR|FIRE']);
    expect(s[0]).toMatchObject({ gross: 150, ri: 15, net: 135 });
  });

  it('offers life-cycle actions by status and permission', () => {
    const maker = (p: string) => p === 'RESERVE_PREPARE';
    const checker = (p: string) => p === 'PERIOD_END_RUN';
    expect(runActions('PREVIEW', maker)).toMatchObject({
      submit: true,
      recalculate: true,
      approve: false,
    });
    expect(runActions('PENDING_APPROVAL', maker).approve).toBe(false);
    expect(runActions('PENDING_APPROVAL', checker)).toMatchObject({ approve: true, reject: true });
    expect(runActions('APPROVED', checker).post).toBe(true);
    expect(runActions('POSTED', checker)).toMatchObject({ post: false, cancel: true });
    expect(runActions('CANCELLED', checker).cancel).toBe(false);
  });

  it('formats dates, factors, labels and changes', () => {
    expect(monthEnd('2026-02-10')).toBe('2026-02-28');
    expect(monthEnd('2028-02-01')).toBe('2028-02-29');
    expect(monthEnd('2026-12-31')).toBe('2026-12-31');
    expect(formatFactor(1.5)).toBe('1.500000');
    expect(formatFactor(undefined)).toBe('');
    expect(reserveLabel('UCR')).toContain('UCR');
    expect(reserveLabel('OTHER')).toBe('OTHER');
    expect(changePct(110, 100)).toBeCloseTo(10);
    expect(changePct(5, 0)).toBeUndefined();
  });
});
