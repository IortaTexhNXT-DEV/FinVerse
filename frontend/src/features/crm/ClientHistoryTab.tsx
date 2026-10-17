import { useQuery } from '@tanstack/react-query';
import { clientsApi } from '@/api/clients';
import type { ClientHistoryEntry } from '@/api/clients';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Tag } from '@/components/ui/Tag';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime, humanize } from '@/utils/format';

/** History (audit trail) of the client under its prospect and client codes, newest first. */
export function ClientHistoryTab({ clientId }: Readonly<{ clientId: number }>) {
  const history = useQuery({
    queryKey: ['crm', 'history', clientId],
    queryFn: () => clientsApi.history(clientId),
  });
  return (
    <Card title="Audit History" flush>
      <ErrorAlert error={history.error} />
      <DataTable<ClientHistoryEntry>
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(h) => h.id}
        emptyMessage="No history recorded"
        columns={[
          {
            key: 'when',
            header: 'Date and Time',
            kind: 'datetime',
            render: (h) => formatDateTime(h.occurredAt),
          },
          { key: 'who', header: 'User', render: (h) => <UserName login={h.username} withRole /> },
          {
            key: 'action',
            header: 'Action',
            kind: 'status',
            render: (h) => <Tag tone="info">{humanize(h.action)}</Tag>,
          },
          { key: 'summary', header: 'Details', render: (h) => h.summary },
        ]}
      />
    </Card>
  );
}
