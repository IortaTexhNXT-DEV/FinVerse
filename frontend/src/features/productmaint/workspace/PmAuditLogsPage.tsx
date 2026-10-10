import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { AuditEntry } from '@/api/admin';
import { auditExportParams, pmAuditApi } from '@/api/pmWorkspace';
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
import { AUDIT_ACTIONS, dateRangeError } from '@/features/admin/auditTrail';
import { today } from '@/utils/format';
import { PM_AUDIT_COLUMNS } from './auditColumns';
import { ExportButtons } from './ExportButtons';

interface Filters {
  from: string;
  to: string;
  username: string;
  entityId: string;
  action: string;
}

const DEFAULT_SORT: SortState = { key: 'occurredAt', direction: 'desc' };

/**
 * Audit Logs of Product Maintenance (BDOI FRS FRPM.021.01): every activity on packages, products,
 * package, quotation and deactivation requests, filtered by date range, action type, reference
 * number and user, with sortable columns; the filtered entries export to CSV and Excel as "Audit
 * Logs_MMDDYYYY".
 */
export default function PmAuditLogsPage() {
  const [filters, setFilters] = useState<Filters>({
    from: today(),
    to: today(),
    username: '',
    entityId: '',
    action: '',
  });
  const [sort, setSort] = useState<SortState>(DEFAULT_SORT);
  const [page, setPage] = useState(0);
  const rangeError = dateRangeError(filters.from, filters.to);
  const audit = useQuery({
    queryKey: ['pm-audit', filters, sort, page],
    queryFn: () =>
      pmAuditApi.search({ ...filters, sort: sort.key, direction: sort.direction, page }),
    enabled: rangeError === null,
  });
  const set = (patch: Partial<Filters>) => {
    setFilters((f) => ({ ...f, ...patch }));
    setPage(0);
  };
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Audit Logs"
        description="Who changed packages, products and requests, and when."
        actions={
          <ExportButtons
            report="PM-AUDIT"
            params={auditExportParams(filters)}
            formats={['CSV', 'XLSX']}
          />
        }
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
        </div>
      </Card>
      {rangeError !== null && <Notice tone="error">{rangeError}</Notice>}
      <ErrorAlert error={audit.error} />
      <Card flush>
        <DataTable<AuditEntry>
          loading={audit.isLoading}
          rows={audit.data?.content ?? []}
          rowKey={(a) => a.id}
          sort={sort}
          onSort={(next) => {
            setSort(next);
            setPage(0);
          }}
          columns={PM_AUDIT_COLUMNS}
          emptyMessage="No activity for these filters."
        />
        <PageFooter data={audit.data} onPage={setPage} />
      </Card>
    </div>
  );
}
