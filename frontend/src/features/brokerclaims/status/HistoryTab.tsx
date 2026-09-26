import { useQuery } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatDateTime } from '@/utils/format';
import { claimStatusApi } from './api';
import type { FieldChange, StatusChange } from './api';
import { FIELD_LABELS, PHASE_LABELS } from './statusLogic';

function statusText(label: string | undefined, phase: StatusChange['toPhase'] | undefined) {
  if (label === undefined || phase === undefined) {
    return '—';
  }
  return `${label} (${PHASE_LABELS[phase]})`;
}

/**
 * History tab of a claim (FR-CM-003/042/053): every status change with the days spent in the
 * previous status, user, time and remark, and every change of a tracked field (follow-up date,
 * action plan, settlement, adjuster, handler, and the CL1-A fields) with old and new values. The
 * rows are append-only; nothing can be edited here.
 */
export function HistoryTab({
  claimId,
  companyId,
}: Readonly<{ claimId: number; companyId: number }>) {
  const history = useQuery({
    queryKey: ['broker-claims', 'history', claimId],
    queryFn: () => claimStatusApi.history(claimId, companyId),
  });
  return (
    <div className="stack">
      <ErrorAlert error={history.error} />
      <Card title="Status History" flush>
        <DataTable<StatusChange>
          caption="Status history"
          loading={history.isLoading}
          rows={history.data?.statusChanges ?? []}
          rowKey={(h) => `${h.stamp.at}-${h.toStatus}-${h.toPhase}`}
          emptyMessage="No status set yet"
          columns={[
            { key: 'at', header: 'Date and Time', render: (h) => formatDateTime(h.stamp.at) },
            { key: 'from', header: 'From', render: (h) => statusText(h.fromLabel, h.fromPhase) },
            { key: 'to', header: 'To', render: (h) => statusText(h.toLabel, h.toPhase) },
            {
              key: 'days',
              header: 'Days in Previous Status',
              numeric: true,
              render: (h) => h.daysInPrevious ?? '—',
            },
            { key: 'by', header: 'User', render: (h) => h.stamp.by },
            { key: 'remark', header: 'Remark', render: (h) => h.stamp.remark ?? '' },
          ]}
        />
      </Card>
      <Card title="Changes" flush>
        <DataTable<FieldChange>
          caption="Field changes"
          loading={history.isLoading}
          rows={history.data?.fieldChanges ?? []}
          rowKey={(f) => `${f.stamp.at}-${f.field}-${f.newValue ?? ''}`}
          emptyMessage="No changes yet"
          columns={[
            { key: 'at', header: 'Date and Time', render: (f) => formatDateTime(f.stamp.at) },
            { key: 'field', header: 'Field', render: (f) => FIELD_LABELS[f.field] ?? f.field },
            { key: 'old', header: 'Old Value', render: (f) => f.oldValue ?? '—' },
            { key: 'new', header: 'New Value', render: (f) => f.newValue ?? '—' },
            { key: 'by', header: 'User', render: (f) => f.stamp.by },
            { key: 'reason', header: 'Reason', render: (f) => f.stamp.remark ?? '' },
          ]}
        />
      </Card>
    </div>
  );
}
