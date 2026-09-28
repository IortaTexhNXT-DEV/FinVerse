import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { legacyInquiryApi } from '@/api/legacyInquiry';
import type { AccessLogEntry, AccessLogFilter } from '@/api/legacyInquiry';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { reasonLabel } from './reason';
import { UserName } from '@/components/ui/UserName';

const ACTIONS = ['SEARCH', 'VIEW', 'DOWNLOAD', 'EXPORT'];

const COLUMNS: Column<AccessLogEntry>[] = [
  {
    key: 'when',
    header: 'When',
    kind: 'datetime',
    render: (a) => <CellStack main={formatDateTime(a.accessedAt)} sub={a.sourceAddress ?? ''} />,
  },
  { key: 'user', header: 'User', render: (a) => <UserName login={a.username} /> },
  {
    key: 'action',
    header: 'Action',
    kind: 'status',
    render: (a) => (
      <StatusBadge
        status={a.action === 'EXPORT' ? 'WARNING' : 'ACTIVE'}
        label={a.action.charAt(0) + a.action.slice(1).toLowerCase()}
      />
    ),
  },
  {
    key: 'what',
    header: 'Criteria / Records',
    render: (a) => <CellStack main={a.criteria ?? ''} sub={a.recordKeys ?? ''} />,
  },
  { key: 'count', header: 'Count', numeric: true, render: (a) => a.resultCount },
  {
    key: 'reason',
    header: 'Reason',
    render: (a) => <CellStack main={reasonLabel(a.reasonCode)} sub={a.reasonText ?? ''} />,
  },
];

/**
 * Access Log of the legacy archive (DATA_MIGRATION_DESIGN section 16): every search, view,
 * download and export with its user, address, criteria, records and reason; append-only.
 */
export default function AccessLogPage() {
  const companyId = useCompanyId();
  const [filter, setFilter] = useState<AccessLogFilter>({});
  const [page, setPage] = useState(0);
  const log = useQuery({
    queryKey: ['legacy-inquiry', 'access-log', companyId, filter, page],
    queryFn: () => legacyInquiryApi.accessLog(companyId, filter, page),
    enabled: companyId > 0,
  });
  const set = (key: keyof AccessLogFilter, value: string) => {
    setPage(0);
    setFilter((f) => ({ ...f, [key]: value === '' ? undefined : value }));
  };
  return (
    <div className="stack">
      <PageHeader
        section="Legacy Inquiry"
        title="Access Log"
        description="Every access to the legacy archive. The log cannot be changed or deleted."
      />
      <Card>
        <div className="worklist-filters form-grid">
          <Field label="User">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filter.username ?? ''}
                onChange={(e) => set('username', e.target.value)}
              />
            )}
          </Field>
          <Field label="Action">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filter.action ?? ''}
                onChange={(e) => set('action', e.target.value)}
              >
                <option value="">All</option>
                {ACTIONS.map((a) => (
                  <option key={a} value={a}>
                    {a.charAt(0) + a.slice(1).toLowerCase()}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={filter.from ?? ''}
                onChange={(e) => set('from', e.target.value)}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={filter.to ?? ''}
                onChange={(e) => set('to', e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <Card flush>
        <ErrorAlert error={log.error} onRetry={() => void log.refetch()} />
        <DataTable<AccessLogEntry>
          loading={log.isLoading}
          rows={log.data?.content ?? []}
          rowKey={(a) => String(a.id)}
          emptyMessage="No access in the period"
          columns={COLUMNS}
        />
        <PageFooter data={log.data} noun="entries" onPage={setPage} />
      </Card>
    </div>
  );
}
