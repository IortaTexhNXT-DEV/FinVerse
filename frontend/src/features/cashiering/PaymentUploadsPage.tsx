import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { bulkApi } from '@/api/bulk';
import type { BulkJob } from '@/api/bulk';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { Payment } from './cashieringApi';
import { runTiles } from './cashieringLogic';
import './cashiering.css';

const FILE_TYPES = [
  { id: 'PAY_BILLS', label: 'Bills Payment' },
  { id: 'PAY_TRADE', label: 'Trade' },
  { id: 'PAY_CLPC', label: 'CLPC' },
  { id: 'PAY_DIRECT_CREDIT', label: 'Direct Credit' },
  { id: 'PAY_PDC', label: 'PDC' },
] as const;
type FileType = (typeof FILE_TYPES)[number]['id'];

const COLUMNS: Column<Payment>[] = [
  { key: 'row', header: 'Row', render: (p) => p.rowNo ?? '' },
  { key: 'ref', header: 'Reference', render: (p) => p.reference ?? '' },
  { key: 'payor', header: 'Payor', render: (p) => p.payorName },
  { key: 'amount', header: 'Amount', numeric: true, render: (p) => <Amount value={p.amount} /> },
  {
    key: 'applied',
    header: 'Applied',
    numeric: true,
    render: (p) => <Amount value={p.appliedAmount} />,
  },
  {
    key: 'unapplied',
    header: 'Unapplied',
    numeric: true,
    render: (p) => <Amount value={p.unappliedAmount} />,
  },
  { key: 'cat', header: 'Outcome', render: (p) => <StatusBadge status={p.matchCategory} /> },
  { key: 'msg', header: 'Message', render: (p) => p.message ?? humanize(p.matchedRef ?? '') },
];

function RunSummary({ job, companyId }: Readonly<{ job: BulkJob; companyId: number }>) {
  const download = useFileDownload();
  const payments = useQuery({
    queryKey: ['cashiering', 'run', companyId, job.jobNo],
    queryFn: () => cashieringApi.payments(companyId, job.jobNo),
  });
  const rows = payments.data?.content ?? [];
  return (
    <Card
      title={`Run Summary ${job.jobNo}`}
      actions={
        <Button
          variant="secondary"
          icon={<Download size={16} />}
          busy={download.isPending}
          onClick={() => download.mutate(() => bulkApi.report(job.id))}
        >
          Download Report
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={payments.error ?? download.error} />
        <div className="csh-tiles">
          {runTiles(rows, job.failedRows + job.invalidRows).map((t) => (
            <div key={t.id} className={`csh-tile csh-tile-${t.id}`}>
              <span className="csh-total-label">{t.label}</span>
              <span className="csh-tile-count">{t.count}</span>
            </div>
          ))}
        </div>
        {job.failedRows + job.invalidRows > 0 && (
          <p className="muted">
            To reprocess the failed rows, correct them in the downloaded report and upload them
            again as a new file.
          </p>
        )}
        <DataTable
          caption="Payments of the run"
          columns={COLUMNS}
          rows={rows}
          rowKey={(p) => p.id}
          loading={payments.isLoading}
          emptyMessage="No payment accepted in this run"
        />
      </div>
    </Card>
  );
}

/**
 * Payment Uploads (CSHID.008): the bank and channel payment files (Bills Payment, Trade, CLPC,
 * Direct Credit, PDC) in their configurable TXT layouts. Each accepted row becomes a payment with
 * an AR, matched at once; the run summary counts applied, unapplied, pre-booked, excess and failed.
 */
export default function PaymentUploadsPage() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [type, setType] = useState<FileType>('PAY_BILLS');
  const [job, setJob] = useState<BulkJob>();
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Payment Uploads"
        description="Upload a payment file of a bank or channel: every row becomes a payment with its AR and is matched at once."
      />
      <Card flush>
        <Tabs
          tabs={FILE_TYPES}
          active={type}
          onChange={(t) => {
            setType(t);
            setJob(undefined);
          }}
        />
      </Card>
      <BulkUploadWizard
        key={type}
        handler={type}
        onCommitted={(j) => {
          setJob(j);
          void queryClient.invalidateQueries({ queryKey: ['cashiering'] });
        }}
      />
      {job && <RunSummary job={job} companyId={companyId} />}
    </div>
  );
}
