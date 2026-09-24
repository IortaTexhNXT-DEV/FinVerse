import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import type { ReportRun } from '@/api/operations';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { orUndefined } from './invoiceSearch';
import './operations.css';

/**
 * Report archive (CSHID.017/018): every run and export of the Operations reports with its
 * parameters, creator and time. Users allowed to export download an archived file again; view-only
 * users see the list and run the report on screen from the Report Centre.
 */
export default function ReportArchivePage() {
  const download = useFileDownload();
  const [text, setText] = useState('');
  const [code, setCode] = useState<string>();
  const [page, setPage] = useState(0);
  const runs = useQuery({
    queryKey: ['ops', 'report-runs', code, page],
    queryFn: () => opsApi.reportRuns(code, page),
  });
  const columns: Column<ReportRun>[] = [
    {
      key: 'report',
      header: 'Report',
      render: (r) => (
        <>
          <strong>{r.title}</strong>
          <div className="ops-muted">{r.reportCode}</div>
        </>
      ),
    },
    { key: 'params', header: 'Parameters', render: (r) => r.parameters ?? '' },
    {
      key: 'action',
      header: 'Action',
      render: (r) => (
        <StatusBadge status={r.action === 'EXPORT' ? `EXPORTED_${r.format ?? ''}` : 'VIEWED'} />
      ),
    },
    { key: 'rows', header: 'Rows', numeric: true, render: (r) => r.rowCount },
    {
      key: 'by',
      header: 'Generated',
      render: (r) => `${formatDateTime(r.createdAt)} · ${r.createdBy}`,
    },
    {
      key: 'file',
      header: 'File',
      render: (r) =>
        r.action === 'EXPORT' ? (
          <Button
            size="sm"
            variant="secondary"
            icon={<Download size={14} />}
            onClick={() => download.mutate(() => opsApi.reportRunFile(r.id))}
          >
            Download
          </Button>
        ) : (
          ''
        ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Report Archive"
        description="Operations reports generated on screen or exported, with their parameters and who generated them."
      />
      <ErrorAlert error={runs.error ?? download.error} />
      <Card>
        <div className="stack">
          <form
            className="ops-toolbar"
            onSubmit={(e) => {
              e.preventDefault();
              setCode(orUndefined(text));
              setPage(0);
            }}
          >
            <label className="visually-hidden" htmlFor="ops-report-code">
              Search Report Code
            </label>
            <input
              id="ops-report-code"
              className="input"
              placeholder="Search Report Code, e.g. CSH-APPLIED-PREM"
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
            <Button type="submit">Search</Button>
          </form>
          <DataTable
            caption="Archived report runs"
            columns={columns}
            rows={runs.data?.content ?? []}
            rowKey={(r) => r.id}
            loading={runs.isLoading}
            emptyMessage="No items to display"
          />
          <PageFooter data={runs.data} noun="runs" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
