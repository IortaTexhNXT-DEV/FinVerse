import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat, ReportResult } from '@/api/reports';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useWorkspace } from '@/context/workspaceContext';
import { menuFormats } from './exportFormats';
import { ParameterInput } from './ParameterInput';
import { PrintOptionsFields } from './PrintOptionsFields';
import { ReportVariants } from './ReportVariants';
import { ReportTable } from './ReportTable';
import { DEFAULT_PRINT, filterPairs, reportOptionsApi } from './reportOptions';
import type { ColumnFilters, PrintOptions } from './reportOptions';
import { initialValue, parameterErrors, resolveValue, runParameters } from './reportParams';
import { RecentRuns, ReportActions } from './ReportRunnerParts';
import { Notice } from '@/components/ui/Notice';

/** The result of a run: the parameters used, the table with its column filters and the notes. */
function ResultCard({
  result,
  filters,
  onFilterChange,
}: Readonly<{
  result: ReportResult;
  filters: ColumnFilters;
  onFilterChange: (column: string, text: string) => void;
}>) {
  return (
    <Card title={result.title} flush>
      <div className="report-echo muted">{result.parameterEcho.join(' · ')}</div>
      <ReportTable result={result} filters={filters} onFilterChange={onFilterChange} />
      {result.notes.length > 0 && (
        <Notice tone="info" className="report-note" items={result.notes} />
      )}
    </Card>
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
  const [search] = useSearchParams();
  // A link from a dashboard figure opens the report with its parameters filled in.
  const [values, setValues] = useState<Record<string, string>>(() =>
    Object.fromEntries(search.entries()),
  );
  const [checked, setChecked] = useState(false);
  const [filters, setFilters] = useState<ColumnFilters>({});
  const [print, setPrint] = useState<PrintOptions>(DEFAULT_PRINT);

  const params = () => runParameters(entry?.parameters ?? [], values, company?.id, branchId);

  const queryClient = useQueryClient();
  const refreshRuns = () => queryClient.invalidateQueries({ queryKey: ['report-runs', code] });
  const run = useMutation({
    mutationFn: () => reportApi.run(code, params()),
    onSuccess: async () => {
      setFilters({});
      await refreshRuns();
    },
  });
  const applyVariant = useCallback(
    (saved: Record<string, string>) => {
      const resolved: Record<string, string> = {};
      Object.entries(saved).forEach(([k, v]) => {
        resolved[k] = resolveValue(v, branchId);
      });
      setValues(resolved);
      setChecked(false);
    },
    [branchId],
  );
  const exporter = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportOptionsApi.export(code, params(), format, print, filters),
    onSuccess: async ({ blob, fileName }) => {
      saveFile(blob, fileName);
      await refreshRuns();
    },
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
        description={entry.description}
      />
      <Card title="Parameters">
        <div className="stack">
          <ReportVariants code={code} title={entry.title} values={params} onApply={applyVariant} />
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
          <details className="report-print-options">
            <summary>Page Setup for PDF and Print</summary>
            <div className="form-grid">
              <PrintOptionsFields value={print} onChange={setPrint} />
            </div>
          </details>
          <ReportActions
            formats={menuFormats(entry)}
            valid={valid}
            ran={run.data !== undefined}
            running={run.isPending}
            exporting={exporter.isPending}
            printing={printer.isPending}
            onRun={guarded(() => run.mutate())}
            onExport={(format) => guarded(() => exporter.mutate(format))()}
            onPrint={guarded(() => printer.mutate())}
          />
          {filtered && (
            <p className="muted report-filter-note">
              The download holds the rows that match the column filters of the result.
            </p>
          )}
        </div>
      </Card>
      <ErrorAlert error={run.error ?? exporter.error ?? printer.error} />
      {run.data !== undefined && (
        <ResultCard
          result={run.data}
          filters={filters}
          onFilterChange={(column, text) => setFilters((f) => ({ ...f, [column]: text }))}
        />
      )}
      <RecentRuns code={code} />
    </div>
  );
}
