import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TSU_STATUS, renewalProposalApi } from '@/api/renewalProposal';
import type { TsuView } from '@/api/renewalProposal';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';

const TABS = [
  { id: 'PENDING_TL_APPROVAL', label: 'Pending Team Lead Approval' },
  { id: 'APPROVED', label: 'Approved' },
  { id: 'FOR_TSU_PROCESSING', label: 'For TSU Processing' },
  { id: 'COMPLETED', label: 'Completed' },
] as const;

type TabId = (typeof TABS)[number]['id'];

/**
 * TSU Requests (FRRN.018.01): the quotation requests of the renewal accounts For Quotation by
 * status - waiting for the Team Lead, approved for a TSU Officer, in TSU processing and completed;
 * a request opens its renewal account.
 */
export default function TsuQueuePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [tab, setTab] = useState<TabId>('PENDING_TL_APPROVAL');
  const rows = useQuery({
    queryKey: ['renewal', 'tsu-queue', companyId, tab],
    queryFn: () => renewalProposalApi.tsuQueue(companyId, tab),
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="TSU Requests"
        description="Quotation requests of the renewal accounts For Quotation."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Card flush>
        <ErrorAlert error={rows.error} />
        <DataTable<TsuView>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => r.requestNo}
          onRowClick={(r) =>
            void navigate(`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`)
          }
          emptyMessage="No TSU request"
          columns={[
            { key: 'no', header: 'Request', kind: 'code', render: (r) => r.requestNo },
            { key: 'ref', header: 'Reference Number', render: (r) => r.renewalRef },
            { key: 'client', header: 'Client', render: (r) => r.clientName },
            { key: 'risk', header: 'Risk Code', render: (r) => r.riskCode ?? '' },
            { key: 'status', header: 'Status', render: (r) => TSU_STATUS[r.status] ?? r.status },
            { key: 'officer', header: 'TSU Officer', render: (r) => r.tsuOfficer ?? '' },
          ]}
        />
      </Card>
    </div>
  );
}
