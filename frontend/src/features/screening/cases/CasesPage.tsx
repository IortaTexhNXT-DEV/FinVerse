import { useQuery, useQueryClient } from '@tanstack/react-query';
import { UserCog } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { casesApi } from './api';
import type { CaseRow, CaseTab } from './api';
import { BulkReassignDialog } from './AssignDialogs';
import {
  CASE_STAGES,
  CASE_TABS,
  CASE_TYPES,
  SLA_STATES,
  filtersOf,
  periodError,
  searchError,
  searchParams,
  slaLabel,
  stageLabel,
  tabOf,
  toSearch,
} from './caseLogic';
import type { CaseFilters, FilterKey } from './caseLogic';

function Select({
  label,
  value,
  options,
  onChange,
  format = humanize,
}: Readonly<{
  label: string;
  value?: string;
  options: readonly string[];
  onChange: (value: string) => void;
  format?: (value: string) => string;
}>) {
  return (
    <Field label={label}>
      {(id) => (
        <select
          id={id}
          className="select"
          value={value ?? ''}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">All</option>
          {options.map((o) => (
            <option key={o} value={o}>
              {format(o)}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

const TEXT_FILTERS = [
  ['marketingUnit', 'Marketing Unit'],
  ['unitHead', 'Unit Head'],
  ['riskCategory', 'Risk Category'],
  ['assignee', 'Assignee'],
] as const;

function FilterBar({
  filters,
  onChange,
}: Readonly<{ filters: CaseFilters; onChange: (f: CaseFilters) => void }>) {
  const set = (key: FilterKey, value: string) => onChange({ ...filters, [key]: value });
  const dateError = periodError(filters.createdFrom, filters.createdTo);
  return (
    <div className="worklist-filters form-grid">
      <Field label="Created From">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={filters.createdFrom ?? ''}
            onChange={(e) => set('createdFrom', e.target.value)}
          />
        )}
      </Field>
      <Field label="Created To" error={dateError}>
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={filters.createdTo ?? ''}
            onChange={(e) => set('createdTo', e.target.value)}
          />
        )}
      </Field>
      <Select
        label="Stage"
        value={filters.stage}
        options={CASE_STAGES}
        format={stageLabel}
        onChange={(v) => set('stage', v)}
      />
      <Select
        label="Case Type"
        value={filters.caseType}
        options={CASE_TYPES}
        onChange={(v) => set('caseType', v)}
      />
      <Select
        label="SLA"
        value={filters.sla}
        options={SLA_STATES}
        format={slaLabel}
        onChange={(v) => set('sla', v)}
      />
      <Field label="Disposition">
        {(id) => (
          <LovSelect
            id={id}
            type="SCR_DISPOSITION"
            value={filters.disposition ?? ''}
            placeholder="All"
            onChange={(v) => set('disposition', v)}
          />
        )}
      </Field>
      {TEXT_FILTERS.map(([key, label]) => (
        <Field key={key} label={label}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={filters[key] ?? ''}
              onChange={(e) => set(key, e.target.value)}
            />
          )}
        </Field>
      ))}
    </div>
  );
}

function columns(
  rows: CaseRow[],
  selection: ReturnType<typeof useRowSelection>,
  selectable: boolean,
) {
  const base = [
    {
      key: 'caseNo',
      header: 'Case No.',
      render: (c: CaseRow) => <span className="mono">{c.caseNo}</span>,
    },
    {
      key: 'client',
      header: 'Client',
      render: (c: CaseRow) => (
        <>
          <strong>{c.clientName}</strong>
          <span className="cell-sub mono">{c.clientCode}</span>
        </>
      ),
    },
    {
      key: 'type',
      header: 'Case Type / Risk Category',
      render: (c: CaseRow) => (
        <>
          {humanize(c.caseType)}
          <span className="cell-sub">{c.riskCategory ?? '—'}</span>
        </>
      ),
    },
    {
      key: 'unit',
      header: 'Marketing Unit / Unit Head',
      render: (c: CaseRow) => (
        <>
          {c.marketingUnit ?? '—'}
          <span className="cell-sub">{c.unitHead ?? '—'}</span>
        </>
      ),
    },
    {
      key: 'stage',
      header: 'Stage / Assignee',
      render: (c: CaseRow) => (
        <>
          <StatusBadge status={c.stage} />
          <span className="cell-sub">{c.assignee ?? 'Queue'}</span>
        </>
      ),
    },
    {
      key: 'dates',
      header: 'Created / Due',
      render: (c: CaseRow) => (
        <>
          {formatDate(c.createdAt)}
          <span className="cell-sub">{c.dueAt ? formatDateTime(c.dueAt) : '—'}</span>
        </>
      ),
    },
    {
      key: 'sla',
      header: 'SLA',
      render: (c: CaseRow) => (c.slaState === 'NONE' ? '—' : <StatusBadge status={c.slaState} />),
    },
  ];
  return selectable
    ? [
        selectionColumn(
          rows,
          (c) => String(c.id),
          selection,
          (c) => c.caseNo,
        ),
        ...base,
      ]
    : base;
}

/**
 * Cases (SNSRP-402-404; FR-SS-041 to 043): the screening cases the user may see by tab (My Cases,
 * Team, For Approval, Committee, STR, Closed), with the search on case number and client, the
 * filters kept in the address, the SLA badge and the bulk re-assignment.
 */
export default function CasesPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [search, setSearch] = useSearchParams();
  const tab = tabOf(search.get('tab'));
  const filters = filtersOf(search);
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [bulk, setBulk] = useState(false);
  const selection = useRowSelection();
  const invalid = searchError(filters.q) ?? periodError(filters.createdFrom, filters.createdTo);
  const cases = useQuery({
    queryKey: ['screening', 'cases', companyId, tab, filters, page],
    queryFn: () => casesApi.search(searchParams(companyId, tab, filters, page)),
    enabled: companyId > 0 && invalid === undefined,
  });
  const update = (nextTab: CaseTab, next: CaseFilters) => {
    selection.clear();
    setPage(0);
    setSearch(toSearch(nextTab, next));
  };
  const rows = cases.data?.content ?? [];
  const selectable = can('SCR_CASE_ASSIGN');
  const chosen = rows.filter((c) => selection.has(String(c.id)));
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Sanction Screening"
        title="Cases"
        description="Screening cases by stage: investigate, approve, review escalations, decide in committee and prepare STRs. Open a case to work it; filters stay in the address so a list can be shared."
      />
      <Tabs tabs={CASE_TABS} active={tab} onChange={(t) => update(t, filters)} />
      <Card flush>
        <WorklistToolbar
          placeholder="Search case no. or client"
          initial={filters.q ?? ''}
          onSearch={(q) => update(tab, { ...filters, q })}
          filters={{ open: showFilters, onToggle: () => setShowFilters(!showFilters) }}
        >
          {selectable && (
            <Button
              variant="secondary"
              icon={<UserCog size={16} />}
              disabled={chosen.length === 0}
              onClick={() => setBulk(true)}
            >
              Re-assign
            </Button>
          )}
        </WorklistToolbar>
        {showFilters && <FilterBar filters={filters} onChange={(f) => update(tab, f)} />}
        {invalid !== undefined && (
          <p className="field-error" role="alert">
            {invalid}
          </p>
        )}
        <ErrorAlert error={cases.error} />
        <DataTable<CaseRow>
          caption="Screening cases"
          rows={rows}
          rowKey={(c) => c.id}
          loading={cases.isLoading}
          onRowClick={(c) => void navigate(`/screening/cases/${c.id}`)}
          emptyMessage="No screening cases for this tab and these filters"
          columns={columns(rows, selection, selectable)}
        />
        <PageFooter data={cases.data} noun="cases" onPage={setPage} />
      </Card>
      {bulk && (
        <BulkReassignDialog
          cases={chosen}
          onClose={() => setBulk(false)}
          onDone={() => {
            setBulk(false);
            selection.clear();
            void queryClient.invalidateQueries({ queryKey: ['screening', 'cases'] });
          }}
        />
      )}
    </div>
  );
}
