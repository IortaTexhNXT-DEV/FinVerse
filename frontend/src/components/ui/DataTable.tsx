import { ChevronDown, ChevronUp, ChevronsUpDown } from 'lucide-react';
import type { KeyboardEvent, ReactNode } from 'react';
import { EmptyState } from './EmptyState';

/**
 * Kind of a column, which fixes its alignment and width (BDO table conventions): text left,
 * amounts right, codes fixed width and never wrapped, dates in one format, status pills centred.
 */
export type ColumnKind = 'text' | 'amount' | 'code' | 'date' | 'datetime' | 'status' | 'center';

export interface Column<T> {
  key: string;
  header: ReactNode;
  render: (row: T) => ReactNode;
  /** Right-aligned number column (same as kind 'amount'). */
  numeric?: boolean;
  kind?: ColumnKind;
  width?: string;
  /** Sort key sent to the API; the header becomes a sort button when the table has `onSort`. */
  sortKey?: string;
}

export type SortDirection = 'asc' | 'desc';

export interface SortState {
  key: string;
  direction: SortDirection;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  onRowClick?: (row: T) => void;
  emptyMessage?: string;
  /** Next step shown under the empty message (e.g. a button). */
  emptyAction?: ReactNode;
  loading?: boolean;
  caption?: string;
  /** Current sort of the list (server side). */
  sort?: SortState;
  onSort?: (sort: SortState) => void;
  /** Totals row(s) rendered in the table footer with the totals style. */
  footer?: ReactNode;
  /** Key of the selected row (highlighted). */
  selectedKey?: string | number;
  /** Number of skeleton rows while loading. */
  skeletonRows?: number;
}

const KIND_CLASS: Record<ColumnKind, string | undefined> = {
  text: undefined,
  amount: 'num',
  code: 'col-code',
  date: 'col-date',
  datetime: 'col-datetime',
  status: 'col-status',
  center: 'center',
};

function cellClass<T>(c: Column<T>): string | undefined {
  if (c.numeric) {
    return 'num';
  }
  return c.kind === undefined ? undefined : KIND_CLASS[c.kind];
}

function ariaSort<T>(c: Column<T>, sort: SortState | undefined) {
  if (c.sortKey === undefined || sort?.key !== c.sortKey) {
    return undefined;
  }
  return sort.direction === 'asc' ? 'ascending' : 'descending';
}

function SortHeader<T>({
  column,
  sort,
  onSort,
}: Readonly<{ column: Column<T>; sort?: SortState; onSort: (s: SortState) => void }>) {
  const key = column.sortKey ?? column.key;
  const active = sort?.key === key;
  const next: SortDirection = active && sort.direction === 'asc' ? 'desc' : 'asc';
  let icon = <ChevronsUpDown size={14} aria-hidden="true" className="sort-idle" />;
  if (active) {
    icon =
      sort.direction === 'asc' ? (
        <ChevronUp size={14} aria-hidden="true" />
      ) : (
        <ChevronDown size={14} aria-hidden="true" />
      );
  }
  return (
    <button type="button" className="th-sort" onClick={() => onSort({ key, direction: next })}>
      {column.header}
      {icon}
    </button>
  );
}

type BodyProps<T> = Pick<
  DataTableProps<T>,
  'columns' | 'rows' | 'rowKey' | 'onRowClick' | 'emptyMessage' | 'emptyAction' | 'selectedKey'
> & { loading: boolean; skeletonRows: number };

function SkeletonRows<T>({ columns, count }: Readonly<{ columns: Column<T>[]; count: number }>) {
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
      <tr className="visually-hidden">
        <td colSpan={columns.length}>
          <span className="spinner" aria-label="Loading" />
        </td>
      </tr>
    </>
  );
}

function TableBody<T>({
  columns,
  rows,
  rowKey,
  onRowClick,
  emptyMessage,
  emptyAction,
  loading,
  selectedKey,
  skeletonRows,
}: Readonly<BodyProps<T>>) {
  if (loading) {
    return (
      <tbody>
        <SkeletonRows columns={columns} count={skeletonRows} />
      </tbody>
    );
  }
  if (rows.length === 0) {
    return (
      <tbody>
        <tr>
          <td colSpan={columns.length}>
            <EmptyState message={emptyMessage} action={emptyAction} />
          </td>
        </tr>
      </tbody>
    );
  }
  const keyDown = (row: T) => (e: KeyboardEvent<HTMLTableRowElement>) => {
    if (e.key === 'Enter') {
      onRowClick?.(row);
    }
  };
  return (
    <tbody>
      {rows.map((row) => {
        const key = rowKey(row);
        return (
          <tr
            key={key}
            className={onRowClick ? 'clickable' : undefined}
            aria-selected={selectedKey === key || undefined}
            onClick={onRowClick ? () => onRowClick(row) : undefined}
            onKeyDown={onRowClick ? keyDown(row) : undefined}
            tabIndex={onRowClick ? 0 : undefined}
          >
            {columns.map((c) => (
              <td key={c.key} className={cellClass(c)}>
                {c.render(row)}
              </td>
            ))}
          </tr>
        );
      })}
    </tbody>
  );
}

/**
 * Accessible data table (BDO): Header Blue sticky header, zebra rows, row hover and selection,
 * keyboard-operable row click, sortable headers where the API sorts, skeleton rows while loading,
 * a designed empty state and a totals footer.
 */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  onRowClick,
  emptyMessage = 'No items to display',
  emptyAction,
  loading = false,
  caption,
  sort,
  onSort,
  footer,
  selectedKey,
  skeletonRows = 5,
}: Readonly<DataTableProps<T>>) {
  return (
    <div className="table-wrap">
      <table className="table" aria-busy={loading || undefined}>
        {caption !== undefined && <caption className="visually-hidden">{caption}</caption>}
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                className={cellClass(c)}
                style={{ width: c.width }}
                aria-sort={ariaSort(c, sort)}
                scope="col"
              >
                {onSort !== undefined && c.sortKey !== undefined ? (
                  <SortHeader column={c} sort={sort} onSort={onSort} />
                ) : (
                  c.header
                )}
              </th>
            ))}
          </tr>
        </thead>
        <TableBody
          columns={columns}
          rows={rows}
          rowKey={rowKey}
          onRowClick={onRowClick}
          emptyMessage={emptyMessage}
          emptyAction={emptyAction}
          loading={loading}
          selectedKey={selectedKey}
          skeletonRows={skeletonRows}
        />
        {footer !== undefined && !loading && rows.length > 0 && <tfoot>{footer}</tfoot>}
      </table>
    </div>
  );
}
