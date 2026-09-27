import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { Approval, IaafView, TorView } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import type { ActionNote } from '@/api/workflow';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { SBM_LOV } from '../common/submittedCodes';

type Kind = 'return' | 'cancel';
type Step = 'submit' | 'approve' | 'issue';

const OPEN = ['DRAFT', 'RETURNED', 'FOR_APPROVAL'];

/** What the signed-in user may do on a document now. */
function allowed(approval: Approval, prepare: boolean, approve: boolean, me: string | undefined) {
  const status = approval.status;
  return {
    submit: prepare && (status === 'DRAFT' || status === 'RETURNED'),
    approve: approve && status === 'FOR_APPROVAL' && me !== approval.preparedBy,
    cancel: prepare && OPEN.includes(status),
  };
}

function BackDialog({
  kind,
  noun,
  record,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<{
  kind: Kind;
  noun: string;
  record: string;
  busy: boolean;
  error: unknown;
  onConfirm: (note: ActionNote) => void;
  onClose: () => void;
}>) {
  const back = kind === 'return';
  return (
    <ActionDialog
      title={back ? `Return ${noun}` : `Cancel ${noun}`}
      record={record}
      effect={back ? `The ${noun} goes back to its preparer.` : `The ${noun} is cancelled.`}
      reasonLov={SBM_LOV.returnReason}
      confirmLabel={back ? 'Return' : `Cancel ${noun}`}
      busy={busy}
      error={error}
      onConfirm={onConfirm}
      onClose={onClose}
    />
  );
}

/** The actions of an IAAF by status: Submit, Approve, Return, Send and Cancel. */
export function IaafActions({ iaaf, onDone }: Readonly<{ iaaf: IaafView; onDone: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const [dialog, setDialog] = useState<Kind | null>(null);
  const may = allowed(iaaf.approval, can('IAAF_PREPARE'), can('IAAF_APPROVE'), user?.username);
  const step = useMutation({
    mutationFn: (action: Step) => submittedApi.iaafAction(iaaf.id, action),
    onSuccess: (i) => {
      toast.success(`IAAF ${i.iaafNo} updated`);
      onDone();
    },
  });
  const back = useMutation({
    mutationFn: ({ kind, note }: { kind: Kind; note: ActionNote }) =>
      submittedApi.iaafReturn(iaaf.id, kind, note.reasonCode ?? '', note.comment),
    onSuccess: () => {
      setDialog(null);
      onDone();
    },
  });
  const send = can('IAAF_PREPARE') && iaaf.approval.status === 'APPROVED';
  return (
    <span className="form-actions">
      <ErrorAlert error={step.error} />
      {may.submit && <Button onClick={() => step.mutate('submit')}>Submit</Button>}
      {may.approve && (
        <ConfirmButton
          confirm={{
            title: `Approve IAAF ${iaaf.iaafNo}`,
            record: iaaf.assuredName,
            effect: `Level ${String(iaaf.approval.currentLevel)} is signed; the IAAF moves to the next level or is approved.`,
          }}
          onConfirm={() => step.mutateAsync('approve')}
        >
          Approve
        </ConfirmButton>
      )}
      {may.approve && (
        <Button variant="secondary" onClick={() => setDialog('return')}>
          Return
        </Button>
      )}
      {send && (
        <ConfirmButton
          confirm={{
            title: `Send IAAF ${iaaf.iaafNo}`,
            record: iaaf.assuredName,
            effect: 'The signed IAAF is e-mailed to the bank counterpart and the IAAF is issued.',
          }}
          onConfirm={() => step.mutateAsync('issue')}
        >
          Send to Bank
        </ConfirmButton>
      )}
      {may.cancel && (
        <Button variant="secondary" onClick={() => setDialog('cancel')}>
          Cancel IAAF
        </Button>
      )}
      {dialog !== null && (
        <BackDialog
          kind={dialog}
          noun="IAAF"
          record={`${iaaf.iaafNo} · ${iaaf.assuredName}`}
          busy={back.isPending}
          error={back.error}
          onConfirm={(note) => back.mutate({ kind: dialog, note })}
          onClose={() => setDialog(null)}
        />
      )}
    </span>
  );
}

/** The actions of a TOR by status: Submit, Approve, Return, Cancel and Download. */
export function TorActions({ tor, onDone }: Readonly<{ tor: TorView; onDone: () => void }>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const download = useFileDownload();
  const [dialog, setDialog] = useState<Kind | null>(null);
  const may = allowed(tor.approval, can('TOR_PREPARE'), can('TOR_APPROVE'), user?.username);
  const step = useMutation({
    mutationFn: (action: 'submit' | 'approve') => submittedApi.torAction(tor.id, action),
    onSuccess: (t) => {
      toast.success(`TOR ${t.torNo} updated`);
      onDone();
    },
  });
  const back = useMutation({
    mutationFn: ({ kind, note }: { kind: Kind; note: ActionNote }) =>
      submittedApi.torReturn(tor.id, kind, note.reasonCode ?? '', note.comment),
    onSuccess: () => {
      setDialog(null);
      onDone();
    },
  });
  const signed = ['APPROVED', 'RELEASED'].includes(tor.approval.status);
  return (
    <span className="form-actions">
      <ErrorAlert error={step.error ?? download.error} />
      {may.submit && <Button onClick={() => step.mutate('submit')}>Submit</Button>}
      {may.approve && (
        <ConfirmButton
          confirm={{
            title: `Approve TOR ${tor.torNo}`,
            record: tor.assuredName ?? undefined,
            effect:
              'The level is signed; an approved TOR is released to the Account Officer on download.',
          }}
          onConfirm={() => step.mutateAsync('approve')}
        >
          Approve
        </ConfirmButton>
      )}
      {may.approve && (
        <Button variant="secondary" onClick={() => setDialog('return')}>
          Return
        </Button>
      )}
      {signed && (
        <Button
          variant="secondary"
          onClick={() => download.mutate(() => submittedApi.torPdf(tor.id))}
        >
          Download TOR
        </Button>
      )}
      {may.cancel && (
        <Button variant="secondary" onClick={() => setDialog('cancel')}>
          Cancel TOR
        </Button>
      )}
      {dialog !== null && (
        <BackDialog
          kind={dialog}
          noun="TOR"
          record={tor.torNo}
          busy={back.isPending}
          error={back.error}
          onConfirm={(note) => back.mutate({ kind: dialog, note })}
          onClose={() => setDialog(null)}
        />
      )}
    </span>
  );
}
