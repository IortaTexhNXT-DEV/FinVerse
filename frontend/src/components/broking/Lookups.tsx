import { useQuery } from '@tanstack/react-query';
import { api } from '@/api/client';
import { catalogApi } from '@/api/catalog';
import { mastersApi } from '@/api/masters';
import type { DirectoryEntry } from '@/api/users';
import { Combobox } from '@/components/ui/Combobox';
import type { ComboOption } from '@/components/ui/comboOptions';
import { useCompanyId } from '@/context/workspaceContext';

/** What every lookup field takes: the field id, the stored code and its change. */
export interface LookupProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  required?: boolean;
  /** In a filter: the text of the empty choice ("All"). */
  emptyLabel?: string;
  'aria-invalid'?: boolean;
}

/**
 * A searchable choice from a reference list; while the list cannot be read (no access to it, or
 * the server is not reachable) the field takes the code as typed, so the form stays usable.
 */
function LookupField({
  options,
  failed,
  loading,
  upperCase = false,
  ...props
}: Readonly<
  LookupProps & {
    options: readonly ComboOption[];
    failed: boolean;
    loading: boolean;
    upperCase?: boolean;
  }
>) {
  if (failed) {
    const { id, value, onChange, disabled, required } = props;
    return (
      <input
        id={id}
        className="input"
        value={value}
        disabled={disabled}
        required={required}
        aria-invalid={props['aria-invalid']}
        onChange={(e) => onChange(upperCase ? e.target.value.toUpperCase() : e.target.value)}
      />
    );
  }
  return <Combobox {...props} options={options} loading={loading} />;
}

/** The currencies (code, with the name to search by). */
export function CurrencySelect(props: Readonly<LookupProps>) {
  const list = useQuery({
    queryKey: ['currencies'],
    queryFn: mastersApi.currencies,
    staleTime: 10 * 60_000,
    retry: false,
  });
  const options = (list.data ?? [])
    .filter((c) => c.active || c.code === props.value)
    .map((c) => ({ value: c.code, label: c.code, hint: c.name }));
  return (
    <LookupField
      {...props}
      options={options}
      failed={list.isError}
      loading={list.isLoading}
      upperCase
    />
  );
}

/** The cost centres of the company (name, with the code to search by). */
export function CostCentreSelect(props: Readonly<LookupProps>) {
  const companyId = useCompanyId();
  const list = useQuery({
    queryKey: ['dimensions', companyId, 'COST_CENTER'],
    queryFn: () => mastersApi.dimensions(companyId, 'COST_CENTER'),
    enabled: companyId > 0,
    staleTime: 10 * 60_000,
    retry: false,
  });
  const options = (list.data ?? [])
    .filter((d) => d.active || d.code === props.value)
    .map((d) => ({ value: d.code, label: d.name, hint: d.code }));
  return (
    <LookupField
      {...props}
      options={options}
      failed={list.isError}
      loading={list.isLoading}
      upperCase
    />
  );
}

/** The users of the platform by name (with the role to tell namesakes apart); stores the login. */
export function UserLookup(props: Readonly<LookupProps>) {
  const list = useQuery({
    queryKey: ['users', 'directory'],
    queryFn: () => api.get<DirectoryEntry[]>('/users/directory'),
    staleTime: 10 * 60_000,
    retry: false,
  });
  const options = (list.data ?? [])
    .map((u) => ({ value: u.username, label: u.displayName, hint: u.roleName }))
    .sort((a, b) => a.label.localeCompare(b.label));
  return (
    <LookupField {...props} options={options} failed={list.isError} loading={list.isLoading} />
  );
}

/** The sales units of the company (regions, departments, teams) by name; stores the unit code. */
export function SalesUnitSelect(props: Readonly<LookupProps>) {
  const companyId = useCompanyId();
  const org = useQuery({
    queryKey: ['catalog', 'sales-organisation', companyId],
    queryFn: () => catalogApi.salesOrganisation(companyId),
    enabled: companyId > 0,
    staleTime: 10 * 60_000,
    retry: false,
  });
  const options = (org.data?.units ?? []).map((u) => ({
    value: u.code,
    label: u.name,
    hint: u.code,
  }));
  return (
    <LookupField
      {...props}
      options={options}
      failed={org.isError}
      loading={org.isLoading}
      upperCase
    />
  );
}
