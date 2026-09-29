import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { Authorizable, CatalogKind } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import type { RowAction } from '@/components/ui/RowActionMenu';
import { useToast } from '@/components/ui/toastContext';
import { awaitsOtherChecker } from '@/utils/makerChecker';

interface Props {
  kind: CatalogKind;
  record: Authorizable;
  /** What the menu is about, read by screen readers and shown in the dialogs. */
  label: string;
  /** Query keys refreshed after the action. */
  refresh: readonly (readonly unknown[])[];
  /** Permissions that authorize this kind (any one); MASTER_AUTHORIZE by default. */
  authorizers?: readonly string[];
  /** Permissions that deactivate this kind (any one); MASTER_MAINTAIN by default. */
  maintainers?: readonly string[];
  /** Further actions of the page, shown before Deactivate. */
  extra?: readonly RowAction[];
}

const MASTER_AUTHORIZERS = ['MASTER_AUTHORIZE'] as const;
const MASTER_MAINTAINERS = ['MASTER_MAINTAIN'] as const;

type Pending = 'authorize' | 'deactivate';

/**
 * Row action menu of a catalog record: Authorize (a checker other than the maker) and Deactivate
 * (with the reason, kept in the audit trail), each confirmed in a dialog. Every catalog master
 * follows maker-checker: new and changed records wait for authorization and are not used by rating
 * or accounts until authorized.
 */
export function RecordActions({
  kind,
  record,
  label,
  refresh,
  authorizers = MASTER_AUTHORIZERS,
  maintainers = MASTER_MAINTAINERS,
  extra = [],
}: Readonly<Props>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<Pending | null>(null);
  const act = useMutation({
    mutationFn: (reason: string) =>
      pending === 'authorize'
        ? catalogApi.authorize(kind, record.id)
        : catalogApi.deactivate(kind, record.id, reason),
    onSuccess: async (r) => {
      setPending(null);
      await Promise.all(refresh.map((queryKey) => queryClient.invalidateQueries({ queryKey })));
      toast.success(`${r.reference} ${pending === 'authorize' ? 'authorized' : 'deactivated'}`);
    },
  });
  const canAuthorize =
    authorizers.some((p) => can(p)) && awaitsOtherChecker(record, user?.username);
  const canDeactivate = maintainers.some((p) => can(p)) && record.recordStatus === 'ACTIVE';
  const actions: RowAction[] = [
    ...(canAuthorize ? [{ label: 'Authorize', onSelect: () => setPending('authorize') }] : []),
    ...extra,
    ...(canDeactivate
      ? [{ label: 'Deactivate', danger: true, onSelect: () => setPending('deactivate') }]
      : []),
  ];
  if (actions.length === 0) {
    return null;
  }
  return (
    <>
      <RowActionMenu label={label} actions={actions} />
      {pending === 'authorize' && (
        <ConfirmDialog
          title="Authorize Record"
          record={label}
          effect="The record becomes active and is used by rating and accounts from now on."
          confirmLabel="Authorize"
          busy={act.isPending}
          error={act.error}
          onConfirm={act.mutate}
          onClose={() => setPending(null)}
        />
      )}
      {pending === 'deactivate' && (
        <ConfirmDialog
          title="Deactivate Record"
          record={label}
          effect="The record is no longer offered on new business; it stays on existing accounts and in history. The reason is kept in the audit trail."
          confirmLabel="Deactivate"
          destructive
          reason="required"
          busy={act.isPending}
          error={act.error}
          onConfirm={act.mutate}
          onClose={() => setPending(null)}
        />
      )}
    </>
  );
}
