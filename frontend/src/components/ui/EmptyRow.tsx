import type { ReactNode } from 'react';

/**
 * The body row of an empty edit grid or matrix: the table keeps its headers and says, in one muted
 * line, that it has no rows yet and what to do (the DataTable standard for hand-made tables).
 */
export function EmptyRow({
  columns,
  message,
  action,
}: Readonly<{ columns: number; message: string; action?: ReactNode }>) {
  return (
    <tr className="empty-row">
      <td colSpan={columns}>
        <span className="muted">{message}</span>
        {action}
      </td>
    </tr>
  );
}
