import type { OpenItem } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { formatDate } from '@/utils/format';
import { allocationTotal, fifoAllocate, round2 } from './receivablesMath';

interface AllocationTableProps {
  items: OpenItem[];
  amount: number;
  allocations: Record<number, number>;
  onChange: (allocations: Record<number, number>) => void;
  loading?: boolean;
}

/**
 * Open debit notes of the payer with an amount to apply per note; "Fill FIFO" proposes the oldest
 * due first. What is not allocated stays on account.
 */
export function AllocationTable({
  items,
  amount,
  allocations,
  onChange,
  loading = false,
}: Readonly<AllocationTableProps>) {
  const allocated = allocationTotal(allocations);
  const setAmount = (id: number, value: number) => {
    const next: Record<number, number> = Object.fromEntries(
      Object.entries(allocations).filter(([key]) => Number(key) !== id),
    );
    if (value > 0) {
      next[id] = value;
    }
    onChange(next);
  };

  return (
    <div className="stack">
      <div className="row">
        <span>
          Allocated{' '}
          <strong>
            <Amount value={allocated} />
          </strong>{' '}
          · on account{' '}
          <strong>
            <Amount value={round2(Math.max(amount - allocated, 0))} />
          </strong>
        </span>
        <div className="spacer" />
        <Button size="sm" variant="secondary" onClick={() => onChange(fifoAllocate(items, amount))}>
          Fill FIFO
        </Button>
        <Button size="sm" variant="ghost" onClick={() => onChange({})}>
          Clear
        </Button>
      </div>
      <DataTable<OpenItem>
        loading={loading}
        rows={items}
        rowKey={(i) => i.id}
        caption="Open debit notes"
        emptyMessage="The payer has no open debit notes in this currency."
        columns={[
          { key: 'no', header: 'Document', render: (i) => `${i.documentType} ${i.documentNo}` },
          { key: 'date', header: 'Date', render: (i) => formatDate(i.documentDate) },
          { key: 'due', header: 'Due', render: (i) => formatDate(i.dueDate) },
          {
            key: 'amt',
            header: 'Amount',
            numeric: true,
            render: (i) => <Amount value={i.amount} />,
          },
          {
            key: 'os',
            header: 'Outstanding',
            numeric: true,
            render: (i) => <Amount value={i.outstanding} />,
          },
          {
            key: 'apply',
            header: 'Apply',
            numeric: true,
            render: (i) => (
              <input
                aria-label={`Amount applied to ${i.documentNo}`}
                className="input num"
                type="number"
                min={0}
                step="0.01"
                max={i.outstanding}
                value={allocations[i.id] ?? ''}
                onChange={(e) => setAmount(i.id, Number(e.target.value))}
              />
            ),
          },
        ]}
      />
    </div>
  );
}
