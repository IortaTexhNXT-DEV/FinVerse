import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CalendarClock } from 'lucide-react';
import { useState } from 'react';
import { RELATIVE_PERIODS, recipientsOf, reportSubscriptionApi } from '@/api/reportSubscriptions';
import type { DayOfWeek, RelativePeriod, SubscriptionFrequency } from '@/api/reportSubscriptions';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';

const FREQUENCIES = [
  { value: 'DAILY', label: 'Daily' },
  { value: 'WEEKLY', label: 'Weekly' },
  { value: 'MONTHLY', label: 'Monthly' },
  { value: 'LAST_WORKING_DAY', label: 'Last working day of the month' },
];

const PERIODS = [{ value: '', label: 'As entered on the screen' }, ...RELATIVE_PERIODS];

const DAYS: { value: DayOfWeek; label: string }[] = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

interface Props {
  reportCode: string;
  title: string;
  parameters: Record<string, string>;
  formats: readonly string[];
  onClose: () => void;
}

/**
 * Subscribes the user to a report with the parameters of the screen: frequency, file format and
 * recipients, the time of day and, for the date parameters, a period relative to each run (previous
 * month, month to date...). The file is sent by the scheduler, checked against the user's report
 * rights and data scope at every run; the last working day follows the holiday calendar.
 */
export function SubscribeDialog({
  reportCode,
  title,
  parameters,
  formats,
  onClose,
}: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [frequency, setFrequency] = useState<SubscriptionFrequency>('DAILY');
  const [dayOfWeek, setDayOfWeek] = useState<DayOfWeek>('MONDAY');
  const [dayOfMonth, setDayOfMonth] = useState<number | undefined>(1);
  const [format, setFormat] = useState(formats[0] ?? 'PDF');
  const [recipients, setRecipients] = useState('');
  const [runTime, setRunTime] = useState('06:00');
  const [period, setPeriod] = useState('');
  const save = useMutation({
    mutationFn: () =>
      reportSubscriptionApi.subscribe({
        reportCode,
        parameters,
        frequency,
        dayOfWeek: frequency === 'WEEKLY' ? dayOfWeek : null,
        dayOfMonth: frequency === 'MONTHLY' ? (dayOfMonth ?? null) : null,
        format,
        recipients: recipientsOf(recipients),
        runTime: runTime === '' ? null : runTime,
        datePeriod: period === '' ? null : (period as RelativePeriod),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['report-subscriptions'] });
      toast.success(`Subscribed to ${title}`);
      onClose();
    },
  });
  return (
    <Modal
      title="Subscribe to Report"
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
          Subscribe
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <p className="muted">{title} is sent with the parameters entered on the screen.</p>
      <div className="form-grid">
        <SelectInput
          label="Frequency"
          required
          value={frequency}
          options={FREQUENCIES}
          onChange={(v) => setFrequency(v as SubscriptionFrequency)}
        />
        {frequency === 'WEEKLY' && (
          <SelectInput
            label="Day of the week"
            required
            value={dayOfWeek}
            options={DAYS}
            onChange={(v) => setDayOfWeek(v as DayOfWeek)}
          />
        )}
        {frequency === 'MONTHLY' && (
          <NumberInput
            label="Day of the month"
            required
            step="1"
            hint="31 runs on the last day of every month"
            value={dayOfMonth}
            onChange={setDayOfMonth}
          />
        )}
        <Field label="Time of day" hint="Business time; the file is sent within the hour">
          {(id) => (
            <input
              id={id}
              className="input"
              type="time"
              value={runTime}
              onChange={(e) => setRunTime(e.target.value)}
            />
          )}
        </Field>
        <SelectInput
          label="Date period"
          hint="Sets the date parameters from the date of each run"
          value={period}
          options={PERIODS}
          onChange={setPeriod}
        />
        <SelectInput
          label="Format"
          required
          value={format}
          options={formats.map((f) => ({ value: f, label: f }))}
          onChange={setFormat}
        />
        <TextInput
          label="Recipients"
          required
          hint="E-mail addresses separated by commas"
          value={recipients}
          onChange={setRecipients}
        />
      </div>
    </Modal>
  );
}

/** Subscribe button of a report screen: opens the dialog with the parameters entered. */
export function SubscribeButton({
  reportCode,
  title,
  parameters,
  formats,
}: Readonly<{
  reportCode: string;
  title: string;
  parameters: () => Record<string, string>;
  formats: readonly string[];
}>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="secondary" icon={<CalendarClock size={14} />} onClick={() => setOpen(true)}>
        Subscribe
      </Button>
      {open && (
        <SubscribeDialog
          reportCode={reportCode}
          title={title}
          parameters={parameters()}
          formats={formats}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
