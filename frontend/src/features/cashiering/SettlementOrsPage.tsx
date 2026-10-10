import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import type { SettlementOr } from './settlementOrsApi';
import {
  SETTLEMENT_OR_STATUS_LABELS,
  batchOf,
  canIssueAgain,
  settlementOrsApi,
} from './settlementOrsApi';

/**
 * Settlement ORs (FRS.CSH.07.01.01 to 07.01.04): the commission and incentive ORs of the insurer
 * settlements, kept until Disbursement approves the payment request and then issued, with the OR
 * number, and issuing again an OR that could not be issued.
 */
export default function SettlementOrsPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['cashiering', 'settlement-ors', companyId, page],
    queryFn: () => settlementOrsApi.list(companyId, page),
    enabled: companyId > 0,
  });
  const issue = useMutation({
    mutationFn: (id: number) => settlementOrsApi.issueAgain(id),
    onSuccess: async (or) => {
      if (or.status === 'ISSUED') {
        toast.success(`Official receipt ${or.receiptNo ?? ''} issued`);
      } else {
        toast.error(or.message ?? 'The OR could not be issued');
      }
      await queryClient.invalidateQueries({ queryKey: ['cashiering', 'settlement-ors'] });
    },
  });
  const columns: Column<SettlementOr>[] = [
    { key: 'batch', header: 'Settlement Batch', render: (o) => <strong>{batchOf(o)}</strong> },
    {
      key: 'type',
      header: 'OR Type',
      render: (o) => (o.orType === 'INCENTIVE' ? 'Incentive' : 'Commission'),
    },
    { key: 'payee', header: 'Insurer', truncate: true, render: (o) => o.payeeName ?? '' },
    { key: 'request', header: 'Payment Request', render: (o) => o.awaitRef },
    { key: 'currency', header: 'Currency', render: (o) => o.currency },
    { key: 'amount', header: 'Amount', numeric: true, render: (o) => <Amount value={o.amount} /> },
    { key: 'or', header: 'OR Number', render: (o) => o.receiptNo ?? '' },
    { key: 'kept', header: 'Requested', render: (o) => formatDateTime(o.createdAt) },
    {
      key: 'issued',
      header: 'Issued',
      render: (o) => (o.issuedAt ? formatDateTime(o.issuedAt) : ''),
    },
    {
      key: 'status',
      header: 'Status',
      render: (o) => (
        <StatusBadge status={o.status} label={SETTLEMENT_OR_STATUS_LABELS[o.status]} />
      ),
    },
    { key: 'message', header: 'Remarks', truncate: true, render: (o) => o.message ?? '' },
    {
      key: 'actions',
      header: '',
      width: '56px',
      render: (o) => (
        <RowActionMenu
          label={batchOf(o)}
          actions={[
            {
              label: 'Issue Again',
              onSelect: () => issue.mutate(o.id),
              disabled: !canIssueAgain(o) || issue.isPending,
            },
          ]}
        />
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Settlement ORs"
        description="Commission and incentive ORs of the insurer settlements, issued once Disbursement approves the payment request."
      />
      <ErrorAlert error={list.error ?? issue.error} />
      <Card title="ORs of Insurer Settlements">
        <DataTable
          caption="ORs of insurer settlements"
          list="cashiering-settlement-ors"
          columns={columns}
          rows={list.data?.content ?? []}
          rowKey={(o) => o.id}
          loading={list.isLoading}
          emptyMessage="No settlement OR yet"
        />
        <PageFooter data={list.data} noun="ORs" onPage={setPage} />
      </Card>
    </div>
  );
}
