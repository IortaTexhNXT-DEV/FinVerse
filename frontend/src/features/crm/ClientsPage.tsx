import { useQuery } from '@tanstack/react-query';
import { FileUp, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { ClientListItem, ClientSearch } from '@/api/clients';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { ClientSearchPanel } from './ClientSearchPanel';
import type { SearchCriteria } from './ClientSearchPanel';
import { QUICK_FILTERS, applyQuickFilter } from './clientLabels';
import type { QuickFilter } from './clientLabels';

/**
 * Clients (BRNB.046/090): search prospects and confirmed clients by any criterion, with quick
 * filters for prospects, confirmed clients and KYC reviews due. Opens the client page.
 */
export default function ClientsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [criteria, setCriteria] = useState<SearchCriteria>({});
  const [quick, setQuick] = useState<QuickFilter>('ALL');
  const [page, setPage] = useState(0);
  const query: ClientSearch = { ...applyQuickFilter(criteria, quick), companyId, page };
  const clients = useQuery({
    queryKey: ['crm', 'clients', query],
    queryFn: () => clientsApi.search(query),
    enabled: companyId > 0,
  });
  const open = (c: ClientListItem) => void navigate(`/crm/clients/${String(c.id)}`);

  return (
    <div className="stack">
      <PageHeader
        section="Clients"
        title="Clients"
        description="Prospects and confirmed clients: search by code, name, TIN, ID, e-mail or mobile and open the complete client record."
        actions={
          <>
            {can('BULK_PROCESS') && can('CLIENT_MAINTAIN') && (
              <Button
                variant="secondary"
                icon={<FileUp size={16} />}
                onClick={() => void navigate('/bulk/CLIENT_CREATE')}
              >
                Bulk upload
              </Button>
            )}
            {can('CLIENT_MAINTAIN') && (
              <Button
                variant="accent"
                icon={<UserPlus size={16} />}
                onClick={() => void navigate('/crm/clients/new')}
              >
                New client
              </Button>
            )}
          </>
        }
      />
      <ClientSearchPanel
        value={criteria}
        onSearch={(c) => {
          setCriteria(c);
          setPage(0);
        }}
      />
      <div className="quick-filters" role="group" aria-label="Quick filters">
        {QUICK_FILTERS.map((f) => (
          <button
            key={f.id}
            type="button"
            className="quick-filter"
            aria-pressed={quick === f.id}
            onClick={() => {
              setQuick(f.id);
              setPage(0);
            }}
          >
            {f.label}
          </button>
        ))}
      </div>
      <ErrorAlert error={clients.error} />
      <Card flush>
        <DataTable<ClientListItem>
          loading={clients.isLoading}
          rows={clients.data?.content ?? []}
          rowKey={(c) => c.id}
          onRowClick={open}
          emptyMessage="No client matches the search. Change the criteria or create a new client."
          columns={[
            { key: 'code', header: 'Code', render: (c) => <span className="mono">{c.code}</span> },
            { key: 'name', header: 'Name', render: (c) => c.displayName },
            { key: 'type', header: 'Type', render: (c) => humanize(c.clientType) },
            { key: 'segment', header: 'Segment', render: (c) => c.marketSegment ?? '' },
            { key: 'bank', header: 'Bank', render: (c) => (c.bankClient ? 'Yes' : 'No') },
            { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
            { key: 'kyc', header: 'KYC', render: (c) => <StatusBadge status={c.kycStatus} /> },
            { key: 'due', header: 'KYC review', render: (c) => formatDate(c.kycReviewDue) },
            { key: 'contact', header: 'Contact', render: (c) => c.mobile ?? c.email ?? '' },
          ]}
        />
        <PageFooter data={clients.data} noun="clients" onPage={setPage} />
      </Card>
    </div>
  );
}
