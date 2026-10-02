import { AlertTriangle } from 'lucide-react';
import { formatDate, formatDateTime } from '@/utils/format';
import { EmptyCell } from './CellStack';

interface DueDateProps {
  /** Due date (yyyy-MM-dd) or due time (an instant). */
  at: string | null | undefined;
  /** Past due: shown in red with the warning icon and the word "Overdue", never by colour alone. */
  overdue: boolean;
  /** Show the date only (dd-MMM-yyyy), without the time. */
  dateOnly?: boolean;
}

/**
 * A due date or time on one line ("10-Sep-2026 10:00"). Red only when past due, and then with the
 * warning icon and "Overdue" for screen readers and the tooltip; an item without a due time shows
 * the muted dash.
 */
export function DueDate({ at, overdue, dateOnly = false }: Readonly<DueDateProps>) {
  if (!at) {
    return <EmptyCell />;
  }
  const text = dateOnly ? formatDate(at) : formatDateTime(at);
  if (!overdue) {
    return <span className="due-date">{text}</span>;
  }
  return (
    <span className="due-date overdue" title={`Overdue: due ${text}`}>
      <AlertTriangle size={14} aria-hidden="true" />
      <span>{text}</span>
      <span className="visually-hidden"> Overdue</span>
    </span>
  );
}
