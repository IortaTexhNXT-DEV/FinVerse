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

/** Cell class: numbers right-aligned, dates kept on one line. */
function cellClass(type: string): string | undefined {
  if (type === 'DATE') {
    return 'nowrap';
  }
  return type === 'TEXT' ? undefined : 'num';
}

type Cells = ReportRow['cells'];

/** Leading columns without a value in a row (where a subtotal or total label can go). */
function leadingEmpty(columns: ReportResult['columns'], cells: Cells): number {
  const first = columns.findIndex((c) => cells[c.key] !== null && cells[c.key] !== undefined);
  return first === -1 ? columns.length : first;
}

/**
 * Whether a separate label column is needed: a labelled subtotal or total row has a value in the
 * first column, so its label cannot span the leading empty columns.
 */
function needsLabelColumn(result: ReportResult): boolean {
  return result.rows.some(
    (r) =>
      r.kind !== 'GROUP_HEADER' &&
      r.kind !== 'SECTION' &&
      Boolean(r.label) &&
      leadingEmpty(result.columns, r.cells) === 0,
  );
}

function ValueCells({
  columns,
  cells,
  from,
}: Readonly<{ columns: ReportResult['columns']; cells: Cells; from: number }>) {
  return (
    <>
      {columns.slice(from).map((c) => (
        <td key={c.key} className={cellClass(c.type)}>
          {cellText(cells[c.key], c.type)}
        </td>
      ))}
    </>
  );
}

/**
 * On-screen rendering of a report result with group headers, subtotals and totals. Group headers
 * span the full width; a subtotal or total label spans the leading columns it leaves empty, so a
 * separate label column is shown only when a label has no room.
 */
export function ReportTable({ result }: Readonly<{ result: ReportResult }>) {
  const { columns } = result;
  const labels = needsLabelColumn(result);
  const span = columns.length + (labels ? 1 : 0);
  return (
    <div className="table-wrap report-result">
      <table className="table">
        <caption className="visually-hidden">{result.title}</caption>
        <thead>
          <tr>
            {labels && <th aria-label="Row label" className="report-label" />}
            {columns.map((c) => (
              <th key={c.key} className={cellClass(c.type)}>
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {result.rows.map((row, i) => {
            const label = row.label ? `${'  '.repeat(row.level)}${row.label}` : '';
            if (row.kind === 'GROUP_HEADER' || row.kind === 'SECTION') {
              return (
                <tr key={i} className={ROW_CLASS[row.kind]}>
                  <td colSpan={span}>{label}</td>
                </tr>
              );
            }
            if (labels) {
              return (
                <tr key={i} className={ROW_CLASS[row.kind]}>
                  <td className="report-label">{label}</td>
                  <ValueCells columns={columns} cells={row.cells} from={0} />
                </tr>
              );
            }
            const empty = label === '' ? 0 : leadingEmpty(columns, row.cells);
            return (
              <tr key={i} className={ROW_CLASS[row.kind]}>
                {empty > 0 && <td colSpan={empty}>{label}</td>}
                <ValueCells columns={columns} cells={row.cells} from={empty} />
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
