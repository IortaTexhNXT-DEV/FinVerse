import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { accountsApi } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { TextInput } from '@/features/assets/FormControls';
import { DetailList } from '@/features/catalog/DetailList';
import type { DetailRow } from '@/features/catalog/DetailList';
import { formatDate, formatDateTime, today } from '@/utils/format';
import { useAccountRefresh } from './useAccountRefresh';

type Dialog = 'ffy' | 'ffy-cancel' | 'tsu' | null;

const CLOSED = new Set(['BOOKED', 'CANCELLED', 'VOIDED']);

function ffyText(a: Account): string {
  const f = a.freeFirstYear;
  if (f.active) {
    return `${formatDate(f.start)} to ${formatDate(f.end)}`;
  }
  return f.cancelledAt
    ? `Cancelled ${formatDateTime(f.cancelledAt)} (${f.cancelReason ?? ''})`
    : 'No';
}

function tsuText(a: Account): string {
  if (!a.tsu.required) {
    return 'Not required';
  }
  return a.tsu.clearedBy
    ? `Cleared by ${a.tsu.clearedBy} ${formatDateTime(a.tsu.clearedAt)}`
    : `Required (${a.tsu.rule ?? ''})`;
}

function tagRows(a: Account): DetailRow[] {
  return [
    ['Free First Year', ffyText(a)],
    ['Direct payment', a.directPayment ? `Yes (${a.directPaymentTaggedBy ?? ''})` : 'No'],
    ['TSU review', tsuText(a)],
  ];
}

function useDone(account: Account, onClose: () => void) {
  const toast = useToast();
  const refresh = useAccountRefresh(account.id);
  return async (updated: Account, message: string) => {
    onClose();
    await refresh(updated);
    toast.success(message);
  };
}

function TagDialogs({
  account,
  dialog,
  onClose,
}: Readonly<{ account: Account; dialog: Dialog; onClose: () => void }>) {
  const done = useDone(account, onClose);
  const [ffyStart, setFfyStart] = useState(account.periodFrom ?? today());
  const tagFfy = useMutation({
    mutationFn: () => accountsApi.tagFfy(account.id, ffyStart),
    onSuccess: (a) => done(a, 'Tagged Free First Year'),
  });
  const cancelFfy = useMutation({
    mutationFn: (note: { reasonCode?: string; comment?: string }) =>
      accountsApi.cancelFfy(account.id, note.reasonCode ?? '', note.comment),
    onSuccess: (a) => done(a, 'Free First Year cancelled'),
  });
  const clearTsu = useMutation({
    mutationFn: (comment?: string) => accountsApi.clearTsu(account.id, comment),
    onSuccess: (a) => done(a, 'TSU clearance recorded'),
  });
  if (dialog === 'ffy-cancel') {
    return (
      <ActionDialog
        title="Cancel Free First Year"
        reasonLov="FFY_CANCEL_REASON"
        confirmLabel="Cancel FFY"
        busy={cancelFfy.isPending}
        error={cancelFfy.error}
        onClose={onClose}
        onConfirm={(note) => cancelFfy.mutate(note)}
      />
    );
  }
  if (dialog === 'tsu') {
    return (
      <ActionDialog
        title="TSU Clearance"
        confirmLabel="Record clearance"
        busy={clearTsu.isPending}
        error={clearTsu.error}
        onClose={onClose}
        onConfirm={(note) => clearTsu.mutate(note.comment)}
      />
    );
  }
  return (
    <Modal
      title="Tag Free First Year"
      open={dialog === 'ffy'}
      onClose={onClose}
      footer={
        <Button variant="accent" busy={tagFfy.isPending} onClick={() => tagFfy.mutate()}>
          Tag
        </Button>
      }
    >
      <ErrorAlert error={tagFfy.error} />
      <TextInput label="FFY start" type="date" required value={ffyStart} onChange={setFfyStart} />
    </Modal>
  );
}

function TagButtons({
  account,
  onDialog,
}: Readonly<{ account: Account; onDialog: (d: Dialog) => void }>) {
  const { can } = useAuth();
  const done = useDone(account, () => onDialog(null));
  const arrangement = useMutation({
    mutationFn: () =>
      accountsApi.setPaymentArrangement(
        account.id,
        account.directPayment ? 'VIA_BDOI' : 'DIRECT_TO_INSURER',
      ),
    onSuccess: (a) => done(a, a.directPayment ? 'Tagged direct payment' : 'Premium through BDOI'),
  });
  const maintain = can('ACCOUNT_MAINTAIN') && !CLOSED.has(account.status);
  const ffy = account.freeFirstYear.active;
  const tsuPending = account.tsu.required && !account.tsu.clearedBy;
  return (
    <div className="row">
      <ErrorAlert error={arrangement.error} />
      {maintain && (
        <>
          <Button
            size="sm"
            variant="secondary"
            onClick={() => onDialog(ffy ? 'ffy-cancel' : 'ffy')}
          >
            {ffy ? 'Cancel FFY' : 'Tag FFY'}
          </Button>
          <Button
            size="sm"
            variant="secondary"
            busy={arrangement.isPending}
            onClick={() => arrangement.mutate()}
          >
            {account.directPayment ? 'Pay through BDOI' : 'Tag direct payment'}
          </Button>
        </>
      )}
      {can('TSU_PROCESS') && tsuPending && (
        <Button size="sm" variant="secondary" onClick={() => onDialog('tsu')}>
          Record TSU Clearance
        </Button>
      )}
    </div>
  );
}

/**
 * Tags of an account: Free First Year (BRNB.101-104), payment arrangement / direct payment
 * (BRNB.114) and TSU clearance (BRNB.098), with the actions the user may take.
 */
export function AccountTagsCard({ account }: Readonly<{ account: Account }>) {
  const [dialog, setDialog] = useState<Dialog>(null);
  return (
    <Card title="Tags" actions={<TagButtons account={account} onDialog={setDialog} />}>
      <DetailList rows={tagRows(account)} />
      {dialog !== null && (
        <TagDialogs account={account} dialog={dialog} onClose={() => setDialog(null)} />
      )}
    </Card>
  );
}
