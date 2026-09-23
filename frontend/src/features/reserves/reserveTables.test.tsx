import { render, screen } from '@testing-library/react';
import type { TriangleData } from '@/api/reserves';
import { RESERVES_HELP } from './help';
import { reservesModule } from './module';
import {
  LineSummaryTable,
  LinesTable,
  MovementsTable,
  TakafulTable,
  TotalsTable,
} from './RunTables';
import { TriangleGrid } from './TriangleGrid';

const triangle: TriangleData = {
  businessLine: 'FIRE',
  basis: 'INCURRED',
  period: 'YEAR',
  paid: [[0, 50], [80]],
  incurred: [[100, 150], [200]],
  factors: [1.5],
  rows: [
    {
      accidentPeriod: '2025',
      latest: 150,
      cumulativeFactor: 1,
      ultimate: 150,
      incurred: 150,
      ibnr: 0,
    },
    {
      accidentPeriod: '2026',
      latest: 200,
      cumulativeFactor: 1.5,
      ultimate: 300,
      incurred: 200,
      ibnr: 100,
    },
  ],
  ibnr: 100,
};

describe('reserve tables', () => {
  it('renders the development triangle with factors and IBNR', () => {
    render(<TriangleGrid data={triangle} />);
    expect(screen.getByText('Age 1')).toBeInTheDocument();
    expect(screen.getAllByText('1.500000')).toHaveLength(2);
    expect(screen.getByText('Age-to-age factor')).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: '2026' })).toBeInTheDocument();
  });

  it('renders a paid triangle from the paid rows', () => {
    render(<TriangleGrid data={{ ...triangle, basis: 'PAID' }} />);
    expect(screen.getByText('80.00')).toBeInTheDocument();
  });

  it('renders totals, line summaries, lines, movements and takaful', () => {
    const lines = [
      {
        type: 'IBNR',
        branchId: 1,
        branchCode: 'HO',
        businessLine: 'PA',
        productCode: 'PA-IND',
        sourceType: 'AGENT',
        gross: 1000,
        ri: 100,
        net: 900,
        base: 20000,
        rate: 5,
        method: 'RATE',
      },
    ];
    render(
      <>
        <TotalsTable
          totals={[{ type: 'UPR', label: 'Unearned premium', gross: 5, ri: 1, net: 4 }]}
        />
        <LineSummaryTable lines={lines} />
        <LinesTable lines={lines} />
        <MovementsTable
          movements={[
            {
              branchCode: 'HO',
              businessLine: 'PA',
              eventType: 'IBNR_PROVISION',
              component: 'IBNR_CHANGE',
              amount: 1000,
            },
          ]}
        />
        <TakafulTable
          loading={false}
          items={[
            {
              policyNo: 'P-1',
              insuredName: 'Juan',
              businessLine: 'PA',
              productCode: 'PA-IND',
              expiryDate: '2026-03-31',
              gross: 100,
              discount: 0,
              loading: 0,
              commission: 10,
              claims: 0,
              applicable: 90,
              retakaful: 20,
              tax: 2,
              payable: 47,
            },
          ]}
        />
      </>,
    );
    expect(screen.getByText('Unearned premium')).toBeInTheDocument();
    expect(screen.getByText('IBNR_CHANGE')).toBeInTheDocument();
    expect(screen.getByText('P-1')).toBeInTheDocument();
    expect(screen.getByText('Rate')).toBeInTheDocument();
  });

  it('declares the menu section and its help', () => {
    expect(reservesModule.section).toBe('Actuarial Reserves');
    expect(reservesModule.screens.filter((s) => !s.hidden)).toHaveLength(4);
    expect(RESERVES_HELP.screens.map((s) => s.path)).toContain('/reserves/runs');
  });
});
