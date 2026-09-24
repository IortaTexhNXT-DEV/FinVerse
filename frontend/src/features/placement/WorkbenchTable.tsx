import { Link } from 'react-router-dom';
import type { WorkbenchRow } from '@/api/placement';
import { selectionColumn } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { placementLink } from './placementLogic';

function PlacementNote({ row }: Readonly<{ row: WorkbenchRow }>) {
  if (row.holdCoverStatus !== undefined && row.holdCoverExpiry !== undefined) {
    return (
      <span className="cell-sub">
        Hold cover {humanize(row.holdCoverStatus).toLowerCase()} until{' '}
        {formatDate(row.holdCoverExpiry)}
      </span>
    );
  }
  return null;
}

/**
 * Accounts of a workbench tab (BDO Insure Placement & Booking table): selection, name with the
 * client code below, proposal number (ARN), status pill, product line and department, with the
 * current slip and hold cover.
 */
export function WorkbenchTable({
  rows,
  loading,
  selection,
}: Readonly<{ rows: WorkbenchRow[]; loading: boolean; selection: RowSelection }>) {
  return (
    <DataTable<WorkbenchRow>
      caption="Accounts"
      loading={loading}
      rows={rows}
      rowKey={(r) => r.arn}
      emptyMessage="No items to display"
      columns={[
        selectionColumn(
          rows,
          (r) => r.arn,
          selection,
          (r) => r.arn,
        ),
        {
          key: 'name',
          header: 'Name / Client Code',
          render: (r) => (
            <span>
              <Link to={placementLink(r.arn)}>{r.clientName}</Link>
              <span className="cell-sub">{r.clientCode}</span>
            </span>
          ),
        },
        {
          key: 'arn',
          header: 'Proposal No.',
          render: (r) => (
            <span>
              <code>{r.arn}</code>
              {r.slipNo !== undefined && <span className="cell-sub">Slip {r.slipNo}</span>}
            </span>
          ),
        },
        {
          key: 'status',
          header: 'Status',
          render: (r) => (
            <span>
              <StatusBadge status={r.status} />
              <PlacementNote row={r} />
            </span>
          ),
        },
        {
          key: 'line',
          header: 'Product Line',
          render: (r) => (
            <span>
              {humanize(r.lineCode)}
              <span className="cell-sub">
                {r.productCode}
                {r.insurerCode !== undefined && ` · ${r.insurerCode}`}
              </span>
            </span>
          ),
        },
        { key: 'dept', header: 'Department', render: (r) => r.department ?? '—' },
        {
          key: 'premium',
          header: 'Gross Premium',
          numeric: true,
          render: (r) => formatAmount(r.grossPremium),
        },
      ]}
    />
  );
}
