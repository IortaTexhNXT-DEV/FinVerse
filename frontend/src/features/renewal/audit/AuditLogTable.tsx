import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { renewalDashboardApi } from '@/api/renewalDashboard';
import type { AuditEntry, AuditFilters } from '@/api/renewalDashboard';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';

/** The audit log entries of the filters with the CSV and Excel export (FRRN.043.01, .02). */
export function AuditLogTable({
  filters,
  showReference = true,
}: Readonly<{ filters: AuditFilters; showReference?: boolean }>) {
  const companyId = useCompanyId();
  const download = useFileDownload();
  const logs = useQuery({
    queryKey: ['renewal', 'audit-logs', companyId, filters],
    queryFn: () => renewalDashboardApi.auditLogs(companyId, filters),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <div className="rnw-actions">
        {(['CSV', 'XLSX'] as const).map((format) => (
          <Button
            key={format}
            variant="ghost"
            icon={<Download size={16} />}
            busy={download.isPending}
            onClick={() =>
              download.mutate(() => renewalDashboardApi.exportAuditLogs(companyId, filters, format))
            }
          >
            {format === 'CSV' ? 'Export CSV' : 'Export Excel'}
          </Button>
        ))}
      </div>
      <ErrorAlert error={logs.error} onRetry={() => void logs.refetch()} />
      <DataTable<AuditEntry>
        loading={logs.isLoading}
        rows={logs.data ?? []}
        rowKey={(e) => `${e.at ?? ''}-${e.ref}-${e.description ?? ''}-${e.newValue ?? ''}`}
        emptyMessage="No audit log entries"
        columns={[
          { key: 'at', header: 'Timestamp', kind: 'datetime', render: (e) => formatDateTime(e.at) },
          { key: 'module', header: 'Module', render: (e) => e.module },
          ...(showReference
            ? [
                {
                  key: 'ref',
                  header: 'Reference Number',
                  kind: 'code' as const,
                  render: (e: AuditEntry) => e.ref,
                },
                {
                  key: 'client',
                  header: "Client / Assured's Name",
                  render: (e: AuditEntry) => e.client ?? '',
                },
              ]
            : []),
          { key: 'type', header: 'Action Type', render: (e) => e.actionType },
          { key: 'desc', header: 'Description', render: (e) => e.description ?? '' },
          { key: 'old', header: 'Old Value', render: (e) => e.oldValue ?? '' },
          { key: 'new', header: 'New Value', render: (e) => e.newValue ?? '' },
          {
            key: 'by',
            header: 'Performed By',
            render: (e) => (e.performedBy === null ? '' : <UserName login={e.performedBy} />),
          },
          { key: 'remarks', header: 'Remarks', render: (e) => e.remarks ?? '' },
        ]}
      />
    </div>
  );
}
