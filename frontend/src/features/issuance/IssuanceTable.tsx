import { Link } from 'react-router-dom';
import type { IssuanceRow, IssuanceTab } from '@/api/issuance';
import { selectionColumn } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime, humanize } from '@/utils/format';
import { rowKeyOf } from './issuanceLogic';

function tabColumn(tab: IssuanceTab): Column<IssuanceRow> {
  if (tab === 'REVIEW' || tab === 'READY_TO_DISPATCH') {
    return {
      key: 'epolicy',
      header: 'E-policy',
      render: (r) => (
        <span>
          {r.epolicyId !== undefined && tab === 'REVIEW' ? (
            <Link to={`/issuance/epolicies/${r.epolicyId}`}>{r.epolicyFile}</Link>
          ) : (
            r.epolicyFile
          )}
          <span className="cell-sub">received {formatDateTime(r.receivedAt)}</span>
        </span>
      ),
    };
  }
  if (tab === 'IA_TO_GENERATE') {
    return {
      key: 'mortgagee',
      header: 'Mortgagee',
      render: (r) => humanize(r.mortgageeBank ?? ''),
    };
  }
  return {
    key: 'upload',
    header: '',
    render: (r) => (
      <Link className="btn btn-secondary btn-sm" to={`/issuance/upload?arn=${r.arn}`}>
        Upload E-policy
      </Link>
    ),
  };
}

/**
 * Accounts or e-policies of an Issuance Workbench tab: selection, client, proposal number,
 * status, product line and insurer, policy numbers, and the tab's own column.
 */
export function IssuanceTable({
  tab,
  rows,
  loading,
  selection,
}: Readonly<{ tab: IssuanceTab; rows: IssuanceRow[]; loading: boolean; selection: RowSelection }>) {
  const selectable = tab === 'READY_TO_DISPATCH' || tab === 'IA_TO_GENERATE';
  const columns: Column<IssuanceRow>[] = [
    {
      key: 'name',
      header: 'Name / Client Code',
      render: (r) => (
        <span>
          <Link to={`/placement/accounts/${r.arn}`}>{r.clientName}</Link>
          <span className="cell-sub">{r.clientCode}</span>
        </span>
      ),
    },
    { key: 'arn', header: 'Proposal No.', render: (r) => <code>{r.arn}</code> },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <StatusBadge status={r.epolicyStatus ?? r.status} />,
    },
    {
      key: 'line',
      header: 'Product Line',
      render: (r) => (
        <span>
          {humanize(r.lineCode)}
          <span className="cell-sub">
            {r.productCode} · {r.insurerCode ?? '—'}
          </span>
        </span>
      ),
    },
    {
      key: 'policy',
      header: 'Policy No.',
      render: (r) => (r.policyNumbers.length > 0 ? r.policyNumbers.join(', ') : '—'),
    },
    tabColumn(tab),
  ];
  return (
    <DataTable<IssuanceRow>
      caption="Issuance work list"
      loading={loading}
      rows={rows}
      rowKey={rowKeyOf}
      emptyMessage="No items to display"
      columns={
        selectable
          ? [selectionColumn(rows, rowKeyOf, selection, (r) => r.arn), ...columns]
          : columns
      }
    />
  );
}
