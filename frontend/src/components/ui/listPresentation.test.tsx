import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DataTable } from './DataTable';
import type { Column } from './DataTable';
import { DueDate } from './DueDate';
import { StatusBadge } from './StatusBadge';

interface Row {
  id: number;
  type: string;
  name: string;
}

const COLUMNS: Column<Row>[] = [
  { key: 'type', header: 'Type', truncate: true, width: '160px', render: (r) => r.type },
  { key: 'name', header: 'Name', render: (r) => r.name },
  {
    key: 'actions',
    header: '',
    kind: 'actions',
    render: () => <button type="button">More</button>,
  },
];

const ROWS: Row[] = [
  { id: 1, type: 'Disbursement Voucher', name: 'Pacific Harbor Group' },
  { id: 2, type: '', name: '' },
];

describe('DataTable list presentation', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('uses the page scroll with no scroll box of its own when the table fits its card', () => {
    const { container } = render(<DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />);
    expect(container.querySelector('.table-wrap')).toHaveClass('table-page');
  });

  it('scrolls sideways inside its card only when the table is wider than the card', () => {
    class WideObserver {
      constructor(private readonly callback: () => void) {}
      observe() {
        this.callback();
      }
      disconnect() {
        return undefined;
      }
    }
    vi.stubGlobal('ResizeObserver', WideObserver);
    const width = vi.spyOn(HTMLElement.prototype, 'scrollWidth', 'get').mockReturnValue(2000);
    const client = vi.spyOn(HTMLElement.prototype, 'clientWidth', 'get').mockReturnValue(800);
    const { container } = render(<DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />);
    expect(container.querySelector('.table-wrap')).not.toHaveClass('table-page');
    width.mockRestore();
    client.mockRestore();
  });

  it('keeps a truncating column on one line with the full text in the tooltip', () => {
    render(<DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />);
    const type = screen.getByText('Disbursement Voucher');
    expect(type).toHaveClass('truncate');
    expect(type).toHaveAttribute('title', 'Disbursement Voucher');
    expect(type.closest('td')).toHaveClass('col-truncate');
  });

  it('shows one muted dash for every empty cell, never a blank', () => {
    render(<DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />);
    const dashes = screen.getAllByText('—');
    expect(dashes).toHaveLength(2);
    dashes.forEach((d) => expect(d).toHaveClass('muted'));
  });

  it('keeps the row action menu in a narrow column at the end of the row', () => {
    render(<DataTable columns={COLUMNS} rows={ROWS.slice(0, 1)} rowKey={(r) => r.id} />);
    expect(screen.getByRole('button', { name: 'More' }).closest('td')).toHaveClass('col-actions');
  });
});

describe('DueDate', () => {
  it('shows a due time on one line in the plain text colour when not overdue', () => {
    render(<DueDate at="2026-09-10T02:00:00Z" overdue={false} />);
    const due = screen.getByText('10-Sep-2026 10:00');
    expect(due).toHaveClass('due-date');
    expect(due).not.toHaveClass('overdue');
  });

  it('marks an overdue date with the icon and the word, not by colour alone', () => {
    const { container } = render(<DueDate at="2026-09-10" overdue dateOnly />);
    const due = container.querySelector('.due-date');
    expect(due).toHaveClass('overdue');
    expect(due).toHaveAttribute('title', 'Overdue: due 10-Sep-2026');
    expect(due?.querySelector('svg')).not.toBeNull();
    expect(screen.getByText('Overdue', { exact: false })).toHaveClass('visually-hidden');
  });

  it('shows the muted dash without a due date', () => {
    render(<DueDate at={undefined} overdue={false} />);
    expect(screen.getByText('—')).toHaveClass('muted');
  });
});

describe('StatusBadge', () => {
  it('labels acronym statuses in capitals and keeps one pill element per tone', () => {
    render(
      <>
        <StatusBadge status="RA_SENT" />
        <StatusBadge status="DRAFT" />
        <StatusBadge status="RETURNED_TO_MARKETING" />
      </>,
    );
    expect(screen.getByText('RA Sent')).toHaveClass('badge', 'info');
    expect(screen.getByText('Draft')).toHaveClass('badge', 'neutral');
    expect(screen.getByText('Returned to Marketing')).toHaveClass('badge', 'danger');
  });
});
