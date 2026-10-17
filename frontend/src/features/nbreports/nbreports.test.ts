import type { CatalogueEntry } from '@/api/reports';
import type { NbDashboard, StatusCount } from '@/api/nbReports';
import {
  accountsPath,
  barWidth,
  funnelPath,
  funnelShare,
  groupName,
  monthStartOf,
  overdueBreakdown,
  overdueQualifier,
  overdueQueuePath,
  requestPath,
  requestsBreakdown,
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

  it('breaks the overdue items down by business name in the right number, each opening its queue', () => {
    expect(overdueQualifier([])).toBe('Every item is within its service level');
    expect(overdueBreakdown([])).toEqual([]);
    const lines = overdueBreakdown([
      count('BCL_CLAIM', 'BCL_CLAIM', 1, 'BCL_CLAIM'),
      count('FRBS_SERVICE_FEE', 'FRBS_SERVICE_FEE', 1, 'FRBS_SERVICE_FEE'),
      count('NB_ACCOUNT', 'NB_ACCOUNT', 31, 'Accounts'),
      count('DISB_VOUCHER', 'DISB_VOUCHER', 4, 'DISB_VOUCHER'),
      count('DISB_FUNDING', 'DISB_FUNDING', 1, 'DISB_FUNDING'),
    ]);
    expect(lines.map((l) => `${String(l.count)} ${l.label}`)).toEqual([
      '1 Claim',
      '1 Service fee run',
      '31 Accounts',
      '4 Disbursement vouchers',
      '1 Account funding',
    ]);
    expect(lines.every((l) => !l.label.includes('_'))).toBe(true);
    expect(lines[2]?.to).toBe('/my-work?workflow=NB_ACCOUNT&overdue=true');
    expect(overdueQueuePath()).toBe('/my-work?overdue=true');
    expect(overdueQualifier([count('W', 'NB_ACCOUNT', 3)])).toBe('Past their service level');
  });

  it('lists the quotations and proposal requests in progress as separate lines', () => {
    const d = {
      requests: [count('QUOTATION', 'DRAFT', 2), count('PROPOSAL', 'DRAFT', 1)],
    } as NbDashboard;
    expect(requestsBreakdown(d).map((l) => `${l.label}: ${String(l.count)}`)).toEqual([
      'Quotations in progress: 2',
      'Proposal request in progress: 1',
    ]);
    expect(groupName('REQUEST')).toBe('Quotation Requests');
    expect(groupName('PROPOSAL')).toBe('Proposal Requests');
    expect(groupName('QUOTATION')).toBe('Quotations');
    expect(monthStartOf('2026-10-08')).toBe('01-Oct-2026');
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
