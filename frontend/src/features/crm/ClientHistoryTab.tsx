import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '@/api/clients';
import type { ClientHistoryEntry } from '@/api/clients';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { formatDateTime } from '@/utils/format';

/** History (audit trail) of the client under its prospect and client codes, newest first. */
export function ClientHistoryTab({ clientId }: Readonly<{ clientId: number }>) {
  const history = useQuery({
    queryKey: ['crm', 'history', clientId],
    queryFn: () => clientsApi.history(clientId),
  });
  return (
    <Card title="Audit history" flush>
      <ErrorAlert error={history.error} />
      <DataTable<ClientHistoryEntry>
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(h) => h.id}
        emptyMessage="No history."
        columns={[
          { key: 'when', header: 'When', render: (h) => formatDateTime(h.occurredAt) },
          { key: 'who', header: 'User', render: (h) => h.username },
          {
            key: 'action',
            header: 'Action',
            render: (h) => <span className="badge">{h.action}</span>,
          },
          { key: 'summary', header: 'Details', render: (h) => h.summary },
        ]}
      />
    </Card>
  );
}
