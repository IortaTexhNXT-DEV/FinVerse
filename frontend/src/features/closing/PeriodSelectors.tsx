import { Field } from '@/components/ui/Field';
import type { usePeriodPicker } from './usePeriodPicker';

type Picker = ReturnType<typeof usePeriodPicker>;

/** Fiscal year and (optionally) period selectors. */
export function PeriodSelectors({
  picker,
  withPeriod = true,
}: Readonly<{ picker: Picker; withPeriod?: boolean }>) {
  return (
    <>
      <Field label="Fiscal year">
        {(id) => (
          <select
            id={id}
            className="select"
            value={picker.year?.id ?? ''}
            onChange={(e) => picker.selectYear(Number(e.target.value))}
          >
            {picker.years.map((y) => (
              <option key={y.id} value={y.id}>
                FY {y.yearCode} ({y.status})
              </option>
            ))}
          </select>
        )}
      </Field>
      {withPeriod && (
        <Field label="Period">
          {(id) => (
            <select
              id={id}
              className="select"
              value={picker.period?.id ?? ''}
              onChange={(e) => picker.selectPeriod(Number(e.target.value))}
            >
              {picker.periods.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.status})
                </option>
              ))}
            </select>
          )}
        </Field>
      )}
    </>
  );
}
