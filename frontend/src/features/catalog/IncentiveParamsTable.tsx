import { Plus, Trash2 } from 'lucide-react';
import type { IncentiveRuleParameter } from '@/api/productCatalog';
import { Button } from '@/components/ui/Button';
import { EmptyRow } from '@/components/ui/EmptyRow';
import type { ParamRow } from './incentiveForm';

const VALUE_HINT: Record<IncentiveRuleParameter['valueType'], string> = {
  AMOUNT: 'Amount',
  NUMBER: 'Number',
  PERCENT: 'Percent',
  TEXT: 'Text',
};

interface Props {
  rows: ParamRow[];
  definitions: readonly IncentiveRuleParameter[];
  /** Errors keyed params.<row index>. */
  errors: Record<string, string>;
  /** No incentive type chosen yet: the parameters are not known. */
  typeMissing: boolean;
  onChange: (rows: ParamRow[]) => void;
}

/**
 * The rule parameters of an incentive criterion as parameter / value rows: the parameter from the
 * parameters of the incentive type, the value typed as the parameter asks (amount, number,
 * percent or text).
 */
export function IncentiveParamsTable({
  rows,
  definitions,
  errors,
  typeMissing,
  onChange,
}: Readonly<Props>) {
  const update = (index: number, patch: Partial<ParamRow>) =>
    onChange(rows.map((r, i) => (i === index ? { ...r, ...patch } : r)));
  const unused = definitions.filter((d) => !rows.some((r) => r.key === d.key));
  return (
    <fieldset className="param-rows stack" data-callout="rule-parameters">
      <legend>Rule Parameters</legend>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th scope="col">Parameter</th>
              <th scope="col">Value</th>
              <th scope="col">Remove</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <EmptyRow columns={3} message="No parameter yet. Add the parameters of the rule." />
            )}
            {rows.map((row, i) => {
              const definition = definitions.find((d) => d.key === row.key);
              const error = errors[`params.${String(i)}`];
              const errorId = `param-error-${String(i)}`;
              const name = definition?.label ?? `parameter ${String(i + 1)}`;
              return (
                <tr key={`${row.key}-${String(i)}`}>
                  <td>
                    <select
                      className="select"
                      aria-label={`Parameter ${String(i + 1)}`}
                      value={row.key}
                      onChange={(e) => update(i, { key: e.target.value })}
                    >
                      <option value="">Select</option>
                      {definitions
                        .filter((d) => d.key === row.key || !rows.some((r) => r.key === d.key))
                        .map((d) => (
                          <option key={d.key} value={d.key}>
                            {d.label}
                          </option>
                        ))}
                    </select>
                  </td>
                  <td>
                    <input
                      className="input"
                      aria-label={`Value of ${name}`}
                      placeholder={definition ? VALUE_HINT[definition.valueType] : ''}
                      inputMode={definition?.valueType === 'TEXT' ? 'text' : 'decimal'}
                      value={row.value}
                      aria-invalid={error ? true : undefined}
                      aria-describedby={error ? errorId : undefined}
                      onChange={(e) => update(i, { value: e.target.value })}
                    />
                    {error && (
                      <span id={errorId} className="field-error" role="alert">
                        {error}
                      </span>
                    )}
                  </td>
                  <td className="center">
                    <Button
                      size="sm"
                      variant="ghost"
                      aria-label={`Remove ${definition?.label ?? 'parameter'}`}
                      icon={<Trash2 size={14} />}
                      onClick={() => onChange(rows.filter((_, j) => j !== i))}
                    />
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="row">
        <Button
          size="sm"
          variant="secondary"
          icon={<Plus size={14} />}
          disabled={typeMissing || unused.length === 0}
          onClick={() => onChange([...rows, { key: unused[0]?.key ?? '', value: '' }])}
        >
          Add Parameter
        </Button>
        {typeMissing && <span className="muted">Select the incentive type first</span>}
      </div>
    </fieldset>
  );
}
