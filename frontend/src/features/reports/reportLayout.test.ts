import type { MisField } from '@/api/misFields';
import type { ReportResult } from '@/api/reports';
import {
  NO_LAYOUT,
  groupOptions,
  inactiveWarnings,
  layoutParams,
  splitLayout,
  valueOptions,
} from './reportLayout';

const RESULT: ReportResult = {
  code: 'NB-PREMIUM',
  title: 'Premium Register',
  parameterEcho: [],
  columns: [
    { key: 'policy_no', label: 'Policy No.', type: 'TEXT', summed: false },
    { key: 'insurer_code', label: 'Insurer Code', type: 'TEXT', summed: false },
    { key: 'premium', label: 'Premium', type: 'AMOUNT', summed: true },
  ],
  rows: [],
  notes: [],
};

const INSURER: MisField = {
  id: 1,
  code: 'INSURER',
  label: 'Insurer',
  entity: 'INSURER',
  source: 'INSURER',
  columnKeys: ['insurer', 'insurer_code'],
  lovType: null,
  recordStatus: 'ACTIVE',
  maker: 'SYSTEM',
  authorizedBy: 'SYSTEM',
  authorizedAt: null,
};

describe('report layout', () => {
  it('travels with the parameters and comes back from a variant', () => {
    const params = layoutParams({
      group1: 'insurer_code',
      group2: 'policy_no',
      chart: 'BAR',
      chartValue: 'premium',
    });
    expect(params).toEqual({
      'layout.group1': 'insurer_code',
      'layout.group2': 'policy_no',
      'layout.chart': 'BAR',
      'layout.chartValue': 'premium',
    });
    expect(layoutParams(NO_LAYOUT)).toEqual({});
    const split = splitLayout({ companyId: '1', ...params });
    expect(split.values).toEqual({ companyId: '1' });
    expect(split.layout.chart).toBe('BAR');
  });

  it('offers the MIS fields first with their label, and the totalled amounts for the chart', () => {
    expect(groupOptions(RESULT, [INSURER])).toEqual([
      { value: 'insurer_code', label: 'Insurer (MIS field)' },
      { value: 'policy_no', label: 'Policy No.' },
    ]);
    expect(valueOptions(RESULT).map((o) => o.value)).toEqual(['COUNT', 'premium']);
  });

  it('warns of a deactivated MIS field', () => {
    const layout = { ...NO_LAYOUT, group1: 'insurer_code' };
    expect(inactiveWarnings(layout, [INSURER])).toEqual([]);
    expect(inactiveWarnings(layout, [{ ...INSURER, recordStatus: 'INACTIVE' }])).toHaveLength(1);
  });
});
