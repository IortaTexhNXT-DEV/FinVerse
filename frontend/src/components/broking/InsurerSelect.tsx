import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { Combobox } from '@/components/ui/Combobox';
import { SEARCH_FROM } from '@/components/ui/comboOptions';
import { useCompanyId } from '@/context/workspaceContext';

/**
 * A choice of the insurers of the company, by name (never a typed insurer code); the value is the
 * insurer's party code.
 */
export function InsurerSelect({
  id,
  value,
  onChange,
  exclude,
  placeholder = 'Select…',
  ...rest
}: Readonly<{
  id: string;
  value: string;
  onChange: (code: string) => void;
  exclude?: string | null;
  placeholder?: string;
  'aria-invalid'?: boolean;
}>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
  });
  const shown = (insurers.data ?? []).filter((i) => i.partyCode !== exclude);
  if (shown.length >= SEARCH_FROM) {
    return (
      <Combobox
        id={id}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        options={shown.map((i) => ({ value: i.partyCode, label: i.name }))}
        {...rest}
      />
    );
  }
  return (
    <select
      id={id}
      className="select"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      {...rest}
    >
      <option value="">{placeholder}</option>
      {shown.map((i) => (
        <option key={i.partyCode} value={i.partyCode}>
          {i.name}
        </option>
      ))}
    </select>
  );
}
