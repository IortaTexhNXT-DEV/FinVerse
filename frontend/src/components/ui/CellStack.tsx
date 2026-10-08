import type { ReactNode } from 'react';

/**
 * A table cell's content (BDO): the primary value and at most one muted secondary line under it,
 * always in the same position. An empty primary value shows a muted dash; a code or reference on
 * either line never breaks.
 */
export function CellStack({ main, sub }: Readonly<{ main: ReactNode; sub?: ReactNode }>) {
  const empty = main === undefined || main === null || main === '';
  return (
    <span className="cell-stack">
      <span className={isCode(main) ? 'nowrap' : undefined}>
        {empty ? <span className="muted">—</span> : main}
      </span>
      {sub !== undefined && sub !== null && sub !== '' && (
        <span className={isCode(sub) ? 'muted nowrap' : 'muted'}>{sub}</span>
      )}
    </span>
  );
}

/** A reference or code (CL-2026-000003, ARN-2026-000001): kept on one line, never broken. */
function isCode(value: ReactNode): boolean {
  return (
    typeof value === 'string' && value.length <= 24 && /^[A-Z0-9]+(?:-[A-Z0-9]+)+$/.test(value)
  );
}

/** The muted dash of an empty cell. */
export function EmptyCell() {
  return <span className="muted">—</span>;
}
