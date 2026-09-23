import { Field } from '@/components/ui/Field';
import type { Granularity, PeriodChoice } from './taxPeriods';
import { indexOptions } from './taxPeriods';

interface Props {
  value: PeriodChoice;
  onChange: (value: PeriodChoice) => void;
  /** Allowed granularities (both by default). */
  granularities?: Granularity[];
}

/** Year, month / quarter and index selectors of a tax period. */
export function PeriodPicker({
  value,
  onChange,
  granularities = ['QUARTER', 'MONTH'],
}: Readonly<Props>) {
  return (
    <div className="row">
      <Field label="Year">
        {(id) => (
          <input
            id={id}
            className="input"
            type="number"
            min={2000}
            max={2100}
            value={value.year}
            onChange={(e) => onChange({ ...value, year: Number(e.target.value) })}
          />
        )}
      </Field>
      {granularities.length > 1 && (
        <Field label="Period type">
          {(id) => (
            <select
              id={id}
              className="select"
              value={value.granularity}
              onChange={(e) =>
                onChange({
                  ...value,
                  granularity: e.target.value === 'MONTH' ? 'MONTH' : 'QUARTER',
                  index: 1,
                })
              }
            >
              <option value="QUARTER">Quarter</option>
              <option value="MONTH">Month</option>
            </select>
          )}
        </Field>
      )}
      <Field label={value.granularity === 'QUARTER' ? 'Quarter' : 'Month'}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.index}
            onChange={(e) => onChange({ ...value, index: Number(e.target.value) })}
          >
            {indexOptions(value.granularity).map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}
