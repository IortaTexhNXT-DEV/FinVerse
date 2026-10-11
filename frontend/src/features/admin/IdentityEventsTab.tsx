import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { identityApi, EVENT_STATUS_LABELS, EVENT_TYPE_LABELS } from '@/api/identity';
import type { IdentityEvent, IdentityEventStatus } from '@/api/identity';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { outcomeStatus } from './identitySync';

const STATUSES = Object.keys(EVENT_STATUS_LABELS) as IdentityEventStatus[];

/**
 * The provisioning events received from UIDM-ISC and the Enterprise SSO platform, newest first,
 * with their outcome; a refused or failed event is processed again from its row (BDOI FRS
 * FRUM.002.02 and FRUM.003.03: failures recorded for administrative review).
 */
export function IdentityEventsTab() {
  const toast = useToast();
  const client = useQueryClient();
  const [filter, setFilter] = useState({ status: '', windowsId: '', from: '', to: '' });
  const [page, setPage] = useState(0);
  const events = useQuery({
    queryKey: ['identity-events', filter, page],
    queryFn: () => identityApi.events({ ...filter, page }),
  });
  const reprocess = useMutation({
    mutationFn: (id: number) => identityApi.reprocess(id),
    onSuccess: (event) => {
      toast.success(
        `Event ${String(event.id)} processed again: ${EVENT_STATUS_LABELS[event.status]}`,
      );
      void client.invalidateQueries({ queryKey: ['identity-events'] });
    },
    onError: (error) => toast.error(error.message),
  });
  const set = (patch: Partial<typeof filter>) => {
    setFilter((f) => ({ ...f, ...patch }));
    setPage(0);
  };
  return (
    <div className="stack">
      <Card>
        <div className="form-grid">
          <Field label="Outcome">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filter.status}
                onChange={(e) => set({ status: e.target.value })}
              >
                <option value="">All</option>
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {EVENT_STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Windows ID">
            {(id) => (
              <input
                id={id}
                className="input"
                value={filter.windowsId}
                onChange={(e) => set({ windowsId: e.target.value })}
              />
            )}
          </Field>
          <Field label="Received From">
            {(id) => (
              <DateInput
                id={id}
                value={filter.from}
                onChange={(e) => set({ from: e.target.value })}
              />
            )}
          </Field>
          <Field label="Received To">
            {(id) => (
              <DateInput id={id} value={filter.to} onChange={(e) => set({ to: e.target.value })} />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={events.error} />
      <Card flush>
        <DataTable<IdentityEvent>
          loading={events.isLoading}
          rows={events.data?.content ?? []}
          rowKey={(e) => e.id}
          columns={[
            { key: 'n', header: 'Event No.', render: (e) => e.id },
            {
              key: 't',
              header: 'Received',
              kind: 'datetime',
              render: (e) => formatDateTime(e.receivedAt),
            },
            { key: 's', header: 'Source', render: (e) => e.sourceLabel },
            { key: 'y', header: 'Event', render: (e) => EVENT_TYPE_LABELS[e.eventType] },
            { key: 'w', header: 'Windows ID', render: (e) => e.windowsId ?? '' },
            { key: 'u', header: 'User ID', render: (e) => e.userId ?? '' },
            { key: 'r', header: 'UIDM Request No.', render: (e) => e.uidmRequestNo ?? '' },
            {
              key: 'o',
              header: 'Outcome',
              kind: 'status',
              render: (e) => (
                <StatusBadge status={outcomeStatus(e)} label={EVENT_STATUS_LABELS[e.status]} />
              ),
            },
            { key: 'm', header: 'Details', render: (e) => e.message ?? '' },
            {
              key: 'b',
              header: 'Processed By',
              render: (e) => (e.processedBy ? <UserName login={e.processedBy} /> : ''),
            },
            { key: 'c', header: 'Attempts', numeric: true, render: (e) => e.attempts },
            {
              key: 'a',
              header: 'Actions',
              render: (e) =>
                e.reprocessable ? (
                  <RowActionMenu
                    label={`Event ${String(e.id)}`}
                    actions={[{ label: 'Process again', onSelect: () => reprocess.mutate(e.id) }]}
                  />
                ) : (
                  ''
                ),
            },
          ]}
        />
        <PageFooter data={events.data} onPage={setPage} />
      </Card>
    </div>
  );
}
