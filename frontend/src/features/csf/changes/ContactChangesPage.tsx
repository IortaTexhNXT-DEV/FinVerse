import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { csfApi } from '@/api/csf';
import type { ChangeFilters } from '@/api/csf';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { CSF_SECTION } from '../csfCodes';
import { useChangeColumns } from '../view/changeColumns';
import { ChangeFields } from '../view/HistoryTab';

const STATUSES = [
  { id: '', label: 'All' },
  { id: 'APPLIED', label: 'Applied' },
  { id: 'REFUSED', label: 'Refused' },
  { id: 'REFERRED', label: 'Referred' },
] as const;

/**
 * Contact Changes (FR-CSF-021, 022, 041): the contact changes, refused changes and referrals of
 * the company, newest first, with the values before and after, the verification, the agent and
 * the sending to the legacy systems; filter by status, agent, dates or client.
 */
export default function ContactChangesPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const columns = useChangeColumns(true);
  const [filters, setFilters] = useState<ChangeFilters>({});
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);
  const list = useQuery({
    queryKey: ['csf', 'changes', companyId, filters, page, size],
    queryFn: () => csfApi.changes(companyId, filters, page, size),
  });
  const set = (next: ChangeFilters) => {
    setPage(0);
    setFilters({ ...filters, ...next });
  };
  const rows = list.data?.content ?? [];
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        title="Contact Changes"
        description="Contact changes, refusals and referrals made by the contact centre."
      />
      <Card>
        <div className="filter-bar">
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.status ?? ''}
                onChange={(e) => set({ status: e.target.value || undefined })}
              >
                {STATUSES.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Agent">
            {(id) => (
              <input
                id={id}
                className="input"
                placeholder="User ID"
                value={filters.agent ?? ''}
                onChange={(e) => set({ agent: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <DateInput
                id={id}
                value={filters.from ?? ''}
                onChange={(e) => set({ from: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <DateInput
                id={id}
                value={filters.to ?? ''}
                onChange={(e) => set({ to: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="Search">
            {(id) => (
              <input
                id={id}
                className="input"
                placeholder="Change No. or client"
                value={filters.q ?? ''}
                onChange={(e) => set({ q: e.target.value || undefined })}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable
          columns={columns}
          rows={rows}
          rowKey={(c) => c.id}
          loading={list.isLoading}
          onRowClick={(c) => void navigate(`/csf/clients/${String(c.clientId)}?tab=history`)}
          expanded={new Set(rows.map((c) => c.id))}
          renderExpanded={(c) => <ChangeFields change={c} />}
          caption="Contact changes"
        />
        <PageFooter data={list.data} noun="changes" onPage={setPage} onSize={setSize} />
      </Card>
    </div>
  );
}
