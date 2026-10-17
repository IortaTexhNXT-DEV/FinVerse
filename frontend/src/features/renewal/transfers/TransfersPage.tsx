import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { TransferView } from '@/api/renewal';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { RowActions } from '@/components/ui/RowActions';
import { SalesUnitName } from '@/components/broking/LovLabel';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { TextDialog } from '../common/ActionDialogs';
import { transferActions } from '../common/presentation';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';

type Side = 'incoming' | 'outgoing';
interface Decision {
  kind: 'accept' | 'decline' | 'cancel';
  transfer: TransferView;
}

const TABS: { id: Side; label: string }[] = [
  { id: 'incoming', label: 'Incoming' },
  { id: 'outgoing', label: 'Outgoing' },
];

function useDecision(onDone: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ d, remarks }: { d: Decision; remarks: string }) => {
      if (d.kind === 'accept')
        return renewalApi.acceptTransfer(d.transfer.id, remarks || undefined);
      if (d.kind === 'decline') return renewalApi.declineTransfer(d.transfer.id, remarks);
      return renewalApi.cancelTransfer(d.transfer.id);
    },
    onSuccess: async (t) => {
      onDone();
      toast.success(`Transfer of ${t.renewalRef} ${t.status.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
}

/**
 * Transfers (FR-RN-031, 032): requests to move a renewal to another Marketing unit. The receiving Team
 * Leader accepts or declines them with remarks; the requesting unit may cancel an open request.
 */
export default function TransfersPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [side, setSide] = useState<Side>('incoming');
  const [decision, setDecision] = useState<Decision>();
  const rows = useQuery({
    queryKey: ['renewal', 'transfers', side, companyId],
    queryFn: () =>
      side === 'incoming' ? renewalApi.transfersIn(companyId) : renewalApi.transfersOut(companyId),
    enabled: companyId > 0,
  });
  const decide = useDecision(() => setDecision(undefined));
  const act = (kind: Decision['kind'], transfer: TransferView) => setDecision({ kind, transfer });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Transfers"
        description="Renewals moving between Marketing units."
      />
      <ErrorAlert error={rows.error} onRetry={() => void rows.refetch()} />
      <Card flush>
        <Tabs tabs={TABS} active={side} onChange={setSide} />
        <DataTable<TransferView>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No transfers"
          onRowClick={(r) =>
            void navigate(`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`)
          }
          columns={[
            { key: 'ref', header: 'Renewal Reference', kind: 'code', render: (r) => r.renewalRef },
            {
              key: 'client',
              header: 'Client',
              render: (r) => (
                <CellStack main={r.clientName} sub={`Expiry ${formatDate(r.expiry)}`} />
              ),
            },
            {
              key: 'units',
              header: 'From / To',
              render: (r) => (
                <CellStack
                  main={<SalesUnitName code={r.fromUnit} />}
                  sub={
                    <>
                      to <SalesUnitName code={r.toUnit} />
                    </>
                  }
                />
              ),
            },
            { key: 'remarks', header: 'Remarks', render: (r) => r.remarks },
            {
              key: 'by',
              header: 'Requested',
              render: (r) => (
                <CellStack
                  main={<UserName login={r.requestedBy} />}
                  sub={formatDateTime(r.requestedAt)}
                />
              ),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (r) => <StatusBadge status={r.status} />,
            },
            {
              key: 'act',
              header: '',
              render: (r) => (
                <RowActions
                  record={r.renewalRef}
                  actions={transferActions(side, r.status, {
                    accept: () => act('accept', r),
                    decline: () => act('decline', r),
                    cancel: () => act('cancel', r),
                  })}
                />
              ),
            },
          ]}
        />
      </Card>
      {decision?.kind === 'cancel' && (
        <ConfirmDialog
          title="Cancel the transfer request"
          record={decision.transfer.renewalRef}
          effect="The renewal stays with your unit."
          confirmLabel="Cancel Request"
          busy={decide.isPending}
          error={decide.error}
          onClose={() => setDecision(undefined)}
          onConfirm={() => decide.mutate({ d: decision, remarks: '' })}
        />
      )}
      {decision !== undefined && decision.kind !== 'cancel' && (
        <TextDialog
          title={`${decision.kind === 'accept' ? 'Accept' : 'Decline'} transfer of ${decision.transfer.renewalRef}`}
          label="Remarks"
          confirmLabel={decision.kind === 'accept' ? 'Accept' : 'Decline'}
          required={decision.kind === 'decline'}
          busy={decide.isPending}
          error={decide.error}
          onClose={() => setDecision(undefined)}
          onConfirm={(remarks) => decide.mutate({ d: decision, remarks })}
        />
      )}
    </div>
  );
}
