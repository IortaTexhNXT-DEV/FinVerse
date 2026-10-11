import { fireEvent, render, screen } from '@testing-library/react';
import { lovApi } from '@/api/lov';
import { renewalApi } from '@/api/renewal';
import { exportLink, renewalDashboardApi } from '@/api/renewalDashboard';
import type { Drill, RenewalDashboard } from '@/api/renewalDashboard';
import BudgetPage from '../budget/BudgetPage';
import { annualTotals } from '../budget/budgetTotals';
import { renewalWrapper } from '../testWrapper';
import { defaultPeriod, percent, searchRows } from './dashboardFormat';
import ProcessingDashboardPage from './ProcessingDashboardPage';
import { processingColumns } from './processingColumns';
import RenewalDashboardPage from './RenewalDashboardPage';

const cell = (key: string, metric: string, count: number) => ({
  key,
  metric,
  count,
  premium: count * 1000,
  commission: count * 100,
});

const DASHBOARD: RenewalDashboard = {
  filter: { from: '2027-01-01', to: '2027-12-31', segment: null, officer: null },
  production: [
    {
      measure: 'PREMIUM',
      label: 'Basic Premium',
      actual: 8500000,
      budget: 10000000,
      percentVsBudget: 85,
      variance: -1500000,
      growth: 90,
      adjustments: 0,
    },
  ],
  pipeline: {
    rows: [
      {
        category: 'RENEWAL',
        cells: [cell('FOR_SURVEY', 'PIPELINE|RENEWAL|FOR_SURVEY', 2)],
        total: cell('TOTAL', 'PIPELINE|RENEWAL|ALL', 2),
      },
    ],
    total: {
      category: 'ALL',
      cells: [cell('FOR_SURVEY', 'PIPELINE|ALL|FOR_SURVEY', 2)],
      total: cell('TOTAL', 'PIPELINE|ALL|ALL', 2),
    },
  },
  ageing: [cell('PLUS1', 'AGEING|PLUS1', 1), cell('TOTAL', 'AGEING|ALL', 1)],
  closing: [{ category: 'RENEWAL', inProcess: 5, posted: 5, booked: 40, total: 50, ratio: 80 }],
  topDeals: [
    {
      accountNo: 'ARN-1',
      renewalRef: 'RNW-1',
      assured: 'Juan Dela Cruz',
      riskCode: 'MTR10',
      businessType: 'Renewal',
      team: 'T-CBG1',
      premium: 25000,
      commission: 2500,
    },
  ],
  topOptions: [50, 75, 100],
  persistency: [
    {
      key: 'M0',
      label: '2027-10',
      renewable: 100,
      renewed: 90,
      countRatio: 90,
      premiumRatio: 88.5,
      commissionRatio: 88.5,
    },
  ],
  productMix: [cell('MOTOR', 'MIX|MOTOR', 3), cell('TOTAL', 'MIX|ALL', 3)],
  insurer: [
    {
      group: 'RETAIL',
      totalRenewAsIs: 3,
      approved: 1,
      pending: 1,
      returned: 1,
      approvalRatio: 33.33,
    },
  ],
  insurerPending: [{ key: 'INS-MGIC', metric: 'INSURER_PENDING|INS-MGIC', count: 1 }],
  cards: [
    { key: 'TOTAL_EXPIRING', metric: 'CARD|TOTAL_EXPIRING', count: 12 },
    { key: 'FOR_DISPOSITION', metric: 'CARD|FOR_DISPOSITION', count: 4 },
  ],
  workload: [{ key: 'ao', metric: 'WORKLOAD|ao', count: 4 }],
};

const DRILL: Drill = {
  metric: 'CARD|FOR_DISPOSITION',
  columns: [{ key: 'returnReason', label: 'Return Reason', kind: 'TEXT' }],
  rows: [
    {
      ref: 'RNW-1',
      businessType: 'Renewal',
      arn: null,
      invoiceNo: null,
      expiringInvoiceNo: 'INV-1',
      assured: 'Juan Dela Cruz',
      productLine: 'Motor',
      riskCode: 'MTR10',
      premium: 25000,
      commission: 2500,
      status: 'In Process Renewal',
      assignedUser: 'ao',
      insurerDisposition: null,
      insurerRemarks: null,
      expiryDate: '2027-10-15',
      extra: { returnReason: 'For correction' },
    },
    {
      ref: 'RNW-2',
      businessType: 'Renewal',
      arn: null,
      invoiceNo: null,
      expiringInvoiceNo: 'INV-2',
      assured: 'Maria Santos',
      productLine: 'Fire',
      riskCode: 'FIR01',
      premium: 1000,
      commission: 100,
      status: 'Posted',
      assignedUser: null,
      insurerDisposition: 'Approved',
      insurerRemarks: null,
      expiryDate: '2027-11-15',
      extra: { returnReason: null },
    },
  ],
};

beforeEach(() => {
  vi.spyOn(renewalApi, 'officers').mockResolvedValue([{ username: 'ao', fullName: 'Ana Officer' }]);
  vi.spyOn(renewalApi, 'processingOfficers').mockResolvedValue([
    { username: 'proc', fullName: 'Pedro Processor' },
  ]);
  vi.spyOn(lovApi, 'options').mockResolvedValue([]);
});

afterEach(() => vi.restoreAllMocks());

