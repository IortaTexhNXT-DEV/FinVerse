import type { LovValue } from '@/api/lov';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, today } from '@/utils/format';
import { effectivity } from './lovForm';

export type LovAction = 'edit' | 'deactivate' | 'authorize';

interface LovValuesTableProps {
  values: LovValue[];
  loading: boolean;
  /** Whether the user maintains the list (edit, deactivate). */
  manage: boolean;
  /** Whether the user may authorize a value (checker, not its maker). */
  mayAuthorize: (value: LovValue) => boolean;
  onAction: (value: LovValue, action: LovAction) => void;
}

function ValueActions({
  value,
  manage,
  authorize,
  onAction,
}: Readonly<{
  value: LovValue;
  manage: boolean;
  authorize: boolean;
  onAction: (value: LovValue, action: LovAction) => void;
}>) {
  const editable = manage && value.status !== 'INACTIVE';
  return (
    <div className="row">
      {editable && (
        <Button size="sm" variant="ghost" onClick={() => onAction(value, 'edit')}>
          Edit
        </Button>
      )}
      {editable && (
        <Button size="sm" variant="ghost" onClick={() => onAction(value, 'deactivate')}>
          Deactivate
        </Button>
      )}
      {authorize && (
        <Button size="sm" variant="primary" onClick={() => onAction(value, 'authorize')}>
          Authorize
        </Button>
      )}
    </div>
  );
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
          <span>{v.maker ?? '—'}</span>
          <span className="muted">{v.authorizedBy ?? '—'}</span>
        </span>
      ),
    },
    {
      key: 'actions',
      header: '',
      render: (v) => (
        <ValueActions value={v} manage={manage} authorize={mayAuthorize(v)} onAction={onAction} />
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
