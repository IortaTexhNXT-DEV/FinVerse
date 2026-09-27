import { CheckCircle2, Download, FilePlus2, XCircle } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import type { ProgrammeView } from '@/api/eb';
import { ebMarketApi } from '@/api/ebMarket';
import type { Proposal, Tor } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { ebLabel } from '../common/ebCodes';
import { ActionButton } from '../common/ActionButton';
import { useCycleList } from '../common/useCycleList';
import { useEbMutation } from '../common/useEbMutation';
import { ProposalDialog } from './ProposalDialog';

type Acting = { mode: 'record' } | { mode: 'validate' | 'reject'; proposal: Proposal };

function columns(
  mayValidate: boolean,
  onAct: (a: Acting) => void,
  onDownload: (p: Proposal) => void,
): Column<Proposal>[] {
  return [
    {
      key: 'no',
      header: 'Proposal',
      kind: 'code',
      render: (p) => <CellStack main={p.proposalNo} sub={p.insurerName} />,
    },
    {
      key: 'kind',
      header: 'Kind',
      render: (p) => <CellStack main={ebLabel(p.kind)} sub={`Version ${String(p.versionNo)}`} />,
    },
    { key: 'received', header: 'Received', kind: 'date', render: (p) => formatDate(p.receivedOn) },
    { key: 'valid', header: 'Valid Until', kind: 'date', render: (p) => formatDate(p.validUntil) },
    {
      key: 'lines',
      header: 'Plans',
      render: (p) => p.lines.map((l) => `${l.benefitLine} ${l.planCode}`).join(', '),
    },
    {
      key: 'premium',
      header: 'Annual Premium',
      kind: 'amount',
      render: (p) => <Amount value={p.totalPremium} />,
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (p) => (
        <CellStack
          main={
            <StatusBadge
              status={p.status}
              tone={p.status === 'VALIDATED' ? 'success' : undefined}
            />
          }
          sub={p.rejectReason ?? undefined}
        />
      ),
    },
    {
      key: 'actions',
      header: '',
      width: '132px',
      render: (p) => (
        <span className="eb-actions">
          <Button
            variant="ghost"
            size="sm"
            aria-label={`Download ${p.proposalNo}`}
            icon={<Download size={14} />}
            onClick={() => onDownload(p)}
          />
          {mayValidate && p.status === 'SUBMITTED' && (
            <>
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Validate ${p.proposalNo}`}
                icon={<CheckCircle2 size={14} />}
                onClick={() => onAct({ mode: 'validate', proposal: p })}
              />
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Reject ${p.proposalNo}`}
                icon={<XCircle size={14} />}
                onClick={() => onAct({ mode: 'reject', proposal: p })}
              />
            </>
          )}
        </span>
      ),
    },
  ];
}

/** Validate or reject a proposal. */
function DecisionDialog({
  acting,
  onClose,
}: Readonly<{ acting: { mode: 'validate' | 'reject'; proposal: Proposal }; onClose: () => void }>) {
  const validate = useEbMutation(
    (c, id: number) => ebMarketApi.validateProposal(c, id),
    (p: Proposal) => `Proposal ${p.proposalNo} validated`,
    onClose,
  );
  const reject = useEbMutation(
    (c, v: { id: number; reason: string }) => ebMarketApi.rejectProposal(c, v.id, v.reason),
    (p: Proposal) => `Proposal ${p.proposalNo} rejected`,
    onClose,
  );
  const record = `${acting.proposal.proposalNo} – ${acting.proposal.insurerName}`;
  if (acting.mode === 'validate') {
    return (
      <ConfirmDialog
        title="Validate Proposal"
        record={record}
        effect="The proposal enters the comparative of the cycle."
        confirmLabel="Validate"
        busy={validate.isPending}
        error={validate.error}
        onConfirm={() => validate.mutate(acting.proposal.id)}
        onClose={onClose}
      />
    );
  }
  return (
    <ConfirmDialog
      title="Reject Proposal"
      record={record}
      effect="The proposal stays on file and is left out of the comparative."
      confirmLabel="Reject"
      reason="required"
      destructive
      busy={reject.isPending}
      error={reject.error}
      onConfirm={(reason) => reject.mutate({ id: acting.proposal.id, reason })}
      onClose={onClose}
    />
  );
}

/**
 * Proposals tab: the incumbent's indicative terms, the proposals and the revised proposals of the
 * current cycle with their premiums; Record Proposal, then validate or reject each one. Only
 * validated proposals enter the comparative.
 */
export function ProposalsTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const { can } = useAuth();
  const download = useFileDownload();
  const cycleId = programme.currentCycleId ?? undefined;
  const [acting, setActing] = useState<Acting>();
  const close = () => setActing(undefined);
  const proposals = useCycleList('proposals', cycleId, ebMarketApi.proposals);
  const tors = useCycleList<Tor>('tor', cycleId, ebMarketApi.tors);
  const market = can('EB_MARKET') && cycleId !== undefined;
  const released = tors.rows.find((t) => t.status === 'RELEASED');
  return (
    <Card
      title="Proposals"
      actions={
        <ActionButton
          shown={market}
          icon={<FilePlus2 size={14} />}
          label="Record Proposal"
          onClick={() => setActing({ mode: 'record' })}
        />
      }
    >
      <ErrorAlert
        error={proposals.error ?? download.error}
        onRetry={() => void proposals.refetch()}
      />
      <DataTable<Proposal>
        loading={proposals.isLoading}
        rows={proposals.rows}
        rowKey={(p) => p.id}
        columns={columns(market, setActing, (p) =>
          download.mutate(() => attachmentsApi.download(p.attachmentId)),
        )}
        emptyMessage="No proposal recorded"
      />
      {acting?.mode === 'record' && cycleId !== undefined && (
        <ProposalDialog cycleId={cycleId} items={released?.items ?? []} onClose={close} />
      )}
      {acting !== undefined && acting.mode !== 'record' && (
        <DecisionDialog acting={acting} onClose={close} />
      )}
    </Card>
  );
}
