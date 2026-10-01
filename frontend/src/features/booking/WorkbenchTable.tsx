import { Link } from 'react-router-dom';
import type { WorkbenchRow, WorkbenchTab } from '@/api/booking';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { rowLink, toggle, toggleAll } from './bookingForm';
import { queuedActions } from './bookingRowActions';
import { LineLabel } from '@/components/broking/LovLabel';

interface Props {
  tab: WorkbenchTab;
  rows: WorkbenchRow[];
  loading: boolean;
  selection: ReadonlySet<number>;
  onSelect: (selection: Set<number>) => void;
  onEdit: (row: WorkbenchRow) => void;
  onRemove: (row: WorkbenchRow) => void;
  onOpen: (row: WorkbenchRow) => void;
}

const STATUS_LABELS: Record<WorkbenchTab, string> = {
  READY: 'READY_TO_BOOK',
  QUEUED: 'QUEUED',
  BOOKED: 'BOOKED',
  FAILED: 'FAILED',
};

function selectColumn(
  rows: WorkbenchRow[],
  selection: ReadonlySet<number>,
  onSelect: (s: Set<number>) => void,
): Column<WorkbenchRow> {
  return {
    key: 'select',
    header: (
      <input
        type="checkbox"
        className="booking-select"
        aria-label="Select all rows shown"
        disabled={rows.length === 0}
        checked={rows.length > 0 && rows.every((r) => selection.has(r.id))}
        onChange={() =>
          onSelect(
            toggleAll(
              selection,
              rows.map((r) => r.id),
            ),
          )
        }
      />
    ),
    width: '48px',
    render: (r) => (
      <input
        type="checkbox"
        className="booking-select"
        aria-label={`Select ${r.arn}`}
        checked={selection.has(r.id)}
        onClick={(e) => e.stopPropagation()}
        onChange={() => onSelect(toggle(selection, r.id))}
      />
    ),
  };
}

/** Rows of a Booking Workbench tab (BRNB.036) with the selection for the bulk actions. */
export function WorkbenchTable({
  tab,
  rows,
  loading,
  selection,
  onSelect,
  onEdit,
  onRemove,
  onOpen,
}: Readonly<Props>) {
  const columns: Column<WorkbenchRow>[] = [
    {
      key: 'client',
      header: 'Name / Client Code',
      render: (r) => (
        <>
          <div>{r.clientName}</div>
          <div className="muted">{r.clientCode}</div>
        </>
      ),
    },
    {
      key: 'arn',
      header: 'ARN',
      render: (r) => (
        <Link to={rowLink(r)} onClick={(e) => e.stopPropagation()}>
          {r.arn}
        </Link>
      ),
    },
    {
      key: 'invoice',
      header: 'Invoice No.',
      render: (r) => (r.invoiceNo ? <ReferenceChip value={r.invoiceNo} /> : ''),
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (
        <>
          <StatusBadge status={STATUS_LABELS[tab]} />
          {r.message && tab === 'FAILED' && <div className="text-danger">{r.message}</div>}
          {r.source === 'DIRECT_BOOKING' && <div className="muted">Direct booking</div>}
        </>
      ),
    },
    { key: 'line', header: 'Product Line', render: (r) => <LineLabel code={r.lineCode} /> },
    { key: 'department', header: 'Department', render: (r) => r.department ?? '' },
    { key: 'date', header: 'Booking Date', render: (r) => formatDate(r.bookingDate) },
  ];
  if (tab !== 'BOOKED') {
    columns.unshift(selectColumn(rows, selection, onSelect));
  }
  if (tab === 'QUEUED') {
    columns.push({
      key: 'actions',
      header: 'Actions',
      render: (r) => (
        <RowActions
          record={r.arn}
          actions={queuedActions(
            () => onEdit(r),
            () => onRemove(r),
          )}
        />
      ),
    });
  }
  return (
    <DataTable<WorkbenchRow>
      caption="Booking workbench"
      loading={loading}
      rows={rows}
      rowKey={(r) => r.id}
      onRowClick={onOpen}
      emptyMessage="No items to display"
      columns={columns}
    />
  );
}
