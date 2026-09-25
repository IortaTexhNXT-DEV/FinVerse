import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileCheck2 } from 'lucide-react';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { CommissionLine } from './cashieringApi';

const COLUMNS: Column<CommissionLine>[] = [
  { key: 'job', header: 'Upload', render: (l) => `${l.jobNo} · row ${l.rowNo}` },
  {
    key: 'insurer',
    header: 'Insurer / Payee',
    render: (l) => (
      <>
        {l.insurerCode}
        <span className="cell-sub">{l.payeeName}</span>
      </>
    ),
  },
  { key: 'ref', header: 'Payment Ref.', render: (l) => l.paymentRef ?? l.certificateRef ?? '' },
  { key: 'invoice', header: 'Invoice', render: (l) => l.invoiceNo ?? '' },
  { key: 'gross', header: 'Gross', numeric: true, render: (l) => <Amount value={l.gross} /> },
  { key: 'vat', header: 'VAT', numeric: true, render: (l) => <Amount value={l.vat} /> },
  { key: 'wtax', header: 'WTAX', numeric: true, render: (l) => <Amount value={l.wtax} /> },
  { key: 'date', header: 'Paid', render: (l) => formatDate(l.paymentDate) },
  { key: 'or', header: 'OR', render: (l) => l.receiptNo ?? l.message ?? '' },
  { key: 'status', header: 'Status', render: (l) => <StatusBadge status={l.status} /> },
];

/**
 * Commission ORs (CSHID.002/011): the insurers' commission payment file is uploaded, the lines are
 * staged, and one Head Office official receipt is issued per insurer payment with its VAT and
 * withholding tax.
 */
export default function CommissionOrsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const lines = useQuery({
    queryKey: ['cashiering', 'commission-lines', companyId],
    queryFn: () => cashieringApi.commissionLines(companyId),
    enabled: companyId > 0,
  });
  const issue = useMutation({
    mutationFn: () => cashieringApi.issueCommissionOrs(companyId),
    onSuccess: async (ors) => {
      toast.success(
        ors.length === 0 ? 'No OR to issue' : `${ors.length} OR(s) issued: ${ors.join(', ')}`,
      );
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const staged = lines.data ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Commission ORs"
        description="Upload the commission payments of the insurers and issue their official receipts."
        actions={
          can('CASH_RECEIPT') && (
            <Button
              variant="accent"
              icon={<FileCheck2 size={16} />}
              disabled={staged.length === 0}
              busy={issue.isPending}
              onClick={() => issue.mutate()}
            >
              Issue ORs
            </Button>
          )
        }
      />
      <BulkUploadWizard
        handler="COMMISSION_PAYMENT"
        onCommitted={() =>
          void queryClient.invalidateQueries({ queryKey: ['cashiering', 'commission-lines'] })
        }
      />
      <ErrorAlert error={lines.error ?? issue.error} />
      <Card title="Staged Commission Payments" flush>
        <DataTable
          caption="Staged commission payments"
          columns={COLUMNS}
          rows={staged}
          rowKey={(l) => l.id}
          loading={lines.isLoading}
          emptyMessage="No commission payment waiting for an OR"
        />
      </Card>
    </div>
  );
}
