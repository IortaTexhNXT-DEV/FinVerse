import { Lock } from 'lucide-react';
import type { ConsolidationRun } from '@/api/consolidation';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';

interface Props {
  runs: ConsolidationRun[];
  loading: boolean;
  canFinalize: boolean;
  onSelect: (run: ConsolidationRun) => void;
  onFinalize: (run: ConsolidationRun) => void;
}

/** Runs of a group; a DRAFT run can be finalized (locked). Clicking a run shows its results. */
export function RunsTable({ runs, loading, canFinalize, onSelect, onFinalize }: Readonly<Props>) {
  const finalizeButton = (r: ConsolidationRun) =>
    canFinalize &&
    r.status === 'DRAFT' && (
      <Button size="sm" variant="ghost" icon={<Lock size={14} />} onClick={() => onFinalize(r)}>
        Finalize
      </Button>
    );
  return (
    <Card title="Runs" flush>
      <DataTable<ConsolidationRun>
        loading={loading}
        rows={runs}
        rowKey={(r) => r.id}
        onRowClick={onSelect}
        columns={[
          { key: 'n', header: 'Run', render: (r) => <strong>{r.runNo}</strong> },
          { key: 'd', header: 'As of', render: (r) => formatDate(r.asOfDate) },
          { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
          {
            key: 'dr',
            header: 'Total Debit',
            numeric: true,
            render: (r) => <Amount value={r.totalDebit} />,
          },
          {
            key: 'cr',
            header: 'Total Credit',
            numeric: true,
            render: (r) => <Amount value={r.totalCredit} />,
          },
          { key: 'a', header: 'Actions', render: finalizeButton },
        ]}
      />
    </Card>
  );
}
