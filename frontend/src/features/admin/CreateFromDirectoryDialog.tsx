import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { identityApi } from '@/api/identity';
import type { DirectoryAccount } from '@/api/identity';
import { Button } from '@/components/ui/Button';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';

/**
 * Creates a user from an existing and active Enterprise SSO account (BDOI FRS FRUM.002.01): the
 * account is looked up by its Windows ID, its details are shown as the platform holds them, and the
 * user is created with them; the group profiles are then given through a request.
 */
export function CreateFromDirectoryDialog({
  open,
  onClose,
  onCreated,
}: Readonly<{ open: boolean; onClose: () => void; onCreated: () => void }>) {
  const toast = useToast();
  const [windowsId, setWindowsId] = useState('');
  const [account, setAccount] = useState<DirectoryAccount | null>(null);
  const lookup = useMutation({
    mutationFn: () => identityApi.lookup(windowsId.trim()),
    onSuccess: setAccount,
    onError: () => setAccount(null),
  });
  const create = useMutation({
    mutationFn: () => identityApi.createFromDirectory(windowsId.trim()),
    onSuccess: (event) => {
      toast.success(`User ${event.username ?? ''} created from the Enterprise SSO account`);
      setWindowsId('');
      setAccount(null);
      onCreated();
      onClose();
    },
  });
  return (
    <Modal
      title="Create User from Enterprise SSO"
      open={open}
      onClose={onClose}
      footer={
        <>
          <Button
            variant="secondary"
            busy={lookup.isPending}
            disabled={windowsId.trim() === ''}
            onClick={() => lookup.mutate()}
          >
            Look Up Account
          </Button>
          <Button
            variant="accent"
            busy={create.isPending}
            disabled={account === null}
            onClick={() => create.mutate()}
          >
            Create User
          </Button>
        </>
      }
    >
      <div className="stack">
        <Field label="Windows ID" required>
          {(id) => (
            <input
              id={id}
              className="input"
              placeholder="DOMAIN\juan.delacruz"
              value={windowsId}
              onChange={(e) => {
                setWindowsId(e.target.value);
                setAccount(null);
              }}
            />
          )}
        </Field>
        <ErrorAlert error={lookup.error ?? create.error} />
        {account !== null && (
          <DefinitionGrid
            items={[
              { label: 'User ID', value: account.userId },
              { label: 'Display name', value: account.displayName ?? '' },
              { label: 'AD e-mail', value: account.email },
              { label: 'AD group', value: account.adGroup ?? '' },
              { label: 'Unit / segment', value: account.unitSegment ?? '' },
              { label: 'Department', value: account.department ?? '' },
              { label: 'Location', value: account.location ?? '' },
              { label: 'Team leader', value: account.teamLeaderName ?? '' },
              { label: 'Unit head', value: account.unitHeadName ?? '' },
              { label: 'UIDM request no.', value: account.uidmRequestNo ?? '' },
            ]}
          />
        )}
      </div>
    </Modal>
  );
}
