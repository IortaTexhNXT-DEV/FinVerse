import type { RecordOriginKind } from '@/api/types';
import { Field } from './Field';

/**
 * The Origin filter of a list that holds migrated records (BRD-13): all records, records created
 * in BIBS or records migrated from a legacy system.
 */
export function OriginFilter({
  value,
  onChange,
}: Readonly<{
  value: RecordOriginKind | undefined;
  onChange: (v: RecordOriginKind | undefined) => void;
}>) {
  return (
    <Field label="Origin">
      {(id) => (
        <select
          id={id}
          className="select"
          value={value ?? ''}
          onChange={(e) =>
            onChange(e.target.value === '' ? undefined : (e.target.value as RecordOriginKind))
          }
        >
          <option value="">All origins</option>
          <option value="BIBS">BIBS</option>
          <option value="MIGRATED">Migrated</option>
        </select>
      )}
    </Field>
  );
}
