import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { adminApi } from '@/api/admin';
import type { AuditEntry } from '@/api/admin';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { formatDateTime, humanize, today } from '@/utils/format';
import { AuditExportButtons } from './AuditExportButtons';
import { DateInput } from '@/components/ui/DateInput';
import { UserName } from '@/components/ui/UserName';
import { StatusBadge } from '@/components/ui/StatusBadge';

/** Audit trail inquiry: originator, modifier and authorizer activity with timestamps. */
export default function AuditTrailPage() {
  const [filters, setFilters] = useState({
    from: today(),
    to: today(),
    username: '',
    entityType: '',
    page: 0,
  });
  const audit = useQuery({ queryKey: ['audit', filters], queryFn: () => adminApi.audit(filters) });
  const set = (patch: Partial<typeof filters>) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const data = audit.data;

  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Audit Trail"
        description="Every financial and non-financial action, who performed it and when."
        actions={<AuditExportButtons filters={filters} />}
      />
      <Card>
        <div className="form-grid">
          <Field label="From">
            {(id) => (
              <DateInput
                id={id}
                value={filters.from}
                onChange={(e) => set({ from: e.target.value })}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <DateInput id={id} value={filters.to} onChange={(e) => set({ to: e.target.value })} />
            )}
          </Field>
          <Field label="User">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.username}
                onChange={(e) => set({ username: e.target.value })}
              />
            )}
          </Field>
          <Field label="Entity type">
            {(id) => (
              <input
                id={id}
                className="input"
                placeholder="e.g. JournalBatch"
                value={filters.entityType}
                onChange={(e) => set({ entityType: e.target.value })}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={audit.error} />
      <Card flush>
        <DataTable<AuditEntry>
          loading={audit.isLoading}
          rows={data?.content ?? []}
          rowKey={(a) => a.id}
          columns={[
            {
              key: 't',
              header: 'When',
              kind: 'datetime',
              render: (a) => formatDateTime(a.occurredAt),
            },
            { key: 'u', header: 'User', render: (a) => <UserName login={a.username} /> },
            {
              key: 'a',
              header: 'Action',
              kind: 'status',
              render: (a) => <StatusBadge status={a.action} tone="neutral" />,
            },
            { key: 'e', header: 'Record Type', render: (a) => humanize(a.entityType) },
            { key: 'k', header: 'Reference', render: (a) => a.entityId ?? '' },
            { key: 's', header: 'Details', render: (a) => a.summary },
          ]}
        />
        <PageFooter data={data} onPage={(p) => setFilters((f) => ({ ...f, page: p }))} />
      </Card>
    </div>
  );
}
