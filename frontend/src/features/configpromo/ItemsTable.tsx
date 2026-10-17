import type { ChangeType, ItemView } from '@/api/configPromotion';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { itemRows } from './promotion';
import type { ItemRow } from './promotion';

interface ItemsTableProps {
  items: ItemView[];
  loading?: boolean;
  /** Wording of the kinds of difference (import or drift). */
  label: (type: ChangeType) => string;
  /** Headers of the two sides, e.g. "This environment" and "Package". */
  fromHeader: string;
  toHeader: string;
}

const TONES = {
  ADDED: 'success',
  CHANGED: 'warning',
  UNCHANGED: 'neutral',
  ONLY_IN_TARGET: 'info',
} as const;

/**
 * The items of a dataset difference, one line per field: the item by its natural key and the kind
 * of difference on its first line, then each field with its value on each side (only the changed
 * fields of a changed item).
 */
export function ItemsTable({
  items,
  loading,
  label,
  fromHeader,
  toHeader,
}: Readonly<ItemsTableProps>) {
  return (
    <DataTable<ItemRow>
      callout="difference-items"
      loading={loading}
      rows={itemRows(items)}
      rowKey={(r) => r.id}
      emptyMessage="No differences of this kind."
      columns={[
        {
          key: 'key',
          header: 'Item',
          kind: 'code',
          render: (r) => (r.first ? <strong>{r.key}</strong> : ''),
        },
        {
          key: 'type',
          header: 'Difference',
          kind: 'status',
          width: '200px',
          render: (r) =>
            r.first ? (
              <StatusBadge status={r.type} label={label(r.type)} tone={TONES[r.type]} />
            ) : (
              ''
            ),
        },
        { key: 'field', header: 'Field', render: (r) => r.field },
        { key: 'from', header: fromHeader, render: (r) => r.from },
        { key: 'to', header: toHeader, render: (r) => r.to },
      ]}
    />
  );
}
