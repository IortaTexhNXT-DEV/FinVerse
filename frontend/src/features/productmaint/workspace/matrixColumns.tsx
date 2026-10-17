import type { MatrixRow } from '@/api/pmWorkspace';
import type { Column } from '@/components/ui/DataTable';
import type { RowAction } from '@/components/ui/RowActionMenu';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import type { Tone } from '@/components/ui/statusTones';
import { UserName } from '@/components/ui/UserName';
import { formatDate } from '@/utils/format';

/** What the row menu of the Product Matrix may do. */
export interface MatrixActions {
  view: (row: MatrixRow) => void;
  update?: (row: MatrixRow) => void;
  deactivate?: (row: MatrixRow) => void;
  openDeactivation: (row: MatrixRow) => void;
}

const TONES: Readonly<Record<string, Tone>> = {
  Active: 'success',
  Expiring: 'warning',
  Expired: 'neutral',
  Deactivated: 'neutral',
};

/**
 * The row actions of a Product Matrix row (BDOI FRS FRPM.002.02 and FRPM.003.04): view, update and
 * deactivate an active package; a pending deactivation request opens instead.
 *
 * @param row row
 * @param actions what the user may do
 * @returns menu entries
 */
export function matrixRowActions(row: MatrixRow, actions: MatrixActions): RowAction[] {
  const items: RowAction[] = [{ label: 'View Package', onSelect: () => actions.view(row) }];
  const active = row.status === 'Active' || row.status === 'Expiring';
  if (active && actions.update !== undefined) {
    const update = actions.update;
    items.push({ label: 'Update Package', onSelect: () => update(row) });
  }
  if (row.deactivationId !== null) {
    items.push({
      label: `View Deactivation Request ${row.deactivationNo ?? ''}`.trim(),
      onSelect: () => actions.openDeactivation(row),
    });
  } else if (active && actions.deactivate !== undefined) {
    const deactivate = actions.deactivate;
    items.push({ label: 'Deactivate Package', danger: true, onSelect: () => deactivate(row) });
  }
  return items;
}

/**
 * The columns of the Product Matrix (BDOI FRS FRPM.003.01): Line of Insurance, Sub-Line, Package
 * Name with its description, Insurer, Status, Effective and Expiry Date, Last Updated By and Date.
 *
 * @param actions row actions
 * @returns columns
 */
export function matrixColumns(actions: MatrixActions): Column<MatrixRow>[] {
  return [
    { key: 'line', header: 'Line of Insurance', sortKey: 'line', render: (r) => r.lineOfInsurance },
    { key: 'sub', header: 'Sub-Line', sortKey: 'subLine', render: (r) => r.subLine ?? '' },
    {
      key: 'name',
      header: 'Package Name',
      sortKey: 'name',
      render: (r) => (
        <>
          <strong>{r.packageName}</strong>
          <div className="muted">
            {r.productCode} · {r.productType}
          </div>
        </>
      ),
    },
    {
      key: 'desc',
      header: 'Package Description',
      truncate: true,
      render: (r) => r.description ?? '',
    },
    { key: 'insurer', header: 'Insurer', truncate: true, render: (r) => r.insurers ?? '' },
    {
      key: 'status',
      header: 'Status',
      sortKey: 'status',
      render: (r) => <StatusBadge status={r.status} label={r.status} tone={TONES[r.status]} full />,
    },
    {
      key: 'from',
      header: 'Effective Date',
      sortKey: 'effectiveDate',
      render: (r) => formatDate(r.effectiveDate),
    },
    {
      key: 'to',
      header: 'Expiry Date',
      sortKey: 'expiryDate',
      render: (r) => (
        <>
          {formatDate(r.expiryDate)}
          {r.daysLeft !== null && r.daysLeft >= 0 && (
            <div className="muted">{r.daysLeft} days left</div>
          )}
        </>
      ),
    },
    {
      key: 'by',
      header: 'Last Updated By',
      sortKey: 'updatedBy',
      render: (r) => <UserName login={r.lastUpdatedBy} />,
    },
    {
      key: 'at',
      header: 'Last Updated Date',
      sortKey: 'updatedAt',
      render: (r) => formatDate(r.lastUpdatedAt?.slice(0, 10)),
    },
    {
      key: 'actions',
      header: 'Actions',
      kind: 'actions',
      render: (r) => <RowActionMenu label={r.productCode} actions={matrixRowActions(r, actions)} />,
    },
  ];
}
