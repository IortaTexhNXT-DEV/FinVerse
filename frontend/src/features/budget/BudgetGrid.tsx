import { SlidersHorizontal, Trash2 } from 'lucide-react';
import type { GlAccount } from '@/api/gl';
import type { DimensionValue } from '@/api/masters';
import { Button } from '@/components/ui/Button';
import { formatAmount } from '@/utils/format';
import { MONTH_LABELS, monthTotals, sum } from './budgetMath';

export interface GridRow {
  key: number;
  accountCode: string;
  costCenter: string;
  months: number[];
}

interface Props {
  rows: GridRow[];
  accounts: GlAccount[];
  costCenters: DimensionValue[];
  readOnly: boolean;
  onChange: (rows: GridRow[]) => void;
  onSpread: (row: GridRow) => void;
}

/**
 * Budget grid: account × cost centre rows, twelve month columns, row totals (annual) and column
 * totals. Amounts are in natural sign (income and expense budgets both positive).
 */
export function BudgetGrid({
  rows,
  accounts,
  costCenters,
  readOnly,
  onChange,
  onSpread,
}: Readonly<Props>) {
  const totals = monthTotals(rows);
  const update = (key: number, patch: Partial<GridRow>) => {
    onChange(rows.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  };
  const setMonth = (row: GridRow, index: number, value: string) => {
    const months = row.months.map((m, i) => (i === index ? Number(value) || 0 : m));
    update(row.key, { months });
  };

  return (
    <div className="table-wrap">
      <datalist id="budget-accounts">
        {accounts.map((a) => (
          <option key={a.id} value={a.code}>
            {a.name}
          </option>
        ))}
      </datalist>
      <table className="table">
        <caption className="visually-hidden">Budget lines by month</caption>
        <thead>
          <tr>
            <th>Account</th>
            <th>Cost centre</th>
            {MONTH_LABELS.map((m) => (
              <th key={m} className="num">
                {m}
              </th>
            ))}
            <th className="num">Annual</th>
            {!readOnly && <th aria-label="Row actions" />}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const account = accounts.find((a) => a.code === row.accountCode);
            const label = `${row.accountCode || 'new line'} ${row.costCenter}`.trim();
            return (
              <tr key={row.key}>
                <td style={{ minWidth: 180 }}>
                  <input
                    className="input"
                    list="budget-accounts"
                    aria-label={`Account of ${label}`}
                    value={row.accountCode}
                    disabled={readOnly}
                    onChange={(e) => update(row.key, { accountCode: e.target.value.trim() })}
                  />
                  <div className="muted" style={{ fontSize: 12 }}>
                    {account?.name ?? ''}
                  </div>
                </td>
                <td>
                  <select
                    className="select"
                    aria-label={`Cost centre of ${label}`}
                    value={row.costCenter}
                    disabled={readOnly}
                    onChange={(e) => update(row.key, { costCenter: e.target.value })}
                  >
                    <option value="">(all)</option>
                    {costCenters.map((c) => (
                      <option key={c.code} value={c.code}>
                        {c.code}
                      </option>
                    ))}
                  </select>
                </td>
                {row.months.map((value, i) => (
                  <td key={MONTH_LABELS[i]}>
                    <input
                      className="input num"
                      type="number"
                      step="0.01"
                      style={{ minWidth: 96 }}
                      aria-label={`${MONTH_LABELS[i] ?? ''} amount of ${label}`}
                      value={value}
                      disabled={readOnly}
                      onChange={(e) => setMonth(row, i, e.target.value)}
                    />
                  </td>
                ))}
                <td className="num">
                  <strong>{formatAmount(sum(row.months))}</strong>
                </td>
                {!readOnly && (
                  <td>
                    <div className="row">
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label={`Spread ${label}`}
                        icon={<SlidersHorizontal size={14} />}
                        onClick={() => onSpread(row)}
                      />
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label={`Remove ${label}`}
                        icon={<Trash2 size={14} />}
                        onClick={() => onChange(rows.filter((r) => r.key !== row.key))}
                      />
                    </div>
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
        <tfoot>
          <tr className="row-total">
            <td colSpan={2}>Total</td>
            {totals.map((t, i) => (
              <td key={MONTH_LABELS[i]} className="num">
                {formatAmount(t)}
              </td>
            ))}
            <td className="num">{formatAmount(sum(totals))}</td>
            {!readOnly && <td />}
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
