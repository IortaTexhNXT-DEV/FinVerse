import type { ReactNode } from 'react';

/**
 * A table cell's content (BDO): the primary value and at most one muted secondary line under it,
 * always in the same position. An empty primary value shows a muted dash.
 */
export function CellStack({ main, sub }: Readonly<{ main: ReactNode; sub?: ReactNode }>) {
  const empty = main === undefined || main === null || main === '';
  return (
    <span className="cell-stack">
      <span>{empty ? <span className="muted">—</span> : main}</span>
      {sub !== undefined && sub !== null && sub !== '' && <span className="muted">{sub}</span>}
    </span>
  );
}

/** The muted dash of an empty cell. */
export function EmptyCell() {
  return <span className="muted">—</span>;
}
