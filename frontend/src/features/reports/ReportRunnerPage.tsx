import { useMutation, useQuery } from '@tanstack/react-query';
import { Download, Play } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat, ParameterSpec } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useWorkspace } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { ParameterInput } from './ParameterInput';
import { ReportTable } from './ReportTable';

function initialValue(spec: ParameterSpec): string {
  const now = today();
  switch (spec.defaultValue) {
    case 'TODAY':
      return now;
    case 'MONTH_START':
      return `${now.slice(0, 7)}-01`;
    case 'YEAR_START':
      return `${now.slice(0, 4)}-01-01`;
    default:
      return spec.defaultValue ?? '';
  }
}

const FORMATS: ExportFormat[] = ['PDF', 'XLSX', 'CSV'];

/** Parameter form, on-screen result and export for one report. */
export default function ReportRunnerPage() {
  const code = useParams().code ?? '';
  const { company, branchId } = useWorkspace();
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const entry = catalogue.data?.find((e) => e.code === code);
  const [values, setValues] = useState<Record<string, string>>({});

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
              onClick={() => run.mutate()}
            >
              Run report
            </Button>
            {FORMATS.map((f) => (
              <Button
                key={f}
                variant="secondary"
                icon={<Download size={15} />}
                busy={exporter.isPending && exporter.variables === f}
                onClick={() => exporter.mutate(f)}
              >
                {f}
              </Button>
            ))}
          </div>
        }
      >
        <div className="form-grid">
          {visible.map((p) => (
            <ParameterInput
              key={p.name}
              spec={p}
              value={values[p.name] ?? initialValue(p)}
              onChange={(v) => setValues((s) => ({ ...s, [p.name]: v }))}
            />
          ))}
        </div>
      </Card>
      <ErrorAlert error={run.error ?? exporter.error} />
      {run.data !== undefined && (
        <Card title={run.data.title} flush>
          <div style={{ padding: '8px 16px' }} className="muted">
            {run.data.parameterEcho.join(' · ')}
          </div>
          <ReportTable result={run.data} />
          {run.data.notes.map((n) => (
            <div key={n} className="alert" style={{ margin: 12 }}>
              {n}
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}
