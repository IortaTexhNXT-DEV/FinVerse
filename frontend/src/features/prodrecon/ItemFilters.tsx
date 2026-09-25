import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';

/** Filters of the items of a cycle (PRCID.014/021). */
export interface ItemExtraFilters {
  ao?: string;
  unit?: string;
  segment?: string;
  productLine?: string;
}

const FIELDS: readonly { key: keyof ItemExtraFilters; label: string }[] = [
  { key: 'ao', label: 'Marketing AO / AB' },
  { key: 'unit', label: 'Sales Unit' },
  { key: 'segment', label: 'Market Segment' },
  { key: 'productLine', label: 'Product Line' },
];

function clean(values: ItemExtraFilters): ItemExtraFilters {
  const out: ItemExtraFilters = {};
  FIELDS.forEach(({ key }) => {
    const value = values[key]?.trim();
    if (value) {
      out[key] = value;
    }
  });
  return out;
}

/** The filter panel below the toolbar: AO, sales unit, segment and product line. */
export function ItemFilters({
  initial,
  onApply,
}: Readonly<{ initial: ItemExtraFilters; onApply: (f: ItemExtraFilters) => void }>) {
  const [values, setValues] = useState<ItemExtraFilters>(initial);
  return (
    <div className="worklist-filters">
      {FIELDS.map((f) => (
        <Field key={f.key} label={f.label}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={values[f.key] ?? ''}
              onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
            />
          )}
        </Field>
      ))}
      <Button variant="secondary" onClick={() => onApply(clean(values))}>
        Apply Filters
      </Button>
      <Button
        variant="ghost"
        onClick={() => {
          setValues({});
          onApply({});
        }}
      >
        Clear
      </Button>
    </div>
  );
}
