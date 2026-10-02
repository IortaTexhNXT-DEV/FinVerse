import type { CatalogueEntry } from '@/api/reports';
import type { NbDashboard, StatusCount } from '@/api/nbReports';
import {
  accountsPath,
  barWidth,
  funnelPath,
  funnelShare,
  groupName,
  overdueHint,
  requestPath,
  requestsHint,
  total,
} from './dashboardData';
import { groupReports } from './reportGroups';
import { emptyTarget, formOf, monthRange, targetErrors, toTarget } from './targetForm';

const count = (group: string, code: string, n: number, label = code): StatusCount => ({
  group,
  code,
  label,
  count: n,
});

describe('NB dashboard data', () => {
  it('builds the drill-down routes', () => {
    expect(accountsPath('POLICY_ISSUED')).toBe('/accounts?status=POLICY_ISSUED');
    expect(requestPath(count('REQUEST', 'NEW', 1))).toBe('/quotations/requests');
    expect(requestPath(count('PROPOSAL', 'WITH_TSU', 1))).toBe('/proposals');
    expect(requestPath(count('QUOTATION', 'APPROVED', 1))).toBe('/quotations?tab=review');
    expect(requestPath(count('QUOTATION', 'UNKNOWN', 1))).toBe('/quotations?tab=drafts');
    expect(funnelPath('booked')).toBe('/booking?tab=BOOKED');
    expect(funnelPath('placed')).toBe('/accounts?status=PLACED');
    expect(funnelPath('other')).toBe('/accounts');
  });

  it('computes totals, shares and bar widths', () => {
    const funnel = [
      count('F', 'quoted', 40),
      count('F', 'sent', 10),
      count('F', 'accounts', 20),
      count('F', 'booked', 5),
    ];
    expect(total(funnel)).toBe(75);
    expect(funnelShare(funnel, 'sent')).toBe(25);
    expect(funnelShare(funnel, 'booked')).toBe(25);
    expect(funnelShare([], 'sent')).toBe(0);
    expect(barWidth(0, 10)).toBe(0);
    expect(barWidth(1, 1000)).toBe(2);
    expect(barWidth(5, 10)).toBe(50);
    expect(barWidth(5, 0)).toBe(0);
  });

  it('summarises overdue items and requests', () => {
    expect(overdueHint([])).toBe('Every item is within its service level');
    expect(overdueHint([count('W', 'NB_ACCOUNT', 3, 'Accounts')])).toBe('3 Accounts');
    const d = {
      requests: [count('QUOTATION', 'DRAFT', 2), count('PROPOSAL', 'DRAFT', 1)],
    } as NbDashboard;
    expect(requestsHint(d)).toBe('2 quotations · 1 PRFs in progress');
    expect(groupName('REQUEST')).toBe('Request');
    expect(groupName('QUOTATION')).toBe('Quotation');
  });
});

describe('NB report groups', () => {
  const entry = (code: string, category = 'NEW_BUSINESS'): CatalogueEntry => ({
    code,
    title: code,
    category,
    categoryLabel: category,
    description: '',
    parameters: [],
  });

  it('groups the NB reports by process step and keeps unknown ones', () => {
    const groups = groupReports([
      entry('NB-PRODUCTION'),
      entry('NB-ACC-STATUS'),
      entry('NB-NEW'),
      entry('GL-TB', 'GENERAL_LEDGER'),
    ]);
    expect(groups.map((g) => g.title)).toEqual([
      'Accounts and Workflow',
      'Booking and Production',
      'Other New Business Reports',
    ]);
    expect(groups.flatMap((g) => g.reports.map((r) => r.code))).not.toContain('GL-TB');
  });
});

describe('production target form', () => {
  it('covers whole months', () => {
    expect(monthRange('2026-02')).toEqual({ from: '2026-02-01', to: '2026-02-28' });
    expect(monthRange('2028-12')).toEqual({ from: '2028-12-01', to: '2028-12-31' });
  });

  it('validates and converts the form', () => {
    const empty = emptyTarget('TEAM', '2026-09');
    expect(Object.keys(targetErrors(empty))).toEqual(['unitCode']);
    const bad = {
      ...empty,
      unitCode: 'T1',
      periodTo: '2026-08-01',
      targetCount: '1.5',
      targetPremium: '-1',
      targetCommission: 'x',
    };
    expect(Object.keys(targetErrors(bad)).sort((x, y) => x.localeCompare(y))).toEqual([
      'periodTo',
      'targetCommission',
      'targetCount',
      'targetPremium',
    ]);
    expect(targetErrors({ ...empty, periodFrom: '' }).periodTo).toBe('Enter the period');
    const good = { ...empty, unitCode: ' T1 ', targetCount: '4', targetPremium: '1000' };
    expect(targetErrors(good)).toEqual({});
    const target = toTarget(good);
    expect(target).toMatchObject({ unitCode: 'T1', targetCount: 4, targetPremium: 1000 });
    expect(formOf({ ...target, targetCommission: 2.5 }).targetCommission).toBe('2.50');
  });
});
