import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Flag } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { EstimatedItem } from './commissionApi';

function FlagDialog({
  invoiceNo,
  estimated,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  invoiceNo: string | undefined;
  estimated: boolean;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (invoiceNo: string, reason: string) => void;
}>) {
  const [no, setNo] = useState(invoiceNo ?? '');
  const [reason, setReason] = useState('');
  return (
    <Modal
      title={estimated ? 'Flag Invoice as Estimated' : `Clear Estimate of ${invoiceNo ?? ''}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            disabled={no.trim() === '' || reason.trim() === ''}
            onClick={() => onSave(no.trim(), reason.trim())}
          >
            {estimated ? 'Flag Estimated' : 'Clear Estimate'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Invoice No." required>
          {(id) => (
            <input
              id={id}
              className="input"
              disabled={invoiceNo !== undefined}
              value={no}
              onChange={(e) => setNo(e.target.value)}
            />
          )}
        </Field>
        <Field label="Reason" required>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={200}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Estimated items (CMRID.014): invoices booked on an estimated premium, shown on the production
 * register and the commission reports until the final premium is known.
 */
export default function EstimatedItemsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<{ invoiceNo?: string; estimated: boolean }>();
  const items = useQuery({
    queryKey: ['commission', 'estimated', companyId, page],
    queryFn: () => commissionApi.estimated(companyId, page),
    enabled: companyId > 0,
  });
  const flag = useMutation({
    mutationFn: (v: { invoiceNo: string; estimated: boolean; reason: string }) =>
      commissionApi.flagEstimated(v.invoiceNo, v.estimated, v.reason),
    onSuccess: async (i) => {
      setDialog(undefined);
      await queryClient.invalidateQueries({ queryKey: ['commission', 'estimated'] });
      toast.success(`${i.invoiceNo} ${i.estimated ? 'flagged estimated' : 'estimate cleared'}`);
    },
  });
  const mayEdit = can('COMMREC_PROCESS');
  const columns: Column<EstimatedItem>[] = [
    { key: 'invoice', header: 'Invoice No.', render: (i) => <strong>{i.invoiceNo}</strong> },
    { key: 'arn', header: 'ARN', render: (i) => i.arn ?? '' },
    { key: 'insurer', header: 'Insurer', render: (i) => i.insurerCode ?? '' },
    { key: 'assured', header: 'Assured', render: (i) => i.assuredName ?? '' },
    { key: 'booked', header: 'Booked', render: (i) => formatDate(i.bookingDate) },
    {
      key: 'gross',
      header: 'Gross Premium',
      numeric: true,
      render: (i) => <Amount value={i.grossPremium} />,
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (i) =>
        mayEdit ? (
          <Button
            size="sm"
            variant="secondary"
            onClick={() => setDialog({ invoiceNo: i.invoiceNo, estimated: false })}
          >
            Clear Estimate
          </Button>
        ) : null,
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="Estimated Items"
        description="Invoices booked on an estimated premium, until the final premium is known."
        actions={
          mayEdit ? (
            <Button icon={<Flag size={16} />} onClick={() => setDialog({ estimated: true })}>
              Flag Invoice
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={items.error} />
      <Card>
        <div className="stack">
          <DataTable
            caption="Estimated items"
            columns={columns}
            rows={items.data?.content ?? []}
            rowKey={(i) => i.invoiceNo}
            loading={items.isLoading}
            emptyMessage="No items to display"
          />
          <PageFooter data={items.data} noun="invoices" onPage={setPage} />
        </div>
      </Card>
      {dialog !== undefined && (
        <FlagDialog
          invoiceNo={dialog.invoiceNo}
          estimated={dialog.estimated}
          busy={flag.isPending}
          error={flag.error}
          onClose={() => setDialog(undefined)}
          onSave={(invoiceNo, reason) =>
            flag.mutate({ invoiceNo, estimated: dialog.estimated, reason })
          }
        />
      )}
    </div>
  );
}
