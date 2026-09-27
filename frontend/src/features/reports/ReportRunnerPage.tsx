import { useMutation, useQuery } from '@tanstack/react-query';
import { Play, Printer } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { CatalogueEntry, ExportFormat } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useWorkspace } from '@/context/workspaceContext';
import { ExportButtons } from './ExportButtons';
import { menuFormats } from './exportFormats';
import { ParameterInput } from './ParameterInput';
import { PrintOptionsFields } from './PrintOptionsFields';
import { ReportVariants } from './ReportVariants';
import { ReportTable } from './ReportTable';
import { DEFAULT_PRINT, filterPairs, reportOptionsApi } from './reportOptions';
import type { ColumnFilters, PrintOptions } from './reportOptions';
import { initialValue, parameterErrors } from './reportParams';

/** The download buttons: Excel and PDF, Word for documents and schedules (client requirement 16). */
function ReportDownloads({
  entry,
  filtered,
  busy,
  format,
  onExport,
}: Readonly<{
  entry: CatalogueEntry;
  filtered: boolean;
  busy: boolean;
  format?: ExportFormat;
  onExport: (format: ExportFormat) => void;
}>) {
  return (
    <div className="report-downloads">
      <span className="muted">{filtered ? 'Download filtered rows' : 'Download'}</span>
      <ExportButtons
        formats={menuFormats(entry)}
        variant="ghost"
        pending={busy ? format : undefined}
        onExport={onExport}
      />
    </div>
  );
}

/** Opens a PDF in a new tab for the browser's print preview (BRNB.031). */
function openForPrint(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

/**
 * Parameter form, on-screen result with column filters (FRBS 2.4.4) and export or print with the
 * print options (FRBS 2.4.9); exports hold the filtered rows. Every report downloads as Excel and
 * PDF; documents and schedules also as Word (client requirement 16).
 */
export default function ReportRunnerPage() {
  const code = useParams().code ?? '';
  const { company, branchId } = useWorkspace();
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const entry = catalogue.data?.find((e) => e.code === code);
  const [values, setValues] = useState<Record<string, string>>({});
  const [checked, setChecked] = useState(false);
  const [filters, setFilters] = useState<ColumnFilters>({});
  const [print, setPrint] = useState<PrintOptions>(DEFAULT_PRINT);

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

  const run = useMutation({
    mutationFn: () => reportApi.run(code, params()),
    onSuccess: () => setFilters({}),
  });
  const exporter = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportOptionsApi.export(code, params(), format, print, filters),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });
  const printer = useMutation({
    mutationFn: () => reportOptionsApi.export(code, params(), 'PDF', print, filters),
    onSuccess: ({ blob }) => openForPrint(blob),
  });
  const filtered = filterPairs(filters).length > 0;
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
          <div className="form-grid">
            <PrintOptionsFields value={print} onChange={setPrint} />
          </div>
          <ReportDownloads
            entry={entry}
            filtered={filtered}
            busy={exporter.isPending}
            format={exporter.variables}
            onExport={(format) => guarded(() => exporter.mutate(format))()}
          />
        </div>
      </Card>
      <ErrorAlert error={run.error ?? exporter.error ?? printer.error} />
      {run.data !== undefined && (
        <Card title={run.data.title} flush>
          <div className="report-echo muted">{run.data.parameterEcho.join(' · ')}</div>
          <ReportTable
            result={run.data}
            filters={filters}
            onFilterChange={(column, text) => setFilters((f) => ({ ...f, [column]: text }))}
          />
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
