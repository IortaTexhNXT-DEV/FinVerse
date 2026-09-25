import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw, RotateCcw, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { integrationEventsApi } from '@/api/integrationEvents';
import type {
  DeadLetter,
  DeadLetterStatus,
  IntegrationTopic,
  OutboxEvent,
  OutboxStatus,
} from '@/api/integrationEvents';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

type View = 'dead-letters' | 'outbox' | 'topics';

const VIEWS = [
  { id: 'dead-letters', label: 'Dead Letters' },
  { id: 'outbox', label: 'Outbox' },
  { id: 'topics', label: 'Topics' },
] as const;

const DEAD_LETTER_STATUSES: readonly (DeadLetterStatus | 'ALL')[] = [
  'NEW',
  'RETRIED',
  'DISCARDED',
  'ALL',
];
const OUTBOX_STATUSES: readonly OutboxStatus[] = ['FAILED', 'PENDING', 'SENT', 'LOCAL'];
const QUERY_KEY = ['integration-events'];

/** Invalidates the screen's queries and confirms an action. */
function useDone() {
  const toast = useToast();
  const queryClient = useQueryClient();
  return (message: string) => async () => {
    await queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    toast.success(message);
  };
}

function StatusSelect<T extends string>({
  value,
  options,
  onChange,
}: Readonly<{ value: T; options: readonly T[]; onChange: (value: T) => void }>) {
  return (
    <Field label="Status">
      {(id) => (
        <select
          id={id}
          className="select"
          value={value}
          onChange={(e) => onChange(e.target.value as T)}
        >
          {options.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
}

function DeadLettersCard() {
  const done = useDone();
  const [status, setStatus] = useState<DeadLetterStatus | 'ALL'>('NEW');
  const letters = useQuery({
    queryKey: [...QUERY_KEY, 'dead-letters', status],
    queryFn: () => integrationEventsApi.deadLetters(status),
  });
  const retry = useMutation({
    mutationFn: integrationEventsApi.retryDeadLetter,
    onSuccess: done('Event published again to its topic'),
  });
  const discard = useMutation({
    mutationFn: integrationEventsApi.discardDeadLetter,
    onSuccess: done('Dead letter discarded'),
  });
  return (
    <>
      <ErrorAlert error={letters.error ?? retry.error ?? discard.error} />
      <Card
        title="Dead letters"
        actions={
          <StatusSelect value={status} options={DEAD_LETTER_STATUSES} onChange={setStatus} />
        }
        flush
      >
        <DataTable<DeadLetter>
          loading={letters.isLoading}
          rows={letters.data?.content ?? []}
          rowKey={(d) => d.id}
          emptyMessage="No dead letters."
          columns={[
            { key: 'r', header: 'Received', render: (d) => formatDateTime(d.receivedAt) },
            { key: 't', header: 'Topic', render: (d) => <code>{d.originalTopic}</code> },
            { key: 'k', header: 'Key', render: (d) => d.key ?? '' },
            { key: 'g', header: 'Consumer', render: (d) => d.consumerGroup ?? '' },
            { key: 'e', header: 'Error', render: (d) => d.errorMessage ?? '' },
            { key: 's', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (d) =>
                d.status === 'NEW' && (
                  <span className="row">
                    <Button
                      size="sm"
                      variant="secondary"
                      icon={<RotateCcw size={14} />}
                      busy={retry.isPending && retry.variables === d.id}
                      onClick={() => retry.mutate(d.id)}
                    >
                      Retry
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={<Trash2 size={14} />}
                      busy={discard.isPending && discard.variables === d.id}
                      onClick={() => discard.mutate(d.id)}
                    >
                      Discard
                    </Button>
                  </span>
                ),
            },
          ]}
        />
      </Card>
    </>
  );
}

function OutboxCard() {
  const done = useDone();
  const [status, setStatus] = useState<OutboxStatus>('FAILED');
  const events = useQuery({
    queryKey: [...QUERY_KEY, 'outbox', status],
    queryFn: () => integrationEventsApi.outbox(status),
  });
  const retry = useMutation({
    mutationFn: integrationEventsApi.retryOutbox,
    onSuccess: done('Event queued for sending'),
  });
  return (
    <>
      <ErrorAlert error={events.error ?? retry.error} />
      <Card
        title="Outbox"
        actions={<StatusSelect value={status} options={OUTBOX_STATUSES} onChange={setStatus} />}
        flush
      >
        <DataTable<OutboxEvent>
          loading={events.isLoading}
          rows={events.data?.content ?? []}
          rowKey={(o) => o.id}
          emptyMessage="No events with this status."
          columns={[
            { key: 'o', header: 'Occurred', render: (o) => formatDateTime(o.occurredAt) },
            { key: 't', header: 'Topic', render: (o) => <code>{o.topic}</code> },
            { key: 'k', header: 'Key', render: (o) => o.key },
            { key: 'n', header: 'Attempts', numeric: true, render: (o) => o.attempts },
            { key: 'e', header: 'Last error', render: (o) => o.lastError ?? '' },
            { key: 's', header: 'Status', render: (o) => <StatusBadge status={o.status} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (o) =>
                o.status === 'FAILED' && (
                  <Button
                    size="sm"
                    variant="secondary"
                    icon={<RotateCcw size={14} />}
                    busy={retry.isPending && retry.variables === o.id}
                    onClick={() => retry.mutate(o.id)}
                  >
                    Send Again
                  </Button>
                ),
            },
          ]}
        />
      </Card>
    </>
  );
}

function TopicsCard({
  topics,
  loading,
}: Readonly<{ topics: IntegrationTopic[]; loading: boolean }>) {
  return (
    <Card title="Topic catalogue" flush>
      <DataTable<IntegrationTopic>
        loading={loading}
        rows={topics}
        rowKey={(t) => t.name}
        columns={[
          { key: 'n', header: 'Topic', render: (t) => <code>{t.name}</code> },
          { key: 'e', header: 'Event types', render: (t) => t.eventTypes.join(', ') },
          { key: 'd', header: 'Content', render: (t) => t.description },
          {
            key: 'l',
            header: 'Dead-letter topic',
            render: (t) => <code>{t.deadLetterTopic}</code>,
          },
        ]}
      />
    </Card>
  );
}

/**
 * Support screen of the integration events (Kafka): dead letters with retry and discard, the
 * transactional outbox (send a FAILED event again) and the topic catalogue. System Administrator
 * only.
 */
export default function IntegrationEventsPage() {
  const queryClient = useQueryClient();
  const [view, setView] = useState<View>('dead-letters');
  const topics = useQuery({
    queryKey: [...QUERY_KEY, 'topics'],
    queryFn: integrationEventsApi.topics,
  });
  const kafkaEnabled = topics.data?.[0]?.kafkaEnabled ?? true;
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Integration Events"
        description={
          kafkaEnabled
            ? 'Business events published to Kafka through the transactional outbox, and the events a consumer could not process.'
            : 'Kafka is disabled: events are recorded as delivered in-process (LOCAL).'
        }
        actions={
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            onClick={() => void queryClient.invalidateQueries({ queryKey: QUERY_KEY })}
          >
            Refresh
          </Button>
        }
      />
      <Tabs tabs={VIEWS} active={view} onChange={setView} />
      <ErrorAlert error={topics.error} />
      {view === 'dead-letters' && <DeadLettersCard />}
      {view === 'outbox' && <OutboxCard />}
      {view === 'topics' && <TopicsCard topics={topics.data ?? []} loading={topics.isLoading} />}
    </div>
  );
}
