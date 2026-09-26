import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { screeningMatchesApi } from '../matches/api';
import type { RiskProfileRow, ScreeningMatch } from '../matches/api';
import { matchedText, scoreText } from '../matches/matchLogic';
import { casesApi } from './api';
import type { CaseRow } from './api';

/**
 * The Screening tab of the client page (design section 9; SNSRP-302, 303, 402): the client's
 * screening cases, its matches against the watchlists and the history of its risk rating and tags.
 * Shown only to holders of SCR_VIEW.
 */
export function ClientScreeningTab({ clientId }: Readonly<{ clientId: number }>) {
  const navigate = useNavigate();
  const cases = useQuery({
    queryKey: ['screening', 'client', clientId, 'cases'],
    queryFn: () => casesApi.clientCases(clientId),
  });
  const matches = useQuery({
    queryKey: ['screening', 'client', clientId, 'matches'],
    queryFn: () => screeningMatchesApi.clientMatches(clientId),
  });
  const profile = useQuery({
    queryKey: ['screening', 'client', clientId, 'risk-profile'],
    queryFn: () => screeningMatchesApi.riskProfile(clientId),
  });
  return (
    <div className="stack">
      <Card flush title="Screening Cases">
        <ErrorAlert error={cases.error} />
        <DataTable<CaseRow>
          caption="Screening cases of the client"
          rows={cases.data ?? []}
          rowKey={(c) => c.id}
          loading={cases.isLoading}
          onRowClick={(c) => void navigate(`/screening/cases/${c.id}`)}
          emptyMessage="No screening case for this client"
          columns={[
            {
              key: 'no',
              header: 'Case No.',
              render: (c) => <span className="mono">{c.caseNo}</span>,
            },
            { key: 'type', header: 'Case Type', render: (c) => humanize(c.caseType) },
            { key: 'stage', header: 'Stage', render: (c) => <StatusBadge status={c.stage} /> },
            { key: 'assignee', header: 'Assignee', render: (c) => c.assignee ?? '—' },
            { key: 'created', header: 'Created', render: (c) => formatDate(c.createdAt) },
          ]}
        />
      </Card>
      <Card flush title="Watchlist Matches">
        <ErrorAlert error={matches.error} />
        <DataTable<ScreeningMatch>
          caption="Watchlist matches of the client"
          rows={matches.data ?? []}
          rowKey={(m) => m.id}
          loading={matches.isLoading}
          emptyMessage="The client has no watchlist match"
          columns={[
            {
              key: 'entry',
              header: 'List Entry',
              render: (m) => (
                <>
                  <strong>{m.entryName}</strong>
                  <span className="cell-sub">
                    {m.sourceCode} · {humanize(m.listType)}
                  </span>
                </>
              ),
            },
            { key: 'score', header: 'Score', numeric: true, render: (m) => scoreText(m.score) },
            { key: 'fields', header: 'Matched On', render: matchedText },
            { key: 'status', header: 'Status', render: (m) => <StatusBadge status={m.status} /> },
            { key: 'at', header: 'Recorded', render: (m) => formatDateTime(m.createdAt) },
          ]}
        />
      </Card>
      <Card flush title="Risk Profile History">
        <ErrorAlert error={profile.error} />
        <DataTable<RiskProfileRow>
          caption="Risk profile history of the client"
          rows={profile.data ?? []}
          rowKey={(r) => r.id}
          loading={profile.isLoading}
          emptyMessage="No risk-profile change by screening"
          columns={[
            { key: 'at', header: 'Date / Time', render: (r) => formatDateTime(r.effectiveAt) },
            { key: 'source', header: 'Source', render: (r) => humanize(r.source) },
            {
              key: 'rating',
              header: 'Rating',
              render: (r) => `${r.previousRating ?? '—'} → ${r.riskRating ?? '—'}`,
            },
            { key: 'category', header: 'Category', render: (r) => r.categoryCode ?? '—' },
            { key: 'tags', header: 'Active Tags', render: (r) => r.activeTags || '—' },
            { key: 'why', header: 'Justification', render: (r) => r.justification ?? '—' },
            { key: 'by', header: 'By', render: (r) => r.by },
          ]}
        />
      </Card>
    </div>
  );
}
