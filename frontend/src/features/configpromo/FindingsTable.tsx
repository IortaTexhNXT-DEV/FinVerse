import type { ImportMessages } from '@/api/configPromotion';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { findings } from './promotion';
import type { Finding } from './promotion';

/**
 * The findings of a dry run in one table: what refuses the package or blocks the approval first,
 * then the warnings and the notes on versions.
 */
export function FindingsTable({ messages }: Readonly<{ messages: ImportMessages }>) {
  const rows = findings(messages);
  if (rows.length === 0) {
    return null;
  }
  return (
    <Card title="Findings of the Dry Run" flush>
      <DataTable<Finding>
        callout="dry-run-findings"
        rows={rows}
        rowKey={(f) => f.id}
        columns={[
          {
            key: 'severity',
            header: 'Severity',
            kind: 'status',
            width: '140px',
            render: (f) => (
              <StatusBadge
                status={f.blocking ? 'BLOCKED' : 'WARNING'}
                label={f.blocking ? 'Blocks' : 'Warning'}
                tone={f.blocking ? 'danger' : 'warning'}
              />
            ),
          },
          { key: 'dataset', header: 'Dataset', render: (f) => f.dataset },
          { key: 'item', header: 'Item', kind: 'code', render: (f) => f.item },
          { key: 'message', header: 'Finding', render: (f) => f.message },
        ]}
      />
    </Card>
  );
}
