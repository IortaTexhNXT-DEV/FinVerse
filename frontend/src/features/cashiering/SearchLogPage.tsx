import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { TextField } from './CashFields';

interface SearchLogRow {
  id: number;
  searchedAt: string;
  user: string;
  criteria: string;
  results: number;
}

interface LogFilters {
  user: string;
  from: string;
  to: string;
}

const COLUMNS: Column<SearchLogRow>[] = [
  { key: 'at', header: 'Date and Time', render: (r) => formatDateTime(r.searchedAt) },
  { key: 'user', header: 'User', render: (r) => r.user },
  { key: 'criteria', header: 'Criteria', render: (r) => r.criteria },
  { key: 'results', header: 'Results', numeric: true, render: (r) => r.results },
];

/**
 * Search Log (FRS.CSH.08.01.04): every receipt search with the user, the criteria in words, the
 * date and time and the number of results, for the Auditor and the Cashiering Team Leader.
 */
export default function SearchLogPage() {
  const companyId = useCompanyId();
  const [filters, setFilters] = useState<LogFilters>({ user: '', from: '', to: '' });
  const [applied, setApplied] = useState<LogFilters>(filters);
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['cashiering', 'search-log', companyId, applied, page],
    queryFn: () =>
      api.get<PageResponse<SearchLogRow>>(
        `/cashiering/search-log${toQuery({ companyId, ...applied, page })}`,
      ),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Search Log"
        description="The receipt searches with their user, criteria and number of results."
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <div className="worklist-filters csh-filters">
          <TextField
            label="User"
            value={filters.user}
            onChange={(user) => setFilters({ ...filters, user })}
          />
          <TextField
            label="From"
            type="date"
            value={filters.from}
            onChange={(from) => setFilters({ ...filters, from })}
          />
          <TextField
            label="To"
            type="date"
            value={filters.to}
            onChange={(to) => setFilters({ ...filters, to })}
          />
        </div>
        <div className="worklist-toolbar">
          <Button
            variant="secondary"
            onClick={() => {
              setApplied(filters);
              setPage(0);
            }}
          >
            Search
          </Button>
        </div>
        <DataTable
          caption="Receipt searches"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={list.isLoading}
          emptyMessage="No search in this period"
        />
        <PageFooter data={list.data} noun="searches" onPage={setPage} />
      </Card>
    </div>
  );
}
