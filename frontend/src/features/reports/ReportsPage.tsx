import { useQuery } from '@tanstack/react-query';
import { Download, FileBarChart2, Layers } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { reportApi } from '@/api/reports';
import type { CatalogueEntry } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { ReportBatchDialog } from './ReportBatchDialog';
import { downloadBatch, reportOptionsApi } from './reportOptions';
import type { ReportBatch } from './reportOptions';

function groupByCategory(entries: CatalogueEntry[]): [string, CatalogueEntry[]][] {
  const groups = new Map<string, CatalogueEntry[]>();
  entries.forEach((e) => {
    groups.set(e.categoryLabel, [...(groups.get(e.categoryLabel) ?? []), e]);
  });
  return [...groups.entries()];
}

function RecentBatches() {
  const batches = useQuery({ queryKey: ['report-batches'], queryFn: reportOptionsApi.batches });
  const rows = batches.data?.content ?? [];
  if (rows.length === 0) {
    return null;
  }
  return (
    <Card title="My report batches" flush>
      <ErrorAlert error={batches.error} />
      <DataTable<ReportBatch>
        rows={rows}
        rowKey={(b) => b.id}
        caption="My report batches"
        columns={[
          { key: 'no', header: 'Batch No.', render: (b) => <strong>{b.batchNo}</strong> },
          { key: 'at', header: 'Run At', render: (b) => formatDateTime(b.createdAt) },
          {
            key: 'fmt',
            header: 'Output',
            render: (b) => (b.mergedPdf ? 'Merged PDF' : `ZIP of ${b.format}`),
          },
          { key: 'st', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
          {
            key: 'dl',
            header: '',
            render: (b) =>
              b.fileName !== undefined && (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<Download size={14} />}
                  onClick={() => void downloadBatch(b.id)}
                >
                  Download
                </Button>
              ),
          },
        ]}
      />
    </Card>
  );
}

/**
 * Report centre: every report the user may run, grouped by menu category, and report batches
 * (several reports downloaded or printed together, FRBS 2.4.5 / 2.4.7).
 */
export default function ReportsPage() {
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const [search, setSearch] = useState('');
  const [batch, setBatch] = useState(false);
  const groups = useMemo(() => {
    const term = search.trim().toLowerCase();
    const entries = (catalogue.data ?? []).filter(
      (e) =>
        term === '' || e.title.toLowerCase().includes(term) || e.code.toLowerCase().includes(term),
    );
    return groupByCategory(entries);
  }, [catalogue.data, search]);

  return (
    <div className="stack">
      <PageHeader
        section="Reports"
        title="Report Centre"
        description="Run any report on screen, print it or download it as PDF, Excel, ODS, CSV or XML; download or print several at once as a batch."
        actions={
          <>
            <input
              className="input report-search"
              aria-label="Search reports"
              placeholder="Search by title or code"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Button variant="accent" icon={<Layers size={16} />} onClick={() => setBatch(true)}>
              Report Batch
            </Button>
          </>
        }
      />
      <ReportBatchDialog
        open={batch}
        entries={catalogue.data ?? []}
        onClose={() => setBatch(false)}
      />
      <RecentBatches />
      <ErrorAlert error={catalogue.error} />
      {catalogue.isLoading && <span className="spinner" aria-label="Loading" />}
      {groups.map(([category, entries]) => (
        <Card key={category} title={category}>
          <div className="report-grid">
            {entries.map((e) => (
              <Link key={e.code} to={`/reports/${e.code}`} className="report-card">
                <FileBarChart2 size={20} aria-hidden="true" />
                <span>
                  <strong>{e.title}</strong>
                  <span className="muted">
                    {e.code} · {e.description}
                  </span>
                </span>
              </Link>
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}
