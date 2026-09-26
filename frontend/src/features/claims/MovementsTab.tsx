import { useQuery } from '@tanstack/react-query';
import { claimsApi } from '@/api/claims';
import type { Claim, Movement } from '@/api/claims';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatDate, humanize } from '@/utils/format';
import { estimateTypeLabel, runningOutstanding } from './claimMath';

/**
 * Movement history: every approved estimate change, payment and recovery with its Reports Book type,
 * the company share, its base-currency equivalent and the running outstanding reserve.
 */
export function MovementsTab({ claim }: Readonly<{ claim: Claim }>) {
  const movements = useQuery({
    queryKey: ['claim-docs', claim.id, 'movements'],
    queryFn: () => claimsApi.movements(claim.id),
  });
  const rows = movements.data ?? [];
  const running = runningOutstanding(rows);
  const outstanding = new Map(rows.map((m, i) => [m.id, running[i]]));
  return (
    <Card title="Movement history (company share)" flush>
      <ErrorAlert error={movements.error} />
      <DataTable<Movement>
        loading={movements.isLoading}
        rows={rows}
        rowKey={(m) => m.id}
        caption="Claim movements"
        columns={[
          { key: 'date', header: 'Date', render: (m) => formatDate(m.movementDate) },
          {
            key: 'kind',
            header: 'Movement',
            render: (m) => `${humanize(m.kind)} · ${humanize(m.side)}`,
          },
          { key: 'type', header: 'Type', render: (m) => estimateTypeLabel(m.estimateType) },
          { key: 'cost', header: 'Cost', render: (m) => humanize(m.costType) },
          {
            key: 'a100',
            header: '100 %',
            numeric: true,
            render: (m) => <Amount value={m.amount100} />,
          },
          {
            key: 'amt',
            header: 'Our Share',
            numeric: true,
            render: (m) => <Amount value={m.amount} />,
          },
          {
            key: 'base',
            header: 'Base',
            numeric: true,
            render: (m) => <Amount value={m.baseAmount} />,
          },
          {
            key: 'os',
            header: 'Outstanding',
            numeric: true,
            render: (m) => <Amount value={outstanding.get(m.id)} />,
          },
          { key: 'jnl', header: 'Journal', render: (m) => m.journalBatchNo ?? '—' },
          { key: 'ref', header: 'Reference', render: (m) => m.reference },
        ]}
      />
    </Card>
  );
}
