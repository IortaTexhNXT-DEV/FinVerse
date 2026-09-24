import { useMutation, useQuery } from '@tanstack/react-query';
import { FilePlus2, FileSpreadsheet, Upload } from 'lucide-react';
import { placementApi } from '@/api/placement';
import type { BillingBatch, BillingCandidate } from '@/api/placement';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';

interface BatchesProps {
  companyId: number;
  page: number;
  onPage: (page: number) => void;
  onUploadFor: (batch: BillingBatch) => void;
  onChanged: () => void;
}

/**
 * CLPC billing (BRNB.067): the CBG Fire accounts awaiting payment not yet billed, billed on a new
 * batch (selected or all), and the batches with their billing file in Excel or OpenDocument.
 * Transport to CLPC is parked (Q28): the file is downloaded.
 */
export function BillingBatches({
  companyId,
  page,
  onPage,
  onUploadFor,
  onChanged,
}: Readonly<BatchesProps>) {
  const toast = useToast();
  const selection = useRowSelection();
  const download = useFileDownload();
  const candidates = useQuery({
    queryKey: ['placement', 'billing-candidates', companyId],
    queryFn: () => placementApi.candidates(companyId),
    enabled: companyId > 0,
  });
  const batches = useQuery({
    queryKey: ['placement', 'billing-batches', companyId, page],
    queryFn: () => placementApi.batches(companyId, page),
    enabled: companyId > 0,
  });
  const create = useMutation({
    mutationFn: () => placementApi.createBatch(companyId, selection.keys),
    onSuccess: (batch) => {
      selection.clear();
      toast.success(`${batch.batchNo} created with ${batch.itemCount} account(s)`);
      onChanged();
    },
  });
  const rows = candidates.data ?? [];
  return (
    <>
      <ErrorAlert error={candidates.error ?? batches.error ?? create.error ?? download.error} />
      <Card
        title="CBG Fire Awaiting Payment"
        flush
        actions={
          <Button
            variant="primary"
            icon={<FilePlus2 size={16} />}
            busy={create.isPending}
            disabled={rows.length === 0}
            onClick={() => create.mutate()}
          >
            {selection.keys.length > 0 ? `Bill Selected (${selection.keys.length})` : 'Bill All'}
          </Button>
        }
      >
        <DataTable<BillingCandidate>
          caption="Accounts to bill"
          loading={candidates.isLoading}
          rows={rows}
          rowKey={(c) => c.arn}
          emptyMessage="Every CBG Fire account awaiting payment is billed."
          columns={[
            selectionColumn(
              rows,
              (c) => c.arn,
              selection,
              (c) => c.arn,
            ),
            { key: 'arn', header: 'Reference', render: (c) => <code>{c.arn}</code> },
            { key: 'borrower', header: 'Borrower', render: (c) => c.clientName },
            { key: 'pn', header: 'PN No.', render: (c) => c.pnNumbers.join(', ') || '—' },
            {
              key: 'loan',
              header: 'Loan Application No.',
              render: (c) => c.loanApplicationNo ?? '—',
            },
            {
              key: 'premium',
              header: 'Premium',
              numeric: true,
              render: (c) => formatAmount(c.grossPremium),
            },
          ]}
        />
      </Card>
      <Card title="Billing Batches" flush>
        <DataTable<BillingBatch>
          caption="Billing batches"
          loading={batches.isLoading}
          rows={batches.data?.content ?? []}
          rowKey={(b) => b.id}
          emptyMessage="No items to display"
          columns={[
            { key: 'no', header: 'Batch', render: (b) => <code>{b.batchNo}</code> },
            { key: 'date', header: 'Billing Date', render: (b) => formatDate(b.billingDate) },
            { key: 'status', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
            { key: 'count', header: 'Accounts', numeric: true, render: (b) => b.itemCount },
            {
              key: 'total',
              header: 'Total Premium',
              numeric: true,
              render: (b) => formatAmount(b.totalPremium),
            },
            {
              key: 'actions',
              header: '',
              render: (b) => (
                <span className="row">
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<FileSpreadsheet size={14} />}
                    onClick={() => download.mutate(() => placementApi.batchFile(b.id, 'XLSX'))}
                  >
                    Excel
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<FileSpreadsheet size={14} />}
                    onClick={() => download.mutate(() => placementApi.batchFile(b.id, 'ODS'))}
                  >
                    ODS
                  </Button>
                  {b.status !== 'CLOSED' && (
                    <Button
                      size="sm"
                      variant="secondary"
                      icon={<Upload size={14} />}
                      onClick={() => onUploadFor(b)}
                    >
                      Upload Report
                    </Button>
                  )}
                </span>
              ),
            },
          ]}
        />
        <PageFooter data={batches.data} noun="batches" onPage={onPage} />
      </Card>
    </>
  );
}
