import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { remittanceApi } from './api';
import type { AccountHit, FeedRecord, FeedRun, OrUpload } from './api';
import { TemplateButton, UploadForm } from './RemittanceParts';
import { OR_TEMPLATE } from './remittanceLabels';
import './remittance.css';

const UPDATED: Column<AccountHit>[] = [
  {
    key: 'batch',
    header: 'Batch No.',
    render: (a) => <Link to={`/remittance/batches/${a.batchId}`}>{a.batchNo}</Link>,
  },
  { key: 'inv', header: 'Invoice No.', render: (a) => a.line.invoiceNo },
  { key: 'or', header: 'OR No.', render: (a) => a.line.insurerOr?.orNo ?? '' },
  { key: 'date', header: 'OR Date', render: (a) => formatDate(a.line.insurerOr?.orDate) },
  {
    key: 'amt',
    header: 'OR Amount',
    numeric: true,
    render: (a) => <Amount value={a.line.insurerOr?.amount} />,
  },
  {
    key: 'paid',
    header: 'Paid PR',
    numeric: true,
    render: (a) => <Amount value={a.line.amounts.paidAr} />,
  },
  {
    key: 'status',
    header: 'Status',
    render: (a) => <StatusBadge status={a.line.insurerOr?.status ?? 'UNKNOWN'} />,
  },
];

const RECORDS: Column<FeedRecord>[] = [
  { key: 'key', header: 'Record', render: (r) => r.key },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'ref', header: 'Result', render: (r) => r.reference ?? '' },
  { key: 'msg', header: 'Reason', render: (r) => r.message ?? '' },
];

const RUNS: Column<FeedRun>[] = [
  { key: 'no', header: 'Upload Run', render: (r) => r.runNo },
  { key: 'file', header: 'File', render: (r) => r.fileName ?? '' },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'ok', header: 'Accepted', numeric: true, render: (r) => r.accepted },
  { key: 'fail', header: 'Failed', numeric: true, render: (r) => r.failed },
  {
    key: 'at',
    header: 'Uploaded',
    render: (r) => `${formatDateTime(r.startedAt)} · ${r.createdBy}`,
  },
];

/** The exception report of one upload (RMTID.016). */
function ExceptionReport({ result }: Readonly<{ result: OrUpload }>) {
  return (
    <Card title={`Exception Report ${result.run.runNo}`}>
      <div className="stack">
        <p>{result.run.message ?? result.run.errorDetail}</p>
        <div className="grid-4">
          <Kpi label="Records Read" value={result.run.read} />
          <Kpi label="OR Matches Paid PR" value={result.matched} />
          <Kpi label="Amount Mismatch" value={result.mismatched} accent={result.mismatched > 0} />
          <Kpi label="Refused" value={result.run.failed} accent={result.run.failed > 0} />
        </div>
        <DataTable
          caption="Accounts updated"
          columns={UPDATED}
          rows={result.updated}
          rowKey={(a) => `${a.batchNo}-${a.line.invoiceNo}`}
          emptyMessage="No account was updated by this upload"
        />
        <DataTable
          caption="Records of the upload"
          columns={RECORDS}
          rows={result.records}
          rowKey={(r) => r.key}
        />
      </div>
    </Card>
  );
}

/**
 * Insurer OR upload (RMTID.012/013/016): the insurer returns the remittance schedule with its OR
 * number, date and amount per account; each account of an approved batch is updated and compared
 * with the paid PR, and the exception report lists the mismatches and the refused records.
 */
export default function InsurerOrPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<OrUpload>();
  const runs = useQuery({
    queryKey: ['remittance', 'or-runs', page],
    queryFn: () => remittanceApi.insurerOrRuns(page),
  });
  const upload = useMutation({
    mutationFn: (file: File) => remittanceApi.uploadInsurerOr(file),
    onSuccess: async (r) => {
      setResult(r);
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${r.run.runNo}: ${r.run.message ?? r.run.status}`);
    },
  });
  const open = useMutation({
    mutationFn: (run: FeedRun) => remittanceApi.insurerOrRun(run.id),
    onSuccess: setResult,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Insurer OR Upload"
        description="Upload the remittance schedules returned by the insurers with their official receipts, and review the exceptions."
        actions={<TemplateButton name="insurer-or-template.csv" content={OR_TEMPLATE} />}
      />
      <ErrorAlert error={upload.error ?? open.error ?? runs.error} />
      <Card title="Upload Insurer Schedule">
        <UploadForm
          label="Insurer schedule (batchNo, invoiceNo, orNo, orDate, orAmount)"
          busy={upload.isPending}
          onUpload={(file) => upload.mutate(file)}
        />
      </Card>
      {result !== undefined && <ExceptionReport result={result} />}
      <Card title="Upload History" flush>
        <div>
          <DataTable
            caption="Insurer OR uploads"
            columns={RUNS}
            rows={runs.data?.content ?? []}
            rowKey={(r) => r.id}
            loading={runs.isLoading}
            onRowClick={(r) => open.mutate(r)}
          />
          <PageFooter data={runs.data} noun="uploads" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
