import { useQuery } from '@tanstack/react-query';
import { Lock, Paperclip } from 'lucide-react';
import { messagingApi } from '@/api/messaging';
import type { OutboundMessage } from '@/api/messaging';
import { DataTable } from '@/components/ui/DataTable';
import { purposeLabel } from './messagePurpose';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';

/** E-mails sent (or queued) for a record, with outcome and reason (BRNB.008 send log). */
export function SentMessages({
  entityType,
  entityId,
}: Readonly<{ entityType: string; entityId: string | number }>) {
  const messages = useQuery({
    queryKey: ['messages', entityType, String(entityId)],
    queryFn: () => messagingApi.forRecord(entityType, entityId),
  });
  return (
    <DataTable<OutboundMessage>
      caption="E-mails"
      loading={messages.isLoading}
      rows={messages.data ?? []}
      rowKey={(m) => m.id}
      emptyMessage="No e-mails sent for this record"
      columns={[
        {
          key: 'when',
          header: 'Queued',
          kind: 'datetime',
          render: (m) => formatDateTime(m.createdAt),
        },
        { key: 'purpose', header: 'Purpose', render: (m) => purposeLabel(m.purpose) },
        { key: 'to', header: 'To', render: (m) => m.recipients },
        {
          key: 'subject',
          header: 'Subject',
          render: (m) => (
            <span>
              {m.subject}{' '}
              {m.attachments.length > 0 && (
                <span className="muted" title={m.attachments.map((a) => a.fileName).join(', ')}>
                  <Paperclip size={12} aria-hidden="true" /> {m.attachments.length}
                  {m.attachments.some((a) => a.passwordProtected) && (
                    <Lock size={12} aria-label="password protected" />
                  )}
                </span>
              )}
            </span>
          ),
        },
        {
          key: 'status',
          header: 'Outcome',
          kind: 'status',
          render: (m) => <StatusBadge status={m.status} />,
        },
        {
          key: 'sent',
          header: 'Sent / Reason',
          render: (m) =>
            m.lastError ? (
              <span className="field-error">{m.lastError}</span>
            ) : (
              <span className="nowrap">{formatDateTime(m.sentAt) || '—'}</span>
            ),
        },
      ]}
    />
  );
}
