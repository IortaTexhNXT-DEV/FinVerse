import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { TorView } from '@/api/submitted';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { SUBMITTED_SECTION, policyLink } from '../common/submittedCodes';
import { TorActions } from './IaafActions';

const TABS = [
  { id: 'FOR_APPROVAL', label: 'Pending Approval', statuses: ['FOR_APPROVAL'] },
  { id: 'DRAFT', label: 'Draft and Returned', statuses: ['DRAFT', 'RETURNED'] },
  { id: 'APPROVED', label: 'Approved', statuses: ['APPROVED'] },
  { id: 'RELEASED', label: 'Released', statuses: ['RELEASED'] },
  { id: 'CANCELLED', label: 'Cancelled', statuses: ['CANCELLED'] },
] as const;

type TabId = (typeof TABS)[number]['id'];

/**
 * Terms of Reference (FR-SP-051 to 053): the TOR of the policies above the insurer limits, pending,
 * approved and released to the Account Officer, with the approvers' actions.
 */
export default function TorPage() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'FOR_APPROVAL',
  );
  const statuses = TABS.find((t) => t.id === tab)?.statuses ?? ['FOR_APPROVAL'];
  const list = useQuery({
    queryKey: ['submitted', 'tors', companyId, tab],
    queryFn: () => submittedApi.tors(companyId, statuses),
    enabled: companyId > 0,
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Terms of Reference"
        description="Terms proposed for the policies above the insurer limits and their approval."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable<TorView>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(t) => t.id}
          emptyMessage="No TOR in this tab"
          columns={[
            { key: 'no', header: 'TOR No.', kind: 'code', render: (t) => t.torNo },
            {
              key: 'sbm',
              header: 'Masterlist No.',
              kind: 'code',
              render: (t) =>
                t.policyId === null ? (
                  '—'
                ) : (
                  <Link to={`${policyLink(t.policyId)}?tab=tor`}>{t.sbmNo}</Link>
                ),
            },
            { key: 'assured', header: 'Assured', render: (t) => t.assuredName ?? '—' },
            { key: 'breaches', header: 'Limits Exceeded', render: (t) => t.breaches },
            {
              key: 'ao',
              header: 'Account Officer',
              render: (t) => <UserName login={t.aoUsername} />,
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (t) => <StatusBadge status={t.approval.status} />,
            },
            {
              key: 'at',
              header: 'Submitted',
              kind: 'datetime',
              render: (t) => formatDateTime(t.approval.submittedAt),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (t) => <TorActions tor={t} onDone={refresh} />,
            },
          ]}
        />
      </Card>
    </div>
  );
}
