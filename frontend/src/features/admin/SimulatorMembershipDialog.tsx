import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { identityApi } from '@/api/identity';
import type { DirectoryAccount, IdentityEvent } from '@/api/identity';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { groupCodes } from './identitySync';

/**
 * Sends the group profiles of a simulator account as UIDM-ISC would (SIT and UAT): they replace
 * the group profiles of the user at once, after the same checks as the provisioning interface.
 */
export function SimulatorMembershipDialog({
  account,
  onClose,
  onSent,
}: Readonly<{
  account: DirectoryAccount;
  onClose: () => void;
  onSent: (event: IdentityEvent) => void;
}>) {
  const [groups, setGroups] = useState('');
  const [requestNo, setRequestNo] = useState(account.uidmRequestNo ?? '');
  const send = useMutation({
    mutationFn: () =>
      identityApi.sendSimulatorMembership(
        account.windowsId,
        groupCodes(groups),
        requestNo.trim() === '' ? undefined : requestNo.trim(),
      ),
    onSuccess: (event) => {
      onSent(event);
      onClose();
    },
  });
  return (
    <Modal
      title={`Send Group Profiles of ${account.windowsId}`}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={send.isPending} onClick={() => send.mutate()}>
          Send
        </Button>
      }
    >
      <div className="stack">
        <Notice tone="info">
          The group profiles sent replace those the user holds; leave the list blank to remove them
          all.
        </Notice>
        <div className="form-grid">
          <Field label="Group profile codes" hint="Separated by commas, e.g. MKT_AO, TSU">
            {(id) => (
              <input
                id={id}
                className="input mono"
                value={groups}
                onChange={(e) => setGroups(e.target.value)}
              />
            )}
          </Field>
          <Field label="UIDM request no.">
            {(id) => (
              <input
                id={id}
                className="input"
                value={requestNo}
                onChange={(e) => setRequestNo(e.target.value)}
              />
            )}
          </Field>
        </div>
        <ErrorAlert error={send.error} />
      </div>
    </Modal>
  );
}
