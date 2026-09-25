import type { ReportResult } from '@/api/reports';
import { filterPairs, filterRows } from './reportOptions';

const result: ReportResult = {
  code: 'T',
  title: 'Test',
  parameterEcho: [],
  columns: [
    { key: 'code', label: 'Code', type: 'TEXT' },
    { key: 'amount', label: 'Amount', type: 'AMOUNT' },
  ],
  rows: [
    { kind: 'GROUP_HEADER', level: 0, label: 'Assets', cells: {} },
    { kind: 'DETAIL', level: 1, cells: { code: '1111', amount: 1500 } },
    { kind: 'DETAIL', level: 1, cells: { code: '4100', amount: 20 } },
    { kind: 'TOTAL', level: 0, label: 'Total', cells: { amount: 1520 } },
  ],
  notes: [],
} as unknown as ReportResult;

describe('report column filters (FRBS 2.4.4)', () => {
  it('keeps every row without a filter', () => {
    expect(filterRows(result, {})).toHaveLength(4);
    expect(filterRows(result, { code: '  ' })).toHaveLength(4);
  });

  it('keeps the matching detail rows only', () => {
    expect(filterRows(result, { code: '11' }).map((r) => r.cells.code)).toEqual(['1111']);
    expect(filterRows(result, { code: '1', amount: '20' }).map((r) => r.cells.code)).toEqual([
      '4100',
    ]);
  });

  it('turns filters into query pairs', () => {
    expect(filterPairs({ code: ' 11 ', name: '' })).toEqual(['code:11']);
  });
});
