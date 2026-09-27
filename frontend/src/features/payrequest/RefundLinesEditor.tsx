import { Plus, Search, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { formatAmount } from '@/utils/format';
import { emptyLine, linesTotal } from './requestForm';
import type { FieldErrors, LineDraft } from './requestForm';
import './payrequest.css';

interface RefundLinesEditorProps {
  lines: LineDraft[];
  errors: FieldErrors;
  onChange: (lines: LineDraft[]) => void;
}

type TextKey = 'arNo' | 'invoiceNo' | 'clientCode' | 'assuredName' | 'amount' | 'branchUnit';

function TextCell({
  line,
  index,
  field,
  label,
  error,
  onEdit,
}: Readonly<{
  line: LineDraft;
  index: number;
  field: TextKey;
  label: string;
  error?: string;
  onEdit: (i: number, patch: Partial<LineDraft>) => void;
}>) {
  return (
    <td>
      <input
        className="input"
        aria-label={`${label} ${String(index + 1)}`}
        aria-invalid={error !== undefined}
        inputMode={field === 'amount' ? 'decimal' : undefined}
        value={line[field]}
        onChange={(e) => onEdit(index, { [field]: e.target.value })}
      />
      {error && <span className="field-error">{error}</span>}
    </td>
  );
}

/**
 * The accounts of a Refund Request Form (Appendix D, MKT 1.10.0): AR number, invoice (looked up in
 * the Operations ledger to fill the client and assured), amount, reason, branch / unit and the
 * categories, with the running total.
 */
export function RefundLinesEditor({ lines, errors, onChange }: Readonly<RefundLinesEditorProps>) {
  const [lookupError, setLookupError] = useState<string>();
  const edit = (i: number, patch: Partial<LineDraft>) =>
    onChange(lines.map((l, j) => (j === i ? { ...l, ...patch } : l)));
  const lookup = async (i: number) => {
    const invoiceNo = lines[i]?.invoiceNo.trim() ?? '';
    if (invoiceNo === '') {
      return;
    }
    setLookupError(undefined);
    try {
      const family = await opsApi.family(invoiceNo);
      const found = family.find((f) => f.keys.invoiceNo === invoiceNo) ?? family[0];
      if (found) {
        edit(i, { clientCode: found.parties.clientCode, assuredName: found.parties.assuredName });
      }
    } catch {
      setLookupError(`Invoice ${invoiceNo} was not found in the ledger`);
    }
  };
  const first = lines[0];
  return (
    <div className="stack">
      {errors.lines && (
        <div className="alert danger" role="alert">
          {errors.lines}
        </div>
      )}
      {lookupError && (
        <div className="alert warning" role="status">
          {lookupError}
        </div>
      )}
      <div className="table-wrap prq-lines">
        <table className="table">
          <caption className="visually-hidden">Accounts to refund</caption>
          <thead>
            <tr>
              <th>AR No.</th>
              <th>Invoice No.</th>
              <th>Client No.</th>
              <th>Assured / Client</th>
              <th className="num">Amount</th>
              <th>Reason</th>
              <th>Branch / Unit</th>
              <th>Category A / B</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {lines.map((l, i) => (
              <tr key={`line-${String(i)}`}>
                <TextCell
                  line={l}
                  index={i}
                  field="arNo"
                  label="AR No."
                  error={errors[`arNo${String(i)}`]}
                  onEdit={edit}
                />
                <td>
                  <span className="prq-inline-field">
                    <input
                      className="input"
                      aria-label={`Invoice No. ${String(i + 1)}`}
                      value={l.invoiceNo}
                      onChange={(e) => edit(i, { invoiceNo: e.target.value })}
                      onBlur={() => void lookup(i)}
                    />
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={`Look up invoice ${String(i + 1)}`}
                      icon={<Search size={14} />}
                      onClick={() => void lookup(i)}
                    />
                  </span>
                </td>
                <TextCell
                  line={l}
                  index={i}
                  field="clientCode"
                  label="Client No."
                  error={errors[`clientCode${String(i)}`]}
                  onEdit={edit}
                />
                <TextCell line={l} index={i} field="assuredName" label="Assured" onEdit={edit} />
                <TextCell
                  line={l}
                  index={i}
                  field="amount"
                  label="Amount"
                  error={errors[`amount${String(i)}`]}
                  onEdit={edit}
                />
                <td>
                  <LovSelect
                    id={`reason-${String(i)}`}
                    type="REFUND_REASON"
                    value={l.reasonCode}
                    onChange={(code) => edit(i, { reasonCode: code })}
                  />
                  {errors[`reasonCode${String(i)}`] && (
                    <span className="field-error">{errors[`reasonCode${String(i)}`]}</span>
                  )}
                </td>
                <TextCell
                  line={l}
                  index={i}
                  field="branchUnit"
                  label="Branch / Unit"
                  onEdit={edit}
                />
                <td>
                  <LovSelect
                    id={`cat-a-${String(i)}`}
                    type="RRF_CATEGORY_A"
                    value={l.categoryA}
                    placeholder="Category A"
                    onChange={(code) => edit(i, { categoryA: code })}
                  />
                  <LovSelect
                    id={`cat-b-${String(i)}`}
                    type="RRF_CATEGORY_B"
                    value={l.categoryB}
                    placeholder="Category B"
                    onChange={(code) => edit(i, { categoryB: code })}
                  />
                </td>
                <td>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Remove account ${String(i + 1)}`}
                    icon={<Trash2 size={14} />}
                    disabled={lines.length === 1}
                    onClick={() => onChange(lines.filter((_, j) => j !== i))}
                  />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th colSpan={4}>Total</th>
              <th className="num">{formatAmount(linesTotal(lines))}</th>
              <th colSpan={4} />
            </tr>
          </tfoot>
        </table>
      </div>
      <div>
        <Button
          variant="secondary"
          icon={<Plus size={16} />}
          onClick={() => onChange([...lines, emptyLine(first?.clientCode, first?.assuredName)])}
        >
          Add Account
        </Button>
      </div>
    </div>
  );
}
