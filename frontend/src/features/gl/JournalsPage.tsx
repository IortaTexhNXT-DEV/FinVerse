import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { glApi } from '@/api/gl';
import type { Journal, JournalFilters } from '@/api/gl';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

const STATUSES = ['', 'DRAFT', 'PENDING_APPROVAL', 'POSTED', 'REJECTED', 'CANCELLED', 'REVERSED'];

/** Journal inquiry and authorization queue (filter on "Pending approval" for checkers). */
export default function JournalsPage() {
  const companyId = useCompanyId();
  const { branchId } = useWorkspace();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<Omit<JournalFilters, 'companyId'>>({ page: 0 });

  const query = useQuery({
    queryKey: ['journals', companyId, branchId, filters],
    queryFn: () => glApi.journals({ ...filters, companyId, branchId, size: 25 }),
    enabled: companyId > 0,
  });
  const set = (patch: Partial<JournalFilters>) => setFilters((f) => ({ ...f, ...patch, page: 0 }));
  const data = query.data;

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Journals"
        description="Search vouchers, follow their approval status and drill into any posting."
        actions={
          can('JOURNAL_CREATE') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/gl/journals/new')}
            >
              New journal
            </Button>
          )
        }
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
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s === '' ? 'All' : s.replace('_', ' ')}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Batch no.">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.batchNo ?? ''}
                onChange={(e) => set({ batchNo: e.target.value })}
              />
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={filters.fromDate ?? ''}
                onChange={(e) => set({ fromDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={filters.toDate ?? ''}
                onChange={(e) => set({ toDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="Inputter">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filters.inputter ?? ''}
                onChange={(e) => set({ inputter: e.target.value })}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={query.error} />
      <Card flush>
        <DataTable<Journal>
          loading={query.isLoading}
          rows={data?.content ?? []}
          rowKey={(j) => j.id}
          onRowClick={(j) => void navigate(`/gl/journals/${j.id}`)}
          caption="Journals"
          columns={[
            { key: 'no', header: 'Batch no.', render: (j) => <strong>{j.batchNo}</strong> },
            { key: 'date', header: 'Value date', render: (j) => formatDate(j.valueDate) },
            { key: 'type', header: 'Type', render: (j) => j.journalType },
            { key: 'nar', header: 'Narration', render: (j) => j.narration },
            { key: 'by', header: 'Inputter', render: (j) => j.createdBy },
            { key: 'auth', header: 'Authorizer', render: (j) => j.authorizedBy ?? '' },
            {
              key: 'amt',
              header: 'Amount',
              numeric: true,
              render: (j) => <Amount value={j.totalDebit} />,
            },
            { key: 'st', header: 'Status', render: (j) => <StatusBadge status={j.status} /> },
          ]}
        />
        {data !== undefined && data.totalPages > 1 && (
          <div className="pagination">
            <span className="muted">
              Page {data.page + 1} of {data.totalPages} · {data.totalElements} journals
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
