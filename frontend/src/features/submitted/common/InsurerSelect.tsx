import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { useCompanyId } from '@/context/workspaceContext';

/** A choice of the insurers of the company, by name. */
export function InsurerSelect({
  id,
  value,
  onChange,
  exclude,
  placeholder = 'Select…',
}: Readonly<{
  id: string;
  value: string;
  onChange: (code: string) => void;
  exclude?: string | null;
  placeholder?: string;
}>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
  });
  return (
    <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">{placeholder}</option>
      {(insurers.data ?? [])
        .filter((i) => i.partyCode !== exclude)
        .map((i) => (
          <option key={i.partyCode} value={i.partyCode}>
            {i.name}
          </option>
        ))}
    </select>
  );
}
