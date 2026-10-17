import { useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { supportAdminApi } from '@/api/supportAdmin';
import type { ContentMigrationCount } from '@/api/supportAdmin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { contentAreaName, copiedPercent } from './supportText';

/**
 * Content Migration (Administration): progress of the copy of the file content kept in the
 * database into the file store, by area. The copy runs as the scheduled job FILE_BYTEA_MIGRATION;
 * an area is complete when no file remains.
 */
export default function ContentMigrationPage() {
  const queryClient = useQueryClient();
  const counts = useQuery({
    queryKey: ['content-migration'],
    queryFn: supportAdminApi.contentMigration,
  });
  const rows = counts.data ?? [];
  const total = rows.reduce((n, c) => n + c.total, 0);
  const moved = rows.reduce((n, c) => n + c.moved, 0);
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Content Migration"
        description="Copy of the stored documents from the database into the file store, by area."
        actions={
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            onClick={() => void queryClient.invalidateQueries({ queryKey: ['content-migration'] })}
          >
            Refresh
          </Button>
        }
      />
      <ErrorAlert error={counts.error} />
      <div className="grid-4">
        <Kpi label="Files" value={total.toLocaleString('en-PH')} />
        <Kpi label="Copied" value={moved.toLocaleString('en-PH')} />
        <Kpi label="Remaining" value={(total - moved).toLocaleString('en-PH')} />
        <Kpi label="Progress" value={`${String(copiedPercent({ total, moved }))}%`} />
      </div>
      <Card flush>
        <DataTable<ContentMigrationCount>
          callout="content-migration"
          loading={counts.isLoading}
          rows={rows}
          rowKey={(c) => c.table}
          columns={[
            {
              key: 'a',
              header: 'Area',
              render: (c) => <strong>{contentAreaName(c.table)}</strong>,
            },
            {
              key: 't',
              header: 'Files',
              numeric: true,
              render: (c) => c.total.toLocaleString('en-PH'),
            },
            {
              key: 'm',
              header: 'Copied',
              numeric: true,
              render: (c) => c.moved.toLocaleString('en-PH'),
            },
            {
              key: 'r',
              header: 'Remaining',
              numeric: true,
              render: (c) => c.remaining.toLocaleString('en-PH'),
            },
            {
              key: 'p',
              header: 'Progress',
              numeric: true,
              render: (c) => `${String(copiedPercent(c))}%`,
            },
            {
              key: 's',
              header: 'Status',
              kind: 'status',
              render: (c) =>
                c.remaining === 0 ? (
                  <StatusBadge status="COMPLETED" label="Complete" />
                ) : (
                  <StatusBadge status="IN_PROGRESS" label="In Progress" />
                ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
