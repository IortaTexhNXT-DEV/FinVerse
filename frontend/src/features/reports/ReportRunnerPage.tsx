import { useMutation, useQuery } from '@tanstack/react-query';
import { Download, Play, Printer } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useWorkspace } from '@/context/workspaceContext';
import { ParameterInput } from './ParameterInput';
import { ReportVariants } from './ReportVariants';
import { ReportTable } from './ReportTable';
import { initialValue, parameterErrors } from './reportParams';

const FORMATS: { format: ExportFormat; label: string }[] = [
  { format: 'PDF', label: 'PDF' },
  { format: 'XLSX', label: 'Excel' },
  { format: 'ODS', label: 'ODS' },
  { format: 'CSV', label: 'CSV' },
  { format: 'XML', label: 'XML' },
];

/** Opens a PDF in a new tab for the browser's print preview (BRNB.031). */
function openForPrint(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/** Parameter form, on-screen result and export for one report. */
export default function ReportRunnerPage() {
  const code = useParams().code ?? '';
  const { company, branchId } = useWorkspace();
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const entry = catalogue.data?.find((e) => e.code === code);
  const [values, setValues] = useState<Record<string, string>>({});
  const [checked, setChecked] = useState(false);

  const params = (): Record<string, string> => {
    const out: Record<string, string> = {};
    entry?.parameters.forEach((p) => {
      out[p.name] = values[p.name] ?? initialValue(p);
    });
    if (company !== undefined) {
      out.companyId = String(company.id);
    }
    if (branchId !== undefined && out.branchId === '') {
      out.branchId = String(branchId);
    }
    return out;
  };

  const run = useMutation({ mutationFn: () => reportApi.run(code, params()) });
  const exporter = useMutation({
    mutationFn: (format: ExportFormat) => reportApi.export(code, params(), format),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });
  const printer = useMutation({
    mutationFn: () => reportApi.export(code, params(), 'PDF'),
    onSuccess: ({ blob }) => openForPrint(blob),
  });
  const errors = parameterErrors(entry?.parameters ?? [], values);
  const valid = Object.keys(errors).length === 0;
  /** Runs the action only when the form is valid; otherwise shows the field errors. */
  const guarded = (action: () => void) => () => {
    setChecked(true);
    if (valid) {
      action();
    }
  };

  if (entry === undefined) {
    return catalogue.isLoading ? (
      <span className="spinner" aria-label="Loading" />
    ) : (
      <ErrorAlert error={new Error(`Report ${code} is not available to you`)} />
    );
  }
  const visible = entry.parameters.filter((p) => p.type !== 'COMPANY');

  return (
    <div className="stack">
      <PageHeader
        section={`Reports · ${entry.categoryLabel}`}
        backTo={entry.category === 'NEW_BUSINESS' ? '/nb/reports' : '/reports'}
        title={entry.title}
        description={`${entry.code} — ${entry.description}`}
      />
      <Card
        title="Parameters"
        actions={
          <div className="row">
            <Button
              variant="accent"
              icon={<Play size={16} />}
              busy={run.isPending}
              onClick={guarded(() => run.mutate())}
            >
              Run Report
            </Button>
            <Button
              variant="secondary"
              icon={<Printer size={16} />}
              busy={printer.isPending}
              onClick={guarded(() => printer.mutate())}
            >
              Print
            </Button>
          </div>
        }
      >
        <div className="stack">
          <ReportVariants
            code={code}
            values={params}
            onApply={(saved) => {
              setValues(saved);
              setChecked(false);
            }}
          />
          <div className="form-grid">
            {visible.map((p) => (
              <ParameterInput
                key={p.name}
                spec={p}
                value={values[p.name] ?? initialValue(p)}
                error={checked ? errors[p.name] : undefined}
                onChange={(v) => setValues((s) => ({ ...s, [p.name]: v }))}
              />
            ))}
          </div>
          <div className="report-downloads">
            <span className="muted">Download</span>
            {FORMATS.map((f) => (
              <Button
                key={f.format}
                size="sm"
                variant="ghost"
                icon={<Download size={15} />}
                busy={exporter.isPending && exporter.variables === f.format}
                onClick={guarded(() => exporter.mutate(f.format))}
              >
                {f.label}
              </Button>
            ))}
          </div>
        </div>
      </Card>
      <ErrorAlert error={run.error ?? exporter.error ?? printer.error} />
      {run.data !== undefined && (
        <Card title={run.data.title} flush>
          <div className="report-echo muted">{run.data.parameterEcho.join(' · ')}</div>
          <ReportTable result={run.data} />
          {run.data.notes.map((n) => (
            <div key={n} className="alert report-note">
              {n}
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}
