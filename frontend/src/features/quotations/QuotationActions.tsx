import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { quotationsApi, QUOTATION_ENTITY } from '@/api/quotations';
import type { Quotation } from '@/api/quotations';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { SendEmailDialog } from '@/components/broking/SendEmailDialog';
import type { EmailDraft } from '@/components/broking/SendEmailDialog';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, humanize } from '@/utils/format';
import { AcceptanceDialog } from './AcceptanceDialog';

type Simple = 'submit' | 'approve' | 'revise' | 'create_accounts';

const SIMPLE: Record<Simple, (id: number, comment?: string) => Promise<Quotation>> = {
  submit: quotationsApi.submit,
  approve: quotationsApi.approve,
  revise: quotationsApi.revise,
  create_accounts: quotationsApi.createAccounts,
};

const HANDLED = new Set<string>([...Object.keys(SIMPLE), 'send', 'accept']);

/** The e-mail proposed when a quotation is sent (the user may change it). */
function emailOf(q: Quotation) {
  return {
    to: q.clientEmail ?? '',
    subject: `Insurance quotation ${q.quotationNo} – ${q.productCode}`,
    body: `Dear ${q.clientName},\n\nPlease find attached our quotation ${q.quotationNo} (ARN ${q.arn}), valid until ${formatDate(q.content.validUntil)}. The documents are password protected; the password follows in a separate e-mail.\n\nTo proceed, reply to this e-mail confirming your acceptance.\n\nBDO Insurance and Reinsurance Brokers, Inc.`,
    protect: true,
  };
}

/**
 * Business actions of a quotation offered by the workflow panel (BRNB.014/021/043/045): Submit,
 * Approve (four eyes), Revise, Send via Email (protected, BRNB.013), Record Acceptance (with the
 * client's e-mail) and Create Accounts.
 */
export function QuotationActions({
  quotation,
  actions,
}: Readonly<{ quotation: Quotation; actions: WorkAction[] }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<WorkAction | null>(null);
  const refresh = async (q: Quotation, label: string) => {
    setPending(null);
    queryClient.setQueryData(['quotation', q.id], q);
    await queryClient.invalidateQueries({ queryKey: ['quotations'] });
    await queryClient.invalidateQueries({ queryKey: ['quotation', q.id] });
    await queryClient.invalidateQueries({ queryKey: workflowKey(QUOTATION_ENTITY, q.id) });
    toast.success(`${label}: ${q.quotationNo} is now ${humanize(q.status).toLowerCase()}`);
  };
  const simple = useMutation({
    mutationFn: ({ action, note }: { action: WorkAction; note: ActionNote }) =>
      SIMPLE[action.action as Simple](quotation.id, note.comment),
    onSuccess: (q, { action }) => refresh(q, action.label),
  });
  const send = useMutation({
    mutationFn: (d: EmailDraft) =>
      quotationsApi.send(quotation.id, {
        to: d.to,
        cc: d.cc,
        subject: d.subject,
        body: d.body,
        passwordHint: d.passwordHint,
      }),
    onSuccess: (q) => refresh(q, 'Sent'),
  });
  const accept = useMutation({
    mutationFn: async ({
      file,
      groups,
      comment,
    }: {
      file?: File;
      groups: number[];
      comment?: string;
    }) => {
      if (file !== undefined) {
        await attachmentsApi.uploadMany(QUOTATION_ENTITY, String(quotation.id), [file], {
          documentType: 'CLIENT_ACCEPTANCE',
        });
      }
      return quotationsApi.accept(quotation.id, groups, comment);
    },
    onSuccess: (q) => refresh(q, 'Acceptance recorded'),
  });
  const offered = actions.filter((a) => HANDLED.has(a.action));
  return (
    <>
      {offered.map((a) => (
        <Button key={a.action} size="sm" variant="accent" onClick={() => setPending(a)}>
          {a.action === 'send' ? 'Send via Email' : a.label}
        </Button>
      ))}
      {pending?.action === 'send' && (
        <SendEmailDialog
          title={`Send ${quotation.quotationNo} to the client`}
          initial={emailOf(quotation)}
          attachmentNames={[
            `${quotation.quotationNo}_v${quotation.currentVersion}.pdf`,
            `${quotation.quotationNo}_v${quotation.currentVersion}.xlsx`,
          ]}
          busy={send.isPending}
          error={send.error}
          onClose={() => setPending(null)}
          onSend={(d) => send.mutate(d)}
        />
      )}
      {pending?.action === 'accept' && (
        <AcceptanceDialog
          reference={quotation.quotationNo}
          groups={quotation.content.groups}
          busy={accept.isPending}
          error={accept.error}
          onClose={() => setPending(null)}
          onAccept={(v) => accept.mutate(v)}
        />
      )}
      {pending !== null && pending.action in SIMPLE && (
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
