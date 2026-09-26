import { Pencil, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { WorkbenchRow, WorkbenchTab } from '@/api/booking';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { rowLink, toggle, toggleAll } from './bookingForm';

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
      header: 'Proposal No. (ARN)',
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
    { key: 'line', header: 'Product Line', render: (r) => r.lineCode ?? '' },
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
        <div className="row">
          <Button
            size="sm"
            variant="ghost"
            icon={<Pencil size={14} />}
            aria-label={`Edit ${r.arn}`}
            onClick={(e) => {
              e.stopPropagation();
              onEdit(r);
            }}
          >
            Edit
          </Button>
          <Button
            size="sm"
            variant="ghost"
            icon={<Trash2 size={14} />}
            aria-label={`Remove ${r.arn}`}
            onClick={(e) => {
              e.stopPropagation();
              onRemove(r);
            }}
          >
            Remove
          </Button>
        </div>
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
