import { Plus, Trash2 } from 'lucide-react';
import type { RuleLine } from '@/api/accounting';
import type { Side } from '@/api/gl';
import { Button } from '@/components/ui/Button';
import { useGlLookups } from '@/features/gl/useLookups';
import { emptyRuleLine } from './ruleModel';

interface Props {
  lines: RuleLine[];
  components: string[];
  onChange: (lines: RuleLine[]) => void;
  readOnly: boolean;
}

/**
 * Debit / credit lines of a rule. The account is a GL account code or an "@ROLE" placeholder the
 * publishing module supplies (e.g. @BANK); "Party" carries the event party to the line (control
 * accounts with a sub-ledger).
 */
export function RuleLinesEditor({ lines, components, onChange, readOnly }: Readonly<Props>) {
  const { postableAccounts } = useGlLookups();
  const update = (index: number, patch: Partial<RuleLine>) =>
    onChange(lines.map((l, i) => (i === index ? { ...l, ...patch } : l)));
  const remove = (index: number) => onChange(lines.filter((_, i) => i !== index));

  return (
    <div className="stack">
      <datalist id="rule-accounts">
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
              <th>Dr / Cr</th>
              <th>Account or @ROLE</th>
              <th>Amount component</th>
              <th>Party</th>
              <th>Narration</th>
              {!readOnly && <th aria-label="Actions" />}
            </tr>
          </thead>
          <tbody>
            {lines.map((line, i) => {
              const label = `line ${i + 1}`;
              return (
                <tr key={i}>
                  <td>{i + 1}</td>
                  <td>
                    <select
                      className="select"
                      aria-label={`Side of ${label}`}
                      disabled={readOnly}
                      value={line.side}
                      onChange={(e) => update(i, { side: e.target.value as Side })}
                    >
                      <option value="DEBIT">Debit</option>
                      <option value="CREDIT">Credit</option>
                    </select>
                  </td>
                  <td>
                    <input
                      className="input"
                      list="rule-accounts"
                      aria-label={`Account of ${label}`}
                      disabled={readOnly}
                      value={line.accountCode}
                      onChange={(e) => update(i, { accountCode: e.target.value.trim() })}
                    />
                  </td>
                  <td>
                    <select
                      className="select"
                      aria-label={`Amount component of ${label}`}
                      disabled={readOnly}
                      value={line.amountComponent}
                      onChange={(e) => update(i, { amountComponent: e.target.value })}
                    >
                      <option value="">Select…</option>
                      {components.map((c) => (
                        <option key={c} value={c}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td>
                    <input
                      type="checkbox"
                      aria-label={`Party on ${label}`}
                      disabled={readOnly}
                      checked={line.partyLine}
                      onChange={(e) => update(i, { partyLine: e.target.checked })}
                    />
                  </td>
                  <td>
                    <input
                      className="input"
                      aria-label={`Narration of ${label}`}
                      disabled={readOnly}
                      value={line.narration ?? ''}
                      onChange={(e) => update(i, { narration: e.target.value })}
                    />
                  </td>
                  {!readOnly && (
                    <td>
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label={`Remove ${label}`}
                        icon={<Trash2 size={14} />}
                        onClick={() => remove(i)}
                      />
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {!readOnly && (
        <div className="row">
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => onChange([...lines, emptyRuleLine('DEBIT', components[0])])}
          >
            Add debit line
          </Button>
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => onChange([...lines, emptyRuleLine('CREDIT', components[0])])}
          >
            Add credit line
          </Button>
        </div>
      )}
    </div>
  );
}
