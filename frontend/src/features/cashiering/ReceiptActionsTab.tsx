import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { statusMessage } from '@/components/ui/statusTones';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { LovLabel } from '@/components/broking/LovLabel';
import { RowActions } from '@/components/ui/RowActions';
import { receiptActionLabel } from './cashieringLabels';
import { cashieringApi } from './cashieringApi';
import type { ReceiptAction } from './cashieringApi';
import { UserName } from '@/components/ui/UserName';

const OPEN_STAGES = ['REQUESTED', 'FOR_APPROVAL'];

/**
 * Cancellation and reinstatement requests still open (CSHID.001-005): the checker approves here
 * (never the requester) and the maker resubmits a request returned to Requested.
 */
export function ReceiptActionsTab({ companyId }: Readonly<{ companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['cashiering', 'receipt-actions', companyId, page],
    queryFn: () => cashieringApi.actions(companyId, OPEN_STAGES, page),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<ReceiptAction>) => fn(),
    onSuccess: async (a) => {
      toast.success(statusMessage(a.transactionNo, a.stage, receiptActionLabel(a.action)));
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const columns: Column<ReceiptAction>[] = [
    {
      key: 'no',
      header: 'Transaction No.',
      render: (a) => (
        <>
          <strong>{a.transactionNo}</strong>
          <span className="cell-sub">{receiptActionLabel(a.action)}</span>
        </>
      ),
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (a) =>
        a.reasonText ?? (
          <LovLabel
            type={a.action === 'CANCEL' ? 'RECEIPT_CANCEL_REASON' : 'REINSTATEMENT_REASON'}
            code={a.reasonCode}
          />
        ),
    },
    { key: 'amount', header: 'Amount', numeric: true, render: (a) => <Amount value={a.amount} /> },
    {
      key: 'by',
      header: 'Requested',
      render: (a) => (
        <>
          <UserName login={a.requestedBy} />
          <span className="cell-sub">{formatDateTime(a.requestedAt)}</span>
        </>
      ),
    },
    { key: 'stage', header: 'Status', render: (a) => <StatusBadge status={a.stage} /> },
    {
      key: 'actions',
      header: '',
      render: (a) => (
        <RowActions
          record={a.transactionNo}
          actions={[
            {
              label: 'Open Receipt',
              onSelect: () => void navigate(`/cashiering/receipts/${a.receiptId}`),
            },
            {
              label: 'Approve',
              hidden: !(a.stage === 'FOR_APPROVAL' && can('CASH_APPROVE')),
              confirm: {
                title: 'Approve Receipt Action',
                effect: 'The requested action is applied to the receipt.',
              },
              onSelect: () => act.mutateAsync(() => cashieringApi.approveAction(a.id)),
            },
            {
              label: 'Resubmit',
              hidden: a.stage !== 'REQUESTED',
              onSelect: () => act.mutate(() => cashieringApi.resubmitAction(a.id)),
            },
          ]}
        />
      ),
    },
  ];
  return (
    <>
      <ErrorAlert error={list.error ?? act.error} />
      <DataTable
        caption="Open cancellation and reinstatement requests"
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(a) => a.id}
        loading={list.isLoading}
        emptyMessage="No items to display"
      />
      <PageFooter data={list.data} noun="requests" onPage={setPage} />
    </>
  );
}
