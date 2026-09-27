import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { PROPOSAL_ENTITY, proposalsApi } from '@/api/proposals';
import type { Proposal } from '@/api/proposals';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { SendEmailDialog } from '@/components/broking/SendEmailDialog';
import type { EmailDraft } from '@/components/broking/SendEmailDialog';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { AcceptanceDialog } from '@/features/quotations/AcceptanceDialog';
import type { AcceptanceInput } from '@/features/quotations/AcceptanceDialog';
import { humanize } from '@/utils/format';
import { SlipDialog } from './SlipDialog';
import type { SlipInput } from './SlipDialog';

type Simple = 'submit' | 'approve' | 'approve_qs' | 'approve_ps' | 'create_accounts';

const SIMPLE: Record<Simple, (id: number, comment?: string) => Promise<Proposal>> = {
  submit: proposalsApi.submit,
  approve: proposalsApi.approve,
  approve_qs: proposalsApi.approveQuotationSlip,
  approve_ps: proposalsApi.approveProposalSlip,
  create_accounts: proposalsApi.createAccounts,
};

const SLIP_ACTIONS = new Set(['submit_qs', 'terms_complete', 'submit_ps']);
const HANDLED = new Set([...Object.keys(SIMPLE), ...SLIP_ACTIONS, 'send_to_client', 'accept']);

function runSlip(p: Proposal, action: string, input: SlipInput): Promise<Proposal> {
  switch (action) {
    case 'submit_qs':
      return proposalsApi.submitQuotationSlip(p.id, input.replyBy, input.comment);
    case 'terms_complete':
      return proposalsApi.termsComplete(p.id, input.closePending, input.comment);
    default:
      return proposalsApi.submitProposalSlip(p.id, input.insurerCode, input.comment);
  }
}

type DialogKind = 'send' | 'accept' | 'slip' | 'simple';

function dialogOf(action: WorkAction | null): DialogKind | null {
  if (action === null) {
    return null;
  }
  if (action.action === 'send_to_client') {
    return 'send';
  }
  if (action.action === 'accept') {
    return 'accept';
  }
  return SLIP_ACTIONS.has(action.action) ? 'slip' : 'simple';
}

function emailOf(p: Proposal) {
  return {
    to: p.clientEmail ?? '',
    subject: `Insurance proposal ${p.slips.psNo ?? p.prfNo} – ${p.productCode}`,
    body: `Dear ${p.clientName},\n\nPlease find attached our proposal slip ${p.slips.psNo ?? ''} and the comparative table of the terms received from the insurers approached (ARN ${p.arn}). The documents are password protected; the password follows in a separate e-mail.\n\nTo proceed, reply to this e-mail confirming your acceptance.\n\nBDO Insurance and Reinsurance Brokers, Inc.`,
    protect: true,
  };
}

/**
 * Business actions of a PRF offered by the workflow panel (BRNB.005-017): submit and Marketing
 * approval, quotation slip submission and approval (sent to the insurers), terms complete,
 * proposal slip submission and approval, send to the client, acceptance and account creation.
 */
export function ProposalActions({
  proposal,
  actions,
}: Readonly<{ proposal: Proposal; actions: WorkAction[] }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<WorkAction | null>(null);
  const refresh = async (p: Proposal, label: string) => {
    setPending(null);
    queryClient.setQueryData(['proposal', p.id], p);
    await queryClient.invalidateQueries({ queryKey: ['proposals'] });
    await queryClient.invalidateQueries({ queryKey: ['proposal', p.id] });
    await queryClient.invalidateQueries({ queryKey: workflowKey(PROPOSAL_ENTITY, p.id) });
    toast.success(`${label}: ${p.prfNo} is now ${humanize(p.status).toLowerCase()}`);
  };
  const simple = useMutation({
    mutationFn: ({ action, note }: { action: WorkAction; note: ActionNote }) =>
      SIMPLE[action.action as Simple](proposal.id, note.comment),
    onSuccess: (p, { action }) => refresh(p, action.label),
  });
  const slip = useMutation({
    mutationFn: ({ action, input }: { action: WorkAction; input: SlipInput }) =>
      runSlip(proposal, action.action, input),
    onSuccess: (p, { action }) => refresh(p, action.label),
  });
  const send = useMutation({
    mutationFn: (d: EmailDraft) =>
      proposalsApi.send(proposal.id, {
        to: d.to,
        cc: d.cc,
        subject: d.subject,
        body: d.body,
        passwordHint: d.passwordHint,
      }),
    onSuccess: (p) => refresh(p, 'Sent'),
  });
  const accept = useMutation({
    mutationFn: async ({ file, groups, comment }: AcceptanceInput) => {
      if (file !== undefined) {
        await attachmentsApi.uploadMany(PROPOSAL_ENTITY, String(proposal.id), [file], {
          documentType: 'CLIENT_ACCEPTANCE',
        });
      }
      return proposalsApi.accept(proposal.id, groups, comment);
    },
    onSuccess: (p) => refresh(p, 'Acceptance recorded'),
  });
  const offered = actions.filter((a) => HANDLED.has(a.action));
  const dialog = dialogOf(pending);
  const slipNo = proposal.slips.psNo ?? 'PS';
  return (
    <>
      {offered.map((a) => (
        <Button key={a.action} size="sm" variant="accent" onClick={() => setPending(a)}>
          {a.action === 'send_to_client' ? 'Send via Email' : a.label}
        </Button>
      ))}
      {dialog === 'send' && (
        <SendEmailDialog
          title={`Send ${slipNo} to the client`}
          initial={emailOf(proposal)}
          attachmentNames={[
            `${slipNo}_v${proposal.slips.psVersion}.pdf`,
            `${proposal.prfNo}_comparative.pdf`,
          ]}
          busy={send.isPending}
          error={send.error}
          onClose={() => setPending(null)}
          onSend={(d) => send.mutate(d)}
        />
      )}
      {dialog === 'accept' && (
        <AcceptanceDialog
          reference={proposal.prfNo}
          groups={proposal.groups}
          busy={accept.isPending}
          error={accept.error}
          onClose={() => setPending(null)}
          onAccept={(v) => accept.mutate(v)}
        />
      )}
      {dialog === 'slip' && pending !== null && (
        <SlipDialog
          action={pending}
          proposalId={proposal.id}
          busy={slip.isPending}
          error={slip.error}
          onClose={() => setPending(null)}
          onConfirm={(input) => slip.mutate({ action: pending, input })}
        />
      )}
      {dialog === 'simple' && pending !== null && (
        <ActionDialog
          title={pending.label}
          confirmLabel={pending.label}
          busy={simple.isPending}
          error={simple.error}
          onClose={() => setPending(null)}
          onConfirm={(note) => simple.mutate({ action: pending, note })}
        />
      )}
    </>
  );
}
