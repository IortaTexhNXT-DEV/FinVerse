import { Field } from '@/components/ui/Field';
import { InsurerSelect } from './InsurerSelect';

/**
 * The insurer filter of a list, chosen by name (never a typed insurer code): "All insurers" or one
 * insurer; the value is the insurer's party code.
 */
export function InsurerFilter({
  value,
  onChange,
}: Readonly<{ value: string; onChange: (code: string) => void }>) {
  return (
    <div className="worklist-toolbar">
      <Field label="Insurer">
        {(id) => (
          <InsurerSelect id={id} value={value} placeholder="All insurers" onChange={onChange} />
        )}
      </Field>
    </div>
  );
}
