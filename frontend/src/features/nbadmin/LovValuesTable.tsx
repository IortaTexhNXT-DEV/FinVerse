import type { LovValue } from '@/api/lov';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, today } from '@/utils/format';
import { effectivity, valueActions } from './lovForm';
import { UserName } from '@/components/ui/UserName';

import type { LovAction } from './lovForm';

export type { LovAction };

interface LovValuesTableProps {
  values: LovValue[];
  loading: boolean;
  /** Whether the user maintains the list (edit, deactivate). */
  manage: boolean;
  /** Whether the user may authorize a value (checker, not its maker). */
  mayAuthorize: (value: LovValue) => boolean;
  onAction: (value: LovValue, action: LovAction) => void;
}

const IN_FORCE: Record<ReturnType<typeof effectivity>, string> = {
  FUTURE: 'From a future date',
  ACTIVE: 'In force',
  EXPIRED: 'Ended',
};

const period = (v: LovValue) =>
  `${formatDate(v.effectiveFrom)} – ${v.effectiveTo === undefined ? 'open' : formatDate(v.effectiveTo)}`;

/** Values of a list with effectivity, maker-checker status and the actions of the user. */
export function LovValuesTable({
  values,
  loading,
  manage,
  mayAuthorize,
  onAction,
}: Readonly<LovValuesTableProps>) {
  const now = today();
  // Effectivity is a flag of the value, shown as a chip apart from its status pill (BDO).
  const columns: Column<LovValue>[] = [
    { key: 'code', header: 'Code', render: (v) => <span className="mono">{v.code}</span> },
    { key: 'label', header: 'Label', render: (v) => v.label },
    { key: 'order', header: 'Order', numeric: true, render: (v) => v.sortOrder },
    { key: 'parent', header: 'Parent', render: (v) => v.parentCode ?? '' },
    {
      key: 'eff',
      header: 'Effective',
      render: (v) => (
        <span className="cell-stack">
          {period(v)}
          <span className="tag">{IN_FORCE[effectivity(v, now)]}</span>
        </span>
      ),
    },
    { key: 'status', header: 'Status', render: (v) => <StatusBadge status={v.status} /> },
    {
      key: 'mc',
      header: 'Maker / Checker',
      render: (v) => (
        <span className="cell-stack">
          <UserName login={v.maker} />
          <span className="muted">
            <UserName login={v.authorizedBy} />
          </span>
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (v) => (
        <RowActions
          record={v.label}
          actions={valueActions(v, manage, mayAuthorize(v), (action) => onAction(v, action))}
        />
      ),
    },
  ];
  return (
    <DataTable<LovValue>
      loading={loading}
      rows={values}
      rowKey={(v) => v.id}
      emptyMessage="No items to display"
      columns={columns}
    />
  );
}
