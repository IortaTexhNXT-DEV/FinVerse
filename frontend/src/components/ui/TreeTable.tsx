import { ChevronDown, ChevronRight } from 'lucide-react';
import { useRef, useState } from 'react';
import type { KeyboardEvent, ReactNode } from 'react';
import type { Column } from './DataTable';
import { EmptyState } from './EmptyState';
import { treeKey, visibleRows } from './treeRows';
import type { TreeNode, VisibleRow } from './treeRows';

interface TreeTableProps<T> {
  /** Columns; the first one is the tree column (chevron and indentation). */
  columns: Column<T>[];
  nodes: readonly TreeNode<T>[];
  /** Keys of the expanded rows. */
  expanded: ReadonlySet<string>;
  onToggle: (key: string) => void;
  /** Enter on a row (e.g. open the record). */
  onActivate?: (row: T) => void;
  /** Accessible name of the tree. */
  caption: string;
  /** Stable name of the table for callouts and capture recipes (data-callout). */
  callout?: string;
  loading?: boolean;
  skeletonRows?: number;
  emptyMessage?: string;
  emptyAction?: ReactNode;
  /** Extra class of a row (e.g. an inactive record in muted text). */
  rowClassName?: (row: T) => string | undefined;
}

const INDENT_PX = 24;

/** An empty cell is one muted dash. */
function orDash(value: ReactNode): ReactNode {
  return value === '' || value === null || value === undefined ? (
    <span className="muted">—</span>
  ) : (
    value
  );
}

interface TreeRowProps<T> {
  row: VisibleRow<T>;
  columns: Column<T>[];
  open: boolean;
  tabStop: boolean;
  className?: string;
  onToggle: (key: string) => void;
  onFocusRow: (key: string) => void;
  onKeyDown: (e: KeyboardEvent<HTMLTableRowElement>) => void;
}

function TreeRow<T>({
  row: r,
  columns,
  open,
  tabStop,
  className,
  onToggle,
  onFocusRow,
  onKeyDown,
}: Readonly<TreeRowProps<T>>) {
  const [treeColumn, ...otherColumns] = columns;
  return (
    <tr
      data-key={r.node.key}
      role="row"
      aria-level={r.level}
      aria-posinset={r.posInSet}
      aria-setsize={r.setSize}
      aria-expanded={r.hasChildren ? open : undefined}
      tabIndex={tabStop ? 0 : -1}
      className={className}
      onFocus={(e) => {
        if (e.target === e.currentTarget) {
          onFocusRow(r.node.key);
        }
      }}
      onKeyDown={onKeyDown}
    >
      <td role="gridcell" className="tree-cell">
        <div
          className="tree-cell-body"
          style={{ paddingLeft: `${String((r.level - 1) * INDENT_PX)}px` }}
        >
          {r.hasChildren ? (
            <button
              type="button"
              className="tree-toggle"
              tabIndex={-1}
              aria-label={open ? 'Collapse' : 'Expand'}
              onClick={() => onToggle(r.node.key)}
            >
              {open ? (
                <ChevronDown size={16} aria-hidden="true" />
              ) : (
                <ChevronRight size={16} aria-hidden="true" />
              )}
            </button>
          ) : (
            <span className="tree-toggle-spacer" aria-hidden="true" />
          )}
          <div className="tree-cell-content">{treeColumn?.render(r.node.row)}</div>
        </div>
      </td>
      {otherColumns.map((c) => (
        <td key={c.key} role="gridcell" className={c.kind === 'status' ? 'col-status' : undefined}>
          {orDash(c.render(r.node.row))}
        </td>
      ))}
    </tr>
  );
}

function PlaceholderRows<T>({
  columns,
  loading,
  count,
  emptyMessage,
  emptyAction,
}: Readonly<{
  columns: Column<T>[];
  loading: boolean;
  count: number;
  emptyMessage: string;
  emptyAction?: ReactNode;
}>) {
  if (!loading) {
    return (
      <tr>
        <td colSpan={columns.length}>
          <EmptyState message={emptyMessage} action={emptyAction} />
        </td>
      </tr>
    );
  }
  return (
    <>
      {Array.from({ length: count }, (_, i) => (
        <tr key={`skeleton-${String(i)}`} className="skeleton-row" aria-hidden="true">
          {columns.map((c) => (
            <td key={c.key}>
              <div className="skeleton-line" />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

/**
 * Hierarchical table (treegrid) in the BDO table style: Header Blue header, zebra rows, a tree
 * column with an expand / collapse chevron and one indentation step per level, skeleton rows while
 * loading and the designed empty state. Keyboard: Up / Down move between rows, Right expands (or
 * moves to the first child), Left collapses (or moves to the parent), Home / End, Enter activates.
 */
export function TreeTable<T>({
  columns,
  nodes,
  expanded,
  onToggle,
  onActivate,
  caption,
  callout,
  loading = false,
  skeletonRows = 5,
  emptyMessage = 'No items to display',
  emptyAction,
  rowClassName,
}: Readonly<TreeTableProps<T>>) {
  const body = useRef<HTMLTableSectionElement>(null);
  const [focusKey, setFocusKey] = useState<string>();
  const rows = loading ? [] : visibleRows(nodes, expanded);
  const tabStop = rows.some((r) => r.node.key === focusKey) ? focusKey : rows[0]?.node.key;
  const focusRow = (key: string) => {
    body.current?.querySelector<HTMLTableRowElement>(`tr[data-key="${CSS.escape(key)}"]`)?.focus();
  };
  const keyDown = (index: number) => (e: KeyboardEvent<HTMLTableRowElement>) => {
    if (e.target !== e.currentTarget) {
      return;
    }
    const effect = treeKey(e.key, rows, index, expanded);
    if (effect === null) {
      return;
    }
    e.preventDefault();
    if ('focus' in effect) {
      focusRow(effect.focus);
    } else if ('toggle' in effect) {
      onToggle(effect.toggle);
    } else if (onActivate !== undefined) {
      onActivate(rows[index]?.node.row as T);
    }
  };
  return (
    <div className="table-wrap" data-callout={callout}>
      <table
        className="table tree-table"
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role -- the WAI-ARIA treegrid is a table with row focus
        role="treegrid"
        aria-label={caption}
        aria-busy={loading || undefined}
      >
        <thead>
          <tr role="row">
            {columns.map((c) => (
              <th
                key={c.key}
                role="columnheader"
                scope="col"
                className={c.kind === 'status' ? 'col-status' : undefined}
                style={{ width: c.width }}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody ref={body}>
          {rows.length === 0 ? (
            <PlaceholderRows
              columns={columns}
              loading={loading}
              count={skeletonRows}
              emptyMessage={emptyMessage}
              emptyAction={emptyAction}
            />
          ) : (
            rows.map((r, index) => (
              <TreeRow
                key={r.node.key}
                row={r}
                columns={columns}
                open={expanded.has(r.node.key)}
                tabStop={r.node.key === tabStop}
                className={rowClassName?.(r.node.row)}
                onToggle={onToggle}
                onFocusRow={setFocusKey}
                onKeyDown={keyDown(index)}
              />
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
