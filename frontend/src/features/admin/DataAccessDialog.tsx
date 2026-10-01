import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ALL_COMPANIES, dataScopeApi, scopeOfView } from '@/api/dataScope';
import type { CompanyUnit, DataScopeValue } from '@/api/dataScope';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { DataAccessSection } from './DataAccessSection';
import { describeScope, scopeError } from './dataAccess';

interface DataAccessDialogProps {
  user: { id: number; username: string };
  /** The emergency direct edit is open to the viewer (USER_MANAGE and UAM_DIRECT_ROLE_EDIT). */
  editable: boolean;
  /** Raises a Modify user request for the user (the regular way to change the data access). */
  onRaiseRequest: () => void;
  onClose: () => void;
}

interface FooterProps {
  editable: boolean;
  canSave: boolean;
  busy: boolean;
  onSave: () => void;
  onRaiseRequest: () => void;
  onClose: () => void;
}

function Footer({
  editable,
  canSave,
  busy,
  onSave,
  onRaiseRequest,
  onClose,
}: Readonly<FooterProps>) {
  if (!editable) {
    return (
      <>
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
        <Button variant="accent" onClick={onRaiseRequest}>
          Raise Request
        </Button>
      </>
    );
  }
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button variant="accent" busy={busy} disabled={!canSave} onClick={onSave}>
        Save
      </Button>
    </>
  );
}

/** The scope of the user as loaded, the edited scope and what is missing before a save. */
function useUserScope(userId: number) {
  const units = useQuery({ queryKey: ['data-scope', 'units'], queryFn: dataScopeApi.units });
  const current = useQuery({
    queryKey: ['data-scope', 'user', userId],
    queryFn: () => dataScopeApi.ofUser(userId),
  });
  const [edited, setEdited] = useState<DataScopeValue>();
  const known: CompanyUnit[] = units.data ?? [];
  const loaded = current.data === undefined ? undefined : scopeOfView(current.data);
  const scope = edited ?? loaded;
  const missing = edited === undefined ? undefined : scopeError(edited, known);
  return {
    units: known,
    scope,
    edited,
    setEdited,
    missing,
    loading: units.isLoading || current.isLoading,
    error: current.error ?? units.error,
    version: current.dataUpdatedAt,
  };
}

/**
 * The Data access of one user (DATA_SCOPE_DESIGN.md): the companies and branches the user may act
 * for. Read-only unless the emergency direct edit is open; the regular change is a Modify user
 * request.
 */
export function DataAccessDialog({
  user,
  editable,
  onRaiseRequest,
  onClose,
}: Readonly<DataAccessDialogProps>) {
  const toast = useToast();
  const queries = useQueryClient();
  const s = useUserScope(user.id);
  const save = useMutation({
    mutationFn: (value: DataScopeValue) => dataScopeApi.replace(user.id, value),
    onSuccess: async (saved) => {
      toast.success(`Data access of ${user.username}: ${saved.description}`);
      await queries.invalidateQueries({ queryKey: ['data-scope'] });
      await queries.invalidateQueries({ queryKey: ['users'] });
      onClose();
    },
  });
  const scope = s.scope ?? ALL_COMPANIES;
  return (
    <Modal
      title={`Data access of ${user.username}`}
      open
      onClose={onClose}
      footer={
        <Footer
          editable={editable}
          canSave={s.edited !== undefined && s.missing === undefined}
          busy={save.isPending}
          onSave={() => s.edited && save.mutate(s.edited)}
          onRaiseRequest={onRaiseRequest}
          onClose={onClose}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={s.error ?? save.error} />
        {editable && (
          <Notice tone="warning" title="Emergency direct change">
            The change applies at once and is recorded in the access change log. The regular way is
            a Modify user request.
          </Notice>
        )}
        {s.missing !== undefined && <Notice tone="info">{s.missing}</Notice>}
        <p style={{ margin: 0 }}>
          <strong>{describeScope(scope, s.units)}</strong>
        </p>
        <DataAccessSection
          key={String(s.version)}
          units={s.units}
          scope={scope}
          loading={s.loading}
          onChange={editable ? s.setEdited : undefined}
          caption={`Data access of ${user.username}`}
        />
      </div>
    </Modal>
  );
}
