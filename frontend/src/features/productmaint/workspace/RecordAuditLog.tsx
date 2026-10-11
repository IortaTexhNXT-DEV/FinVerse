import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import type { AuditEntry } from '@/api/admin';
import { auditExportParams, pmAuditApi } from '@/api/pmWorkspace';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { today } from '@/utils/format';
import { RECORD_AUDIT_COLUMNS } from './auditColumns';
import { ExportButtons } from './ExportButtons';

/** The first day searched for the history of a record. */
const SINCE = '2000-01-01';

/**
 * The audit logs of one Product Maintenance record (BDOI FRS FRPM.021.02): every action on the
 * record and its workflow, newest first, with the export to CSV and Excel.
 */
export function RecordAuditLog({ reference }: Readonly<{ reference: string }>) {
  const [page, setPage] = useState(0);
  const query = { from: SINCE, to: today(), entityId: reference };
  const audit = useQuery({
    queryKey: ['pm-audit', 'record', reference, page],
    queryFn: () => pmAuditApi.search({ ...query, sort: 'occurredAt', direction: 'desc', page }, 20),
  });
  return (
    <Card
      title="Audit Logs"
      flush
      actions={
        <ExportButtons
          report="PM-AUDIT"
          params={auditExportParams(query)}
          formats={['CSV', 'XLSX']}
        />
      }
    >
      <ErrorAlert error={audit.error} />
      <DataTable<AuditEntry>
        loading={audit.isLoading}
        rows={audit.data?.content ?? []}
        rowKey={(a) => a.id}
        columns={RECORD_AUDIT_COLUMNS}
        emptyMessage="No activity recorded yet."
      />
      <PageFooter data={audit.data} onPage={setPage} />
    </Card>
  );
}
