import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { adminApi } from '@/api/admin';
import type { AuditEntry } from '@/api/admin';
import type { ExportFormat } from '@/api/reports';
import { UserLookup } from '@/components/broking/Lookups';
import { Card } from '@/components/ui/Card';
import { Combobox } from '@/components/ui/Combobox';
import { DataTable } from '@/components/ui/DataTable';
import type { SortState } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime, today } from '@/utils/format';
import { AuditExportButtons } from './AuditExportButtons';
import { AUDIT_ACTIONS, dateRangeError } from './auditTrail';
import type { AuditFilters } from './auditTrail';

interface AuditTrailViewProps {
  section: string;
  title: string;
  description: string;
  /** Record types searched (a module's own audit logs); every record type when absent. */
  scope?: string;
  formats?: ExportFormat[];
}

const DEFAULT_SORT: SortState = { key: 'occurredAt', direction: 'desc' };

/**
 * The audit trail as BDOI's FRS shows it (FRUM.008.02, FRPM.021.01): filters by date range, user,
 * record type, reference number and action type; sortable columns Timestamp, Module, User Id
 * (Windows ID), Performed By, Role, Action, Activity, From, To and IP Address; export of the
 * filtered entries.
 */
export function AuditTrailView({
  section,
  title,
  description,
  scope,
  formats,
}: Readonly<AuditTrailViewProps>) {
  const [filters, setFilters] = useState<AuditFilters>({
    from: today(),
    to: today(),
    username: '',
    entityType: scope ?? '',
    entityId: '',
    action: '',
  });
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState<SortState>(DEFAULT_SORT);
  const rangeError = dateRangeError(filters.from, filters.to);
  const audit = useQuery({
    queryKey: ['audit', filters, sort, page],
    queryFn: () => adminApi.audit({ ...filters, sort: sort.key, direction: sort.direction, page }),
    enabled: rangeError === null,
  });
  const set = (patch: Partial<AuditFilters>) => {
    setFilters((f) => ({ ...f, ...patch }));
    setPage(0);
  };
  const data = audit.data;

  return (
    <div className="stack">
      <PageHeader
        section={section}
        title={title}
        description={description}
        actions={<AuditExportButtons filters={filters} formats={formats} />}
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
              <UserLookup
                emptyLabel="All"
                id={id}
                value={filters.username}
                onChange={(code) => set({ username: code })}
              />
            )}
          </Field>
          <Field label="Action Type">
            {(id) => (
              <Combobox
                id={id}
                emptyLabel="All"
                value={filters.action}
                options={AUDIT_ACTIONS}
                onChange={(action) => set({ action })}
              />
            )}
          </Field>
          <Field label="Reference Number">
            {(id) => (
              <input
                id={id}
                className="input"
                placeholder="Part of the number"
                value={filters.entityId}
                onChange={(e) => set({ entityId: e.target.value })}
              />
            )}
          </Field>
          {scope === undefined && (
            <Field label="Record Type">
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
          )}
        </div>
      </Card>
      {rangeError !== null && <Notice tone="error">{rangeError}</Notice>}
      <ErrorAlert error={audit.error} />
      <Card flush>
        <DataTable<AuditEntry>
          loading={audit.isLoading}
          rows={data?.content ?? []}
          rowKey={(a) => a.id}
          sort={sort}
          onSort={(next) => {
            setSort(next);
            setPage(0);
          }}
          columns={[
            {
              key: 't',
              header: 'Timestamp',
              kind: 'datetime',
              sortKey: 'occurredAt',
              render: (a) => formatDateTime(a.occurredAt),
            },
            { key: 'm', header: 'Module', render: (a) => a.module ?? '' },
            { key: 'w', header: 'User Id', render: (a) => a.windowsId ?? '' },
            {
              key: 'u',
              header: 'Performed By',
              sortKey: 'username',
              render: (a) => <UserName login={a.username} />,
            },
            { key: 'r', header: 'Role', render: (a) => a.roleNames ?? '' },
            {
              key: 'a',
              header: 'Action',
              kind: 'status',
              sortKey: 'action',
              render: (a) => <StatusBadge status={a.actionLabel ?? a.action} tone="neutral" />,
            },
            { key: 's', header: 'Activity', render: (a) => a.summary },
            { key: 'f', header: 'From', render: (a) => a.oldValue ?? '' },
            { key: 'n', header: 'To', render: (a) => a.newValue ?? '' },
            { key: 'i', header: 'IP Address', render: (a) => a.ipAddress ?? '' },
            {
              key: 'k',
              header: 'Reference',
              sortKey: 'entityId',
              render: (a) => a.entityId ?? '',
            },
          ]}
        />
        <PageFooter data={data} onPage={setPage} />
      </Card>
    </div>
  );
}
