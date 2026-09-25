import { LockOpen } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { Kpi } from '@/components/ui/Kpi';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import type { BooksCutoff, CloseSchedule, CloseSettings } from './closeControlsApi';

/** Close settings and the number of scheduled closes (AQ04). */
export function CloseSettingsKpis({
  settings,
  schedules,
}: Readonly<{ settings?: CloseSettings; schedules: CloseSchedule[] }>) {
  return (
    <div className="grid-4">
      <Kpi
        label="Close only the previous month"
        value={settings?.closeOnlyPreviousMonth === 'false' ? 'No' : 'Yes'}
      />
      <Kpi label="Broking books close (Manila)" value={settings?.brokingCloseTime ?? '—'} />
      <Kpi label="Year-end close deadline" value={settings?.yearEndDeadline ?? '—'} />
      <Kpi
        label="Scheduled closes"
        value={schedules.filter((s) => s.status === 'SCHEDULED').length}
        accent
      />
    </div>
  );
}

interface SchedulesProps {
  rows: CloseSchedule[];
  loading: boolean;
  canWithdraw: boolean;
  withdrawing: boolean;
  onWithdraw: (schedule: CloseSchedule) => void;
}

/** Scheduled and past month-end closes with their outcome (FRBS 2.6.0). */
export function SchedulesCard(p: Readonly<SchedulesProps>) {
  return (
    <Card title="Scheduled and past closes" flush>
      <DataTable<CloseSchedule>
        loading={p.loading}
        rows={p.rows}
        rowKey={(s) => s.id}
        caption="Scheduled closes"
        emptyMessage="No close scheduled yet"
        columns={[
          { key: 'p', header: 'Period', render: (s) => <strong>{s.periodName}</strong> },
          { key: 'at', header: 'Scheduled For', render: (s) => formatDateTime(s.scheduledAt) },
          { key: 'st', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
          { key: 'run', header: 'Run At', render: (s) => formatDateTime(s.executedAt) },
          { key: 'res', header: 'Outcome', render: (s) => s.result ?? '' },
          { key: 'by', header: 'Scheduled By', render: (s) => s.scheduledBy },
          {
            key: 'act',
            header: '',
            render: (s) =>
              p.canWithdraw &&
              s.status === 'SCHEDULED' && (
                <Button
                  size="sm"
                  variant="ghost"
                  busy={p.withdrawing}
                  onClick={() => p.onWithdraw(s)}
                >
                  Withdraw
                </Button>
              ),
          },
        ]}
      />
    </Card>
  );
}

function reopened(b: BooksCutoff): string {
  return b.unlockedBy === undefined ? '' : `${b.unlockedBy} ${formatDateTime(b.unlockedAt)}`;
}

interface BooksProps {
  rows: BooksCutoff[];
  loading: boolean;
  canReopen: boolean;
  onReopen: () => void;
}

/** Cut-off status of the broking books by period (FRBS 3.4.0). */
export function BooksCard(p: Readonly<BooksProps>) {
  return (
    <Card
      title="Broking books by period"
      flush
      actions={
        p.canReopen && (
          <Button size="sm" variant="secondary" icon={<LockOpen size={14} />} onClick={p.onReopen}>
            Reopen Books
          </Button>
        )
      }
    >
      <DataTable<BooksCutoff>
        loading={p.loading}
        rows={p.rows}
        rowKey={(b) => b.periodId}
        caption="Broking books cut-off"
        emptyMessage="The broking books have not been closed yet"
        columns={[
          { key: 'p', header: 'Period', render: (b) => <strong>{b.periodName}</strong> },
          {
            key: 'st',
            header: 'Books',
            render: (b) => <StatusBadge status={b.locked ? 'CLOSED' : 'OPEN'} />,
          },
          {
            key: 'by',
            header: 'Closed',
            render: (b) => `${b.lockedBy ?? '—'} ${formatDateTime(b.lockedAt)}`,
          },
          { key: 're', header: 'Reopened', render: reopened },
          { key: 'note', header: 'Note', render: (b) => b.note ?? '' },
        ]}
      />
    </Card>
  );
}
