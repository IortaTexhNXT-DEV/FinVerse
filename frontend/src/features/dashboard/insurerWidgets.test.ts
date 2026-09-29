import { showsInsurerWidgets, showsWidget } from './insurerWidgets';

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

describe('dashboard widgets of product modules', () => {
  const off = new Set(['UNDERWRITING', 'BUDGET']);
  const can = (p: string) => p.startsWith('MODULE_OFF:') && off.has(p.slice('MODULE_OFF:'.length));

  it('are hidden when their module is switched off', () => {
    expect(showsWidget('premium', can)).toBe(false);
    expect(showsWidget('budget', can)).toBe(false);
  });

  it('are shown when their module is in use', () => {
    expect(showsWidget('claims', can)).toBe(true);
    expect(showsWidget('payables', can)).toBe(true);
  });
});
