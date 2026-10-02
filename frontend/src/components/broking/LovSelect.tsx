import { useQuery } from '@tanstack/react-query';
import { lovApi } from '@/api/lov';

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
 * maintained order. Use it for every coded business field.
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
