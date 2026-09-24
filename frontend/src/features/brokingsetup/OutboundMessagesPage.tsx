import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Lock, RotateCw } from 'lucide-react';
import { useState } from 'react';
import { messagingApi } from '@/api/messaging';
import type { MessageFilters, MessageStatus, OutboundMessage } from '@/api/messaging';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';

const STATUSES: MessageStatus[] = ['QUEUED', 'SENT', 'FAILED'];

/**
 * Outbound e-mail log (BRNB.008/078): every e-mail BrokerVerse sent or tried to send, with
 * recipients, attachments (protected or not), attempts and the reason of failures. Failed e-mails
 * can be queued again.
 */
export default function OutboundMessagesPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<MessageFilters>({});
  const [selected, setSelected] = useState<OutboundMessage | null>(null);
  const messages = useQuery({
    queryKey: ['messages', filters],
    queryFn: () => messagingApi.search(filters),
  });
  const retry = useMutation({
    mutationFn: messagingApi.retry,
    onSuccess: async (m) => {
      setSelected(null);
      await queryClient.invalidateQueries({ queryKey: ['messages'] });
      toast.success(`${m.subject}: ${humanize(m.status).toLowerCase()}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="Outbound Messages"
        description="E-mails sent by BIBS: quotations, slips, placements, e-policies, invoices and their separate password e-mails."
      />
      <ErrorAlert error={messages.error ?? retry.error} />
      <MessageFilterForm filters={filters} onChange={setFilters} />
      <Card flush>
        <DataTable<OutboundMessage>
          loading={messages.isLoading}
          rows={messages.data?.content ?? []}
          rowKey={(m) => m.id}
          onRowClick={setSelected}
          emptyMessage="No e-mails match."
          columns={[
            { key: 'when', header: 'Queued', render: (m) => formatDateTime(m.createdAt) },
            { key: 'purpose', header: 'Purpose', render: (m) => humanize(m.purpose) },
            { key: 'ref', header: 'Reference', render: (m) => m.reference ?? '' },
            { key: 'to', header: 'To', render: (m) => m.recipients },
            {
              key: 'subject',
              header: 'Subject',
              render: (m) => (
                <span>
                  {m.subject}{' '}
                  {m.attachments.some((a) => a.passwordProtected) && (
                    <Lock size={12} aria-label="protected attachments" />
                  )}
                </span>
              ),
            },
            { key: 'status', header: 'Status', render: (m) => <StatusBadge status={m.status} /> },
            { key: 'attempts', header: 'Attempts', numeric: true, render: (m) => m.attempts },
            {
              key: 'error',
              header: 'Reason',
              render: (m) => m.lastError ?? (m.simulated ? 'Simulated delivery' : ''),
            },
          ]}
        />
        <Pager
          page={messages.data?.page ?? 0}
          totalPages={messages.data?.totalPages ?? 0}
          total={messages.data?.totalElements ?? 0}
          noun="e-mails"
          onPage={(page) => setFilters({ ...filters, page })}
        />
      </Card>
      {selected && (
        <MessageDetail
          message={selected}
          retrying={retry.isPending}
          onRetry={() => retry.mutate(selected.id)}
          onClose={() => setSelected(null)}
        />
      )}
    </div>
  );
}

function MessageFilterForm({
  filters,
  onChange,
}: Readonly<{ filters: MessageFilters; onChange: (f: MessageFilters) => void }>) {
  const [text, setText] = useState(filters.text ?? '');
  return (
    <Card>
      <form
        className="form-grid"
        onSubmit={(e) => {
          e.preventDefault();
          onChange({ ...filters, text: text.trim() || undefined, page: 0 });
        }}
      >
        <Field label="Status">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.status ?? ''}
              onChange={(e) =>
                onChange({
                  ...filters,
                  status: (e.target.value || undefined) as MessageStatus,
                  page: 0,
                })
              }
            >
              <option value="">All</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {humanize(s)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Recipient, subject or reference">
          {(id) => (
            <input
              id={id}
              className="input"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          )}
        </Field>
        <div className="row">
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </div>
      </form>
    </Card>
  );
}

interface MessageDetailProps {
  message: OutboundMessage;
  retrying: boolean;
  onRetry: () => void;
  onClose: () => void;
}

function MessageDetail({ message, retrying, onRetry, onClose }: Readonly<MessageDetailProps>) {
  const footer =
    message.status === 'FAILED' ? (
      <Button variant="accent" icon={<RotateCw size={16} />} busy={retrying} onClick={onRetry}>
        Send Again
      </Button>
    ) : undefined;
  return (
    <Modal open title={message.subject} onClose={onClose} footer={footer}>
      <dl className="detail-list">
        <dt>To</dt>
        <dd>{message.recipients}</dd>
        <dt>Cc</dt>
        <dd>{message.cc ?? '—'}</dd>
        <dt>Status</dt>
        <dd>
          <StatusBadge status={message.status} /> {formatDateTime(message.sentAt)}
        </dd>
        <dt>Reason</dt>
        <dd className={message.lastError ? 'field-error' : ''}>{message.lastError ?? '—'}</dd>
        <dt>Attachments</dt>
        <dd>
          <AttachmentList message={message} />
        </dd>
      </dl>
      <pre className="message-body">{message.body}</pre>
    </Modal>
  );
}

function AttachmentList({ message }: Readonly<{ message: OutboundMessage }>) {
  if (message.attachments.length === 0) {
    return <>None</>;
  }
  return (
    <>
      {message.attachments.map((a) => (
        <div key={a.id}>
          {a.fileName} ({Math.ceil(a.sizeBytes / 1024)} KB
          {a.passwordProtected ? ', password protected' : ''})
          <div className="muted mono">SHA-256 {a.sha256}</div>
        </div>
      ))}
    </>
  );
}
