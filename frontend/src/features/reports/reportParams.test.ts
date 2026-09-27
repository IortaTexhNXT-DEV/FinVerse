import type { ParameterSpec } from '@/api/reports';
import { dateRangeError } from '@/utils/dateRange';
import { initialValue, parameterErrors, rangePartner } from './reportParams';

function spec(
  name: string,
  label: string,
  type: ParameterSpec['type'],
  required = false,
  defaultValue?: string,
): ParameterSpec {
  return { name, label, type, required, options: [], defaultValue };
}

const TB = [
  spec('companyId', 'Company', 'COMPANY', true),
  spec('branchId', 'Branch', 'BRANCH'),
  spec('asOfDate', 'As of Date', 'DATE', true, 'TODAY'),
  spec('zeroBalances', 'Include zero balances', 'BOOLEAN'),
];

const PL = [
  spec('fromDate', 'From Date', 'DATE', true, 'YEAR_START'),
  spec('toDate', 'To Date', 'DATE', true, 'TODAY'),
  spec('uwYearFrom', 'UW Year From', 'NUMBER', true),
  spec('uwYearTo', 'UW Year To', 'NUMBER', true),
  spec('partyFrom', 'Party Code From', 'TEXT'),
  spec('partyTo', 'Party Code To', 'TEXT'),
];

describe('date ranges', () => {
  it('rejects a To date before the From date and accepts equal or open ranges', () => {
    expect(dateRangeError('2026-09-30', '2026-09-01')).toBe('To date must not be before From date');
    expect(dateRangeError('2026-09-01', '2026-09-01')).toBeUndefined();
    expect(dateRangeError('2026-09-01', '')).toBeUndefined();
    expect(dateRangeError(undefined, '2026-01-01')).toBeUndefined();
  });
});

describe('report parameter form', () => {
  it('requires a required parameter the user cleared instead of running with a default', () => {
    expect(parameterErrors(TB, {})).toEqual({});
    expect(parameterErrors(TB, { asOfDate: '' })).toEqual({ asOfDate: 'As of Date is required' });
    expect(parameterErrors(TB, { asOfDate: '  ', branchId: '' })).toEqual({
      asOfDate: 'As of Date is required',
    });
  });

  it('rejects reversed date and number ranges on the To field', () => {
    expect(
      parameterErrors(PL, {
        fromDate: '2026-09-01',
        toDate: '2026-01-31',
        uwYearFrom: '2026',
        uwYearTo: '2025',
        partyFrom: 'Z',
        partyTo: 'A',
      }),
    ).toEqual({
      toDate: 'To Date must not be before From Date',
      uwYearTo: 'UW Year To must not be before UW Year From',
    });
    expect(
      parameterErrors(PL, { fromDate: '2026-01-01', uwYearFrom: '2025', uwYearTo: '2026' }),
    ).toEqual({});
  });

  it('pairs parameters by the server naming convention and resolves date keywords', () => {
    expect(rangePartner('fromDate')).toBe('toDate');
    expect(rangePartner('expiryFrom')).toBe('expiryTo');
    expect(rangePartner('From')).toBeUndefined();
    expect(rangePartner('asOfDate')).toBeUndefined();
    expect(initialValue(spec('d', 'D', 'DATE', true, 'MONTH_START'))).toMatch(/^\d{4}-\d{2}-01$/);
    expect(initialValue(spec('d', 'D', 'DATE', true, 'YEAR_START'))).toMatch(/^\d{4}-01-01$/);
    expect(initialValue(spec('s', 'S', 'SELECT', true, 'SUMMARY'))).toBe('SUMMARY');
    expect(initialValue(spec('t', 'T', 'TEXT'))).toBe('');
  });
});
