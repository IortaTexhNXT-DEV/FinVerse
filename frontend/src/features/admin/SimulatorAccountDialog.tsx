import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { DIRECTORY_STATUS_LABELS, identityApi } from '@/api/identity';
import type { DirectoryAccount } from '@/api/identity';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { missingAccountFields, SIMULATOR_STATUSES } from './identitySync';

const TEXT_FIELDS: [keyof DirectoryAccount, string][] = [
  ['firstName', 'First name'],
  ['lastName', 'Last name'],
  ['displayName', 'Display name'],
  ['email', 'AD e-mail'],
  ['adGroup', 'AD group'],
  ['teamLeaderName', "Team leader's name"],
  ['teamHeadName', "Team head's name"],
  ['sectionHeadName', "Section head's name"],
  ['unitHeadName', "Unit head's name"],
  ['unitSegment', 'Unit / segment'],
  ['department', 'Department'],
  ['location', 'Location'],
  ['uidmRequestNo', 'UIDM request no.'],
];

/** Adds or changes an account of the Enterprise SSO simulator (SIT and UAT). */
export function SimulatorAccountDialog({
  account,
  isNew,
  onClose,
  onSaved,
}: Readonly<{
  account: DirectoryAccount;
  isNew: boolean;
  onClose: () => void;
  onSaved: () => void;
}>) {
  const toast = useToast();
  const [draft, setDraft] = useState<DirectoryAccount>(account);
  const missing = missingAccountFields(draft);
  const save = useMutation({
    mutationFn: () =>
      isNew ? identityApi.addSimulatorAccount(draft) : identityApi.changeSimulatorAccount(draft),
    onSuccess: () => {
      toast.success(`Enterprise SSO account ${draft.windowsId} saved`);
      onSaved();
      onClose();
    },
  });
  const set = (key: keyof DirectoryAccount, value: string) =>
    setDraft((d) => ({ ...d, [key]: value }));
  return (
    <Modal
      title={isNew ? 'New Enterprise SSO Account' : `Change ${account.windowsId}`}
      open
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={save.isPending}
          disabled={missing.length > 0}
          onClick={() => save.mutate()}
        >
          Save
        </Button>
      }
    >
      <div className="stack">
        {missing.length > 0 && <Notice tone="info">Still needed: {missing.join(', ')}</Notice>}
        <div className="form-grid">
          <Field label="Windows ID" required>
            {(id) => (
              <input
                id={id}
                className="input"
                disabled={!isNew}
                value={draft.windowsId}
                onChange={(e) => set('windowsId', e.target.value)}
              />
            )}
          </Field>
          <Field label="User ID" required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={draft.userId}
                onChange={(e) => set('userId', e.target.value)}
              />
            )}
          </Field>
          <Field label="AD status" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={draft.status ?? 'ACTIVE'}
                onChange={(e) => set('status', e.target.value)}
              >
                {SIMULATOR_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {DIRECTORY_STATUS_LABELS[s]}
                  </option>
                ))}
              </select>
            )}
          </Field>
          {TEXT_FIELDS.map(([key, label]) => (
            <Field key={key} label={label}>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={draft[key] ?? ''}
                  onChange={(e) => set(key, e.target.value)}
                />
              )}
            </Field>
          ))}
        </div>
        <ErrorAlert error={save.error} />
      </div>
    </Modal>
  );
}
