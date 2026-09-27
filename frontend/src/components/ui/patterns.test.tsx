import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { setUserDirectory } from '@/api/users';
import type { HistoryEntry } from '@/api/workflow';
import { HistoryTable } from '@/components/broking/HistoryTable';
import { historyRows } from '@/components/broking/historyRows';
import { isFormatHint } from '@/utils/presentation';
import { DataTable } from './DataTable';
import { DefinitionGrid } from './DefinitionGrid';
import { Field } from './Field';
import { Pager } from './Pager';
import { StatusBadge } from './StatusBadge';
import { statusShortLabel } from './statusTones';
import { Tag } from './Tag';
import { UserName } from './UserName';

const HISTORY: HistoryEntry[] = [
  {
    toStage: 'DRAFT',
    toStageName: 'Draft',
    action: 'START',
    actor: 'disbmaker',
    automatic: false,
    occurredAt: '2026-09-25T10:00:00Z',
  },
  {
    fromStage: 'DRAFT',
    fromStageName: 'Draft',
    toStage: 'FOR_APPROVAL',
    toStageName: 'For approval',
    action: 'SUBMIT',
    actor: 'disbmaker',
    automatic: false,
    occurredAt: '2026-09-25T10:30:00Z',
  },
  {
    fromStage: 'FOR_APPROVAL',
    fromStageName: 'For approval',
    toStage: 'APPROVED',
    toStageName: 'Approved',
    action: 'APPROVE',
    comment: 'Approved',
    actor: 'disbappr',
    automatic: false,
    occurredAt: '2026-09-25T11:32:00Z',
  },
];

describe('status pills and tags', () => {
  it('shows a status that fits the pill in full, never an abbreviation', () => {
    render(<StatusBadge status="RETURNED_TO_MARKETING" />);
    const pill = screen.getByText('Returned to Marketing');
    expect(pill).toHaveClass('badge', 'danger');
    expect(pill).toHaveAttribute('title', 'Returned to Marketing');
    expect(statusShortLabel('PENDING_AUTHORIZATION')).toBe('Pending Authorization');
  });

  it('keeps the agreed short form for a label longer than the pill', () => {
    expect(statusShortLabel('FOR_MKT_APPROVAL', 'For Marketing Head Approval')).toBe(
      'For Mktg Approval',
    );
  });

  it('keeps short labels in full and accepts a label and tone override', () => {
    expect(statusShortLabel('BOOKED')).toBe('Booked');
    render(<StatusBadge status="CUSTOM" label="Queued" tone="warning" />);
    expect(screen.getByText('Queued')).toHaveClass('badge', 'warning');
  });

  it('renders flag chips with a tooltip and a tone', () => {
    render(
      <>
        <Tag>Direct Payment</Tag>
        <Tag tone="neutral">FFY</Tag>
      </>,
    );
    expect(screen.getByText('Direct Payment')).toHaveClass('tag');
    expect(screen.getByText('Direct Payment')).toHaveAttribute('title', 'Direct Payment');
    expect(screen.getByText('FFY')).toHaveClass('tag', 'neutral');
  });
});

describe('HistoryTable', () => {
  it('lists the history newest first as a table with display names and durations', () => {
    setUserDirectory([
      { username: 'disbappr', displayName: 'Diana Approver', roleName: 'Disbursement Approver' },
    ]);
    render(<HistoryTable history={HISTORY} terminal />);
    const table = screen.getByRole('table', { name: 'Status history' });
    const rows = within(table).getAllByRole('row');
    expect(rows).toHaveLength(4);
    const [, first] = rows;
    if (first === undefined) {
      throw new Error('no data row');
    }
    const cells = within(first).getAllByRole('cell');
    expect(cells[0]).toHaveTextContent('Approved');
    expect(cells[1]).toHaveTextContent('For Approval');
    expect(cells[2]).toHaveTextContent('Approve');
    expect(cells[3]).toHaveTextContent('Diana Approver');
    expect(cells[3]).toHaveTextContent('Disbursement Approver');
    expect(cells[4]).toHaveTextContent('25-Sep-2026 19:32');
    expect(cells[5]).toHaveTextContent('Approved');
    // The final stage of a closed record has no running duration.
    expect(cells[6]).toHaveTextContent('—');
  });

  it('toggles to oldest first', async () => {
    render(<HistoryTable history={HISTORY} terminal />);
    await userEvent.click(screen.getByRole('button', { name: /newest first/i }));
    const [, oldest] = screen.getAllByRole('row');
    if (oldest === undefined) {
      throw new Error('no data row');
    }
    expect(within(oldest).getAllByRole('cell')[0]).toHaveTextContent('Draft');
    expect(screen.getByRole('columnheader', { name: /date and time/i })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
  });

  it('computes the time spent in each stage', () => {
    const rows = historyRows(HISTORY, true);
    expect(rows.map((r) => r.duration)).toEqual(['30m', '1h 2m', '']);
  });

  it('shows an empty state without history', () => {
    render(<HistoryTable history={[]} />);
    expect(screen.getByText('No history recorded')).toBeInTheDocument();
  });
});

describe('DefinitionGrid', () => {
  it('aligns labels and values and shows a dash for empty values', () => {
    render(
      <DefinitionGrid
        label="Identity"
        items={[
          { label: 'Name', value: 'Aquino, Lorna Faye' },
          { label: 'TIN', value: null },
        ]}
      />,
    );
    const grid = screen.getByLabelText('Identity');
    expect(within(grid).getByText('Name').tagName).toBe('DT');
    expect(within(grid).getByText('Aquino, Lorna Faye').tagName).toBe('DD');
    expect(within(grid).getByText('—')).toHaveClass('muted');
  });

  it('collapses an empty section to "Not provided"', () => {
    render(<DefinitionGrid collapseEmpty items={[{ label: 'TIN', value: '' }]} />);
    expect(screen.getByText('Not provided')).toBeInTheDocument();
  });

  it('shows only the filled fields of a mostly empty section until asked', async () => {
    render(
      <DefinitionGrid
        collapseEmpty
        items={[
          { label: 'Mobile', value: '09175550109' },
          { label: 'E-mail', value: null },
          { label: 'Landline', value: null },
        ]}
      />,
    );
    expect(screen.queryByText('Landline')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /show all fields/i }));
    expect(screen.getByText('Landline')).toBeInTheDocument();
  });
});

