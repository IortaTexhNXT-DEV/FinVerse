import type { FiscalYear, Period, PeriodStatus } from '@/api/periods';
import { defaultPeriod, defaultYear } from './periodDefaults';

const year = (id: number, code: number, status: FiscalYear['status'] = 'OPEN'): FiscalYear => ({
  id,
  companyId: 1,
  yearCode: code,
  startDate: `${code}-01-01`,
  endDate: `${code}-12-31`,
  status,
});

const period = (no: number, status: PeriodStatus): Period => {
  const month = String(no).padStart(2, '0');
  return {
    id: no,
    fiscalYearId: 1,
    periodNo: no,
    name: `2026-${month}`,
    startDate: `2026-${month}-01`,
    endDate: `2026-${month}-28`,
    status,
  };
};

describe('period picker defaults', () => {
  // The API lists the newest year first: FY 2027 (all FUTURE) before the current FY 2026.
  const years = [year(3, 2027), year(2, 2026), year(1, 2025, 'CLOSED')];

  it('opens on the year containing today, not the newest year', () => {
    expect(defaultYear(years, '2026-09-23')?.yearCode).toBe(2026);
    expect(defaultYear(years, '2030-01-01')?.yearCode).toBe(2027);
    expect(defaultYear([year(1, 2025, 'CLOSED')], '2030-01-01')?.yearCode).toBe(2025);
    expect(defaultYear([], '2026-09-23')).toBeUndefined();
  });

  it('opens on the period containing today unless it is still FUTURE', () => {
    const periods = [period(8, 'CLOSED'), period(9, 'OPEN'), period(10, 'FUTURE')];
    expect(defaultPeriod(periods, '2026-09-23')?.name).toBe('2026-09');
    expect(defaultPeriod(periods, '2026-10-05')?.name).toBe('2026-09');
    expect(defaultPeriod([period(1, 'FUTURE')], '2026-09-23')?.name).toBe('2026-01');
  });
});
