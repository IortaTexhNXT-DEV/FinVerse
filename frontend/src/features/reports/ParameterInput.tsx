import type { ParameterSpec } from '@/api/reports';
import { Field } from '@/components/ui/Field';
import { useWorkspace } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';

interface Props {
  spec: ParameterSpec;
  value: string;
  onChange: (value: string) => void;
  /** Validation message shown under the field. */
  error?: string;
}

/** Renders the input control for one report parameter according to its declared type. */
export function ParameterInput({ spec, value, onChange, error }: Readonly<Props>) {
  const { branches } = useWorkspace();

  if (spec.type === 'BOOLEAN') {
    return (
      <label className="checkbox" style={{ alignSelf: 'end' }}>
        <input
          type="checkbox"
          checked={value === 'true'}
          onChange={(e) => onChange(String(e.target.checked))}
        />
        {spec.label}
      </label>
    );
  }
  return (
    <Field label={spec.label} required={spec.required} error={error}>
      {(id) => {
        switch (spec.type) {
          case 'DATE':
            return (
              <input
                id={id}
                className="input"
                type="date"
                value={value}
                onChange={(e) => onChange(e.target.value)}
              />
            );
          case 'NUMBER':
            return (
              <input
                id={id}
                className="input"
                type="number"
                value={value}
                onChange={(e) => onChange(e.target.value)}
              />
            );
          case 'SELECT':
            return (
              <select
                id={id}
                className="select"
                value={value}
                onChange={(e) => onChange(e.target.value)}
              >
                {spec.options.map((o) => (
                  <option key={o} value={o}>
                    {humanize(o)}
                  </option>
                ))}
              </select>
            );
          case 'BRANCH':
            return (
              <select
                id={id}
                className="select"
                value={value}
                onChange={(e) => onChange(e.target.value)}
              >
                <option value="">All branches</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} – {b.name}
                  </option>
                ))}
              </select>
            );
          default:
            return (
              <input
                id={id}
                className="input"
                value={value}
                onChange={(e) => onChange(e.target.value)}
              />
            );
        }
      }}
    </Field>
  );
}
