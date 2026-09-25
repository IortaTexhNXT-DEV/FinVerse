import { useMutation, useQuery } from '@tanstack/react-query';
import { FileSpreadsheet, FileText, Layers, ListChecks } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { saveFile } from '@/api/client';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { DEFAULT_PRINT, reportOptionsApi } from '@/features/reports/reportOptions';
import { today } from '@/utils/format';
import { frbsApi } from './api';
import type { PackEntry } from './api';
import { entryLink, groupPack, quickParams } from './schedules';
import './frbs.css';

function ExportButtons({
  entry,
  busy,
  onExport,
}: Readonly<{
  entry: PackEntry;
  busy: boolean;
  onExport: (entry: PackEntry, format: ExportFormat) => void;
}>) {
  const { can } = useAuth();
  if (!entry.available || !can('FRBS_REPORT_EXPORT')) {
    return null;
  }
  return (
    <span className="frbs-actions">
      <Button
        size="sm"
        variant="secondary"
        icon={<FileSpreadsheet size={14} />}
        disabled={busy}
        onClick={() => onExport(entry, 'XLSX')}
      >
        Excel
      </Button>
      <Button
        size="sm"
        variant="secondary"
        icon={<FileText size={14} />}
        disabled={busy}
        onClick={() => onExport(entry, 'PDF')}
      >
        PDF
      </Button>
    </span>
  );
}

/**
 * Report Pack (FRBS 3.2.0, Appendix A): the BDOI report groups - end of day, GARD, subsidiaries,
 * schedules and ageing, Mancom, service fee, government - with every report opened in its runner
 * and exported at once to Excel or PDF (month to date, today's quarter and year). Board-deck
 * schedules for which BDOI asks for a Word document are flagged: the report platform exports them
 * to PDF and Excel only.
 */
export default function FrbsHomePage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const [busyCode, setBusyCode] = useState<string>();
  const pack = useQuery({ queryKey: ['frbs', 'pack'], queryFn: frbsApi.pack });
  const exporter = useMutation({
    mutationFn: ({ entry, format }: { entry: PackEntry; format: ExportFormat }) =>
      reportOptionsApi.export(
        entry.reportCode,
        quickParams(entry, companyId, today()),
        format,
        DEFAULT_PRINT,
        {},
      ),
    onMutate: ({ entry }) => setBusyCode(entry.reportCode + (entry.scheduleCode ?? '')),
    onSuccess: (file, { entry }) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${entry.title} exported`);
    },
    onSettled: () => setBusyCode(undefined),
  });
  const entries = pack.data ?? [];
  const groups = groupPack(entries);
  const columns: Column<PackEntry>[] = [
    {
      key: 'title',
      header: 'Report',
      render: (e) => (
        <>
          {e.available ? <Link to={entryLink(e)}>{e.title}</Link> : e.title}
          <span className="cell-sub">
            {e.scheduleCode ?? e.reportCode}
            {e.sourceRef ? ` · ${e.sourceRef}` : ''}
          </span>
        </>
      ),
    },
    {
      key: 'formats',
      header: 'Formats',
      render: (e) => (
        <span className="tag-list">
          <span className="tag">Excel</span>
          <span className="tag">PDF</span>
          {e.wordRequested && <span className="tag frbs-gap">Word requested (not yet)</span>}
        </span>
      ),
    },
    {
      key: 'export',
      header: 'Export',
      render: (e) => (
        <ExportButtons
          entry={e}
          busy={busyCode === e.reportCode + (e.scheduleCode ?? '')}
          onExport={(entry, format) => exporter.mutate({ entry, format })}
        />
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Accounting Reports"
        title="Report Pack"
        description="The BDOI Comptrollership report pack of Appendix A: open a report with its parameters or export it to Excel or PDF for the month to date."
        actions={
          <>
            <Link className="btn btn-secondary" to="/reports">
              <ListChecks size={16} aria-hidden="true" /> Report Batch
            </Link>
            <Link className="btn btn-primary" to="/frbs/schedules">
              <Layers size={16} aria-hidden="true" /> Account Schedules
            </Link>
          </>
        }
      />
      <ErrorAlert error={pack.error ?? exporter.error} />
      <div className="grid-4">
        <Kpi label="Report Groups" value={groups.length} />
        <Kpi label="Reports" value={entries.length} />
        <Kpi label="Account Schedules" value={entries.filter((e) => e.scheduleCode).length} />
        <Kpi
          label="Word Documents Requested"
          value={entries.filter((e) => e.wordRequested).length}
          hint="Exported to PDF and Excel until the platform produces Word"
        />
      </div>
      {pack.isLoading && <span className="spinner" aria-label="Loading" />}
      {groups.map((g) => (
        <Card key={g.code} title={g.name} flush>
          <DataTable
            caption={g.name}
            columns={columns}
            rows={g.entries}
            rowKey={(e) => e.reportCode + (e.scheduleCode ?? '')}
          />
        </Card>
      ))}
    </div>
  );
}
