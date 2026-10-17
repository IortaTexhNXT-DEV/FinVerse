import type { OptionColumn, TermsView } from '@/api/pmTerms';
import { Button } from '@/components/ui/Button';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { answerName, answerTone, optionHeader } from './termsTable';

interface TermsGridProps {
  view: TermsView;
  /** Whether the Final Terms can be edited. */
  editable: boolean;
  edited: Record<string, string>;
  onEdit: (key: string, value: string) => void;
  /** Opens an option (null option number: a new option of the insurer). */
  onOption?: (column: OptionColumn, optionNo: number | null) => void;
}

function firstOfInsurer(columns: OptionColumn[], index: number): boolean {
  return index === 0 || columns[index - 1]?.insurerCode !== columns[index]?.insurerCode;
}

function lastOfInsurer(columns: OptionColumn[], index: number): boolean {
  return columns[index + 1]?.insurerCode !== columns[index]?.insurerCode;
}

function OptionHead({
  column,
  last,
  names,
  onOption,
}: Readonly<{
  column: OptionColumn;
  last: boolean;
  names: Record<string, string>;
  onOption: TermsGridProps['onOption'];
}>) {
  return (
    <th scope="col">
      <div>{optionHeader(column)}</div>
      <StatusBadge
        status={column.answer ?? 'PENDING'}
        label={answerName(column, names)}
        tone={answerTone(column.answer)}
        full
      />
      {onOption && (
        <div className="row-actions">
          <Button variant="ghost" size="sm" onClick={() => onOption(column, column.optionNo)}>
            Edit
          </Button>
          {last && (
            <Button variant="ghost" size="sm" onClick={() => onOption(column, null)}>
              Add Option
            </Button>
          )}
        </div>
      )}
    </th>
  );
}

/**
 * The grid of the comparative table: one row per field shown, the QS value, one column per option
 * of each insurer with the insurer response, and the Final Terms for Proposal (editable).
 */
export function TermsGrid({ view, editable, edited, onEdit, onOption }: Readonly<TermsGridProps>) {
  const { table, labels, answers } = view;
  return (
    <div className="table-wrap">
      <table className="table" aria-label="Comparative table">
        <thead>
          <tr>
            <th scope="col">Field</th>
            <th scope="col">QS Value</th>
            {table.columns.map((c, i) => (
              <OptionHead
                key={`${c.insurerCode}-${c.optionNo}`}
                column={c}
                last={lastOfInsurer(table.columns, i)}
                names={answers}
                onOption={onOption}
              />
            ))}
            <th scope="col">Final Terms for Proposal</th>
          </tr>
        </thead>
        <tbody>
          {table.shown.map((key) => (
            <tr key={key}>
              <th scope="row">{labels[key] ?? key}</th>
              <td>{table.qsValues[key] ?? '—'}</td>
              {table.columns.map((c, i) => (
                <td
                  key={`${c.insurerCode}-${c.optionNo}`}
                  className={firstOfInsurer(table.columns, i) ? 'group-start' : undefined}
                >
                  {c.values[key] ?? '—'}
                </td>
              ))}
              <td>
                {editable ? (
                  <input
                    className="input"
                    aria-label={`Final Terms for ${labels[key] ?? key}`}
                    maxLength={1000}
                    value={edited[key] ?? table.finalTerms[key] ?? ''}
                    onChange={(e) => onEdit(key, e.target.value)}
                  />
                ) : (
                  (table.finalTerms[key] ?? '—')
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
