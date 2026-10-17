import { formatDate } from '@/utils/format';

interface PeriodCellProps {
  /** Start of the period (ISO date). */
  from: string | null | undefined;
  /** End of the period (ISO date); empty for an open-ended period. */
  to: string | null | undefined;
  /** Text of an open end (e.g. "open"). */
  openLabel?: string;
}

/**
 * A period, coverage term or date range in a list (BDO): the start date on the first line and
 * "to <end date>" on the second, each kept on one line, so a narrow column never breaks a date.
 * An empty period is the muted dash of an empty cell.
 */
export function PeriodCell({ from, to, openLabel = 'open' }: Readonly<PeriodCellProps>) {
  const start = formatDate(from);
  const end = formatDate(to);
  if (start === '' && end === '') {
    return <span className="muted">—</span>;
  }
  return (
    <span className="period-cell" title={`${start || '—'} to ${end || openLabel}`}>
      <span className="nowrap">{start || '—'}</span>
      <span className="nowrap period-cell-to">to {end || openLabel}</span>
    </span>
  );
}
