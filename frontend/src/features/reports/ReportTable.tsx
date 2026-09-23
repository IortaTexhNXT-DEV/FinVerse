import type { ReportResult, ReportRow } from '@/api/reports';
import { formatAmount, formatDate } from '@/utils/format';

const ROW_CLASS: Record<ReportRow['kind'], string | undefined> = {
  DETAIL: undefined,
  GROUP_HEADER: 'row-group',
  SUBTOTAL: 'row-subtotal',
  TOTAL: 'row-total',
  SECTION: 'row-section',
};

function cellText(value: string | number | null | undefined, type: string): string {
  if (value === null || value === undefined) {
    return '';
  }
  switch (type) {
    case 'AMOUNT':
      return formatAmount(value);
    case 'PERCENT':
      return `${Number(value).toFixed(2)}%`;
    case 'DATE':
      return formatDate(String(value));
    default:
      return String(value);
  }
}

/** On-screen rendering of a report result with group headers, subtotals and totals. */
export function ReportTable({ result }: Readonly<{ result: ReportResult }>) {
  const span = result.columns.length + 1;
  return (
    <div className="table-wrap" style={{ maxHeight: '65vh' }}>
      <table className="table">
        <caption className="visually-hidden">{result.title}</caption>
        <thead>
          <tr>
            <th aria-label="Row label" />
            {result.columns.map((c) => (
              <th
                key={c.key}
                className={c.type === 'TEXT' || c.type === 'DATE' ? undefined : 'num'}
              >
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {result.rows.map((row, i) => {
            const label = row.label ? `${'  '.repeat(row.level)}${row.label}` : '';
            if (row.kind === 'GROUP_HEADER' || row.kind === 'SECTION') {
              return (
                <tr key={i} className={ROW_CLASS[row.kind]}>
                  <td colSpan={span}>{label}</td>
                </tr>
              );
            }
            return (
              <tr key={i} className={ROW_CLASS[row.kind]}>
                <td>{label}</td>
                {result.columns.map((c) => (
                  <td
                    key={c.key}
                    className={c.type === 'TEXT' || c.type === 'DATE' ? undefined : 'num'}
                  >
                    {cellText(row.cells[c.key], c.type)}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