describe('Renewal dashboard', () => {
  it('shows the budget figures and opens the accounts of a card', async () => {
    vi.spyOn(renewalDashboardApi, 'dashboard').mockResolvedValue(DASHBOARD);
    const drill = vi.spyOn(renewalDashboardApi, 'drill').mockResolvedValue(DRILL);
    render(renewalWrapper(new Set(['RNW_REPORT_VIEW']), 'mkttl')(<RenewalDashboardPage />));
    expect(await screen.findByText('85.00%')).toBeInTheDocument();
    expect(screen.getByText('80.00%')).toBeInTheDocument();
    expect(screen.getByText('Juan Dela Cruz')).toBeInTheDocument();
    fireEvent.click(screen.getByText('For Disposition'));
    expect(await screen.findByText('For correction')).toBeInTheDocument();
    expect(drill).toHaveBeenCalledWith(1, expect.anything(), 'CARD|FOR_DISPOSITION', 50);
    expect(screen.getByText('2 accounts')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Search the accounts'), { target: { value: 'santos' } });
    expect(screen.getByText('1 accounts')).toBeInTheDocument();
  });

  it('formats percentages and the default period', () => {
    expect(percent(85)).toBe('85.00%');
    expect(percent(null)).toBe('-');
    expect(defaultPeriod(new Date(2026, 9, 9))).toEqual({ from: '2026-01-01', to: '2027-12-31' });
    expect(searchRows(DRILL.rows, 'motor juan').map((r) => r.ref)).toEqual(['RNW-1']);
    expect(
      exportLink('RNW-DASHBOARD', {
        metric: 'CARD|RENEWED',
        from: '2027-01-01',
        segment: undefined,
      }),
    ).toBe('/reports/RNW-DASHBOARD?metric=CARD%7CRENEWED&from=2027-01-01');
  });
});

describe('Processing dashboard', () => {
  it('shows the cards of the combined view and the For Placement tabs', async () => {
    vi.spyOn(renewalDashboardApi, 'processing').mockResolvedValue({
      filter: { from: '2026-01-01', to: '2027-12-31' },
      cards: [
        { key: 'FOR_PLACEMENT', label: 'For Placement', count: 3, newBusiness: 2, renewal: 1 },
      ],
      workload: {},
    });
    const drill = vi.spyOn(renewalDashboardApi, 'processingDrill').mockResolvedValue([]);
    render(
      renewalWrapper(
        new Set(['RNW_PROCESS', 'RNW_PROCESS_ASSIGN']),
        'proctl',
      )(<ProcessingDashboardPage />),
    );
    expect(await screen.findByText('New Business 2 · Renewal 1')).toBeInTheDocument();
    fireEvent.click(screen.getByText('For Placement'));
    expect(await screen.findByRole('tab', { name: 'Assigned' })).toBeInTheDocument();
    expect(drill).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ businessType: 'ALL' }),
      'FOR_PLACEMENT',
      'UNASSIGNED',
    );
    expect(screen.getByText('Assign 0 selected')).toBeDisabled();
  });

  it('lists the columns of each drill-down', () => {
    const headers = (card: string) => processingColumns(card).map((c) => c.header);
    expect(headers('FOR_PLACEMENT')).toEqual(
      expect.arrayContaining(['Date Posted', 'Processor', 'Aging (Days)']),
    );
    expect(headers('WITH_POLICY')).toEqual(
      expect.arrayContaining(['Policy Status', 'TAT Status', 'Placement Issue Resolution Date']),
    );
    expect(headers('UNRELEASED')).toEqual(
      expect.arrayContaining(['Policy Transmittal Status', 'Reason for Reject']),
    );
    expect(headers('RETURNED')).toEqual(expect.arrayContaining(['Reason for Return']));
  });
});

describe('Renewal Annual Budget', () => {
  it('computes the annual totals of the months', () => {
    const months = Array.from({ length: 12 }, (_, i) => ({
      monthNo: i + 1,
      newAmount: 1,
      renewalAmount: 2,
      organicAmount: 3,
    }));
    expect(annualTotals(months)).toEqual({
      newTotal: 12,
      renewalTotal: 24,
      organicTotal: 36,
      grandTotal: 72,
    });
  });

  it('lists the budgets of the fiscal year with their totals and history', async () => {
    vi.spyOn(renewalDashboardApi, 'budgets').mockResolvedValue([
      {
        id: 7,
        fiscalYear: 2027,
        measure: 'PREMIUM',
        segment: 'RETAIL',
        region: 'NCR',
        team: null,
        subTeam: null,
        heads: { unitHead: null, sectionHead: null, teamHead: null, teamLead: 'mkttl' },
        accountOfficer: 'ao',
        months: [],
        totals: { newTotal: 100, renewalTotal: 200, organicTotal: 300, grandTotal: 600 },
        createdBy: 'badmin',
        createdAt: '2026-10-01T00:00:00Z',
        updatedBy: 'badmin',
        updatedAt: '2026-10-02T00:00:00Z',
      },
    ]);
    const history = vi.spyOn(renewalDashboardApi, 'budgetHistory').mockResolvedValue([
      {
        field: 'Renewal budget - March',
        previous: 4000000,
        updated: 4500000,
        modifiedBy: 'badmin',
        modifiedAt: '2026-10-02T00:00:00Z',
      },
    ]);
    render(renewalWrapper(new Set(['RNW_BUDGET']), 'badmin')(<BudgetPage />));
    expect(await screen.findByText('600.00')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Actions for 2027 RETAIL/ }));
    fireEvent.click(screen.getByText('View Budget History'));
    expect(await screen.findByText('Renewal budget - March')).toBeInTheDocument();
    expect(history).toHaveBeenCalledWith(1, 7);
  });
});
