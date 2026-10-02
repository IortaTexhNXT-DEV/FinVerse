import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DataTable } from './DataTable';
import type { Column } from './DataTable';
import { DueDate } from './DueDate';
import { fittedHeight } from './useFitHeight';
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

  it('fits the card to its rows when the page has room: no fixed-height box', () => {
    expect(fittedHeight({ wrap: 300, table: 300, overflow: -200, min: 240 })).toBeNull();
    expect(fittedHeight({ wrap: 300, table: 300, overflow: 0, min: 240 })).toBeNull();
  });

  it('caps a long list to the room left in the window, so the page keeps one scroll bar', () => {
    expect(fittedHeight({ wrap: 1500, table: 1500, overflow: 700, min: 240 })).toBe(800);
    // A capped list grows again when the window grows, up to its rows.
    expect(fittedHeight({ wrap: 800, table: 1500, overflow: -300, min: 240 })).toBe(1100);
    expect(fittedHeight({ wrap: 800, table: 1000, overflow: -300, min: 240 })).toBeNull();
    // Never smaller than the least list height.
    expect(fittedHeight({ wrap: 400, table: 1500, overflow: 380, min: 240 })).toBe(240);
  });

  it('scrolls inside its card with the cap set from the page, the header sticky inside it', () => {
    class Observer {
      constructor(private readonly callback: () => void) {}
      observe() {
        this.callback();
      }
      disconnect() {
        return undefined;
      }
    }
    vi.stubGlobal('ResizeObserver', Observer);
    vi.stubGlobal('requestAnimationFrame', (run: () => void) => {
      run();
      return 1;
    });
    const rect = vi
      .spyOn(HTMLElement.prototype, 'getBoundingClientRect')
      .mockReturnValue({ height: 1500 } as DOMRect);
    const offset = vi.spyOn(HTMLElement.prototype, 'offsetHeight', 'get').mockReturnValue(1500);
    const scroll = vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockReturnValue(2400);
    const client = vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(1500);
    const { container } = render(
      <main className="app-main">
        <DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />
      </main>,
    );
    const wrap = container.querySelector<HTMLElement>('.table-wrap');
    expect(wrap).toHaveAttribute('data-fit');
    // The page overflows by 900 px: the list takes 1500 - 900 = 600 px and scrolls inside.
    expect(wrap?.style.maxHeight).toBe('600px');
    [rect, offset, scroll, client].forEach((m) => m.mockRestore());
  });

  it('leaves a page with several lists to scroll as a whole', () => {
    const { container } = render(
      <main className="app-main">
        <DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />
        <DataTable columns={COLUMNS} rows={ROWS} rowKey={(r) => r.id} />
      </main>,
    );
    container.querySelectorAll<HTMLElement>('.table-wrap').forEach((w) => {
      expect(w.style.maxHeight).toBe('');
    });
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
