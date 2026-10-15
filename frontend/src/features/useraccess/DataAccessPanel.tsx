import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ALL_COMPANIES, dataScopeApi, scopeOfView } from '@/api/dataScope';
import type { DataScopeValue } from '@/api/dataScope';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { DataAccessSection } from '@/features/admin/DataAccessSection';
import { describeScope, scopeError } from '@/features/admin/dataAccess';

/**
 * The data access of a user (DATA_SCOPE_DESIGN.md): the companies and branches the user may act
 * for. Kept in this system (UIDM-ISC does not hold it); a change applies at once and is recorded in
 * the access change log.
 */
export function DataAccessPanel({ user }: Readonly<{ user: { id: number; username: string } }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const { can } = useAuth();
  const units = useQuery({ queryKey: ['data-scope', 'units'], queryFn: dataScopeApi.units });
  const current = useQuery({
    queryKey: ['data-scope', 'user', user.id],
    queryFn: () => dataScopeApi.ofUser(user.id),
  });
  const [edited, setEdited] = useState<DataScopeValue>();
  const known = units.data ?? [];
  const loaded = current.data === undefined ? undefined : scopeOfView(current.data);
  const scope = edited ?? loaded ?? ALL_COMPANIES;
  const missing = edited === undefined ? undefined : scopeError(edited, known);
  const save = useMutation({
    mutationFn: (value: DataScopeValue) => dataScopeApi.replace(user.id, value),
    onSuccess: async (saved) => {
      toast.success(`Data access of ${user.username}: ${saved.description}`);
      setEdited(undefined);
      await queries.invalidateQueries({ queryKey: ['data-scope'] });
      await queries.invalidateQueries({ queryKey: ['users'] });
    },
  });
  return (
    <div className="stack">
      <ErrorAlert error={current.error ?? units.error ?? save.error} />
      {missing !== undefined && <Notice tone="info">{missing}</Notice>}
      <p style={{ margin: 0 }}>
        <strong>{describeScope(scope, known)}</strong>
      </p>
      <DataAccessSection
        key={String(current.dataUpdatedAt)}
        units={known}
        scope={scope}
        loading={units.isLoading || current.isLoading}
        onChange={setEdited}
        caption={`Data access of ${user.username}`}
      />
      {can('USER_MANAGE') && (
        <div className="row" style={{ justifyContent: 'flex-end', gap: 'var(--space-2)' }}>
          <Button
            variant="secondary"
            disabled={edited === undefined}
            onClick={() => setEdited(undefined)}
          >
            Discard Changes
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            disabled={edited === undefined || missing !== undefined}
            onClick={() => edited && save.mutate(edited)}
          >
            Save Data Access
          </Button>
        </div>
      )}
    </div>
  );
}
