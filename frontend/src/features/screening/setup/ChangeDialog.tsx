import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { screeningSetupApi } from './api';
import type { WatchlistChange } from './api';
import { RemarksDialog } from './RemarksDialog';
import { compareValues } from './watchlistLogic';
import type { ValueLine } from './watchlistLogic';

/**
 * A watchlist change for the checker (FR-SS-023): the full entry with the before and after values,
 * the maker's remarks, and Approve / Reject. A decided change is read only; the maker never sees
 * the decision buttons.
 */
export function ChangeDialog({
  change,
  onClose,
}: Readonly<{ change: WatchlistChange; onClose: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [rejecting, setRejecting] = useState(false);
  const decide = useMutation({
    mutationFn: (action: () => Promise<WatchlistChange>) => action(),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['screening-setup', 'watchlist'] });
      toast.success(`${humanize(c.changeType)} of ${c.externalRef} ${c.status.toLowerCase()}`);
      onClose();
    },
  });
  const mayDecide =
    change.status === 'PENDING' && can('SCR_LIST_APPROVE') && change.createdBy !== user?.username;
  return (
    <Modal
      open
      title={`${humanize(change.changeType)} of ${change.externalRef}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {mayDecide && (
            <>
              <Button variant="secondary" onClick={() => setRejecting(true)}>
                Reject
              </Button>
              <Button
                variant="accent"
                busy={decide.isPending}
                onClick={() => decide.mutate(() => screeningSetupApi.approveChange(change.id))}
              >
                Approve
              </Button>
            </>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={decide.error} />
        <dl className="detail-list">
          <dt>Status</dt>
          <dd>
            <StatusBadge status={change.status} />
          </dd>
          <dt>Maker</dt>
          <dd>{`${change.createdBy} ${formatDateTime(change.createdAt)}`}</dd>
          <dt>Maker Remarks</dt>
          <dd>{change.makerRemarks}</dd>
          {change.decidedBy !== undefined && (
            <>
              <dt>Checker</dt>
              <dd>{`${change.decidedBy} ${formatDateTime(change.decidedAt)}`}</dd>
              <dt>Checker Remarks</dt>
              <dd>{change.decisionRemarks ?? '—'}</dd>
            </>
          )}
        </dl>
        <DataTable<ValueLine>
          caption="Before and after values"
          rows={compareValues(change.before, change.after)}
          rowKey={(l) => l.field}
          columns={[
            { key: 'field', header: 'Field', render: (l) => l.field },
            { key: 'before', header: 'Before', render: (l) => l.before || '—' },
            {
              key: 'after',
              header: 'After',
              render: (l) => (l.changed ? <strong>{l.after || '—'}</strong> : l.after || '—'),
            },
          ]}
        />
      </div>
      {rejecting && (
        <RemarksDialog
          title="Reject Change"
          label="Remarks for the rejection"
          confirmLabel="Reject"
          busy={decide.isPending}
          error={decide.error}
          onConfirm={(remarks) =>
            decide.mutate(() => screeningSetupApi.rejectChange(change.id, remarks))
          }
          onClose={() => setRejecting(false)}
        />
      )}
    </Modal>
  );
}
