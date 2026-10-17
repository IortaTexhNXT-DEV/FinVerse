import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { ebApi } from '@/api/eb';
import { useCompanyId } from '@/context/workspaceContext';
import { displayNameOf } from '@/api/users';

interface SelectProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  /** Text of the empty option. */
  placeholder?: string;
  disabled?: boolean;
}

/** The insurers of the company (party code and name). */
export function InsurerSelect({
  id,
  value,
  onChange,
  placeholder = 'Select insurer',
  disabled,
}: Readonly<SelectProps>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
    staleTime: 5 * 60_000,
  });
  return (
    <select
      id={id}
      className="select"
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{placeholder}</option>
      {(insurers.data ?? []).map((i) => (
        <option key={i.partyCode} value={i.partyCode}>
          {i.name}
        </option>
      ))}
    </select>
  );
}

/** The EB account officers (users with the AO rights), by display name. */
export function AccountOfficerSelect({
  id,
  value,
  onChange,
  placeholder = 'Me',
  disabled,
}: Readonly<SelectProps>) {
  const officers = useQuery({
    queryKey: ['eb', 'account-officers'],
    queryFn: ebApi.accountOfficers,
    staleTime: 5 * 60_000,
  });
  return (
    <select
      id={id}
      className="select"
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{placeholder}</option>
      {(officers.data ?? []).map((u) => (
        <option key={u} value={u}>
          {displayNameOf(u)}
        </option>
      ))}
    </select>
  );
}
