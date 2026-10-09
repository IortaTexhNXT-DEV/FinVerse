import type { ReactNode } from 'react';
import type { ParameterSpec } from '@/api/reports';
import { InsurerSelect } from '@/components/broking/InsurerSelect';
import { CurrencySelect } from '@/components/broking/Lookups';
import { DateInput } from '@/components/ui/DateInput';
import { Field } from '@/components/ui/Field';
import { useWorkspace } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { CodeSetParameter, LookupParameter } from './ParameterLists';

interface Props {
  spec: ParameterSpec;
  value: string;
  onChange: (value: string) => void;
  /** Validation message shown under the field. */
  error?: string;
}

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

/** The label of a choice: month numbers as month names, codes in words. */
function optionLabel(spec: ParameterSpec, option: string): string {
  if (spec.name === 'month' && /^\d{1,2}$/.test(option)) {
    return MONTHS[Number(option) - 1] ?? option;
  }
  return humanize(option);
}

type Control = (id: string, props: Readonly<Props>) => ReactNode;

function SelectControl({ id, spec, value, onChange }: Readonly<Props & { id: string }>): ReactNode {
  return (
    <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
      {!spec.required && !spec.options.includes(value) && <option value="">All</option>}
      {spec.options.map((o) => (
        <option key={o} value={o}>
          {optionLabel(spec, o)}
        </option>
      ))}
    </select>
  );
}

function BranchControl({ id, value, onChange }: Readonly<Props & { id: string }>): ReactNode {
  const { branches } = useWorkspace();
  return (
    <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">All branches</option>
      {branches.map((b) => (
        <option key={b.id} value={b.id}>
          {b.name}
        </option>
      ))}
    </select>
  );
}

/** The control of each parameter type; text for the others (references, numbers to type). */
const CONTROLS: Partial<Record<ParameterSpec['type'], Control>> = {
  DATE: (id, p) => (
    <DateInput id={id} value={p.value} onChange={(e) => p.onChange(e.target.value)} />
  ),
  NUMBER: (id, p) => (
    <input
      id={id}
      className="input"
      type="number"
      value={p.value}
      onChange={(e) => p.onChange(e.target.value)}
    />
  ),
  SELECT: (id, p) => <SelectControl id={id} {...p} />,
  BRANCH: (id, p) => <BranchControl id={id} {...p} />,
  INSURER: (id, p) => (
    <InsurerSelect id={id} value={p.value} placeholder="All insurers" onChange={p.onChange} />
  ),
  CURRENCY: (id, p) => (
    <CurrencySelect
      id={id}
      value={p.value}
      onChange={p.onChange}
      required={p.spec.required}
      emptyLabel={p.spec.required ? undefined : 'All'}
    />
  ),
  LOOKUP: (id, p) => (
    <LookupParameter
      id={id}
      source={p.spec.options[0] ?? ''}
      value={p.value}
      required={p.spec.required}
      onChange={p.onChange}
    />
  ),
  CODE_SET: (id, p) => (
    <CodeSetParameter
      id={id}
      source={p.spec.options[0] ?? ''}
      value={p.value}
      required={p.spec.required}
      onChange={p.onChange}
    />
  ),
};

/**
 * The input of one report parameter by its type: dates with the date picker, lists of the platform
 * (users, insurers, units, modules...) as searchable choices by name, selects with their options
 * in words, text for references to type.
 */
export function ParameterInput(props: Readonly<Props>) {
  const { spec, value, onChange, error } = props;
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
  const control = CONTROLS[spec.type];
  return (
    <Field label={spec.label} required={spec.required} error={error}>
      {(id) =>
        control === undefined ? (
          <input
            id={id}
            className="input"
            value={value}
            onChange={(e) => onChange(e.target.value)}
          />
        ) : (
          control(id, props)
        )
      }
    </Field>
  );
}
