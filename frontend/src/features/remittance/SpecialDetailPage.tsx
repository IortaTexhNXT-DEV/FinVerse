import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, ClipboardCheck, FileText, Layers, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { remittanceApi } from './api';
import type { Special } from './api';
import { joinParts } from './remittanceLabels';
import './remittance.css';

const ENTITY = 'SpecialRemittance';

function Summary({ special }: Readonly<{ special: Special }>) {
  return (
    <RecordSummary
      title={special.assuredName}
      chips={
        <>
          <ReferenceChip label="Request" value={special.requestNo} />
          <StatusBadge status={special.stage} />
        </>
      }
      flags={<span className="tag">{special.conditionCode}</span>}
      facts={[
        {
          icon: FileText,
          label: 'Invoice',
          value: <Link to={remittanceApi.invoiceLink(special.invoiceNo)}>{special.invoiceNo}</Link>,
        },
        {
          icon: Building2,
          label: 'Insurer / Segment',
          value: `${special.insurerCode} / ${special.segment ?? '—'}`,
        },
        {
          icon: UserRound,
          label: 'Requested / Approved By',
          value: `${special.requestedBy} / ${special.approvedBy ?? '—'}`,
        },
        { icon: ClipboardCheck, label: 'Validation', value: special.validationNote ?? '—' },
        {
          icon: Layers,
          label: 'Special Batch',
          value: special.batchNo ?? 'Created on approval',
        },
      ]}
    />
  );
}

/**
 * Special remittance request (MKTID.009, RMTID.030/033): the invoice, condition and validation, the
 * OPS_SPECIAL_REMIT workflow (approve into a special batch or reject with a reason) and its
 * history, and the push to Disbursement.
 */
export default function SpecialDetailPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<WorkAction>();
  const special = useQuery({
    queryKey: ['remittance', 'special', id],
    queryFn: () => remittanceApi.special(id),
  });
  const act = useMutation({
    mutationFn: (v: { approve: boolean; reason?: string; comment?: string }) =>
      v.approve
        ? remittanceApi.approveSpecial(id, v.comment)
        : remittanceApi.rejectSpecial(id, v.reason ?? '', v.comment),
    onSuccess: async (s) => {
      setPending(undefined);
      queryClient.setQueryData(['remittance', 'special', id], s);
      await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
      await queryClient.invalidateQueries({ queryKey: ['remittance', 'specials'] });
      toast.success(`${s.requestNo}: ${s.stage.toLowerCase().replaceAll('_', ' ')}`);
    },
  });
  if (special.data === undefined) {
    return special.error ? (
      <ErrorAlert error={special.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const s = special.data;
  return (
    <div className="stack">
      <PageHeader
        section="Remittance · Special Remittance"
        backTo="/remittance/special"
        title={s.requestNo}
        description={joinParts([
          s.invoiceNo,
          `requested ${formatDateTime(s.createdAt)}`,
          s.remarks,
        ])}
      />
      <Summary special={s} />
      {s.rejectedReason !== undefined && <div className="alert">Rejected: {s.rejectedReason}</div>}
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        renderBusinessActions={(actions) =>
          actions
            .filter((a) => a.action === 'approve' || a.action === 'reject')
            .map((a) => (
              <Button
                key={a.action}
                size="sm"
                variant={a.action === 'approve' ? 'primary' : 'secondary'}
                onClick={() => setPending(a)}
              >
                {a.label}
              </Button>
            ))
        }
      />
      {s.batchNo !== undefined && (
        <p>
          Remitted through special batch <strong>{s.batchNo}</strong>; see{' '}
          <Link to="/remittance/batches?tab=REVIEW">Remittance Batches</Link>.
        </p>
      )}
      {pending !== undefined && (
        <ActionDialog
          title={pending.label}
          reasonLov={pending.action === 'reject' ? 'REMIT_RETURN_REASON' : undefined}
          confirmLabel={pending.label}
          busy={act.isPending}
          error={act.error}
          onClose={() => setPending(undefined)}
          onConfirm={(note) =>
            act.mutate({
              approve: pending.action === 'approve',
              reason: note.reasonCode,
              comment: note.comment,
            })
          }
        />
      )}
    </div>
  );
}
