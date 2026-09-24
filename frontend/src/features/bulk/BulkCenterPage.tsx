import { useQuery } from '@tanstack/react-query';
import { ChevronRight, Download, FileSpreadsheet } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { bulkApi } from '@/api/bulk';
import type { BulkHandler, BulkJob } from '@/api/bulk';
import { saveFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Pager } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';

/**
 * Bulk Processing: the upload types you may use (bulk quotations, accounts, client updates,
 * booking...) and the history of uploads with their result reports (BRNB.024/025/039/064).
 */
export default function BulkCenterPage() {
  const companyId = useCompanyId();
  const [page, setPage] = useState(0);
  const handlers = useQuery({ queryKey: ['bulk', 'handlers'], queryFn: bulkApi.handlers });
  const jobs = useQuery({
    queryKey: ['bulk', 'jobs', companyId, page],
    queryFn: () => bulkApi.jobs(companyId, undefined, page),
    enabled: companyId > 0,
  });
  const titles = new Map((handlers.data ?? []).map((h) => [h.code, h.title]));
  const report = async (job: BulkJob) => {
    const f = await bulkApi.report(job.id);
    saveFile(f.blob, f.fileName);
  };
  return (
    <div className="stack">
      <PageHeader
        section="Bulk Processing"
        title="Bulk Processing"
        description="Create or update many records from one file. Download the template, upload the filled file, review every row, then process the valid rows."
      />
      <ErrorAlert error={handlers.error ?? jobs.error} />
      <HandlerTiles handlers={handlers.data} />
      <Card title="Recent uploads">
        <DataTable<BulkJob>
          loading={jobs.isLoading}
          rows={jobs.data?.content ?? []}
          rowKey={(j) => j.id}
          emptyMessage="No uploads yet."
          columns={[
            { key: 'no', header: 'Upload', render: (j) => j.jobNo },
            {
              key: 'type',
              header: 'Type',
              render: (j) => titles.get(j.handlerCode) ?? j.handlerCode,
            },
            { key: 'file', header: 'File', render: (j) => j.fileName },
            { key: 'status', header: 'Status', render: (j) => <StatusBadge status={j.status} /> },
            { key: 'rows', header: 'Rows', numeric: true, render: (j) => j.totalRows },
            { key: 'valid', header: 'Valid', numeric: true, render: (j) => j.validRows },
            { key: 'done', header: 'Processed', numeric: true, render: (j) => j.committedRows },
            {
              key: 'failed',
              header: 'Failed',
              numeric: true,
              render: (j) => j.failedRows + j.invalidRows,
            },
            {
              key: 'by',
              header: 'Uploaded',
              render: (j) => `${j.createdBy} · ${formatDateTime(j.createdAt)}`,
            },
            {
              key: 'report',
              header: '',
              render: (j) => (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<Download size={14} />}
                  onClick={() => void report(j)}
                >
                  Report
                </Button>
              ),
            },
          ]}
        />
        <Pager
          page={jobs.data?.page ?? 0}
          totalPages={jobs.data?.totalPages ?? 0}
          total={jobs.data?.totalElements ?? 0}
          noun="uploads"
          onPage={setPage}
        />
      </Card>
    </div>
  );
}

function HandlerTiles({ handlers }: Readonly<{ handlers: BulkHandler[] | undefined }>) {
  if (handlers?.length === 0) {
    return <p className="muted">No bulk upload is available for your role.</p>;
  }
  return (
    <div className="tile-grid">
      {(handlers ?? []).map((h) => (
        <Link key={h.code} to={`/bulk/${h.code}`} className="action-tile">
          <FileSpreadsheet size={22} aria-hidden="true" />
          <span className="action-tile-title">{h.title}</span>
          <span className="action-tile-meta">
            {h.columns.length} columns · {h.columns.filter((c) => c.required).length} mandatory
          </span>
          <ChevronRight size={16} className="action-tile-go" aria-hidden="true" />
        </Link>
      ))}
    </div>
  );
}
