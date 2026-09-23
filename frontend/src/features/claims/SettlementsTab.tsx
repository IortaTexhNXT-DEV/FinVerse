import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Settlement } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import { DocumentActions } from './DocumentActions';
import type { ClaimTabProps } from './ReservesTab';
import { SettlementDialog } from './SettlementDialog';

/**
 * Settlements with checker actions. An approved settlement is a claim payable of the payee, paid by
 * a payment voucher in Payables (category CLAIM).
 */
export function SettlementsTab({ claim, actions, onChange }: Readonly<ClaimTabProps>) {
  const [open, setOpen] = useState(false);
  const settlements = useQuery({
    queryKey: ['claim-docs', claim.id, 'settlements'],
    queryFn: () => claimsApi.settlements(claim.id),
  });
  return (
    <Card
      title="Settlements"
      flush
      actions={
        actions.settle && (
          <Button
            size="sm"
            variant="accent"
            icon={<Plus size={14} />}
            onClick={() => setOpen(true)}
          >
            New settlement
          </Button>
        )
      }
    >
      <ErrorAlert error={settlements.error} />
      <DataTable<Settlement>
        loading={settlements.isLoading}
        rows={settlements.data ?? []}
        rowKey={(s) => s.id}
        caption="Settlements"
        columns={[
          { key: 'no', header: 'Settlement no.', render: (s) => <strong>{s.settlementNo}</strong> },
          { key: 'payee', header: 'Payee', render: (s) => s.payeeName },
          {
            key: 'type',
            header: 'Type',
            render: (s) => `${humanize(s.settlementType)} · ${humanize(s.costType)}`,
          },
          {
            key: 'ass',
            header: 'Assessed',
            numeric: true,
            render: (s) => <Amount value={s.assessedAmount} />,
          },
          {
            key: 'ded',
            header: 'Deductible + excess',
            numeric: true,
            render: (s) => <Amount value={s.deductible + s.excessAmount} />,
          },
          {
            key: 'net',
            header: 'Net',
            numeric: true,
            render: (s) => <Amount value={s.netAmount} />,
          },
          {
            key: 'our',
            header: 'Our share',
            numeric: true,
            render: (s) => <Amount value={s.ourAmount} />,
          },
          {
            key: 'pay',
            header: 'Payable',
            numeric: true,
            render: (s) => <Amount value={s.payableAmount} />,
          },
          {
            key: 'date',
            header: 'Accounting date',
            render: (s) => formatDate(s.approval.approvalDate),
          },
          {
            key: 'st',
            header: 'Status',
            render: (s) => <StatusBadge status={s.approval.status} />,
          },
          {
            key: 'act',
            header: '',
            render: (s) => (
              <DocumentActions
                kind="settlements"
                documentId={s.id}
                label={s.settlementNo}
                approval={s.approval}
                onDone={onChange}
              />
            ),
          },
        ]}
      />
      <SettlementDialog
        claim={claim}
        open={open}
        onClose={() => setOpen(false)}
        onSaved={onChange}
      />
    </Card>
  );
}
