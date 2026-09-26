import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FolderPlus, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { screeningMatchesApi } from './api';
import type { MatchDetail } from './api';
import { FalsePositiveDialog } from './FalsePositiveDialog';
import { comparisonRows, entryChanged, isOpen, matchedText, scoreText } from './matchLogic';
import type { ComparisonRow } from './matchLogic';

function checkOf(row: ComparisonRow) {
  return row.same === undefined ? null : (
    <StatusBadge status={row.same ? 'MATCHED' : 'DIFFERENT'} />
  );
}

function Summary({ detail }: Readonly<{ detail: MatchDetail }>) {
  const m = detail.match;
  return (
    <dl className="detail-list">
      <dt>Status</dt>
      <dd>
        <StatusBadge status={m.status} />
      </dd>
      <dt>Score</dt>
      <dd>
        {scoreText(m.score)}
        {m.caseThreshold ? ' — reaches the case threshold' : ''}
      </dd>
      <dt>Matched On</dt>
      <dd>{matchedText(m)}</dd>
      <dt>List Entry</dt>
      <dd>
        {m.sourceCode} · {humanize(m.listType)} · version {m.entryVersion}
      </dd>
      <dt>Recorded</dt>
      <dd>{formatDateTime(m.createdAt)}</dd>
      <dt>Case</dt>
      <dd>{m.caseId ? `#${m.caseId}` : 'Not in a case'}</dd>
      {m.decidedBy !== undefined && m.decidedBy !== null && (
        <>
          <dt>Decided</dt>
          <dd>{`${m.decidedBy} ${formatDateTime(m.decidedAt)}`}</dd>
          <dt>Justification</dt>
          <dd>{m.decisionRemarks ?? '—'}</dd>
        </>
      )}
    </dl>
  );
}

/**
 * A screening match with the client and the list entry side by side (FR-SS-032), and the actions
 * Open Case (opens or joins the client's case) and Mark False Positive (justification and
 * evidence, FR-SS-035).
 */
export function MatchDialog({
  matchId,
  onClose,
}: Readonly<{ matchId: number; onClose: () => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [clearing, setClearing] = useState(false);
  const detail = useQuery({
    queryKey: ['screening', 'match', matchId],
    queryFn: () => screeningMatchesApi.match(matchId),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['screening'] });
  const openCase = useMutation({
    mutationFn: () => screeningMatchesApi.openCase(matchId),
    onSuccess: async (opened) => {
      await refresh();
      toast.success(
        opened.joined
          ? `Match added to case ${opened.caseNo}`
          : `Case ${opened.caseNo} opened for the match`,
      );
      onClose();
    },
  });
  const data = detail.data;
  const decidable = data !== undefined && isOpen(data.match) && can('SCR_INVESTIGATE');
  return (
    <Modal
      open
      title={data ? `Match — ${data.match.clientName}` : 'Match'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {decidable && (
            <>
              <Button
                variant="secondary"
                icon={<ShieldCheck size={16} />}
                onClick={() => setClearing(true)}
              >
                Mark False Positive
              </Button>
              <Button
                icon={<FolderPlus size={16} />}
                busy={openCase.isPending}
                onClick={() => openCase.mutate()}
              >
                Open Case
              </Button>
            </>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={detail.error} />
        <ErrorAlert error={openCase.error} />
        {detail.isLoading && <p className="muted">Loading the match…</p>}
        {data !== undefined && (
          <>
            <Summary detail={data} />
            {entryChanged(data) && (
              <p className="muted">
                The list entry has changed since this match (now version {data.entry?.entryVersion}
                ); the next screening matches the new version again.
              </p>
            )}
            <DataTable<ComparisonRow>
              caption="Client and list entry side by side"
              rows={comparisonRows(data)}
              rowKey={(r) => r.label}
              columns={[
                { key: 'label', header: 'Field', render: (r) => r.label },
                { key: 'client', header: 'Client', render: (r) => r.client },
                { key: 'entry', header: 'List Entry', render: (r) => r.entry },
                { key: 'check', header: 'Check', render: checkOf },
              ]}
            />
          </>
        )}
      </div>
      {clearing && data !== undefined && (
        <FalsePositiveDialog
          match={data.match}
          onDone={() => {
            setClearing(false);
            void refresh().then(onClose);
          }}
          onClose={() => setClearing(false)}
        />
      )}
    </Modal>
  );
}
