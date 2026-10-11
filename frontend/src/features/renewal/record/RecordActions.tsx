import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { renewalReferralsApi } from '@/api/renewalReferrals';
import type { CandidateDetail } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { TextDialog } from '../common/ActionDialogs';
import { ReferralRequestDialog } from '../referrals/ReferralDialogs';
import { SubmitPostingDialog } from '../referrals/SubmitPostingDialog';
import { DispositionDialog, ResponseDialog } from './RecordDialogs';
import { AcceptanceDialog, FollowupDialog, PackageDialog } from './ClientDialogs';

type DialogKind =
  | 'dispose'
  | 'push'
  | 'referral'
  | 'response'
  | 'accept'
  | 'followup'
  | 'package'
  | 'remark'
  | 'reopen';

type Can = (permission: string) => boolean;

const FINISHED = new Set(['RENEWED', 'CLOSED']);

interface ActionDef {
  key: string;
  label: string;
  variant: 'primary' | 'secondary' | 'ghost';
  /** A dialog to open, or a direct call. */
  dialog?: DialogKind;
  run?: () => Promise<string>;
}

function isDisposing(d: CandidateDetail, can: Can): boolean {
  return d.row.stage === 'FOR_DISPOSITION' && (can('RNW_DISPOSE') || can('RNW_REVIEW'));
}

function isNbStart(d: CandidateDetail, can: Can): boolean {
  const links = d.lifecycle.links;
  return (
    d.row.stage === 'NB_PATH' &&
    (can('RNW_DISPOSE') || can('RNW_REVIEW')) &&
    links.quotationRef === null &&
    links.proposalRef === null
  );
}

function isReopenable(d: CandidateDetail, can: Can): boolean {
  const stage = d.row.stage;
  const closedNotRenewed = stage === 'CLOSED' && d.lifecycle.links.closedAs === 'NOT_RENEWED';
  return (
    (stage === 'LETTER_PENDING' || closedNotRenewed) &&
    (can('RNW_DISPOSE') || can('RNW_PROCESS_ASSIGN'))
  );
}

function isAccountDue(d: CandidateDetail, can: Can): boolean {
  const stage = d.row.stage;
  return (
    (stage === 'FOR_PROCESSING' || stage === 'IN_PROCESSING') &&
    d.lifecycle.links.renewalArn === null &&
    can('RNW_PROCESS')
  );
}

function isPackageOpen(d: CandidateDetail, can: Can): boolean {
  return (
    !FINISHED.has(d.row.stage) &&
    d.lifecycle.legacyPackage.legacyCode !== null &&
    can('RNW_PACKAGE_REMAP')
  );
}

const ACTIONS: (ActionDef & { when: (d: CandidateDetail, can: Can) => boolean })[] = [
  {
    key: 'dispose',
    label: 'Set Disposition',
    variant: 'primary',
    dialog: 'dispose',
    when: isDisposing,
  },
  {
    key: 'push',
    label: 'Push',
    variant: 'secondary',
    dialog: 'push',
    when: (d, can) => isDisposing(d, can) && d.row.disposition !== null,
  },
  {
    key: 'referral',
    label: 'Transfer to Other Unit',
    variant: 'ghost',
    dialog: 'referral',
    when: (d, can) => !FINISHED.has(d.row.stage) && (can('RNW_DISPOSE') || can('RNW_ASSIGN')),
  },
  { key: 'nb', label: 'Start Quotation / Proposal', variant: 'primary', when: isNbStart },
  { key: 'account', label: 'Create Renewal Account', variant: 'primary', when: isAccountDue },
  {
    key: 'response',
    label: 'Record Insurer Response',
    variant: 'primary',
    dialog: 'response',
    when: (d, can) => d.row.stage === 'WITH_INSURER' && can('RNW_INSURER'),
  },
  {
    key: 'accept',
    label: 'Record Acceptance',
    variant: 'primary',
    dialog: 'accept',
    when: (d, can) => d.row.stage === 'RA_SENT' && can('RNW_ACCEPT'),
  },
  {
    key: 'followup',
    label: 'Add Follow-up',
    variant: 'secondary',
    dialog: 'followup',
    when: (d, can) => !FINISHED.has(d.row.stage) && can('RNW_FOLLOWUP'),
  },
  {
    key: 'remark',
    label: 'Add Remark',
    variant: 'ghost',
    dialog: 'remark',
    when: (_d, can) => ['RNW_DISPOSE', 'RNW_REVIEW', 'RNW_PROCESS', 'RNW_FOLLOWUP'].some(can),
  },
  {
    key: 'package',
    label: 'Propose Package',
    variant: 'ghost',
    dialog: 'package',
    when: isPackageOpen,
  },
  { key: 'reopen', label: 'Re-open', variant: 'ghost', dialog: 'reopen', when: isReopenable },
];

