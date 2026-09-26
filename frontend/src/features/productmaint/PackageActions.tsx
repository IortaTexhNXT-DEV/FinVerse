import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { PACKAGE_REQUEST_ENTITY, productMaintApi } from '@/api/productmaint';
import type { PackageRequest } from '@/api/productmaint';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { titleCase } from '@/utils/format';
import { RecommendDialog, ReviseDialog, SetupDialog, TermsFinalDialog } from './ActionDialogs';

type Simple =
  | 'submit'
  | 'approve'
  | 'approve_no_negotiation'
  | 'release_to_marketing'
  | 'skip_marketing_review'
  | 'accept_terms'
  | 'submit_requirements'
  | 'signoff'
  | 'retire';

const SIMPLE: Record<Simple, (p: PackageRequest, text?: string) => Promise<unknown>> = {
  submit: (p, t) => productMaintApi.submit(p.id, t),
  approve: (p, t) =>
    p.status === 'FOR_MKT_APPROVAL'
      ? productMaintApi.approve(p.id, t)
      : productMaintApi.approveTsu(p.id, t),
  approve_no_negotiation: (p, t) => productMaintApi.approveTsu(p.id, t),
  release_to_marketing: (p, t) => productMaintApi.releaseToMarketing(p.id, t),
  skip_marketing_review: (p, t) => productMaintApi.skipMarketingReview(p.id, t),
  accept_terms: (p, t) => productMaintApi.acceptTerms(p.id, t),
  submit_requirements: (p, t) => productMaintApi.submitRequirements(p.id, t),
  signoff: (p, t) => productMaintApi.signoff(p.id, t),
  retire: (p, t) => productMaintApi.retire(p.id, t),
};

const DIALOGS = new Set(['recommend', 'revise_qs', 'terms_final', 'setup', 'return_incomplete']);

/** The business actions this page runs; the catalog outcomes are system actions (never shown). */
function offeredActions(p: PackageRequest, actions: WorkAction[]): WorkAction[] {
  return actions.filter((a) => {
    if (a.action === 'approve' && p.status === 'FOR_TSU_APPROVAL') {
      return p.negotiationRequired;
    }
    if (a.action === 'approve_no_negotiation') {
      return !p.negotiationRequired;
    }
    if (a.action === 'setup') {
      return p.requestType !== 'RETIRE';
    }
    if (a.action === 'retire') {
      return p.requestType === 'RETIRE';
    }
    return a.action in SIMPLE || DIALOGS.has(a.action);
  });
}

/**
 * Business actions of a package request offered by the workflow panel (BRPM.008-016): submit,
 * approvals, TSU recommendation, revise the quotation slip, terms final, release of the terms,
 * Marketing acceptance, requirements, ManCom sign-off, MBS set-up, return or retirement.
 */
export function PackageActions({
  request,
  actions,
}: Readonly<{ request: PackageRequest; actions: WorkAction[] }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<WorkAction | null>(null);
  const refresh = async (label: string) => {
    setPending(null);
    await queryClient.invalidateQueries({ queryKey: ['package-request', request.id] });
    await queryClient.invalidateQueries({ queryKey: ['package-requests'] });
    await queryClient.invalidateQueries({ queryKey: ['package-request-tab', request.id] });
    await queryClient.invalidateQueries({
      queryKey: workflowKey(PACKAGE_REQUEST_ENTITY, request.id),
    });
    toast.success(`${label}: ${request.requestNo}`);
  };
  const simple = useMutation({
    mutationFn: ({ action, note }: { action: WorkAction; note: ActionNote }) =>
      SIMPLE[action.action as Simple](request, note.comment),
    onSuccess: (_, { action }) => refresh(action.label),
  });
  const returned = useMutation({
    mutationFn: (note: ActionNote) =>
      productMaintApi.returnIncomplete(request.id, note.reasonCode ?? '', note.comment),
    onSuccess: () => refresh('Returned to TSU'),
  });
  const close = () => setPending(null);
  const done = (label: string) => void refresh(label);
  const kind = pending?.action;
  return (
    <>
      {offeredActions(request, actions).map((a, i) => (
        <Button
          key={a.action}
          size="sm"
          variant={i === 0 ? 'accent' : 'secondary'}
          onClick={() => setPending(a)}
        >
          {titleCase(a.label)}
        </Button>
      ))}
      {kind === 'recommend' && <RecommendDialog request={request} onClose={close} onDone={done} />}
      {kind === 'revise_qs' && <ReviseDialog request={request} onClose={close} onDone={done} />}
      {kind === 'terms_final' && (
        <TermsFinalDialog request={request} onClose={close} onDone={done} />
      )}
      {kind === 'setup' && <SetupDialog request={request} onClose={close} onDone={done} />}
      {kind === 'return_incomplete' && (
        <ActionDialog
          title="Return Incomplete Requirements"
          reasonLov="RETURN_REASON"
          confirmLabel="Return to TSU"
          busy={returned.isPending}
          error={returned.error}
          onClose={close}
          onConfirm={(note) => returned.mutate(note)}
        />
      )}
      {pending !== null && pending.action in SIMPLE && (
        <ActionDialog
          title={pending.label}
          confirmLabel={pending.label}
          busy={simple.isPending}
          error={simple.error}
          onClose={close}
          onConfirm={(note) => simple.mutate({ action: pending, note })}
        />
      )}
    </>
  );
}
