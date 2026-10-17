import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { csfCaseApi } from '@/api/csfCases';
import type { CaseCount, DashboardFilters } from '@/api/csfCases';
import { LovSelect } from '@/components/broking/LovSelect';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { CSF_SECTION } from '../csfCodes';
import { CASE_LOV } from './caseCodes';
import { CASE_COLUMNS } from './caseColumns';

const STATUSES = [
  { code: '', label: 'All' },
  { code: 'WITH_FULFILMENT', label: 'Open - With Fulfillment Unit' },
  { code: 'RETURNED_TO_AGENT', label: 'Open - Returned to Agent' },
  { code: 'CSR_ACTION_REQUIRED', label: 'Open - CSR Action Required' },
  { code: 'CLOSED_BY_FULFILMENT', label: 'Closed by Fulfillment Unit' },
  { code: 'CLOSED_BY_CSR', label: 'Closed by CSR' },
] as const;

function firstOfMonth(): string {
  const d = new Date();
  return `${String(d.getFullYear())}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

function Distribution({ title, counts }: Readonly<{ title: string; counts: CaseCount[] }>) {
  return (
    <Card title={title}>
      <DataTable
        columns={[
          { key: 'label', header: 'Value', render: (c) => c.label },
          { key: 'count', header: 'Cases', numeric: true, render: (c) => c.count },
        ]}
        rows={counts}
        rowKey={(c) => c.code}
        emptyMessage="No cases"
        caption={title}
      />
    </Card>
  );
}

/**
 * Case Dashboard (FR-CSF-057, 059): for the period and filters, the case volume, the ageing, the
 * SLA compliance rate, breached versus not breached and the distribution by status, case type,
 * nature of transaction and fulfilment unit, from the same cases as the case reports; the open
 * cases with their SLA indicators, earliest due first.
 */
export default function CaseDashboardPage() {
  const companyId = useCompanyId();
  const [filters, setFilters] = useState<DashboardFilters>({ from: firstOfMonth() });
  const set = (patch: DashboardFilters) => setFilters((f) => ({ ...f, ...patch }));
  const data = useQuery({
    queryKey: ['csf-cases', 'dashboard', companyId, filters],
    queryFn: () => csfCaseApi.dashboard(companyId, filters),
  });
  const d = data.data;
  const approaching = (d?.openCases ?? []).filter((c) => c.sla.status === 'APPROACHING').length;
  return (
    <div className="stack">
      <PageHeader
        section={CSF_SECTION}
        title="Case Dashboard"
        description="Case volume, ageing and SLA compliance of the contact centre."
      />
      <Card>
        <div className="filter-bar">
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
          <Field label="Case Type">
            {(id) => (
              <LovSelect
                id={id}
                type={CASE_LOV.type}
                value={filters.caseType ?? ''}
                placeholder="All"
                onChange={(v) => set({ caseType: v || undefined })}
              />
            )}
          </Field>
          <Field label="Priority">
            {(id) => (
              <LovSelect
                id={id}
                type={CASE_LOV.priority}
                value={filters.priority ?? ''}
                placeholder="All"
                onChange={(v) => set({ priority: v || undefined })}
              />
            )}
          </Field>
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.status ?? ''}
                onChange={(e) => set({ status: e.target.value || undefined })}
              >
                {STATUSES.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={data.error} onRetry={() => void data.refetch()} />
      {d && (
        <>
          <div className="grid-4">
            <Kpi label="Cases created" value={d.created} />
            <Kpi label="Cases closed" value={d.closed} />
            <Kpi label="Open cases" value={d.open} />
            <Kpi
              label="SLA compliance rate"
              value={d.slaCompliance === null ? '-' : `${String(d.slaCompliance)}%`}
              accent
            />
            <Kpi
              label="Average age of closed cases"
              value={d.averageAgeClosed === null ? '-' : `${String(d.averageAgeClosed)} days`}
            />
            <Kpi label="Breached" value={d.breached} />
            <Kpi label="Not breached" value={d.notBreached} />
            <Kpi label="Approaching breach" value={approaching} />
            <Kpi label="Escalated" value={d.escalated} />
          </div>
          <Card title="Open Cases" flush>
            <DataTable
              columns={CASE_COLUMNS}
              rows={d.openCases}
              rowKey={(c) => c.id}
              emptyMessage="No open cases"
              caption="Open cases"
            />
          </Card>
          <div className="grid-2">
            <Distribution title="By Status" counts={d.byStatus} />
            <Distribution title="By Case Type" counts={d.byType} />
            <Distribution title="By Nature of Transaction" counts={d.byNature} />
            <Distribution title="By Fulfilment Unit" counts={d.byUnit} />
            <Distribution title="Open Cases by Ageing" counts={d.ageing} />
          </div>
        </>
      )}
    </div>
  );
}
