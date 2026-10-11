import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { CandidateRow, RenewalFilters, RenewalTab } from '@/api/renewal';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import type { RowSelection } from '@/components/broking/rowSelection';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { ExportButtons } from '@/features/reports/ExportButtons';
import { CandidateFilters } from './CandidateFilters';
import { candidateColumns } from './candidateColumns';
import type { RenewalTabDef } from './renewalCodes';

/** A quick filter chip of a list. */
export interface QuickFilter {
  id: string;
  label: string;
  filters: RenewalFilters;
}

interface CandidateListProps {
  tabs?: readonly RenewalTabDef[];
  initialTab?: RenewalTab;
  /** Criteria always applied (e.g. the user's renewals). */
  base?: RenewalFilters;
  quickFilters?: readonly QuickFilter[];
  /** Bulk actions for the selected renewals; the list is selectable when given. */
  actions?: (selected: string[], selection: RowSelection, tab: RenewalTab) => ReactNode;
  emptyMessage?: string;
}

function QuickFilters({
  filters,
  active,
  onChange,
}: Readonly<{ filters: readonly QuickFilter[]; active: string; onChange: (id: string) => void }>) {
  return (
    <span className="rnw-flags">
      {filters.map((q) => (
        <Button
          key={q.id}
          variant={active === q.id ? 'primary' : 'ghost'}
          onClick={() => onChange(active === q.id ? '' : q.id)}
        >
          {q.label}
        </Button>
      ))}
    </span>
  );
}

function ExportButton({ filters }: Readonly<{ filters: RenewalFilters }>) {
  const companyId = useCompanyId();
  const download = useFileDownload();
  const [format, setFormat] = useState<ExportFormat>('XLSX');
  return (
    <ExportButtons
      formats={['XLSX', 'PDF', 'CSV']}
      variant="ghost"
      prefix="Download"
      pending={download.isPending ? format : undefined}
      onExport={(f) => {
        setFormat(f);
        const as = f === 'PDF' ? 'pdf' : 'csv';
        download.mutate(() =>
          f === 'XLSX'
            ? renewalApi.exportList(companyId, filters)
            : renewalApi.exportListAs(companyId, filters, as),
        );
      }}
    />
  );
}

/** The state of a renewal list: tab, criteria, quick filter, search and page. */
function useListState(
  tabs: readonly RenewalTabDef[] | undefined,
  initialTab: RenewalTab | undefined,
) {
  const [tab, setTab] = useState<RenewalTab>(initialTab ?? tabs?.[0]?.id ?? 'ALL');
  const [criteria, setCriteria] = useState<RenewalFilters>({});
  const [quick, setQuick] = useState('');
  const [text, setText] = useState('');
  const [page, setPage] = useState(0);
  const selection = useRowSelection();
  const changed = () => {
    setPage(0);
    selection.clear();
  };
  return {
    tab,
    criteria,
    quick,
    text,
    page,
    selection,
    setPage,
    setTab: (t: RenewalTab) => {
      setTab(t);
      changed();
    },
    setQuick: (q: string) => {
      setQuick(q);
      changed();
    },
    setText: (q: string) => {
      setText(q);
      changed();
    },
    setCriterion: (key: keyof RenewalFilters, value: string) => {
      setCriteria((c) => ({ ...c, [key]: value === '' ? undefined : value }));
      changed();
    },
  };
}

/**
 * A renewal list (FR-RN-012-014, 040, 062): tabs, search by renewal reference, ARN, invoice,
 * policy, PN or client, the Filters panel with the multi-select criteria ("all except" included)
 * and the expiry range, the columns with the Classification pill and the flag chips, selection for
 * the bulk actions, chunks of 200 rows and the spreadsheet download of the list.
 */
export function CandidateList({
  tabs,
  initialTab,
  base,
  quickFilters,
  actions,
  emptyMessage = 'No renewals to display',
}: Readonly<CandidateListProps>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [showFilters, setShowFilters] = useState(false);
  const s = useListState(tabs, initialTab);
  const quickCriteria = quickFilters?.find((q) => q.id === s.quick)?.filters;
  const filters: RenewalFilters = {
    ...base,
    ...s.criteria,
    ...quickCriteria,
    tab: s.tab,
    q: s.text,
  };
  const list = useQuery({
    queryKey: ['renewal', 'list', companyId, filters, s.page],
    queryFn: () => renewalApi.list(companyId, filters, s.page),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  // The amounts are hidden from the users who may not read them (LAMD, Contact Center): the
  // server leaves them out of the rows, and the list then has no amount column.
  const columns = candidateColumns(rows.some((r) => Boolean(r.money.currency)));
  const allColumns = actions
    ? [
        selectionColumn(
          rows,
          (r) => r.renewalRef,
          s.selection,
          (r) => r.renewalRef,
        ),
        ...columns,
      ]
    : columns;
  return (
    <Card flush>
      {tabs && <Tabs tabs={tabs} active={s.tab} onChange={s.setTab} />}
      <WorklistToolbar
        placeholder="Search Renewal Ref / Policy / PN / Client"
        onSearch={s.setText}
        filters={{ open: showFilters, onToggle: () => setShowFilters((f) => !f) }}
        extra={
          quickFilters && (
            <QuickFilters filters={quickFilters} active={s.quick} onChange={s.setQuick} />
          )
        }
      >
        {actions?.(s.selection.keys, s.selection, s.tab)}
        {can('RNW_EXPORT') && <ExportButton filters={filters} />}
      </WorklistToolbar>
      {showFilters && <CandidateFilters criteria={s.criteria} onChange={s.setCriterion} />}
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <DataTable<CandidateRow>
        loading={list.isLoading}
        rows={rows}
        rowKey={(r) => r.renewalRef}
        onRowClick={(r) => void navigate(`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`)}
        emptyMessage={emptyMessage}
        columns={allColumns}
      />
      <PageFooter data={list.data} noun="renewals" onPage={s.setPage} />
    </Card>
  );
}
