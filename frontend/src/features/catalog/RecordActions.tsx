import { useMutation, useQueryClient } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import type { Authorizable, CatalogKind } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { awaitsOtherChecker } from '@/utils/makerChecker';

interface Props {
  kind: CatalogKind;
  record: Authorizable;
  /** Query keys refreshed after the action. */
  refresh: readonly (readonly unknown[])[];
  /** Permissions that authorize this kind (any one); MASTER_AUTHORIZE by default. */
  authorizers?: readonly string[];
  /** Permissions that deactivate this kind (any one); MASTER_MAINTAIN by default. */
  maintainers?: readonly string[];
}

const MASTER_AUTHORIZERS = ['MASTER_AUTHORIZE'] as const;
const MASTER_MAINTAINERS = ['MASTER_MAINTAIN'] as const;

/**
 * Authorize (checker other than the maker) and Deactivate buttons of a catalog record. Every
 * catalog master follows maker-checker: new and changed records wait for authorization and are
 * not used by rating or accounts until authorized.
 */
export function RecordActions({
  kind,
  record,
  refresh,
  authorizers = MASTER_AUTHORIZERS,
  maintainers = MASTER_MAINTAINERS,
}: Readonly<Props>) {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const done = async (message: string) => {
    await Promise.all(refresh.map((queryKey) => queryClient.invalidateQueries({ queryKey })));
    toast.success(message);
  };
  const authorize = useMutation({
    mutationFn: () => catalogApi.authorize(kind, record.id),
    onSuccess: (r) => done(`${r.reference} authorized`),
    onError: (e) => toast.error(e.message),
  });
  const deactivate = useMutation({
    mutationFn: () => catalogApi.deactivate(kind, record.id),
    onSuccess: (r) => done(`${r.reference} deactivated`),
    onError: (e) => toast.error(e.message),
  });
  const canAuthorize =
    authorizers.some((p) => can(p)) && awaitsOtherChecker(record, user?.username);
  const canDeactivate = maintainers.some((p) => can(p)) && record.recordStatus === 'ACTIVE';
  if (!canAuthorize && !canDeactivate) {
    return null;
  }
  return (
    <div className="row">
      {canAuthorize && (
        <Button
          size="sm"
          variant="secondary"
          busy={authorize.isPending}
          onClick={(e) => {
            e.stopPropagation();
            authorize.mutate();
          }}
        >
          Authorize
        </Button>
      )}
      {canDeactivate && (
        <Button
          size="sm"
          variant="ghost"
          busy={deactivate.isPending}
          onClick={(e) => {
            e.stopPropagation();
            deactivate.mutate();
          }}
        >
          Deactivate
        </Button>
      )}
    </div>
  );
}
