import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { EVENT_STATUS_LABELS, EVENT_TYPE_LABELS, identityApi } from '@/api/identity';
import type { IdentityEvent } from '@/api/identity';
import { userAccessApi } from '@/api/userAccess';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { RemarkDialog } from './RemarkDialog';

type Outcome = 'REFUSED' | 'FAILED';

/**
 * The events UIDM-ISC sent that were refused or failed and still need a decision: Retry processes
 * an event again once the cause is corrected; Dismiss closes it with a remark (the event stays in
 * the Events tab).
 */
export function RefusedEventsTab() {
  const toast = useToast();
  const queries = useQueryClient();
  const [outcome, setOutcome] = useState<Outcome>('REFUSED');
  const [page, setPage] = useState(0);
  const [dismissing, setDismissing] = useState<IdentityEvent | null>(null);
  const events = useQuery({
    queryKey: ['identity-events', 'open', outcome, page],
    queryFn: () => identityApi.events({ status: outcome, open: true, page }),
  });
  const refresh = async () => {
    await queries.invalidateQueries({ queryKey: ['identity-events'] });
    await queries.invalidateQueries({ queryKey: ['identity'] });
  };
  const retry = useMutation({
    mutationFn: (id: number) => identityApi.reprocess(id),
    onSuccess: async (e) => {
      toast.success(`Event ${String(e.id)} processed again: ${EVENT_STATUS_LABELS[e.status]}`);
      await refresh();
    },
    onError: (error) => toast.error(error.message),
  });
  const dismiss = useMutation({
    mutationFn: ({ id, remark }: { id: number; remark: string }) =>
      userAccessApi.dismissEvent(id, remark),
    onSuccess: async () => {
      toast.success('Event dismissed');
      setDismissing(null);
      await refresh();
    },
  });
  return (
    <div className="stack">
      <div className="row" style={{ gap: 'var(--space-2)' }}>
        <label htmlFor="refused-outcome">Outcome</label>
        <select
          id="refused-outcome"
          className="select"
          value={outcome}
          onChange={(e) => {
            setOutcome(e.target.value as Outcome);
            setPage(0);
          }}
        >
          <option value="REFUSED">Refused</option>
          <option value="FAILED">Failed</option>
        </select>
      </div>
      <ErrorAlert error={events.error} />
      <Card flush>
        <DataTable<IdentityEvent>
          caption="Events to review"
          loading={events.isLoading}
          rows={events.data?.content ?? []}
          rowKey={(e) => e.id}
          emptyMessage="No event waits for a decision"
          columns={[
            { key: 'n', header: 'Event No.', render: (e) => e.id },
            {
              key: 't',
              header: 'Received',
              kind: 'datetime',
              render: (e) => formatDateTime(e.receivedAt),
            },
            { key: 'y', header: 'Event', render: (e) => EVENT_TYPE_LABELS[e.eventType] },
            { key: 'w', header: 'Windows ID', render: (e) => e.windowsId ?? '' },
            { key: 'u', header: 'User ID', render: (e) => e.userId ?? '' },
            { key: 'r', header: 'UIDM Request No.', render: (e) => e.uidmRequestNo ?? '' },
            { key: 'm', header: 'Reason', render: (e) => e.message ?? '' },
            { key: 'c', header: 'Attempts', numeric: true, render: (e) => e.attempts },
            {
              key: 'a',
              header: 'Actions',
              kind: 'actions',
              render: (e) => (
                <RowActionMenu
                  label={`Event ${String(e.id)}`}
                  actions={[
                    ...(e.reprocessable
                      ? [{ label: 'Retry', onSelect: () => retry.mutate(e.id) }]
                      : []),
                    { label: 'Dismiss', onSelect: () => setDismissing(e) },
                  ]}
                />
              ),
            },
          ]}
        />
        <PageFooter data={events.data} onPage={setPage} />
      </Card>
      {dismissing !== null && (
        <RemarkDialog
          title={`Dismiss event ${String(dismissing.id)}`}
          record={dismissing.message ?? EVENT_TYPE_LABELS[dismissing.eventType]}
          effect="The event leaves this list; it stays in the Events tab with its outcome."
          confirmLabel="Dismiss"
          busy={dismiss.isPending}
          error={dismiss.error}
          onConfirm={(remark) => dismiss.mutate({ id: dismissing.id, remark })}
          onClose={() => setDismissing(null)}
        />
      )}
    </div>
  );
}
