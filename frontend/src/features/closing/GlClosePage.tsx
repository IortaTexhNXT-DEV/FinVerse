import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarClock, Lock, PlayCircle } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { BooksDialog } from './BooksDialog';
import { BooksCard, CloseSettingsKpis, SchedulesCard } from './CloseTables';
import { ScheduleCloseDialog } from './ScheduleCloseDialog';
import { closeControlsApi } from './closeControlsApi';
import type { BooksCutoff, CloseSchedule } from './closeControlsApi';
import { usePeriodPicker } from './usePeriodPicker';

type Tab = 'close' | 'broking';
type Dialog = 'schedule' | 'closeBooks' | 'reopenBooks' | null;

const TABS = [
  { id: 'close', label: 'Month-End Close' },
  { id: 'broking', label: 'Broking Books Cut-Off' },
] as const;

function scheduleMessage(s: CloseSchedule): string {
  return s.status === 'SCHEDULED'
    ? `Close of ${s.periodName} scheduled`
    : `Close of ${s.periodName}: ${s.status}`;
}

function booksMessage(b: BooksCutoff): string {
  return `Broking books of ${b.periodName} ${b.locked ? 'closed' : 'reopened'}`;
}

/**
 * GL close controls of FRBS: scheduled month-end close of the previous month (FRBS 2.6.0 /
 * 2.6.1) and the cut-off of the broking books at month end (FRBS 3.4.0 / 3.4.1).
 */
export default function GlClosePage() {
  const picker = usePeriodPicker();
  const { companyId } = picker;
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Tab>('close');
  const [dialog, setDialog] = useState<Dialog>(null);
  const scheduler = can('GL_CLOSE_SCHEDULE');

  const settings = useQuery({ queryKey: ['close-settings'], queryFn: closeControlsApi.settings });
  const schedules = useQuery({
    queryKey: ['close-schedules', companyId],
    queryFn: () => closeControlsApi.schedules(companyId),
    enabled: companyId > 0,
  });
  const books = useQuery({
    queryKey: ['broking-books', companyId],
    queryFn: () => closeControlsApi.brokingBooks(companyId),
    enabled: companyId > 0,
  });
  const refresh = async (message: string) => {
    setDialog(null);
    await queryClient.invalidateQueries({ queryKey: ['close-schedules'] });
    await queryClient.invalidateQueries({ queryKey: ['broking-books'] });
    await queryClient.invalidateQueries({ queryKey: ['periods'] });
    toast.success(message);
  };
  const cancel = useMutation({
    mutationFn: (s: CloseSchedule) =>
      closeControlsApi.cancel(s.id, 'Withdrawn by the GL Team Lead'),
    onSuccess: (s) => refresh(`Close of ${s.periodName} withdrawn`),
  });

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="GL Close & Cut-Off"
        description="Schedule the month-end close of the previous month and follow the cut-off of the broking books."
        actions={
          scheduler && (
            <>
              <Button
                variant="secondary"
                icon={<Lock size={16} />}
                onClick={() => setDialog('closeBooks')}
              >
                Close Broking Books
              </Button>
              <Button
                variant="accent"
                icon={<CalendarClock size={16} />}
                onClick={() => setDialog('schedule')}
              >
                Schedule Close
              </Button>
            </>
          )
        }
      />
      <ErrorAlert error={settings.error ?? schedules.error ?? books.error ?? cancel.error} />
      <CloseSettingsKpis settings={settings.data} schedules={schedules.data ?? []} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'close' ? (
        <SchedulesCard
          rows={schedules.data ?? []}
          loading={schedules.isLoading}
          canWithdraw={scheduler}
          withdrawing={cancel.isPending}
          onWithdraw={(s) => cancel.mutate(s)}
        />
      ) : (
        <BooksCard
          rows={books.data ?? []}
          loading={books.isLoading}
          canReopen={scheduler}
          onReopen={() => setDialog('reopenBooks')}
        />
      )}
      <p className="muted">
        <PlayCircle size={14} aria-hidden="true" /> The GL_PERIOD_CLOSE job runs due closes every 15
        minutes; BROKING_BOOKS_CLOSE cuts off the broking books on the last day of the month.
      </p>
      <ScheduleCloseDialog
        open={dialog === 'schedule'}
        picker={picker}
        onDone={(s) => void refresh(scheduleMessage(s))}
        onClose={() => setDialog(null)}
      />
      <BooksDialog
        mode={dialog === 'reopenBooks' ? 'reopen' : 'close'}
        open={dialog === 'closeBooks' || dialog === 'reopenBooks'}
        picker={picker}
        onDone={(b) => void refresh(booksMessage(b))}
        onClose={() => setDialog(null)}
      />
    </div>
  );
}
