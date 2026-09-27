import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { useCompanyId } from '@/context/workspaceContext';

/** A checkbox per insurer of the company; the ticked party codes are the value. */
export function InsurerChecks({
  value,
  onChange,
  exclude = [],
}: Readonly<{ value: string[]; onChange: (codes: string[]) => void; exclude?: string[] }>) {
  const companyId = useCompanyId();
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
    staleTime: 5 * 60_000,
  });
  const toggle = (code: string, on: boolean) =>
    onChange(on ? [...value, code] : value.filter((c) => c !== code));
  const offered = (insurers.data ?? []).filter((i) => !exclude.includes(i.partyCode));
  if (offered.length === 0) {
    return <p className="muted">No insurer to select</p>;
  }
  return (
    <div className="eb-checklist">
      {offered.map((i) => (
        <label key={i.partyCode} className="checkbox">
          <input
            type="checkbox"
            checked={value.includes(i.partyCode)}
            onChange={(e) => toggle(i.partyCode, e.target.checked)}
          />
          {i.name}
        </label>
      ))}
    </div>
  );
}
