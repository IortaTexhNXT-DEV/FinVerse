import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { proposalsApi } from '@/api/submittedProposals';
import type { ProposalBatch, ProposalView } from '@/api/submittedProposals';
import { useAuth } from '@/auth/authContext';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { InsurerName } from '@/components/broking/LovLabel';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageFooter } from '@/components/ui/Pager';
import { RowActions } from '@/components/ui/RowActions';
import type { RowAction } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';

const KEY = ['submitted', 'proposals'] as const;

function proposalActions(
  p: ProposalView,
  may: boolean,
  on: { release: () => unknown; returnIt: (reason: string) => unknown; pdf: () => unknown },
): RowAction[] {
  const actions: RowAction[] = [{ label: 'Download Proposal', onSelect: on.pdf }];
  if (may && p.status === 'FOR_REVIEW') {
    actions.push({
      label: 'Release',
      onSelect: on.release,
      confirm: {
        title: 'Release proposal',
        effect: 'The proposal is available to the renewal hand-off and the letters.',
      },
    });
  }
  if (may && (p.status === 'FOR_REVIEW' || p.status === 'RELEASED')) {
    actions.push({
      label: 'Return',
      onSelect: on.returnIt,
      danger: true,
      confirm: {
        title: 'Return proposal',
        effect: 'The maker edits the rate and generates the proposal again.',
        reason: 'required',
        destructive: true,
      },
    });
  }
  return actions;
}

function Batch({ batch }: Readonly<{ batch: ProposalBatch }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const download = useFileDownload();
  const [insurer, setInsurer] = useState('');
  const may = can('SBM_PROPOSAL');
  const refresh = async (text: string) => {
    toast.success(text);
    selection.clear();
    await queryClient.invalidateQueries({ queryKey: KEY });
  };
  const assign = useMutation({
    mutationFn: () =>
      proposalsApi.assignInsurer(
        batch.proposals.filter((p) => selection.has(String(p.id))).map((p) => p.id),
        insurer,
      ),
    onSuccess: () => refresh('Insurer assigned and nominated rate applied'),
  });
  const act = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      reason === undefined ? proposalsApi.release(id) : proposalsApi.returnProposal(id, reason),
    onSuccess: (p) => refresh(p.status === 'RELEASED' ? 'Proposal released' : 'Proposal returned'),
  });
  const forReview = (p: ProposalView) => (p.status === 'FOR_REVIEW' ? undefined : 'Not for review');
  return (
    <Card title={`${batch.batchNo} · ${formatDateTime(batch.createdAt)}`} flush>
      <ErrorAlert error={assign.error ?? act.error ?? download.error} />
      {may && (
        <div className="form-grid">
          <Field label="Preferred Insurer">
            {(id) => <InsurerSelect id={id} value={insurer} onChange={setInsurer} />}
          </Field>
          <Button
            variant="secondary"
            disabled={insurer === '' || !batch.proposals.some((p) => selection.has(String(p.id)))}
            busy={assign.isPending}
            onClick={() => assign.mutate()}
          >
            Assign Insurer
          </Button>
        </div>
      )}
      <DataTable<ProposalView>
        rows={batch.proposals}
        rowKey={(p) => p.id}
        columns={[
          selectionColumn(
            batch.proposals,
            (p) => String(p.id),
            selection,
            (p) => p.sbmNo,
            forReview,
          ),
          {
            key: 'sbm',
            header: 'Masterlist No.',
            kind: 'code',
            render: (p) => `${p.sbmNo} v${String(p.versionNo)}`,
          },
          { key: 'assured', header: 'Assured', render: (p) => p.assuredName },
          {
            key: 'insurer',
            header: 'Insurer',
            render: (p) => <InsurerName code={p.insurerCode} />,
          },
          {
            key: 'rate',
            header: 'Rate',
            kind: 'amount',
            render: (p) =>
              p.rateReason
                ? `${String(p.appliedRate)}% (${p.rateReason})`
                : `${String(p.appliedRate)}%`,
          },
          {
            key: 'premium',
            header: 'Premium',
            kind: 'amount',
            render: (p) => formatAmount(p.premium),
          },
          {
            key: 'status',
            header: 'Status',
            kind: 'status',
            render: (p) => <StatusBadge status={p.status} />,
          },
          { key: 'reason', header: 'Return Reason', render: (p) => p.returnReason ?? '' },
          {
            key: 'act',
            header: '',
            render: (p) => (
              <RowActions
                record={p.sbmNo}
                actions={proposalActions(p, may, {
                  release: () => act.mutateAsync({ id: p.id }),
                  returnIt: (reason) => act.mutateAsync({ id: p.id, reason }),
                  pdf: () => download.mutate(() => proposalsApi.document(p.id)),
                })}
              />
            ),
          },
        ]}
      />
    </Card>
  );
}

/** Proposal batches with their proposals: release, return, assign insurer, download. */
export function ProposalBatches() {
  const companyId = useCompanyId();
  const [page, setPage] = useState(0);
  const batches = useQuery({
    queryKey: [...KEY, 'batches', companyId, page],
    queryFn: () => proposalsApi.batches(companyId, page),
    enabled: companyId > 0,
  });
  const rows = batches.data?.content ?? [];
  return (
    <div className="stack">
      <ErrorAlert error={batches.error} />
      {!batches.isLoading && rows.length === 0 && <p className="muted">No proposal batch yet</p>}
      {rows.map((b) => (
        <Batch key={b.id} batch={b} />
      ))}
      <PageFooter data={batches.data} noun="batches" onPage={setPage} />
    </div>
  );
}
