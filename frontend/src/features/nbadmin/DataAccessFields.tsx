import { useQuery } from '@tanstack/react-query';
import { ALL_COMPANIES, dataScopeApi, parseScopeText, scopeOfView } from '@/api/dataScope';
import type { UserAccess } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { DataAccessSection } from '@/features/admin/DataAccessSection';
import { describeScope } from '@/features/admin/dataAccess';
import type { AccessRequestErrors, AccessRequestForm } from './accessRequest';

interface DataAccessFieldsProps {
  form: AccessRequestForm;
  set: (p: Partial<AccessRequestForm>) => void;
  errors: AccessRequestErrors;
  users: UserAccess[];
}

/** The current data access of the user of a modification; all companies for a new user. */
function useBaseScope(form: AccessRequestForm, users: UserAccess[]) {
  const modify = form.type === 'MODIFY_USER';
  const userId = modify ? users.find((u) => u.username === form.username)?.id : undefined;
  const units = useQuery({ queryKey: ['data-scope', 'units'], queryFn: dataScopeApi.units });
  const current = useQuery({
    queryKey: ['data-scope', 'user', userId],
    queryFn: () => dataScopeApi.ofUser(userId ?? 0),
    enabled: userId !== undefined,
  });
  const loaded = current.data === undefined ? undefined : scopeOfView(current.data);
  return {
    modify,
    base: modify ? loaded : ALL_COMPANIES,
    units: units.data ?? [],
    loading: units.isLoading || (userId !== undefined && current.isLoading),
    version: `${form.username}-${String(current.dataUpdatedAt)}`,
  };
}

/**
 * The Data access of a user request (DATA_SCOPE_DESIGN.md): the companies and branches the user may
 * act for, applied with the request on approval. A new user gets all companies unless the requester
 * selects some; a modification shows the current data access and changes it only when edited.
 */
export function DataAccessFields({ form, set, errors, users }: Readonly<DataAccessFieldsProps>) {
  const { modify, base, units, loading, version } = useBaseScope(form, users);
  return (
    <fieldset className="stack">
      <legend>Data Access</legend>
      {modify && base !== undefined && (
        <p className="muted" style={{ margin: 0 }}>
          Current: {describeScope(base, units)}
        </p>
      )}
      <DataAccessSection
        key={version}
        units={units}
        scope={form.dataScope ?? base ?? ALL_COMPANIES}
        loading={loading}
        onChange={(next) => set({ dataScope: next })}
        caption="Requested data access"
      />
      {errors.dataScope && (
        <span className="field-error" role="alert">
          {errors.dataScope}
        </span>
      )}
      {form.dataScope !== undefined && (
        <div>
          <Button size="sm" variant="ghost" onClick={() => set({ dataScope: undefined })}>
            {modify ? 'Keep the current data access' : 'Give access to all companies'}
          </Button>
        </div>
      )}
    </fieldset>
  );
}

/** The data access a request asks for, read-only (request details); nothing when unchanged. */
export function RequestedDataAccess({ text }: Readonly<{ text?: string }>) {
  const units = useQuery({ queryKey: ['data-scope', 'units'], queryFn: dataScopeApi.units });
  const scope = parseScopeText(text);
  if (scope === undefined) {
    return null;
  }
  return (
    <>
      <h3 style={{ margin: 0, fontSize: 'var(--font-size-md, 15px)' }}>Requested Data Access</h3>
      <p style={{ margin: 0 }}>{describeScope(scope, units.data ?? [])}</p>
      <DataAccessSection
        units={units.data ?? []}
        scope={scope}
        loading={units.isLoading}
        caption="Requested data access"
      />
    </>
  );
}
