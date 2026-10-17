import { useQuery } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { reportApi } from '@/api/reports';
import { Combobox } from '@/components/ui/Combobox';
import { useCompanyId } from '@/context/workspaceContext';
import { readCodeSet, writeCodeSet } from './codeSetValue';

/** The values of a report parameter list, by name. */
function useParameterList(source: string) {
  const companyId = useCompanyId();
  return useQuery({
    queryKey: ['report-code-set', source, companyId],
    queryFn: () => reportApi.codeSet(source, companyId),
    enabled: companyId > 0 && source !== '',
    staleTime: 10 * 60_000,
  });
}

interface ListProps {
  id: string;
  source: string;
  value: string;
  required: boolean;
  onChange: (value: string) => void;
}

/** One value of a platform list chosen by name (a user, a group profile, a module...). */
export function LookupParameter({ id, source, value, required, onChange }: Readonly<ListProps>) {
  const list = useParameterList(source);
  return (
    <Combobox
      id={id}
      value={value}
      onChange={onChange}
      loading={list.isLoading}
      required={required}
      emptyLabel={required ? undefined : 'All'}
      options={(list.data ?? []).map((o) => ({ value: o.code, label: o.label, hint: o.code }))}
    />
  );
}

/**
 * Several values of a list, "only these" or "all except these" (BRD x.009.2): chosen by name one
 * after the other, shown as chips; no value means every value.
 */
export function CodeSetParameter({ id, source, value, onChange }: Readonly<ListProps>) {
  const list = useParameterList(source);
  const set = readCodeSet(value);
  const labelOf = (code: string) => list.data?.find((o) => o.code === code)?.label ?? code;
  const options = (list.data ?? [])
    .filter((o) => !set.codes.includes(o.code))
    .map((o) => ({ value: o.code, label: o.label }));
  return (
    <div className="code-set">
      <div className="code-set-row">
        <select
          className="select code-set-mode"
          aria-label="Only or all except"
          value={set.exclude ? 'EXCLUDE' : 'INCLUDE'}
          onChange={(e) =>
            onChange(writeCodeSet({ ...set, exclude: e.target.value === 'EXCLUDE' }))
          }
        >
          <option value="INCLUDE">Only</option>
          <option value="EXCLUDE">All except</option>
        </select>
        <Combobox
          id={id}
          value=""
          onChange={(code) =>
            code !== '' && onChange(writeCodeSet({ ...set, codes: [...set.codes, code] }))
          }
          loading={list.isLoading}
          placeholder={set.codes.length === 0 ? 'All' : 'Add…'}
          options={options}
        />
      </div>
      {set.codes.length > 0 && (
        <div className="filter-chips">
          {set.codes.map((code) => (
            <span key={code} className="filter-chip">
              {labelOf(code)}
              <button
                type="button"
                aria-label={`Remove ${labelOf(code)}`}
                onClick={() =>
                  onChange(writeCodeSet({ ...set, codes: set.codes.filter((c) => c !== code) }))
                }
              >
                <X size={14} aria-hidden="true" />
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
