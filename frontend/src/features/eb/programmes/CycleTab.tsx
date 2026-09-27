import { useQuery } from '@tanstack/react-query';
import { ebApi } from '@/api/eb';
import type { CycleView, FeedbackItem, ProgrammeView } from '@/api/eb';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { EbLov } from '../common/EbLabels';
import { EB_LOV, ebLabel } from '../common/ebCodes';

const CYCLE_COLUMNS: Column<CycleView>[] = [
  {
    key: 'cycle',
    header: 'Cycle',
    kind: 'code',
    render: (c) => <CellStack main={c.cycleNo} sub={ebLabel(c.businessType)} />,
  },
  { key: 'year', header: 'Policy Year', kind: 'center', render: (c) => c.policyYear },
  {
    key: 'inception',
    header: 'Target Inception',
    kind: 'date',
    render: (c) => formatDate(c.targetInception),
  },
  {
    key: 'stage',
    header: 'Stage',
    kind: 'status',
    render: (c) => <StatusBadge status={c.stage} />,
  },
  {
    key: 'ra',
    header: 'Renewal Advice',
    kind: 'datetime',
    render: (c) => (c.renewalAdvice ? formatDateTime(c.renewalAdvice.sentAt) : ''),
  },
  {
    key: 'bor',
    header: 'BOR',
    kind: 'status',
    render: (c) => (
      <StatusBadge
        status={c.borStatus}
        tone={c.borStatus === 'VALIDATED' ? 'success' : undefined}
      />
    ),
  },
  {
    key: 'outcome',
    header: 'Outcome',
    render: (c) =>
      c.outcome ? (
        <CellStack
          main={ebLabel(c.outcome)}
          sub={
            c.outcomeReason ? <EbLov type={EB_LOV.lostReason} code={c.outcomeReason} /> : undefined
          }
        />
      ) : (
        ''
      ),
  },
];

const FEEDBACK_COLUMNS: Column<FeedbackItem>[] = [
  { key: 'date', header: 'Received', kind: 'date', render: (f) => formatDate(f.receivedOn) },
  { key: 'channel', header: 'Channel', render: (f) => ebLabel(f.channel) },
  { key: 'text', header: 'Feedback', render: (f) => f.text ?? '' },
  { key: 'files', header: 'Files', kind: 'center', render: (f) => f.fileCount },
  { key: 'by', header: 'Recorded By', render: (f) => <UserName login={f.recordedBy} /> },
];

/** The renewal advice of the current cycle as a definition grid. */
function AdviceCard({ cycle }: Readonly<{ cycle: CycleView }>) {
  const advice = cycle.renewalAdvice;
  if (!advice) {
    return null;
  }
  return (
    <Card title={`Renewal Advice – ${cycle.cycleNo}`}>
      <DefinitionGrid
        columns={2}
        items={[
          { label: 'Sent', value: formatDateTime(advice.sentAt) },
          {
            label: 'Sent By',
            value: advice.manual ? <UserName login={advice.sentBy} /> : 'Renewal advice job',
          },
          { label: 'Expiry Announced', value: formatDate(advice.expiryDate) },
          { label: 'Recipients', value: advice.recipients.join(', ') },
          { label: 'Reminders Sent', value: String(advice.remindersSent) },
          { label: 'Last Reminder', value: formatDateTime(advice.lastReminderAt) },
          { label: 'Feedback Received', value: formatDateTime(advice.feedbackAt) },
        ]}
      />
    </Card>
  );
}

/**
 * Cycle tab (FR-EB-021 to 023): the cycles of the programme with their business type, stage,
 * renewal advice, BOR and outcome; the renewal advice of the current cycle; the client feedback.
 */
export function CycleTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const feedback = useQuery({
    queryKey: ['eb', 'feedback', programme.id],
    queryFn: () => ebApi.feedback(companyId, programme.id),
  });
  const current = programme.cycles.find((c) => c.id === programme.currentCycleId);
  return (
    <div className="stack">
      <Card title="Cycles">
        <DataTable<CycleView>
          rows={programme.cycles}
          rowKey={(c) => c.id}
          columns={CYCLE_COLUMNS}
          selectedKey={programme.currentCycleId ?? undefined}
          emptyMessage="No cycle yet. Open a cycle or send the renewal advice."
        />
      </Card>
      {current && <AdviceCard cycle={current} />}
      <Card title="Client Feedback">
        <ErrorAlert error={feedback.error} onRetry={() => void feedback.refetch()} />
        <DataTable<FeedbackItem>
          loading={feedback.isLoading}
          rows={feedback.data ?? []}
          rowKey={(f) => f.id}
          columns={FEEDBACK_COLUMNS}
          emptyMessage="No feedback recorded"
        />
      </Card>
    </div>
  );
}
