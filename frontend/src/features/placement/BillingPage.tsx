import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { placementApi } from '@/api/placement';
import type { BillingBatch, PaymentReport } from '@/api/placement';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { BillingBatches } from './BillingBatches';
import { UploadReportDialog } from './UploadReportDialog';

const TABS = [
  { id: 'batches', label: 'Billing Batches' },
  { id: 'reports', label: 'Payment Reports' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function ReportsTable({ companyId }: Readonly<{ companyId: number }>) {
  const [page, setPage] = useState(0);
  const reports = useQuery({
    queryKey: ['placement', 'reports', companyId, page],
    queryFn: () => placementApi.reports(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <Card title="Payment Reports" flush>
      <ErrorAlert error={reports.error} />
      <DataTable<PaymentReport>
        caption="Payment reports"
        loading={reports.isLoading}
        rows={reports.data?.content ?? []}
        rowKey={(r) => r.id}
        emptyMessage="No items to display"
        columns={[
          {
            key: 'no',
            header: 'Report',
            render: (r) => <Link to={`/placement/billing/reports/${r.id}`}>{r.reportNo}</Link>,
          },
          { key: 'kind', header: 'Kind', render: (r) => (r.kind === 'CLPC' ? 'CLPC' : 'By ARN') },
          { key: 'file', header: 'File', render: (r) => r.fileName },
          { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
          {
            key: 'match',
            header: 'Matched / Unpaid / Unmatched / Ambiguous',
            render: (r) => `${r.matched} / ${r.unpaid} / ${r.unmatched} / ${r.ambiguous}`,
          },
          {
            key: 'by',
            header: 'Uploaded',
            render: (r) => `${formatDateTime(r.createdAt)} by ${r.createdBy}`,
          },
        ]}
      />
      <PageFooter data={reports.data} noun="reports" onPage={setPage} />
    </Card>
  );
}

/**
 * CLPC Billing (BRNB.067/068): bill the CBG Fire accounts awaiting payment, download the billing
 * file, upload the CLPC or payment report and open its match review. Placement records only the
 * gate decision and its evidence, never receipts.
 */
export default function BillingPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('batches');
  const [page, setPage] = useState(0);
  const [uploading, setUploading] = useState<{ batchId?: number } | null>(null);
  const batches = useQuery({
    queryKey: ['placement', 'billing-batches', companyId, 0],
    queryFn: () => placementApi.batches(companyId, 0),
    enabled: companyId > 0,
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['placement'] });
  return (
    <div className="stack">
      <PageHeader
        section="Placement & Booking"
        title="CLPC Billing"
        description="Billing file of the CBG Fire accounts awaiting payment and payment report matching. The file is exchanged with CLPC outside the system."
        actions={
          <Button variant="primary" icon={<Upload size={16} />} onClick={() => setUploading({})}>
            Upload Payment Report
          </Button>
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'batches' ? (
        <BillingBatches
          companyId={companyId}
          page={page}
          onPage={setPage}
          onUploadFor={(b: BillingBatch) => setUploading({ batchId: b.id })}
          onChanged={refresh}
        />
      ) : (
        <ReportsTable companyId={companyId} />
      )}
      {uploading && (
        <UploadReportDialog
          companyId={companyId}
          batches={batches.data?.content ?? []}
          initialBatchId={uploading.batchId}
          onClose={() => setUploading(null)}
          onUploaded={(report) => {
            setUploading(null);
            refresh();
            void navigate(`/placement/billing/reports/${report.id}`);
          }}
        />
      )}
    </div>
  );
}
