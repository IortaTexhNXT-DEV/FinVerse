import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { screeningMatchesApi } from './api';
import type { ScreeningMatch } from './api';
import { MatchDialog } from './MatchDialog';
import { LIST_TYPES, MATCH_TABS, matchedText, scoreError, scoreText } from './matchLogic';
import type { MatchTab } from './matchLogic';

interface Filters {
  q: string;
  listType: string;
  minScore: string;
  maxScore: string;
  uncased: boolean;
  page: number;
}

const NO_FILTERS: Filters = {
  q: '',
  listType: '',
  minScore: '',
  maxScore: '',
  uncased: false,
  page: 0,
};

function FilterBar({
  filters,
  onChange,
}: Readonly<{ filters: Filters; onChange: (f: Filters) => void }>) {
  const [draft, setDraft] = useState(filters);
  const minError = scoreError(draft.minScore);
  const maxError = scoreError(draft.maxScore);
  const apply = (next: Filters) => {
    setDraft(next);
    if (scoreError(next.minScore) === undefined && scoreError(next.maxScore) === undefined) {
      onChange({ ...next, page: 0 });
    }
  };
  return (
    <div className="worklist-filters form-grid">
      <Field label="List Type">
        {(id) => (
          <select
            id={id}
            className="select"
            value={draft.listType}
            onChange={(e) => apply({ ...draft, listType: e.target.value })}
          >
            <option value="">All list types</option>
            {LIST_TYPES.map((t) => (
              <option key={t} value={t}>
                {humanize(t)}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Score From" error={minError} hint="0 to 1">
        {(id) => (
          <input
            id={id}
            className="input"
            inputMode="decimal"
            value={draft.minScore}
            onChange={(e) => apply({ ...draft, minScore: e.target.value })}
          />
        )}
      </Field>
      <Field label="Score To" error={maxError} hint="0 to 1">
        {(id) => (
          <input
            id={id}
            className="input"
            inputMode="decimal"
            value={draft.maxScore}
            onChange={(e) => apply({ ...draft, maxScore: e.target.value })}
          />
        )}
      </Field>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={draft.uncased}
          onChange={(e) => apply({ ...draft, uncased: e.target.checked })}
        />{' '}
        Not yet in a case
      </label>
    </div>
  );
}

function MatchList({
  status,
  initialQuery,
  onOpen,
}: Readonly<{ status: MatchTab; initialQuery: string; onOpen: (m: ScreeningMatch) => void }>) {
  const companyId = useCompanyId();
  const [filters, setFilters] = useState<Filters>({ ...NO_FILTERS, q: initialQuery });
  const [showFilters, setShowFilters] = useState(false);
  const matches = useQuery({
    queryKey: ['screening', 'matches', companyId, status, filters],
    queryFn: () =>
      screeningMatchesApi.matches({
        companyId,
        status: status === 'ALL' ? '' : status,
        ...filters,
      }),
    enabled: companyId > 0,
  });
  return (
    <Card flush>
      <WorklistToolbar
        placeholder="Search client or list name"
        initial={initialQuery}
        onSearch={(q) => setFilters({ ...filters, q, page: 0 })}
        filters={{ open: showFilters, onToggle: () => setShowFilters(!showFilters) }}
      />
      {showFilters && <FilterBar filters={filters} onChange={setFilters} />}
      <ErrorAlert error={matches.error} />
      <DataTable<ScreeningMatch>
        caption="Screening matches"
        rows={matches.data?.content ?? []}
        rowKey={(m) => m.id}
        loading={matches.isLoading}
        onRowClick={onOpen}
        emptyMessage="No matches for these filters"
        columns={[
          {
            key: 'client',
            header: 'Client',
            render: (m) => (
              <>
                <strong>{m.clientName}</strong>
                <span className="cell-sub mono">{m.clientCode}</span>
              </>
            ),
          },
          {
            key: 'entry',
            header: 'List Entry',
            render: (m) => (
              <>
                <strong>{m.entryName}</strong>
                <span className="cell-sub">
                  {m.sourceCode} · {humanize(m.listType)} · v{m.entryVersion}
                </span>
              </>
            ),
          },
          {
            key: 'score',
            header: 'Score',
            numeric: true,
            render: (m) =>
              m.caseThreshold ? <strong>{scoreText(m.score)}</strong> : scoreText(m.score),
          },
          { key: 'fields', header: 'Matched On', render: matchedText },
          { key: 'status', header: 'Status', render: (m) => <StatusBadge status={m.status} /> },
          {
            key: 'case',
            header: 'Case',
            render: (m) => (m.caseId ? <span className="mono">#{m.caseId}</span> : '—'),
          },
          { key: 'at', header: 'Recorded', render: (m) => formatDateTime(m.createdAt) },
        ]}
      />
      <PageFooter
        data={matches.data}
        noun="matches"
        onPage={(page) => setFilters({ ...filters, page })}
      />
    </Card>
  );
}

/**
 * Matches (SNSRP-301, 304; FR-SS-031, 032): the matches recorded by screening with their score,
 * algorithm and matched fields, filtered by status, list type, score and client. A match opens the
 * side-by-side comparison with the actions Open Case and Mark False Positive.
 */
export default function MatchesPage() {
  const [search] = useSearchParams();
  const [tab, setTab] = useState<MatchTab>('POTENTIAL');
  const [open, setOpen] = useState<number | undefined>(() => {
    const id = Number(search.get('match') ?? 0);
    return id > 0 ? id : undefined;
  });
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Sanction Screening"
        title="Matches"
        description="Clients matched against the sanctions, PEP and internal lists. Compare each potential match with the list entry, then open a case or clear it as a false positive with evidence."
      />
      <Tabs tabs={MATCH_TABS} active={tab} onChange={setTab} />
      <MatchList
        key={tab}
        status={tab}
        initialQuery={search.get('q') ?? ''}
        onOpen={(m) => setOpen(m.id)}
      />
      {open !== undefined && <MatchDialog matchId={open} onClose={() => setOpen(undefined)} />}
    </div>
  );
}