/** The direct calls of the record actions, with their confirmation text. */
function directCalls(companyId: number, ref: string): Record<string, () => Promise<string>> {
  return {
    nb: async () => (await renewalApi.startNbPath(companyId, ref)).reference + ' started',
    account: async () =>
      `Renewal account ${(await renewalApi.createAccount(companyId, ref)).arn} created`,
  };
}

function RecordDialog({
  kind,
  detail,
  busy,
  error,
  onClose,
  onRun,
}: Readonly<{
  kind: DialogKind;
  detail: CandidateDetail;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onRun: (call: () => Promise<unknown>) => void;
}>) {
  const companyId = useCompanyId();
  const ref = detail.row.renewalRef;
  const common = { busy, error, onClose };
  switch (kind) {
    case 'dispose':
      return (
        <DispositionDialog
          {...common}
          initial={detail.row.disposition}
          onConfirm={(input) => onRun(() => renewalApi.dispose(companyId, ref, input))}
        />
      );
    case 'push':
      return (
        <SubmitPostingDialog
          {...common}
          refs={[ref]}
          onConfirm={(approver) =>
            onRun(async () => {
              await renewalApi.push(companyId, [ref], approver);
              return 'Submitted for posting';
            })
          }
        />
      );
    case 'referral':
      return (
        <ReferralRequestDialog
          {...common}
          renewalRef={ref}
          onConfirm={(toUnit, justification, submit) =>
            onRun(async () => {
              const r = await renewalReferralsApi.request(companyId, {
                renewalRef: ref,
                toUnit,
                justification,
                submit,
              });
              return `Transfer request ${r.referralNo}: ${r.statusLabel}`;
            })
          }
        />
      );
    case 'response':
      return (
        <ResponseDialog
          {...common}
          onConfirm={(input) => onRun(() => renewalApi.recordResponse(companyId, ref, input))}
        />
      );
    case 'accept':
      return (
        <AcceptanceDialog
          {...common}
          entityId={detail.lifecycle.id}
          onConfirm={(input) => onRun(() => renewalApi.accept(companyId, ref, input))}
        />
      );
    case 'followup':
      return (
        <FollowupDialog
          {...common}
          onConfirm={(input) => onRun(() => renewalApi.addFollowup(companyId, ref, input))}
        />
      );
    case 'package':
      return (
        <PackageDialog
          {...common}
          onConfirm={(input) => onRun(() => renewalApi.proposePackage(companyId, ref, input))}
        />
      );
    case 'remark':
      return (
        <TextDialog
          {...common}
          title="Add Remark"
          label="Remark"
          confirmLabel="Save"
          onConfirm={(text) => onRun(() => renewalApi.addRemark(companyId, ref, text))}
        />
      );
    default:
      return (
        <TextDialog
          {...common}
          title="Re-open the renewal"
          label="Reason"
          confirmLabel="Re-open"
          onConfirm={(text) => onRun(() => renewalApi.reopen(companyId, ref, text))}
        />
      );
  }
}

/** Business actions of the renewal record the user may take now, and their dialogs. */
export function RecordActions({ detail }: Readonly<{ detail: CandidateDetail }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState<DialogKind>();
  const act = useMutation({
    mutationFn: (call: () => Promise<unknown>) => call(),
    onSuccess: async (result) => {
      setOpen(undefined);
      toast.success(typeof result === 'string' ? result : 'Saved');
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const calls = directCalls(companyId, detail.row.renewalRef);
  const actions = ACTIONS.filter((a) => a.when(detail, can));
  const arn = detail.lifecycle.links.renewalArn;
  return (
    <>
      {actions.map((a) => (
        <Button
          key={a.key}
          variant={a.variant}
          busy={a.dialog === undefined && act.isPending}
          onClick={() => {
            act.reset();
            if (a.dialog !== undefined) {
              setOpen(a.dialog);
            } else {
              act.mutate(calls[a.key] ?? (() => Promise.resolve('')));
            }
          }}
        >
          {a.label}
        </Button>
      ))}
      {arn !== null && (
        <Button
          variant="ghost"
          onClick={() => void navigate(`/accounts/by-arn/${encodeURIComponent(arn)}`)}
        >
          Open Renewal Account
        </Button>
      )}
      {open !== undefined && (
        <RecordDialog
          kind={open}
          detail={detail}
          busy={act.isPending}
          error={act.error}
          onClose={() => setOpen(undefined)}
          onRun={(call) => act.mutate(call)}
        />
      )}
    </>
  );
}
