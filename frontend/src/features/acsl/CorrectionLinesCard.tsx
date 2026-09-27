import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { emptyLine, lineErrors, toDraft, toLine, totals } from './acsl';
import type { LineDraft } from './acsl';
import { acslApi } from './api';
import type { Correction, Side } from './api';
import './acsl.css';

type TextKey =
  'accountCode' | 'amount' | 'partyCode' | 'invoiceNo' | 'component' | 'costCenter' | 'narration';

const TEXT_COLUMNS: { key: TextKey; label: string }[] = [
  { key: 'accountCode', label: 'GL Account' },
  { key: 'amount', label: 'Amount' },
  { key: 'partyCode', label: 'Party' },
  { key: 'invoiceNo', label: 'Invoice' },
  { key: 'component', label: 'Component' },
  { key: 'costCenter', label: 'Cost Centre' },
  { key: 'narration', label: 'Narration' },
];

function LineRow({
  index,
  line,
  error,
  editable,
  onChange,
  onRemove,
}: Readonly<{
  index: number;
  line: LineDraft;
  error?: string;
  editable: boolean;
  onChange: (next: LineDraft) => void;
  onRemove: () => void;
}>) {
  const n = String(index + 1);
  return (
    <tr>
      <td>
        {n}
        {line.origin && <span className="cell-sub">{line.origin.toLowerCase()}</span>}
      </td>
      <td>
        <select
          className="select"
          aria-label={`Line ${n} side`}
          disabled={!editable}
          value={line.side}
          onChange={(e) => onChange({ ...line, side: e.target.value as Side })}
        >
          <option value="DEBIT">Dr</option>
          <option value="CREDIT">Cr</option>
        </select>
      </td>
      {TEXT_COLUMNS.map((c) => (
        <td key={c.key}>
          <input
            className="input"
            aria-label={`Line ${n} ${c.label}`}
            aria-invalid={error !== undefined && (c.key === 'accountCode' || c.key === 'amount')}
            disabled={!editable}
            value={line[c.key]}
            onChange={(e) => onChange({ ...line, [c.key]: e.target.value })}
          />
          {c.key === 'narration' && error && <span className="field-error">{error}</span>}
        </td>
      ))}
      <td>
        {editable && (
          <Button variant="ghost" size="sm" aria-label={`Remove line ${n}`} onClick={onRemove}>
            <Trash2 size={14} />
          </Button>
        )}
      </td>
    </tr>
  );
}

/**
 * The lines of a correction entry (ACSL 2.9.0-2.9.1): editable while in draft, with the running
 * debit and credit totals; only a balanced entry can be submitted.
 */
export function CorrectionLinesCard({
  correction,
  editable,
}: Readonly<{ correction: Correction; editable: boolean }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [lines, setLines] = useState<LineDraft[]>(() => correction.lines.map(toDraft));
  const [errors, setErrors] = useState<Record<number, string>>({});
  const [seen, setSeen] = useState(correction.lines);
  if (seen !== correction.lines) {
    setSeen(correction.lines);
    setLines(correction.lines.map(toDraft));
  }
  const save = useMutation({
    mutationFn: () => acslApi.saveLines(correction.id, lines.map(toLine)),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['acsl', 'correction', correction.id] });
      toast.success('Lines saved');
    },
  });
  const sum = totals(lines.map((l) => ({ side: l.side, amount: Number(l.amount) })));
  const onSave = () => {
    const found = lineErrors(lines);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  return (
    <Card
      title="Correction Lines"
      flush
      actions={
        editable && (
          <>
            <Button
              variant="secondary"
              size="sm"
              icon={<Plus size={14} />}
              onClick={() => setLines([...lines, emptyLine()])}
            >
              Add Line
            </Button>
            <Button variant="primary" size="sm" busy={save.isPending} onClick={onSave}>
              Save Lines
            </Button>
          </>
        )
      }
    >
      <ErrorAlert error={save.error} />
      <div className="table-wrap acsl-lines">
        <table className="table">
          <caption className="visually-hidden">Correction lines</caption>
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">Dr / Cr</th>
              {TEXT_COLUMNS.map((c) => (
                <th key={c.key} scope="col">
                  {c.label}
                </th>
              ))}
              <th scope="col">
                <span className="visually-hidden">Remove</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {lines.map((l, i) => (
              <LineRow
                key={`line-${String(i)}`}
                index={i}
                line={l}
                error={errors[i]}
                editable={editable}
                onChange={(next) => setLines(lines.map((x, j) => (j === i ? next : x)))}
                onRemove={() => setLines(lines.filter((_, j) => j !== i))}
              />
            ))}
            {lines.length === 0 && (
              <tr>
                <td colSpan={TEXT_COLUMNS.length + 3}>No items to display</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      <div className="acsl-totals">
        <span>
          Debit <Amount value={sum.debit} />
        </span>
        <span>
          Credit <Amount value={sum.credit} />
        </span>
        <span className={sum.balanced ? 'badge success' : 'badge warning'}>
          {sum.balanced ? 'Balanced' : 'Not balanced'}
        </span>
      </div>
    </Card>
  );
}
