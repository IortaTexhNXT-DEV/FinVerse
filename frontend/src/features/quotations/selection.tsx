import type { Column } from '@/components/ui/DataTable';

/**
 * A checkbox column for row selection (bulk actions of the work lists), with a header box that
 * selects or clears the rows shown (BDO table pattern).
 */
export function selectionColumn<T>(
  key: (row: T) => number,
  selected: ReadonlySet<number>,
  onToggle: (id: number) => void,
  label: (row: T) => string,
  shown: { rows: T[]; onSetAll: (ids: number[], on: boolean) => void },
): Column<T> {
  const ids = shown.rows.map(key);
  return {
    key: 'select',
    header: (
      <input
        type="checkbox"
        aria-label="Select all rows shown"
        disabled={ids.length === 0}
        checked={ids.length > 0 && ids.every((id) => selected.has(id))}
        onChange={(e) => shown.onSetAll(ids, e.target.checked)}
      />
    ),
    width: '44px',
    render: (row: T) => (
      <input
        type="checkbox"
        aria-label={`Select ${label(row)}`}
        checked={selected.has(key(row))}
        onClick={(e) => e.stopPropagation()}
        onChange={() => onToggle(key(row))}
      />
    ),
  };
}
