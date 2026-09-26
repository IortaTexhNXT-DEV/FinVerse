import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { hasVariance, SOA_TABS } from './acsl';
import type { SoaTab } from './acsl';
import { acslApi } from './api';
import type { LogRow, ReconBucket, ReconRow, RunView, SoaUpload } from './api';
import './acsl.css';

function Variance({ value }: Readonly<{ value?: number }>) {
  if (value === undefined) {
    return <>—</>;
  }
  return (
    <span className={hasVariance(value) ? 'acsl-variance' : undefined}>
      <Amount value={value} />
    </span>
  );
}

const money = (v?: number) => (v === undefined ? '—' : <Amount value={v} />);

const RESULT_COLUMNS: Column<ReconRow>[] = [
  { key: 'row', header: 'Row', render: (r) => r.rowNo },
  {
    key: 'invoice',
    header: 'Invoice / Policy',
    render: (r) => (
      <>
        <strong>{r.invoiceNo ?? '—'}</strong>
        <span className="cell-sub">{r.policyNo ?? ''}</span>
      </>
    ),
  },
  { key: 'assured', header: 'Assured', render: (r) => r.assuredName ?? '—' },
  { key: 'bucket', header: 'Result', render: (r) => <StatusBadge status={r.bucket} /> },
  { key: 'soaPremium', header: 'SOA Premium', numeric: true, render: (r) => money(r.soaPremium) },
  {
    key: 'bookPremium',
    header: 'Book Premium',
    numeric: true,
    render: (r) => money(r.book.premium),
  },
  {
    key: 'pv',
    header: 'Premium Variance',
    numeric: true,
    render: (r) => <Variance value={r.premiumVariance} />,
  },
  { key: 'soaBalance', header: 'SOA Balance', numeric: true, render: (r) => money(r.soaBalance) },
  {
    key: 'bookOs',
    header: 'Book Outstanding',
    numeric: true,
    render: (r) => money(r.book.outstanding),
  },
  {
    key: 'ov',
    header: 'Balance Variance',
    numeric: true,
    render: (r) => <Variance value={r.outstandingVariance} />,
  },
  {
    key: 'remit',
    header: 'Remittance',
    render: (r) =>
      r.book.remittanceBatchNo ? (
        <>
          {r.book.remittanceBatchNo}
          <span className="cell-sub">
            {r.book.remittanceDate ? formatDate(r.book.remittanceDate) : ''}
          </span>
        </>
      ) : (
        '—'
      ),
  },
];

const LOG_COLUMNS: Column<LogRow>[] = [
  { key: 'row', header: 'Row', render: (l) => l.rowNo },
  { key: 'status', header: 'Status', render: (l) => <StatusBadge status={l.status} /> },
  { key: 'invoice', header: 'Invoice', render: (l) => l.invoiceNo ?? '—' },
  { key: 'policy', header: 'Policy', render: (l) => l.policyNo ?? '—' },
  { key: 'assured', header: 'Assured', render: (l) => l.assuredName ?? '—' },
  { key: 'premium', header: 'Gross Premium', numeric: true, render: (l) => money(l.grossPremium) },
  { key: 'error', header: 'Error', render: (l) => l.error ?? '' },
];

const COUNT_OF: Record<ReconBucket, keyof RunView> = {
  OUTSTANDING: 'outstanding',
  FOR_REMITTANCE: 'forRemittance',
  REMITTED: 'remitted',
  CANCELLED: 'cancelled',
  DIRECT_BILLED: 'directBilled',
  NOT_FOUND: 'notFound',
};

function tabsOf(u: SoaUpload) {
  return SOA_TABS.map((t) => {
    let count: number | undefined;
    if (t.id === 'LOG') {
      count = u.rowsFailed;
    } else if (t.id !== 'ALL' && u.run) {
      count = Number(u.run[COUNT_OF[t.id]]);
    }
    return { id: t.id, label: count ? `${t.label} (${String(count)})` : t.label };
  });
}

