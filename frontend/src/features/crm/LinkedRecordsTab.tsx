import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { ClientRecord } from '@/api/clients';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';

/**
 * Linked Records (BRNB.099): accounts, quotations, proposals and other records of the client, and
 * the missing or broken linkages (confirmed client without a party, incomplete data, KYC expired).
 */
export function LinkedRecordsTab({ clientId }: Readonly<{ clientId: number }>) {
  const navigate = useNavigate();
  const view = useQuery({
    queryKey: ['crm', 'records', clientId],
    queryFn: () => clientsApi.records(clientId),
  });
  const warnings = view.data?.warnings ?? [];
  return (
    <div className="stack">
      <ErrorAlert error={view.error} />
      {warnings.length > 0 && (
        <div className="alert warning" role="status">
          <strong>Linkage and data gaps</strong>
          <ul className="warning-list">
            {warnings.map((w) => (
              <li key={w.code}>{w.message}</li>
            ))}
          </ul>
        </div>
      )}
      {view.data !== undefined && warnings.length === 0 && (
        <div className="alert success" role="status">
          No missing linkage: the client, its sub-ledger party and its data are complete.
        </div>
      )}
      <Card title="Records of the client" flush>
        <DataTable<ClientRecord>
          loading={view.isLoading}
          rows={view.data?.records ?? []}
          rowKey={(r) => `${r.kind}:${r.reference}`}
          onRowClick={(r) => {
            if (r.link) {
              void navigate(r.link);
            }
          }}
          emptyMessage="No quotations, proposals or accounts are linked to this client yet."
          columns={[
            { key: 'kind', header: 'Record', render: (r) => r.kind },
            {
              key: 'ref',
              header: 'Reference',
              render: (r) => <span className="mono">{r.reference}</span>,
            },
            { key: 'desc', header: 'Description', render: (r) => r.description ?? '' },
            {
              key: 'status',
              header: 'Status',
              render: (r) => (r.status ? <StatusBadge status={r.status} /> : ''),
            },
            { key: 'date', header: 'Date', render: (r) => formatDate(r.date) },
          ]}
        />
      </Card>
    </div>
  );
}
