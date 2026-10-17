import type { Column } from './DataTable';
import { PeriodCell } from './PeriodCell';

/**
 * A list column showing a period with {@link PeriodCell}; its minimum width keeps both dates on
 * their own line.
 */
export function periodColumn<T>(
  key: string,
  header: string,
  from: (row: T) => string | null | undefined,
  to: (row: T) => string | null | undefined,
  openLabel?: string,
): Column<T> {
  return {
    key,
    header,
    kind: 'period',
    render: (row) => <PeriodCell from={from(row)} to={to(row)} openLabel={openLabel} />,
  };
}
