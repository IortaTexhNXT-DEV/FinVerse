import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { accountingApi } from '@/api/accounting';
import type { EventFilters, EventLog } from '@/api/accounting';
import { glApi } from '@/api/gl';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime, today } from '@/utils/format';
import { parseAmounts } from './ruleModel';
import { useAccountingLookups } from './useAccountingLookups';

type Filters = Omit<EventFilters, 'companyId'>;

function monthStart(): string {
  return `${today().slice(0, 8)}01`;
}

function amountsText(e: EventLog): string {
  return Object.entries(parseAmounts(e.amounts))
    .map(([k, v]) => `${k} ${formatAmount(v)}`)
    .join(', ');
}

/**
 * Event register monitor: every business event received by the accounting engine with its status,
 * the journal it posted (drill-down by batch number) or the error that stopped it.
 */
export default function EventRegisterPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { eventTypes } = useAccountingLookups();
  const [filters, setFilters] = useState<Filters>({ from: monthStart(), to: today(), page: 0 });
  const query = useQuery({
    queryKey: ['accounting-events', companyId, filters],
    queryFn: () => accountingApi.events({ ...filters, companyId, size: 25 }),
    enabled: companyId > 0,
  });
  const drill = useMutation({
    mutationFn: async (batchNo: string) => {
      const page = await glApi.journals({ companyId, batchNo, size: 5 });
      const journal = page.content.find((j) => j.batchNo === batchNo);
      if (journal === undefined) {
        throw new Error(`Journal ${batchNo} not found`);
      }
      return journal.id;
    },
    onSuccess: (id) => void navigate(`/gl/journals/${id}`),
  });
  const set = (patch: Partial<Filters>) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const data = query.data;

  return (
    <div className="stack">
      <PageHeader
        section="Accounting Engine"
        title="Event Register"
        description="Business events processed by the accounting engine. Failed events show why no journal was posted."
      />
      <Card>
        <div className="form-grid">
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.status ?? ''}
                onChange={(e) => set({ status: e.target.value || undefined })}
              >
                <option value="">All</option>
                <option value="POSTED">Posted</option>
                <option value="FAILED">Failed</option>
              </select>
            )}
          </Field>
          <Field label="Event type">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.eventType ?? ''}
                onChange={(e) => set({ eventType: e.target.value || undefined })}
              >
                <option value="">All event types</option>
                {eventTypes.map((t) => (
                  <option key={t.code} value={t.code}>
                    {t.code}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Value date from">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={filters.from}
                onChange={(e) => set({ from: e.target.value })}
              />
            )}
          </Field>
          <Field label="Value date to">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={filters.to}
                onChange={(e) => set({ to: e.target.value })}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={query.error ?? drill.error} />
      <Card flush>
        <DataTable<EventLog>
          loading={query.isLoading}
          rows={data?.content ?? []}
          rowKey={(e) => e.id}
          caption="Accounting events"
          columns={[
            { key: 't', header: 'Processed', render: (e) => formatDateTime(e.processedAt) },
            { key: 'e', header: 'Event type', render: (e) => <strong>{e.eventType}</strong> },
            { key: 's', header: 'Source', render: (e) => `${e.sourceModule} ${e.sourceReference}` },
            { key: 'r', header: 'Reference', render: (e) => e.reference ?? '' },
            { key: 'v', header: 'Value date', render: (e) => formatDate(e.valueDate) },
            { key: 'a', header: 'Amounts', render: amountsText },
            { key: 'st', header: 'Status', render: (e) => <StatusBadge status={e.status} /> },
            {
              key: 'b',
              header: 'Journal / error',
              render: (e) =>
                e.batchNo === undefined ? (
                  <span className="field-error">{e.errorMessage ?? ''}</span>
                ) : (
                  <Button size="sm" variant="ghost" onClick={() => drill.mutate(e.batchNo ?? '')}>
                    {e.batchNo}
                  </Button>
                ),
            },
          ]}
        />
        {data !== undefined && data.totalPages > 1 && (
          <div className="pagination">
            <span className="muted">
              Page {data.page + 1} of {data.totalPages} · {data.totalElements} events
            </span>
            <div className="spacer" />
            <Button
              size="sm"
              variant="secondary"
              disabled={data.page === 0}
              onClick={() => setFilters((f) => ({ ...f, page: data.page - 1 }))}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="secondary"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setFilters((f) => ({ ...f, page: data.page + 1 }))}
            >
              Next
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
}
