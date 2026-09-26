import { useMutation } from '@tanstack/react-query';
import { Play } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat, ReportResult } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { DEFAULT_PRINT, reportOptionsApi } from '@/features/reports/reportOptions';
import { ExportButtons } from '@/features/reports/ExportButtons';
import { ReportTable } from '@/features/reports/ReportTable';
import { today } from '@/utils/format';
import type { Schedule } from './api';
import { ScheduleCommentary } from './ScheduleCommentary';
import { groupByFamily, packFormats, runnerErrors, scheduleParams } from './schedules';
import type { RunnerErrors } from './schedules';

const REPORT = 'GL-SCHEDULE';

interface Choice {
  code: string;
  asOf: string;
  from: string;
}

function ScheduleSelect({
  schedules,
  value,
  error,
  onChange,
}: Readonly<{
  schedules: Schedule[];
  value: string;
  error?: string;
  onChange: (code: string) => void;
}>) {
  return (
    <Field label="Schedule" required error={error}>
      {(id) => (
        <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
          <option value="">Select a schedule</option>
          {groupByFamily(schedules).map((g) => (
            <optgroup key={g.family} label={g.label}>
              {g.schedules.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.code} · {s.values.name}
                </option>
              ))}
            </optgroup>
          ))}
        </select>
      )}
    </Field>
  );
}

function DateField({
  label,
  value,
  error,
  hint,
  required = false,
  onChange,
}: Readonly<{
  label: string;
  value: string;
  error?: string;
  hint?: string;
  required?: boolean;
  onChange: (v: string) => void;
}>) {
  return (
    <Field label={label} required={required} error={error} hint={hint}>
      {(id) => (
        <input
          id={id}
          type="date"
          className="input"
          value={value}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
    </Field>
  );
}

function SelectedNote({ schedule }: Readonly<{ schedule?: Schedule }>) {
  if (schedule === undefined) {
    return null;
  }
  return (
    <p className="frbs-muted">
      {schedule.values.sourceRef ?? schedule.code} ·{' '}
      <StatusBadge status={schedule.values.layoutStatus} />
      {schedule.values.boardDocument &&
        ' · Board document: exported to Word as well as Excel and PDF.'}
    </p>
  );
}

function ResultCard({ result }: Readonly<{ result: ReportResult }>) {
  return (
    <Card title={result.title} flush>
      <ReportTable result={result} />
      {result.notes.length > 0 && (
        <div className="card-body">
          {result.notes.map((n) => (
            <p key={n} className="frbs-muted">
              {n}
            </p>
          ))}
        </div>
      )}
    </Card>
  );
}

/**
 * Runs an account schedule of the report pack (FRBS 3.2.0): schedule, as-of date and optional
 * period start; the result on screen with its notes, the export to Excel or PDF (and Word for a
 * board document), and the
 * commentary of the month for a variance analysis.
 */
export function ScheduleRunner({
  schedules,
  initialCode,
}: Readonly<{ schedules: Schedule[]; initialCode: string }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [choice, setChoice] = useState<Choice>({ code: initialCode, asOf: today(), from: '' });
  const [checked, setChecked] = useState(false);
  const selected = schedules.find((s) => s.code === choice.code);
  const params = () => scheduleParams(companyId, choice.code, choice.asOf, choice.from);
  const run = useMutation({ mutationFn: () => reportApi.run(REPORT, params()) });
  const exporter = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportOptionsApi.export(REPORT, params(), format, DEFAULT_PRINT, {}),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  const errors: RunnerErrors = checked ? runnerErrors(choice.code, choice.asOf, choice.from) : {};
  const guard = (action: () => void) => {
    setChecked(true);
    if (Object.keys(runnerErrors(choice.code, choice.asOf, choice.from)).length === 0) {
      action();
    }
  };
  const set = (next: Partial<Choice>) => setChoice((c) => ({ ...c, ...next }));
  return (
    <div className="stack">
      <Card title="Run a Schedule">
        <div className="frbs-form">
          <ScheduleSelect
            schedules={schedules}
            value={choice.code}
            error={errors.code}
            onChange={(code) => {
              set({ code });
              run.reset();
            }}
          />
          <DateField
            label="As Of"
            required
            value={choice.asOf}
            error={errors.asOf}
            onChange={(asOf) => set({ asOf })}
          />
          <DateField
            label="Period From"
            value={choice.from}
            error={errors.from}
            hint="Blank: first day of the month"
            onChange={(from) => set({ from })}
          />
          <div className="frbs-actions">
            <Button
              icon={<Play size={16} />}
              busy={run.isPending}
              onClick={() => guard(() => run.mutate())}
            >
              Run Schedule
            </Button>
            {can('FRBS_REPORT_EXPORT') && (
              <ExportButtons
                formats={packFormats({ wordRequested: selected?.values.boardDocument === true })}
                size="md"
                prefix="Export to"
                pending={exporter.isPending ? exporter.variables : undefined}
                onExport={(format) => guard(() => exporter.mutate(format))}
              />
            )}
          </div>
        </div>
        <SelectedNote schedule={selected} />
      </Card>
      <ErrorAlert error={run.error ?? exporter.error} />
      {run.data && <ResultCard result={run.data} />}
      {run.data && selected?.values.commentary && (
        <ScheduleCommentary
          code={selected.code}
          asOf={choice.asOf}
          result={run.data}
          onSaved={() => run.mutate()}
        />
      )}
    </div>
  );
}
