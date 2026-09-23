import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Recovery } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import { DocumentActions } from './DocumentActions';
import { RecoveryDialog } from './RecoveryDialog';
import type { ClaimTabProps } from './ReservesTab';

/** Salvage and subrogation recoveries with checker actions. */
export function RecoveriesTab({ claim, actions, onChange }: Readonly<ClaimTabProps>) {
  const [open, setOpen] = useState(false);
  const recoveries = useQuery({
    queryKey: ['claim-docs', claim.id, 'recoveries'],
    queryFn: () => claimsApi.recoveries(claim.id),
  });
  return (
    <Card
      title="Recoveries"
      flush
      actions={
        actions.recover && (
          <Button
            size="sm"
            variant="accent"
            icon={<Plus size={14} />}
            onClick={() => setOpen(true)}
          >
            Record recovery
          </Button>
        )
      }
    >
      <ErrorAlert error={recoveries.error} />
      <p className="muted" style={{ margin: 12 }}>
        Set a recovery estimate on the Reserves tab first; a recovery cannot exceed it.
      </p>
      <DataTable<Recovery>
        loading={recoveries.isLoading}
        rows={recoveries.data ?? []}
        rowKey={(r) => r.id}
        caption="Recoveries"
        columns={[
          { key: 'no', header: 'Recovery no.', render: (r) => <strong>{r.recoveryNo}</strong> },
          { key: 'type', header: 'Type', render: (r) => humanize(r.recoveryType) },
          { key: 'from', header: 'From', render: (r) => r.fromPartyName ?? '—' },
          { key: 'bank', header: 'Bank account', render: (r) => r.bankAccountCode },
          {
            key: 'amt',
            header: 'Amount',
            numeric: true,
            render: (r) => <Amount value={r.amount} />,
          },
          {
            key: 'our',
            header: 'Our share',
            numeric: true,
            render: (r) => <Amount value={r.ourAmount} />,
          },
          {
            key: 'date',
            header: 'Accounting date',
            render: (r) => formatDate(r.approval.approvalDate),
          },
          {
            key: 'st',
            header: 'Status',
            render: (r) => <StatusBadge status={r.approval.status} />,
          },
          {
            key: 'act',
            header: '',
            render: (r) => (
              <DocumentActions
                kind="recoveries"
                documentId={r.id}
                label={r.recoveryNo}
                approval={r.approval}
                onDone={onChange}
              />
            ),
          },
        ]}
      />
      <RecoveryDialog claim={claim} open={open} onClose={() => setOpen(false)} onSaved={onChange} />
    </Card>
  );
}
