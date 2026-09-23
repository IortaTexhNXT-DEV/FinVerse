import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { claimsApi } from '@/api/claims';
import type { Claim, ReserveChange } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import type { ClaimActions } from './claimWorkflow';
import { DocumentActions } from './DocumentActions';
import { ReserveDialog } from './ReserveDialog';

export interface ClaimTabProps {
  claim: Claim;
  actions: ClaimActions;
  onChange: () => Promise<void>;
}

/** Reserve (estimate) history with checker actions on pending changes. */
export function ReservesTab({ claim, actions, onChange }: Readonly<ClaimTabProps>) {
  const [open, setOpen] = useState(false);
  const reserves = useQuery({
    queryKey: ['claim-docs', claim.id, 'reserves'],
    queryFn: () => claimsApi.reserves(claim.id),
  });
  return (
    <Card
      title="Reserve changes"
      flush
      actions={
        actions.reserve && (
          <Button
            size="sm"
            variant="accent"
            icon={<Plus size={14} />}
            onClick={() => setOpen(true)}
          >
            Change reserve
          </Button>
        )
      }
    >
      <ErrorAlert error={reserves.error} />
      <DataTable<ReserveChange>
        loading={reserves.isLoading}
        rows={reserves.data ?? []}
        rowKey={(r) => r.id}
        caption="Reserve changes"
        columns={[
          { key: 'no', header: '#', render: (r) => r.changeNo },
          {
            key: 'side',
            header: 'Estimate',
            render: (r) => `${humanize(r.side)} · ${humanize(r.costType)}`,
          },
          {
            key: 'new',
            header: 'New estimate',
            numeric: true,
            render: (r) => <Amount value={r.newEstimate} />,
          },
          {
            key: 'chg',
            header: 'Change',
            numeric: true,
            render: (r) => <Amount value={r.changeAmount} />,
          },
          {
            key: 'our',
            header: 'Our share',
            numeric: true,
            render: (r) => <Amount value={r.ourChange} />,
          },
          {
            key: 'why',
            header: 'Reason',
            render: (r) => (r.systemGenerated ? `${r.reason} (system)` : r.reason),
          },
          { key: 'by', header: 'Entered by', render: (r) => r.approval.submittedBy },
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
                kind="reserves"
                documentId={r.id}
                label={`reserve change ${String(r.changeNo)}`}
                approval={r.approval}
                onDone={onChange}
              />
            ),
          },
        ]}
      />
      <ReserveDialog claim={claim} open={open} onClose={() => setOpen(false)} onSaved={onChange} />
    </Card>
  );
}
