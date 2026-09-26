import { showsInsurerWidgets } from './insurerWidgets';

describe('insurer dashboard widgets', () => {
  it('are hidden for every BDOI role (no insurer-only permission)', () => {
    const bdoi = new Set(['DASHBOARD_VIEW', 'WORK_VIEW', 'JOURNAL_VIEW', 'REPORT_FINANCIAL']);
    expect(showsInsurerWidgets((p) => bdoi.has(p))).toBe(false);
  });

  it('are kept only with an insurer-only permission', () => {
    expect(showsInsurerWidgets((p) => p === 'POLICY_VIEW')).toBe(true);
    expect(showsInsurerWidgets((p) => p === 'CLAIM_VIEW')).toBe(true);
  });
});