function Counts({ u }: Readonly<{ u: SoaUpload }>) {
  const items: [string, string][] = [
    ['Rows read', String(u.rowsRead)],
    ['Loaded', String(u.rowsLoaded)],
    ['Failed', String(u.rowsFailed)],
    ['With variance', u.run ? String(u.run.withVariance) : '—'],
    ['Not found', u.run ? String(u.run.notFound) : '—'],
  ];
  return (
    <Card
      title={u.run ? `Reconciliation run ${String(u.run.runNo)}` : 'Not reconciled yet'}
      actions={
        u.run && (
          <span className="cell-sub">{`${u.run.runBy} · ${formatDateTime(u.run.runAt)}`}</span>
        )
      }
    >
      <div className="acsl-counts">
        {items.map(([label, value]) => (
          <div key={label} className="acsl-count">
            <strong>{value}</strong>
            <span>{label}</span>
          </div>
        ))}
      </div>
    </Card>
  );
}

function Rows({ id, tab }: Readonly<{ id: number; tab: SoaTab }>) {
  const bucket = tab === 'ALL' || tab === 'LOG' ? undefined : tab;
  const results = useQuery({
    queryKey: ['acsl', 'results', id, bucket],
    queryFn: () => acslApi.results(id, bucket),
    enabled: tab !== 'LOG',
  });
  const log = useQuery({
    queryKey: ['acsl', 'log', id],
    queryFn: () => acslApi.uploadLog(id),
    enabled: tab === 'LOG',
  });
  if (tab === 'LOG') {
    return (
      <>
        <ErrorAlert error={log.error} />
        <DataTable
          caption="Upload log"
          columns={LOG_COLUMNS}
          rows={log.data ?? []}
          rowKey={(l) => l.rowNo}
          loading={log.isLoading}
          emptyMessage="No items to display"
        />
      </>
    );
  }
  return (
    <>
      <ErrorAlert error={results.error} />
      <DataTable
        caption="Reconciliation results"
        columns={RESULT_COLUMNS}
        rows={results.data ?? []}
        rowKey={(r) => r.rowNo}
        loading={results.isLoading}
        emptyMessage="No items to display"
      />
    </>
  );
}

/**
 * One insurer statement (ACSL 2.14.1-2.14.2): the load and reconciliation counts, the lines by
 * result with the premium and balance variances highlighted, the upload log of rejected rows, the
 * reconciliation report and a new run after the books change.
 */
export default function SoaUploadDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<SoaTab>('ALL');
  const upload = useQuery({
    queryKey: ['acsl', 'upload', id],
    queryFn: () => acslApi.getUpload(id),
  });
  const report = useMutation({
    mutationFn: () => acslApi.report(id),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  const again = useMutation({
    mutationFn: () => acslApi.reconcile(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['acsl'] });
      toast.success('Reconciled again');
    },
  });
  if (upload.data === undefined) {
    return upload.error ? (
      <ErrorAlert error={upload.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const u = upload.data;
  return (
    <div className="stack">
      <PageHeader
        backTo="/acsl/soa"
        section="Finance · ACSL"
        title={u.uploadNo}
        description={`${u.insurerCode} statement ${formatDate(u.periodFrom)} – ${formatDate(u.periodTo)} (${u.fileName})`}
        actions={
          <>
            {can('ACSL_REPORT_EXPORT') && (
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                busy={report.isPending}
                onClick={() => report.mutate()}
              >
                Download Report
              </Button>
            )}
            {can('ACSL_UPLOAD') && (
              <Button
                variant="primary"
                icon={<RefreshCw size={16} />}
                busy={again.isPending}
                onClick={() => again.mutate()}
              >
                Reconcile Again
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={report.error ?? again.error} />
      <Counts u={u} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<SoaTab> tabs={tabsOf(u)} active={tab} onChange={setTab} />
        </div>
        <Rows id={id} tab={tab} />
      </Card>
    </div>
  );
}
