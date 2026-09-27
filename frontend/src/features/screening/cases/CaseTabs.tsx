import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import type { ScreeningMatch } from '../matches/api';
import { matchedText, scoreText } from '../matches/matchLogic';
import { casesApi } from './api';
import type { CaseDetail, CaseEvent, CommitteeVote } from './api';
import { stageLabel } from './caseLogic';
import { StepDialog, TextField } from './StepDialog';

interface MatchDecision {
  match: ScreeningMatch;
  kind: 'confirm' | 'clear';
}

function MatchDecisionDialog({
  caseId,
  decision,
  onDone,
  onClose,
}: Readonly<{ caseId: number; decision: MatchDecision; onDone: () => void; onClose: () => void }>) {
  const [text, setText] = useState('');
  const [touched, setTouched] = useState(false);
  const clear = decision.kind === 'clear';
  const save = useMutation({
    mutationFn: () =>
      clear
        ? casesApi.clearMatch(caseId, decision.match.id, text)
        : casesApi.confirmMatch(caseId, decision.match.id, text),
    onSuccess: onDone,
  });
  return (
    <StepDialog
      title={clear ? 'Mark False Positive' : 'Confirm True Match'}
      confirmLabel={clear ? 'Mark False Positive' : 'Confirm Match'}
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (!clear || text.trim() !== '') {
          save.mutate();
        }
      }}
    >
      <p className="muted">
        {decision.match.clientName} against {decision.match.entryName} ({decision.match.sourceCode},
        version {decision.match.entryVersion}).
        {clear
          ? ' The case documents are the evidence; the client is not matched again against this version of the entry.'
          : ' The risk rules are evaluated again on the confirmed match.'}
      </p>
      <TextField
        label={clear ? 'Justification' : 'Remarks'}
        value={text}
        onChange={setText}
        required={clear}
        max={2000}
        error={
          clear && touched && text.trim() === ''
            ? 'Enter the justification and attach the evidence'
            : undefined
        }
      />
    </StepDialog>
  );
}

/** The matches of the case with Confirm and Mark False Positive (SNSRP-301, 304). */
export function MatchesTab({ detail }: Readonly<{ detail: CaseDetail }>) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [deciding, setDeciding] = useState<MatchDecision>();
  const matches = useQuery({
    queryKey: ['screening', 'case', detail.row.id, 'matches'],
    queryFn: () => casesApi.matches(detail.row.id),
  });
  const canDecide = detail.actions.includes('MATCH_DECIDE');
  return (
    <Card flush title="Matches">
      <ErrorAlert error={matches.error} />
      <DataTable<ScreeningMatch>
        caption="Matches of the case"
        rows={matches.data ?? []}
        rowKey={(m) => m.id}
        loading={matches.isLoading}
        emptyMessage="No match is linked to this case"
        columns={[
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
          { key: 'score', header: 'Score', numeric: true, render: (m) => scoreText(m.score) },
          { key: 'fields', header: 'Matched On', render: matchedText },
          { key: 'status', header: 'Status', render: (m) => <StatusBadge status={m.status} /> },
          {
            key: 'actions',
            header: 'Actions',
            render: (m) =>
              canDecide && m.status === 'POTENTIAL' ? (
                <span className="row">
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setDeciding({ match: m, kind: 'confirm' })}
                  >
                    Confirm
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setDeciding({ match: m, kind: 'clear' })}
                  >
                    False Positive
                  </Button>
                </span>
              ) : (
                '—'
              ),
          },
        ]}
      />
      {deciding && (
        <MatchDecisionDialog
          caseId={detail.row.id}
          decision={deciding}
          onClose={() => setDeciding(undefined)}
          onDone={() => {
            setDeciding(undefined);
            toast.success('Match decision recorded');
            void queryClient.invalidateQueries({ queryKey: ['screening', 'case', detail.row.id] });
          }}
        />
      )}
    </Card>
  );
}

function change(e: CaseEvent): string {
  if (e.fromStage || e.toStage) {
    return [e.fromStage, e.toStage]
      .filter(Boolean)
      .map((s) => stageLabel(s ?? ''))
      .join(' → ');
  }
  return [e.fromValue, e.toValue].filter(Boolean).join(' → ');
}

/** The insert-only case timeline (SNSRP-401, 903; FR-SS-040). */
export function TimelineTab({ caseId }: Readonly<{ caseId: number }>) {
  const events = useQuery({
    queryKey: ['screening', 'case', caseId, 'timeline'],
    queryFn: () => casesApi.timeline(caseId),
  });
  return (
    <Card flush title="Timeline">
      <ErrorAlert error={events.error} />
      <DataTable<CaseEvent>
        caption="Case timeline"
        rows={events.data ?? []}
        rowKey={(e) => e.id}
        loading={events.isLoading}
        emptyMessage="No events yet"
        columns={[
          { key: 'at', header: 'Date / Time', render: (e) => formatDateTime(e.occurredAt) },
          { key: 'round', header: 'Round', numeric: true, render: (e) => e.roundNo },
          { key: 'event', header: 'Event', render: (e) => humanize(e.event) },
          { key: 'change', header: 'From → To', render: (e) => change(e) || '—' },
          {
            key: 'reason',
            header: 'Disposition / Reason',
            render: (e) => (e.reasonCode ? humanize(e.reasonCode) : '—'),
          },
          { key: 'remarks', header: 'Remarks', render: (e) => e.remarks ?? '—' },
          { key: 'actor', header: 'User', render: (e) => e.actor },
        ]}
      />
    </Card>
  );
}

/** The decisions of the case: dispositions, recommendation and committee votes (SNSRP-704). */
export function DecisionsTab({ detail }: Readonly<{ detail: CaseDetail }>) {
  const votes = useQuery({
    queryKey: ['screening', 'case', detail.row.id, 'votes'],
    queryFn: () => casesApi.votes(detail.row.id),
  });
  return (
    <div className="stack">
      <Card title="Recommendation">
        <dl className="detail-list">
          <dt>Disposition</dt>
          <dd>{detail.row.disposition ? humanize(detail.row.disposition) : '—'}</dd>
          <dt>Recommendation</dt>
          <dd>{detail.recommendation ?? '—'}</dd>
          <dt>STR Required</dt>
          <dd>{detail.strRequired ? 'Yes' : 'No'}</dd>
          <dt>Committee Decision</dt>
          <dd>
            {detail.committeeDecision ? humanize(detail.committeeDecision) : '—'}
            {detail.committeeDecidedAt ? ` (${formatDateTime(detail.committeeDecidedAt)})` : ''}
          </dd>
        </dl>
      </Card>
      <Card flush title="AML Committee Votes">
        <ErrorAlert error={votes.error} />
        <DataTable<CommitteeVote>
          caption="Committee votes"
          rows={votes.data ?? []}
          rowKey={(v) => v.id}
          loading={votes.isLoading}
          emptyMessage="No committee vote yet"
          columns={[
            { key: 'round', header: 'Round', numeric: true, render: (v) => v.roundNo },
            { key: 'member', header: 'Member', render: (v) => v.member },
            { key: 'decision', header: 'Decision', render: (v) => humanize(v.decision) },
            { key: 'remarks', header: 'Remarks', render: (v) => v.remarks },
            { key: 'at', header: 'Date / Time', render: (v) => formatDateTime(v.votedAt) },
          ]}
        />
      </Card>
    </div>
  );
}
