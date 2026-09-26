import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { screeningMatchesApi } from './api';
import type { ScreeningMatch, ScreeningRun, ScreeningTrigger } from './api';
import { TRIGGERS, matchedText, scoreText } from './matchLogic';

function RunDialog({ run, onClose }: Readonly<{ run: ScreeningRun; onClose: () => void }>) {
  const navigate = useNavigate();
  const matches = useQuery({
    queryKey: ['screening', 'run-matches', run.id],
    queryFn: () => screeningMatchesApi.runMatches(run.id),
  });
  return (
    <Modal
      open
      title={`Screening Run ${run.runNo}`}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="stack">
        <dl className="detail-list">
          <dt>Status</dt>
          <dd>
            <StatusBadge status={run.status} />
          </dd>
          <dt>Trigger</dt>
          <dd>
            {humanize(run.trigger)}
            {run.reference ? ` (${run.reference})` : ''}
          </dd>
          <dt>Scope</dt>
          <dd>
            {run.scope}
            {run.fullRescreen ? ' — full list' : ' — changed entries'}
          </dd>
          <dt>Configuration</dt>
          <dd>
            Matching criteria #{run.matchVersionId}
            {run.riskVersionId ? `, risk rules #${run.riskVersionId}` : ', no risk rules in force'}
          </dd>
          <dt>Counts</dt>
          <dd>
            {run.clientsScreened} client(s), {run.entriesScreened} entr(ies), {run.matches} new
            match(es), {run.riskChanges} risk change(s), {run.casesOpened} case(s)
          </dd>
          <dt>Time</dt>
          <dd>
            {formatDateTime(run.startedAt)} – {formatDateTime(run.endedAt)} by {run.createdBy}
          </dd>
          {run.error && (
            <>
              <dt>Error</dt>
              <dd>{run.error}</dd>
            </>
          )}
        </dl>
        <ErrorAlert error={matches.error} />
        <DataTable<ScreeningMatch>
          caption="Matches recorded by the run"
          rows={matches.data ?? []}
          rowKey={(m) => m.id}
          loading={matches.isLoading}
          onRowClick={(m) => {
            void navigate(`/screening/matches?match=${m.id}`);
          }}
          emptyMessage="The run recorded no new match"
          columns={[
            { key: 'client', header: 'Client', render: (m) => m.clientName },
            { key: 'entry', header: 'List Entry', render: (m) => m.entryName },
            { key: 'score', header: 'Score', numeric: true, render: (m) => scoreText(m.score) },
            { key: 'on', header: 'Matched On', render: matchedText },
            { key: 'status', header: 'Status', render: (m) => <StatusBadge status={m.status} /> },
          ]}
        />
      </div>
    </Modal>
  );
}

/**
 * Screening Runs (SNSRP-602; FR-SS-030): the log of every screening run with its trigger (client
 * registered or changed, account submitted, list change, batch window, manual), scope,
 * configuration versions and counts; a run opens the matches it recorded.
 */
export default function RunsPage() {
  const companyId = useCompanyId();
  const [trigger, setTrigger] = useState<ScreeningTrigger | ''>('');
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState<ScreeningRun>();
  const runs = useQuery({
    queryKey: ['screening', 'runs', companyId, trigger, page],
    queryFn: () => screeningMatchesApi.runs({ companyId, trigger, page }),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Sanction Screening"
        title="Screening Runs"
        description="Every screening run with its trigger, the clients and list entries screened, the configuration used and the matches, risk changes and cases it produced."
      />
      <Card flush>
        <div className="worklist-filters">
          <select
            className="select"
            aria-label="Trigger"
            value={trigger}
            onChange={(e) => {
              setTrigger(e.target.value as ScreeningTrigger | '');
              setPage(0);
            }}
          >
            <option value="">All triggers</option>
            {TRIGGERS.map((t) => (
              <option key={t} value={t}>
                {humanize(t)}
              </option>
            ))}
          </select>
        </div>
        <ErrorAlert error={runs.error} />
        <DataTable<ScreeningRun>
          caption="Screening runs"
          rows={runs.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={runs.isLoading}
          onRowClick={setOpen}
          emptyMessage="No screening run yet"
          columns={[
            {
              key: 'no',
              header: 'Run No.',
              render: (r) => <span className="mono">{r.runNo}</span>,
            },
            {
              key: 'trigger',
              header: 'Trigger',
              render: (r) => (
                <>
                  {humanize(r.trigger)}
                  {r.reference && <span className="cell-sub">{r.reference}</span>}
                </>
              ),
            },
            { key: 'clients', header: 'Clients', numeric: true, render: (r) => r.clientsScreened },
            { key: 'entries', header: 'Entries', numeric: true, render: (r) => r.entriesScreened },
            { key: 'matches', header: 'New Matches', numeric: true, render: (r) => r.matches },
            { key: 'risk', header: 'Risk Changes', numeric: true, render: (r) => r.riskChanges },
            { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            { key: 'start', header: 'Started', render: (r) => formatDateTime(r.startedAt) },
          ]}
        />
        <PageFooter data={runs.data} noun="runs" onPage={setPage} />
      </Card>
      {open !== undefined && <RunDialog run={open} onClose={() => setOpen(undefined)} />}
    </div>
  );
}
