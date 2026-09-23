import { Plus, Trash2 } from 'lucide-react';
import type { JournalLineInput, Side } from '@/api/gl';
import { Button } from '@/components/ui/Button';
import { formatAmount } from '@/utils/format';
import { emptyLine, totals } from './journalMath';
import { useGlLookups } from './useLookups';

interface Props {
  lines: JournalLineInput[];
  onChange: (lines: JournalLineInput[]) => void;
  readOnly?: boolean;
}

/**
 * Editable grid of debit/credit lines with a live balance indicator. Account codes are chosen
 * from the list of active postable accounts (headings cannot be posted to).
 */
export function JournalLinesEditor({ lines, onChange, readOnly = false }: Readonly<Props>) {
  const { postableAccounts, costCenters, businessLines } = useGlLookups();
  const t = totals(lines);

  const update = (index: number, patch: Partial<JournalLineInput>) => {
    onChange(lines.map((l, i) => (i === index ? { ...l, ...patch } : l)));
  };
  const remove = (index: number) => onChange(lines.filter((_, i) => i !== index));

  return (
    <div className="stack">
      <datalist id="postable-accounts">
        {postableAccounts.map((a) => (
          <option key={a.id} value={a.code}>
            {a.name}
          </option>
        ))}
      </datalist>
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Account</th>
              <th>Dr / Cr</th>
              <th className="num">Amount</th>
              <th>Cost centre</th>
              <th>Line of business</th>
              <th>Reference</th>
              <th>Narration</th>
              {!readOnly && <th aria-label="Actions" />}
            </tr>
          </thead>
          <tbody>
            {lines.map((line, i) => {
              const account = postableAccounts.find((a) => a.code === line.accountCode);
              const rowLabel = `line ${i + 1}`;
              return (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td style={{ minWidth: 220 }}>
                    <input
                      className="input"
                      list="postable-accounts"
                      aria-label={`Account for ${rowLabel}`}
                      value={line.accountCode}
                      disabled={readOnly}
                      onChange={(e) => update(i, { accountCode: e.target.value.trim() })}
                    />
                    <div className="muted" style={{ fontSize: 12 }}>
                      {account?.name ?? ''}
                    </div>
                  </td>
                  <td>
                    <select
                      className="select"
                      aria-label={`Debit or credit for ${rowLabel}`}
                      value={line.side}
                      disabled={readOnly}
                      onChange={(e) => update(i, { side: e.target.value as Side })}
                    >
                      <option value="DEBIT">Debit</option>
                      <option value="CREDIT">Credit</option>
                    </select>
                  </td>
                  <td>
                    <input
                      className="input num"
                      type="number"
                      min="0"
                      step="0.01"
                      aria-label={`Amount for ${rowLabel}`}
                      value={line.amount || ''}
                      disabled={readOnly}
                      onChange={(e) => update(i, { amount: Number(e.target.value) })}
                    />
                  </td>
                  <td>
                    <select
                      className="select"
                      aria-label={`Cost centre for ${rowLabel}`}
                      value={line.costCenter ?? ''}
                      disabled={readOnly}
                      onChange={(e) => update(i, { costCenter: e.target.value || undefined })}
                    >
                      <option value="">—</option>
                      {costCenters.map((c) => (
                        <option key={c.code} value={c.code}>
                          {c.code} – {c.name}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <select
                      className="select"
                      aria-label={`Line of business for ${rowLabel}`}
                      value={line.businessLine ?? ''}
                      disabled={readOnly}
                      onChange={(e) => update(i, { businessLine: e.target.value || undefined })}
                    >
                      <option value="">—</option>
                      {businessLines.map((c) => (
                        <option key={c.code} value={c.code}>
                          {c.code}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input
                      className="input"
                      aria-label={`Reference for ${rowLabel}`}
                      value={line.reference ?? ''}
                      disabled={readOnly}
                      onChange={(e) => update(i, { reference: e.target.value })}
                    />
                  </td>
                  <td>
                    <input
                      className="input"
                      aria-label={`Narration for ${rowLabel}`}
                      value={line.narration ?? ''}
                      disabled={readOnly}
                      onChange={(e) => update(i, { narration: e.target.value })}
                    />
                  </td>
                  {!readOnly && (
                    <td>
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`Remove ${rowLabel}`}
                        icon={<Trash2 size={15} />}
                        onClick={() => remove(i)}
                        disabled={lines.length <= 2}
                      />
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="row">
        {!readOnly && (
          <>
            <Button
              variant="secondary"
              size="sm"
              icon={<Plus size={15} />}
              onClick={() => onChange([...lines, emptyLine('DEBIT')])}
            >
              Add debit
            </Button>
            <Button
              variant="secondary"
              size="sm"
              icon={<Plus size={15} />}
              onClick={() => onChange([...lines, emptyLine('CREDIT')])}
            >
              Add credit
            </Button>
          </>
        )}
        <div className="spacer" />
        <span>
          Debit <strong className="num">{formatAmount(t.debit)}</strong>
        </span>
        <span>
          Credit <strong className="num">{formatAmount(t.credit)}</strong>
        </span>
        <span className={`badge ${t.balanced ? 'success' : 'danger'}`} role="status">
          {t.balanced ? 'Balanced' : `Difference ${formatAmount(t.difference)}`}
        </span>
      </div>
    </div>
  );
}