describe('Field', () => {
  it('keeps a short format hint under the field', () => {
    render(
      <Field label="Birth Date" hint="dd-MMM-yyyy">
        {(id) => <input id={id} />}
      </Field>,
    );
    expect(screen.getByText('dd-MMM-yyyy')).toHaveClass('field-format');
  });

  it('moves explanatory guidance to an info tooltip on the label', () => {
    const guidance = 'A prospect is enough to quote; the client must be confirmed first.';
    render(
      <Field label="Client" hint={guidance}>
        {(id) => <input id={id} />}
      </Field>,
    );
    expect(screen.getByRole('button', { name: guidance })).toHaveAttribute('title', guidance);
    expect(screen.getByLabelText('Client')).toBeInTheDocument();
  });

  it('classifies format hints', () => {
    expect(isFormatHint('Max 10 MB, PDF')).toBe(true);
    expect(isFormatHint('Optional: found from the file name or the document when empty')).toBe(
      false,
    );
  });

  it('shows the error inline instead of the hint', () => {
    render(
      <Field label="Amount" hint="2 decimals" error="Enter an amount">
        {(id) => <input id={id} />}
      </Field>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Enter an amount');
    expect(screen.queryByText('2 decimals')).not.toBeInTheDocument();
  });
});

describe('DataTable conventions', () => {
  const columns = [
    {
      key: 'ref',
      header: 'Reference',
      kind: 'code' as const,
      render: (r: Row) => r.ref,
      sortKey: 'ref',
    },
    { key: 'amt', header: 'Amount', kind: 'amount' as const, render: (r: Row) => r.amount },
  ];
  interface Row {
    ref: string;
    amount: string;
  }

  it('renders skeleton rows while loading', () => {
    const { container } = render(
      <DataTable columns={columns} rows={[]} rowKey={(r) => r.ref} loading />,
    );
    expect(container.querySelectorAll('tr.skeleton-row')).toHaveLength(5);
    expect(screen.getByLabelText('Loading')).toBeInTheDocument();
  });

  it('aligns code and amount columns and sorts through the API', async () => {
    const onSort = vi.fn();
    render(
      <DataTable
        columns={columns}
        rows={[{ ref: 'PAY-2026-000010', amount: '1,200.00' }]}
        rowKey={(r) => r.ref}
        sort={{ key: 'ref', direction: 'asc' }}
        onSort={onSort}
        footer={
          <tr className="row-total">
            <td>Total</td>
            <td className="num">1,200.00</td>
          </tr>
        }
      />,
    );
    expect(screen.getByText('PAY-2026-000010')).toHaveClass('nowrap');
    expect(screen.getByText('PAY-2026-000010').closest('td')).toHaveClass('col-code');
    expect(screen.getAllByText('1,200.00')[0]).toHaveClass('num');
    expect(screen.getByRole('columnheader', { name: /reference/i })).toHaveAttribute(
      'aria-sort',
      'ascending',
    );
    await userEvent.click(screen.getByRole('button', { name: /reference/i }));
    expect(onSort).toHaveBeenCalledWith({ key: 'ref', direction: 'desc' });
    expect(screen.getByText('Total')).toBeInTheDocument();
  });

  it('shows the empty message with the next action', () => {
    render(
      <DataTable
        columns={columns}
        rows={[]}
        rowKey={(r) => r.ref}
        emptyMessage="No unapplied payments"
        emptyAction={<button type="button">Refresh</button>}
      />,
    );
    expect(screen.getByText('No unapplied payments')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
  });

  it('offers rows per page on the pager', async () => {
    const onSize = vi.fn();
    render(<Pager page={0} totalPages={3} total={45} size={20} onPage={vi.fn()} onSize={onSize} />);
    await userEvent.selectOptions(screen.getByLabelText('Rows per page'), '50');
    expect(onSize).toHaveBeenCalledWith(50);
  });
});

describe('UserName', () => {
  it('shows the display name, never the login id', () => {
    setUserDirectory([{ username: 'upphandler', displayName: 'Paula Unapplied Handler' }]);
    render(<UserName login="upphandler" />);
    expect(screen.getByText('Paula Unapplied Handler')).toHaveAttribute('title', 'upphandler');
  });

  it('falls back to the login id and shows a dash without a user', () => {
    setUserDirectory([]);
    render(
      <>
        <UserName login="unknown" />
        <UserName login={undefined} />
      </>,
    );
    expect(screen.getByText('unknown')).toBeInTheDocument();
    expect(screen.getByText('—')).toHaveClass('muted');
  });
});
