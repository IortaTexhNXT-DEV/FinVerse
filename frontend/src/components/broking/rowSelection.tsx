import { useCallback, useMemo, useState } from 'react';
import type { Column } from '@/components/ui/DataTable';

/** Selected row keys of a work list (bulk actions enabled by the selection). */
export interface RowSelection {
  keys: string[];
  has: (key: string) => boolean;
  toggle: (key: string) => void;
  setAll: (keys: string[], on: boolean) => void;
  clear: () => void;
}

/** Keeps the selected rows of a list; the selection is cleared by the caller on tab changes. */
export function useRowSelection(): RowSelection {
  const [keys, setKeys] = useState<string[]>([]);
  const has = useCallback((key: string) => keys.includes(key), [keys]);
  const toggle = useCallback(
    (key: string) =>
      setKeys((current) =>
        current.includes(key) ? current.filter((k) => k !== key) : [...current, key],
      ),
    [],
  );
  const setAll = useCallback(
    (all: string[], on: boolean) =>
      setKeys((current) =>
        on ? [...new Set([...current, ...all])] : current.filter((k) => !all.includes(k)),
      ),
    [],
  );
  const clear = useCallback(() => setKeys([]), []);
  return useMemo(() => ({ keys, has, toggle, setAll, clear }), [keys, has, toggle, setAll, clear]);
}

/**
 * The checkbox column of a work list (BDO Insure table pattern): one box per row and a box in
 * the header that selects or clears the rows shown.
 */
export function selectionColumn<T>(
  rows: T[],
  keyOf: (row: T) => string,
  selection: RowSelection,
  labelOf: (row: T) => string,
): Column<T> {
  const keys = rows.map(keyOf);
  const allOn = keys.length > 0 && keys.every((k) => selection.has(k));
  return {
    key: 'select',
    width: '44px',
    header: (
      <input
        type="checkbox"
        aria-label="Select all rows shown"
        checked={allOn}
        disabled={keys.length === 0}
        onChange={(e) => selection.setAll(keys, e.target.checked)}
      />
    ),
    render: (row) => (
      <input
        type="checkbox"
        aria-label={`Select ${labelOf(row)}`}
        checked={selection.has(keyOf(row))}
        onClick={(e) => e.stopPropagation()}
        onChange={() => selection.toggle(keyOf(row))}
      />
    ),
  };
}
