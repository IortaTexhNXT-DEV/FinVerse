import { useState } from 'react';
import type { ReactNode } from 'react';

/** One label / value pair of a detail block. */
export interface Definition {
  label: string;
  value: ReactNode;
  /** Let the value span the full width of the grid (addresses, remarks). */
  wide?: boolean;
}

interface DefinitionGridProps {
  items: readonly Definition[];
  /** Label / value pairs per row: 1 (two columns, cards) or 2 (four columns, full-width sections). */
  columns?: 1 | 2;
  /** Collapse a section that is mostly empty to its filled fields, or to "Not provided". */
  collapseEmpty?: boolean;
  /** Accessible name of the block. */
  label?: string;
}

/** Whether a value counts as empty: null, undefined, '' or false. */
export function isEmptyValue(value: ReactNode): boolean {
  return value === null || value === undefined || value === '' || value === false;
}

/**
 * Key-value detail block (BDO): aligned label / value rows on a grid with one label width. Empty
 * values show a muted dash. With `collapseEmpty`, a section with no values shows "Not provided" and
 * a mostly empty one shows its filled fields with "Show All Fields".
 */
export function DefinitionGrid({
  items,
  columns = 1,
  collapseEmpty = false,
  label,
}: Readonly<DefinitionGridProps>) {
  const [showAll, setShowAll] = useState(false);
  const filled = items.filter((i) => !isEmptyValue(i.value));
  if (collapseEmpty && filled.length === 0) {
    return <p className="def-grid-empty muted">Not provided</p>;
  }
  const mostlyEmpty = collapseEmpty && filled.length * 2 < items.length;
  const shown = mostlyEmpty && !showAll ? filled : items;
  return (
    <>
      <dl className={`def-grid cols-${String(columns)}`} aria-label={label}>
        {shown.map((item) => (
          <div key={item.label} className={item.wide ? 'def-row wide' : 'def-row'}>
            <dt>{item.label}</dt>
            <dd>{isEmptyValue(item.value) ? <span className="muted">—</span> : item.value}</dd>
          </div>
        ))}
      </dl>
      {mostlyEmpty && (
        <button type="button" className="link-button" onClick={() => setShowAll((v) => !v)}>
          {showAll
            ? 'Show Filled Fields Only'
            : `Show All Fields (${String(items.length - filled.length)} not provided)`}
        </button>
      )}
    </>
  );
}
