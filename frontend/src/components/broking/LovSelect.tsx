import { useQuery } from '@tanstack/react-query';
import { lovApi } from '@/api/lov';
import { Combobox } from '@/components/ui/Combobox';
import { SEARCH_FROM } from '@/components/ui/comboOptions';

interface LovSelectProps {
  id: string;
  /** List of values type, e.g. RETURN_REASON. */
  type: string;
  value: string;
  onChange: (code: string) => void;
  /** Text of the empty option; omit to force a choice. */
  placeholder?: string;
  /** Only values whose parent is this code (dependent lists). */
  parentCode?: string;
  disabled?: boolean;
  required?: boolean;
}

/**
 * Select bound to a list of values (BRNB.083): shows the values usable today, in their
 * maintained order; a long list is searchable. Use it for every coded business field.
 */
export function LovSelect({
  id,
  type,
  value,
  onChange,
  placeholder = 'Select…',
  parentCode,
  disabled = false,
  required = false,
}: Readonly<LovSelectProps>) {
  const options = useQuery({
    queryKey: ['lov', type],
    queryFn: () => lovApi.options(type),
    staleTime: 5 * 60_000,
  });
  const visible = (options.data ?? []).filter(
    (o) => parentCode === undefined || o.parentCode === parentCode,
  );
  if (visible.length >= SEARCH_FROM) {
    return (
      <Combobox
        id={id}
        value={value}
        onChange={onChange}
        options={visible.map((o) => ({ value: o.code, label: o.label }))}
        placeholder={placeholder}
        disabled={disabled}
        required={required}
      />
    );
  }
  return (
    <select
      id={id}
      className="select"
      value={value}
      disabled={disabled || options.isLoading}
      required={required}
      onChange={(e) => onChange(e.target.value)}
    >
      <option value="">{options.isLoading ? 'Loading…' : placeholder}</option>
      {visible.map((o) => (
        <option key={o.code} value={o.code}>
          {o.label}
        </option>
      ))}
    </select>
  );
}
