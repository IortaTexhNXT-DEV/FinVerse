import { useQuery } from '@tanstack/react-query';
import { Download, Play, Printer } from 'lucide-react';
import { useState } from 'react';
import { reportApi } from '@/api/reports';
import type { ExportFormat, ReportRunEntry } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { formatDateTime, humanize } from '@/utils/format';
import { FORMAT_LABELS, formatLabel } from './exportFormats';

interface ReportActionsProps {
  formats: readonly ExportFormat[];
  valid: boolean;
  ran: boolean;
  running: boolean;
  exporting: boolean;
  printing: boolean;
  onRun: () => void;
  onExport: (format: ExportFormat) => void;
  onPrint: () => void;
}

/**
 * The actions of a report: Run Report (the primary action), Download as a chosen format next to it,
 * and Print once the report has run.
 */
export function ReportActions({
  formats,
  valid,
  ran,
  running,
  exporting,
  printing,
  onRun,
  onExport,
  onPrint,
}: Readonly<ReportActionsProps>) {
  const [format, setFormat] = useState<ExportFormat>(formats[0] ?? 'XLSX');
  return (
    <div className="report-actions">
      <Button variant="accent" icon={<Play size={16} />} busy={running} onClick={onRun}>
        Run Report
      </Button>
      <div className="report-download" role="group" aria-label="Download as">
        <label htmlFor="report-format" className="report-download-label">
          Download as
        </label>
        <select
          id="report-format"
          className="select"
          value={format}
          onChange={(e) => setFormat(e.target.value as ExportFormat)}
        >
          {formats.map((f) => (
            <option key={f} value={f}>
              {FORMAT_LABELS[f]}
            </option>
          ))}
        </select>
        <Button
          variant="secondary"
          icon={<Download size={16} />}
          busy={exporting}
          disabled={!valid}
          title={valid ? undefined : 'Complete the required parameters first'}
          onClick={() => onExport(format)}
        >
          Download
        </Button>
      </div>
      {ran && (
        <Button variant="secondary" icon={<Printer size={16} />} busy={printing} onClick={onPrint}>
          Print
        </Button>
      )}
    </div>
  );
}

/** The parameters of an archived run as users read them ("From Date: 01-Oct-2026 · ..."). */
function parametersText(parameters: string): string {
  try {
    const values = JSON.parse(parameters) as Record<string, string>;
    return Object.entries(values)
      .filter(([k, v]) => v !== '' && k !== 'companyId')
      .map(([k, v]) => `${humanize(k.replace(/([a-z])([A-Z])/g, '$1_$2'))}: ${v}`)
      .join(' · ');
  } catch {
    return parameters;
  }
}

/** The user's recent runs and downloads of the report. */
export function RecentRuns({ code }: Readonly<{ code: string }>) {
  const { user } = useAuth();
  const runs = useQuery({
    queryKey: ['report-runs', code],
    queryFn: () => reportApi.runs(code, 0, 20),
  });
  const mine = (runs.data?.content ?? [])
    .filter((r) => r.createdBy.toLowerCase() === (user?.username ?? '').toLowerCase())
    .slice(0, 5);
  return (
    <Card title="My Recent Runs" flush>
      <DataTable<ReportRunEntry>
        caption="My recent runs of the report"
        rows={mine}
        loading={runs.isLoading}
        rowKey={(r) => r.id}
        emptyMessage="You have not run this report yet."
        columns={[
          { key: 'at', header: 'Run On', render: (r) => formatDateTime(r.createdAt) },
          {
            key: 'what',
            header: 'Action',
            render: (r) =>
              r.format === undefined ? humanize(r.action) : `Download (${formatLabel(r.format)})`,
          },
          { key: 'rows', header: 'Rows', numeric: true, render: (r) => r.rowCount },
          { key: 'params', header: 'Parameters', render: (r) => parametersText(r.parameters) },
        ]}
      />
    </Card>
  );
}
