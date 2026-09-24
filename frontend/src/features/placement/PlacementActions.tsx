import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { placementApi } from '@/api/placement';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { GenerateSlipsDialog } from './GenerateSlipsDialog';

type Handler = (arn: string, note: ActionNote) => Promise<unknown>;

/** Placement actions of the NB_ACCOUNT workflow run from this page, with their dialog. */
const HANDLED: Record<string, { label: string; reasonLov?: string; run: Handler }> = {
  insurer_return: {
    label: 'Record Insurer Return',
    reasonLov: 'RETURN_REASON',
    run: (arn, n) => placementApi.insurerReturn(arn, n.reasonCode ?? '', n.comment),
  },
  resubmit: {
    label: 'Resubmit for Placement',
    run: (arn, n) => placementApi.resubmit(arn, n.comment),
  },
  cancel_placement: {
    label: 'Cancel Placement',
    reasonLov: 'CANCELLATION_REASON',
    run: (arn, n) => placementApi.cancel([arn], n.reasonCode ?? '', n.comment),
  },
  reactivate: {
    label: 'Reactivate',
    run: (arn, n) => placementApi.reactivate([arn], n.comment),
  },
};

/**
 * Business actions of an account's placement offered by the workflow panel: generate the
 * placement slip (place), record an insurer return (BRNB.034), resubmit a returned placement,
 * cancel (BRNB.062) or reactivate (BRD 2.1.16) the placement.
 */
export function PlacementActions({
  arn,
  status,
  actions,
  onChanged,
}: Readonly<{ arn: string; status: string; actions: WorkAction[]; onChanged: () => void }>) {
  const toast = useToast();
  const companyId = useCompanyId();
  const [pending, setPending] = useState<string | null>(null);
  const [placing, setPlacing] = useState(false);
  const run = useMutation({
    mutationFn: ({ action, note }: { action: string; note: ActionNote }) =>
      HANDLED[action]?.run(arn, note) ?? Promise.reject(new Error('Not available here')),
    onSuccess: (_, { action }) => {
      setPending(null);
      toast.success(`${HANDLED[action]?.label ?? action}: done`);
      onChanged();
    },
  });
  // resubmit is also Marketing's action after a return by Processing (accounts screen)
  const offered = actions.filter(
    (a) => a.action in HANDLED && (a.action !== 'resubmit' || status === 'RETURNED_BY_INSURER'),
  );
  const canPlace = actions.some((a) => a.action === 'place');
  const dialog = pending === null ? undefined : HANDLED[pending];
  return (
    <>
      {canPlace && (
        <Button size="sm" variant="primary" onClick={() => setPlacing(true)}>
          Generate Placement Slip
        </Button>
      )}
      {offered.map((a) => (
        <Button key={a.action} size="sm" variant="secondary" onClick={() => setPending(a.action)}>
          {HANDLED[a.action]?.label}
        </Button>
      ))}
      {pending !== null && dialog !== undefined && (
        <ActionDialog
          title={dialog.label}
          reasonLov={dialog.reasonLov}
          confirmLabel={dialog.label}
          busy={run.isPending}
          error={run.error}
          onClose={() => setPending(null)}
          onConfirm={(note) => run.mutate({ action: pending, note })}
        />
      )}
      {placing && (
        <GenerateSlipsDialog
          companyId={companyId}
          arns={[arn]}
          onClose={() => setPlacing(false)}
          onDone={(slips) => {
            setPlacing(false);
            toast.success(
              `Slip ${slips.map((s) => s.displayNo).join(', ')} generated: send it from the Slips tab`,
            );
            onChanged();
          }}
        />
      )}
    </>
  );
}
