import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { acslApi, CASE_ENTITY } from './api';
import type { AcslCase, CaseOutcome } from './api';
import { FormDialog } from './FormDialog';
import type { DialogField } from './FormDialog';

type Dialog = 'assign' | 'findings' | 'result' | 'correction' | 'reversal' | 'message';

const KINDS = [
  { value: 'WRONG_ACCOUNT', label: 'Posting to a wrong GL account' },
  { value: 'AMOUNT', label: 'Wrong amount' },
  { value: 'RECLASS', label: 'Reclassification' },
  { value: 'OTHER', label: 'Other correction' },
];

const OUTCOMES = [
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'NO_ACTION', label: 'No action needed' },
];

const DIALOGS: Record<
  Dialog,
  { title: string; confirm: string; fields: (c: AcslCase) => DialogField[] }
> = {
  assign: {
    title: 'Assign the Case',
    confirm: 'Assign',
    fields: () => [{ key: 'username', label: 'Processor (User ID)', required: true }],
  },
  findings: {
    title: 'Record Findings',
    confirm: 'Save Findings',
    fields: (c) => [
      { key: 'findings', label: 'Findings', required: true, multiline: true, initial: c.findings },
    ],
  },
  result: {
    title: 'Provide the Result',
    confirm: 'Provide Result',
    fields: () => [
      { key: 'outcome', label: 'Result', required: true, options: OUTCOMES },
      { key: 'remarks', label: 'Remarks', multiline: true },
    ],
  },
  correction: {
    title: 'Raise a Correction Entry',
    confirm: 'Raise Correction',
    fields: () => [
      { key: 'kind', label: 'Correction Kind', required: true, options: KINDS },
      { key: 'originalBatchNo', label: 'Journal to Correct', hint: 'Journal batch no., if known' },
      { key: 'description', label: 'Description', required: true },
    ],
  },
  reversal: {
    title: 'Request a Payment Reversal',
    confirm: 'Request Reversal',
    fields: (c) => [
      { key: 'receiptNo', label: 'AR / OR No.', required: true, initial: c.account.arNo },
      { key: 'amount', label: 'Amount', hint: 'Leave empty to reverse the whole application' },
      { key: 'reason', label: 'Reason', required: true },
    ],
  },
  message: {
    title: 'Message the Account Officer',
    confirm: 'Send Message',
    fields: () => [{ key: 'message', label: 'Message', required: true, multiline: true }],
  },
};

const LABELS: Record<Dialog, string> = {
  assign: 'Assign',
  findings: 'Record Findings',
  result: 'Provide Result',
  correction: 'Raise Correction',
  reversal: 'Request Payment Reversal',
  message: 'Message AO',
};

function offered(c: AcslCase, actions: WorkAction[], can: (p: string) => boolean): Dialog[] {
  const names = new Set(actions.map((a) => a.action));
  const open = c.stage === 'ASSIGNED' || c.stage === 'INVESTIGATING';
  const list: Dialog[] = [];
  if (names.has('assign') || (open && can('ACSL_ASSIGN'))) {
    list.push('assign');
  }
  if (c.stage === 'INVESTIGATING' && can('ACSL_PROCESS')) {
    list.push('findings', 'message');
  }
  if (open && can('ACSL_APPLY') && c.account.invoiceNo) {
    list.push('reversal');
  }
  if (names.has('raise_correction')) {
    list.push('correction');
  }
  if (names.has('provide_result')) {
    list.push('result');
  }
  return list;
}

function run(c: AcslCase, dialog: Dialog, v: Record<string, string>): Promise<unknown> {
  const text = (k: string) => (v[k] ?? '').trim();
  switch (dialog) {
    case 'assign':
      return acslApi.assignCase(c.id, text('username'));
    case 'findings':
      return acslApi.findings(c.id, text('findings'));
    case 'result':
      return acslApi.result(c.id, text('outcome') as CaseOutcome, text('remarks') || undefined);
    case 'correction':
      return acslApi.raiseCorrection(c.id, {
        kind: text('kind'),
        originalBatchNo: text('originalBatchNo') || undefined,
        description: text('description'),
      });
    case 'reversal':
      return acslApi.reversal(c.id, {
        receiptNo: text('receiptNo'),
        amount: text('amount') === '' ? undefined : Number(text('amount')),
        reason: text('reason'),
      });
    default:
      return acslApi.messageAo(c.id, text('message'));
  }
}

/**
 * Actions of an ACSL case (ACSL 2.5.0-2.6.2, 2.9.0): assignment by the team leader, findings, the
 * result to the requester, the correction entry, the payment reversal request to Cashiering and
 * the message to the Account Officer.
 */
export function CaseActions({ c, actions }: Readonly<{ c: AcslCase; actions: WorkAction[] }>) {
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialog | null>(null);
  const act = useMutation({
    mutationFn: ({ d, values }: { d: Dialog; values: Record<string, string> }) => run(c, d, values),
    onSuccess: async (result, { d }) => {
      setDialog(null);
      await queryClient.invalidateQueries({ queryKey: ['acsl'] });
      await queryClient.invalidateQueries({ queryKey: workflowKey(CASE_ENTITY, c.id) });
      toast.success(`${LABELS[d]}: done`);
      if (d === 'correction' && result && typeof result === 'object' && 'id' in result) {
        void navigate(`/acsl/corrections/${String((result as { id: number }).id)}`);
      }
    },
  });
  return (
    <>
      {offered(c, actions, can).map((d) => (
        <Button
          key={d}
          variant={d === 'result' ? 'accent' : 'secondary'}
          onClick={() => setDialog(d)}
        >
          {LABELS[d]}
        </Button>
      ))}
      {dialog && (
        <FormDialog
          title={`${DIALOGS[dialog].title} · ${c.caseNo}`}
          confirmLabel={DIALOGS[dialog].confirm}
          fields={DIALOGS[dialog].fields(c)}
          busy={act.isPending}
          error={act.error}
          onConfirm={(values) => act.mutate({ d: dialog, values })}
          onClose={() => setDialog(null)}
        />
      )}
    </>
  );
}
