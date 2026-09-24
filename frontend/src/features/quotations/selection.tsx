import type { Column } from '@/components/ui/DataTable';

/** A checkbox column for row selection (bulk actions of the work lists). */
export function selectionColumn<T>(
  key: (row: T) => number,
  selected: ReadonlySet<number>,
  onToggle: (id: number) => void,
  label: (row: T) => string,
): Column<T> {
  return {
    key: 'select',
    header: 'Select',
    width: '36px',
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
