import { render, screen, within } from '@testing-library/react';
import { formatDays, formatPeriod } from '@/utils/format';
import { DataTable } from './DataTable';
import { PeriodCell } from './PeriodCell';
import { periodColumn } from './periodColumn';

interface Row {
  id: number;
  from?: string;
  to?: string;
}

describe('period cell', () => {
  it('shows the from date and "to" the end date on two lines, each kept whole', () => {
    const { container } = render(<PeriodCell from="2026-10-20" to="2027-10-20" />);
    const lines = container.querySelectorAll('.period-cell > span');
    expect(lines).toHaveLength(2);
    expect(lines[0]).toHaveTextContent('20-Oct-2026');
    expect(lines[1]).toHaveTextContent('to 20-Oct-2027');
    lines.forEach((line) => expect(line).toHaveClass('nowrap'));
    expect(container.querySelector('.period-cell')).toHaveAttribute(
      'title',
      '20-Oct-2026 to 20-Oct-2027',
    );
  });

  it('shows an open end and an empty period as a dash', () => {
    const { container, rerender } = render(<PeriodCell from="2026-01-01" to={undefined} />);
    expect(container).toHaveTextContent('to open');
    rerender(<PeriodCell from={undefined} to={undefined} />);
    expect(container).toHaveTextContent('—');
  });

  it('gives list columns the period kind with its minimum width', () => {
    const column = periodColumn<Row>(
      'p',
      'Period',
      (r) => r.from,
      (r) => r.to,
    );
    render(
      <DataTable
        columns={[column]}
        rows={[{ id: 1, from: '2026-09-10', to: '2027-09-10' }]}
        rowKey={(r) => r.id}
      />,
    );
    expect(screen.getByRole('columnheader', { name: 'Period' })).toHaveClass('col-period');
    const cell = screen.getByRole('cell');
    expect(cell).toHaveClass('col-period');
    expect(within(cell).getByText('to 10-Sep-2027')).toBeInTheDocument();
  });

  it('turns a period written as one text into the two-line cell in any list', () => {
    render(
      <DataTable
        columns={[{ key: 'p', header: 'Cover', render: () => '20-Oct-2026 – 20-Oct-2027' }]}
        rows={[{ id: 1 }]}
        rowKey={(r) => r.id}
      />,
    );
    expect(screen.getByText('20-Oct-2026')).toHaveClass('nowrap');
    expect(screen.getByText('to 20-Oct-2027')).toBeInTheDocument();
  });

  it('keeps amounts, dates and codes whole', () => {
    render(
      <DataTable
        columns={[
          { key: 'c', header: 'Code', render: () => 'ARN-2026-900013' },
          { key: 'd', header: 'Date', render: () => '20-Oct-2026' },
          { key: 'a', header: 'Amount', kind: 'amount', render: () => '25,037.75' },
        ]}
        rows={[{ id: 1 }]}
        rowKey={(r) => r.id}
      />,
    );
    expect(screen.getByText('ARN-2026-900013')).toHaveClass('nowrap');
    expect(screen.getByText('20-Oct-2026')).toHaveClass('nowrap');
    expect(screen.getByRole('cell', { name: '25,037.75' })).toHaveClass('num');
  });
});

describe('period and day texts', () => {
  it('writes a period on one line for detail blocks', () => {
    expect(formatPeriod('2026-10-20', '2027-10-20')).toBe('20-Oct-2026 to 20-Oct-2027');
    expect(formatPeriod('2026-10-20', undefined)).toBe('from 20-Oct-2026');
    expect(formatPeriod(undefined, undefined)).toBe('');
  });

  it('writes aging in days, never "d"', () => {
    expect(formatDays(0)).toBe('0 days');
    expect(formatDays(1)).toBe('1 day');
    expect(formatDays(12)).toBe('12 days');
    expect(formatDays(undefined)).toBe('');
  });
});
