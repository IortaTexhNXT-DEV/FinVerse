import { render, screen } from '@testing-library/react';
import type { ClaimMovement, FacPlacement, Soa, SoaLayout } from '@/api/reinsurance';
import { previewTotals, sharesByLayer } from './allocation';
import { ageInDays, facActions, placedShare } from './fac';
import { previousQuarter, quarterOf, soaActions, soaReportParams, statementRows } from './soa';
import { SoaStatement } from './SoaStatement';

const layout: SoaLayout = {
  lines: [
    { label: 'Premium ceded', income: 1000, outgo: 0 },
    { label: 'Commission', income: 0, outgo: 300 },
  ],
  incomeSubtotal: 1000,
  outgoSubtotal: 300,
  balance: 700,
  balanceLabel: 'Balance due to reinsurer',
  balanceOnIncome: false,
  total: 1000,
  amountInWords: 'Philippine Peso Seven Hundred and 00/100 only',
};

const soa: Soa = {
  id: 1,
  soaNo: 'SOA-FVI-2026-000001',
  treatyCode: 'FIRE-QS-26',
  treatyName: 'Fire Quota Share',
  treatyType: 'QUOTA_SHARE',
  businessLine: 'FIRE',
  reinsurerCode: 'R-0001',
  reinsurerName: 'National Re',
  year: 2026,
  quarter: 1,
  periodFrom: '2026-01-01',
  periodTo: '2026-03-31',
  statementDate: '2026-04-15',
  currency: 'PHP',
  balance: 700,
  status: 'PENDING_APPROVAL',
  layout,
  preparedBy: 'reinsurer',
};

describe('statement of account', () => {
  it('computes quarters', () => {
    expect(quarterOf('2026-05-10')).toBe(2);
    expect(previousQuarter('2026-02-01')).toEqual({ year: 2025, quarter: 4 });
    expect(previousQuarter('2026-09-23')).toEqual({ year: 2026, quarter: 2 });
  });

  it('places the balance on the smaller side', () => {
    const rows = statementRows(layout);
    expect(rows[0]).toMatchObject({ label: 'Premium ceded', income: 1000, outgo: undefined });
    expect(rows.find((r) => r.kind === 'balance')).toEqual({
      label: 'Balance due to reinsurer',
      outgo: 700,
      kind: 'balance',
    });
    const due = statementRows({ ...layout, balanceOnIncome: true, balanceLabel: 'Due from' });
    expect(due.find((r) => r.kind === 'balance')?.income).toBe(700);
    expect(rows.at(-1)).toMatchObject({ kind: 'total', income: 1000, outgo: 1000 });
  });

  it('offers approval to another checker and settlement once approved', () => {
    const all = () => true;
    expect(soaActions(soa, 'reinsurer', all).approve).toBe(false);
    expect(soaActions(soa, 'fmanager', all)).toEqual({ approve: true, settle: false });
    expect(soaActions({ ...soa, status: 'APPROVED' }, 'fmanager', all).settle).toBe(true);
    expect(soaActions({ ...soa, status: 'APPROVED' }, 'fmanager', () => false).settle).toBe(false);
    expect(soaReportParams(soa, 3)).toMatchObject({
      companyId: '3',
      quarter: '1',
      treatyYear: '2026',
    });
  });

  it('renders the printable statement with the amount in words', () => {
    render(
      <SoaStatement soa={{ ...soa, settlementDate: '2026-07-31', bankAccountCode: '1111' }} />,
    );
    expect(screen.getByText('Balance due to reinsurer')).toBeInTheDocument();
    expect(screen.getByText(/Seven Hundred/)).toBeInTheDocument();
    expect(screen.getByText(/Settled on/)).toBeInTheDocument();
  });
});

describe('facultative and allocation helpers', () => {
  it('applies the slip workflow', () => {
    const slip = { status: 'PENDING_APPROVAL', submittedBy: 'reinsurer' } as Pick<
      FacPlacement,
      'status' | 'submittedBy'
    >;
    expect(facActions(slip, 'reinsurer', () => true).approve).toBe(false);
    expect(facActions(slip, 'fmanager', () => true).approve).toBe(true);
    expect(facActions({ ...slip, status: 'PROVISIONAL' }, 'x', () => true).edit).toBe(true);
    expect(facActions({ ...slip, status: 'PLACED' }, 'x', () => true).close).toBe(true);
    expect(placedShare([{ sharePct: 60 }, { sharePct: 30.5 }])).toBe(90.5);
    expect(ageInDays('2026-01-01', '2026-01-31')).toBe(30);
    expect(ageInDays('2026-02-01', '2026-01-31')).toBe(0);
  });

  it('totals previews and splits claim shares by layer', () => {
    const row = { ourPremium: 100, quotaShare: 40, surplus: 30, fac: 10, retention: 20 };
    const totals = previewTotals([row, row] as never[]);
    expect(totals).toEqual({ premium: 200, treaty: 140, fac: 20, retention: 40 });
    const m = {
      shares: [
        { layer: 'QUOTA_SHARE', baseAmount: 10 },
        { layer: 'QUOTA_SHARE', baseAmount: 5 },
        { layer: 'XOL', baseAmount: 7 },
      ],
    } as Pick<ClaimMovement, 'shares'>;
    expect(sharesByLayer(m)).toEqual({ QUOTA_SHARE: 15, XOL: 7 });
  });
});
