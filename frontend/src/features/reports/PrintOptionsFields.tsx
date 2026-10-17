import { Field } from '@/components/ui/Field';
import type { PrintOptions } from './reportOptions';

const PAPERS: PrintOptions['paper'][] = ['A4', 'LETTER', 'LEGAL', 'A3'];
const ORIENTATIONS: { id: PrintOptions['orientation']; label: string }[] = [
  { id: 'AUTO', label: 'Automatic (by width)' },
  { id: 'PORTRAIT', label: 'Portrait' },
  { id: 'LANDSCAPE', label: 'Landscape' },
];

/** PDF print options (FRBS 2.4.9): paper size, orientation and fit to page width. */
export function PrintOptionsFields({
  value,
  onChange,
}: Readonly<{ value: PrintOptions; onChange: (value: PrintOptions) => void }>) {
  return (
    <>
      <Field label="Paper">
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.paper}
            onChange={(e) => onChange({ ...value, paper: e.target.value as PrintOptions['paper'] })}
          >
            {PAPERS.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Orientation">
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.orientation}
            onChange={(e) =>
              onChange({ ...value, orientation: e.target.value as PrintOptions['orientation'] })
            }
          >
            {ORIENTATIONS.map((o) => (
              <option key={o.id} value={o.id}>
                {o.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Width">
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.fitToWidth ? 'FIT' : 'NATURAL'}
            onChange={(e) => onChange({ ...value, fitToWidth: e.target.value === 'FIT' })}
          >
            <option value="FIT">Fit to page width</option>
            <option value="NATURAL">Natural column widths</option>
          </select>
        )}
      </Field>
    </>
  );
}
