import { render, screen } from '@testing-library/react';
import type { ReportResult } from '@/api/reports';
import { ReportTable } from './ReportTable';

const result = (firstValue: string | null): ReportResult => ({
  code: 'X',
  title: 'X',
  parameterEcho: [],
  columns: [
    { key: 'arn', label: 'ARN', type: 'TEXT', summed: false },
    { key: 'due', label: 'Due', type: 'DATE', summed: false },
    { key: 'amount', label: 'Amount', type: 'AMOUNT', summed: true },
  ],
  rows: [
    { kind: 'GROUP_HEADER', level: 0, label: 'Stage : Draft', cells: {} },
    { kind: 'DETAIL', level: 0, cells: { arn: 'ARN-1', due: '2026-09-03', amount: 10 } },
    {
      kind: 'SUBTOTAL',
      level: 0,
      label: 'Total Stage : Draft',
      cells: { arn: firstValue, amount: 10 },
    },
  ],
  notes: [],
});

describe('report table', () => {
  it('lets a subtotal label span the empty leading columns', () => {
    render(<ReportTable result={result(null)} />);
    expect(screen.getAllByRole('columnheader')).toHaveLength(3);
    expect(screen.getByText('Total Stage : Draft')).toHaveAttribute('colspan', '2');
    expect(screen.getByText('03-09-2026')).toHaveClass('nowrap');
    expect(screen.getByText('Stage : Draft')).toHaveAttribute('colspan', '3');
  });

  it('adds a label column when a labelled row fills the first column', () => {
    render(<ReportTable result={result('x')} />);
    expect(screen.getAllByRole('columnheader')).toHaveLength(4);
    expect(screen.getByText('Total Stage : Draft')).toHaveClass('report-label');
  });
});
