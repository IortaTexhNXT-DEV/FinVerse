import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { IaafView } from '@/api/submitted';
import { LovLabel } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';
import { SBM_LOV, SUBMITTED_SECTION, policyLink } from '../common/submittedCodes';
import { IaafActions } from './IaafActions';

const TABS = [
  { id: 'FOR_APPROVAL', label: 'For Approval', statuses: ['FOR_APPROVAL'] },
  { id: 'DRAFT', label: 'Draft and Returned', statuses: ['DRAFT', 'RETURNED'] },
  { id: 'APPROVED', label: 'Approved', statuses: ['APPROVED'] },
  { id: 'ISSUED', label: 'Sent', statuses: ['ISSUED'] },
  { id: 'CANCELLED', label: 'Cancelled', statuses: ['CANCELLED'] },
] as const;

type TabId = (typeof TABS)[number]['id'];

/**
 * Reviews & IAAF (FR-SP-040, 041): the IAAF by status with their approval level, and the actions
 * of the preparer and the approvers. The reviews are recorded on the policy record.
 */
export default function IaafPage() {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'FOR_APPROVAL',
  );
  const statuses = TABS.find((t) => t.id === tab)?.statuses ?? ['FOR_APPROVAL'];
  const list = useQuery({
    queryKey: ['submitted', 'iaafs', companyId, tab],
    queryFn: () => submittedApi.iaafs(companyId, statuses),
    enabled: companyId > 0,
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Reviews & IAAF"
        description="Insurance adequacy assessments of the reviewed policies and their approval."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable<IaafView>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(i) => i.id}
          emptyMessage="No IAAF in this tab"
          columns={[
            { key: 'no', header: 'IAAF No.', kind: 'code', render: (i) => i.iaafNo },
            {
              key: 'sbm',
              header: 'Masterlist No.',
              kind: 'code',
              render: (i) => <Link to={`${policyLink(i.policyId)}?tab=review`}>{i.sbmNo}</Link>,
            },
            { key: 'assured', header: 'Assured', render: (i) => i.assuredName },
            {
              key: 'segment',
              header: 'Segment',
              defaultHidden: true,
              render: (i) => <LovLabel type={SBM_LOV.segment} code={i.segment} />,
            },
            {
              key: 'si',
              header: 'Sum Insured',
              kind: 'amount',
              render: (i) => (i.sumInsured === null ? '—' : formatAmount(i.sumInsured)),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (i) => <StatusBadge status={i.approval.status} />,
            },
            {
              key: 'level',
              header: 'Level',
              defaultHidden: true,
              render: (i) =>
                i.approval.totalLevels === 0
                  ? '—'
                  : `${String(i.approval.currentLevel)} of ${String(i.approval.totalLevels)}`,
            },
            {
              key: 'by',
              header: 'Prepared By',
              render: (i) => <UserName login={i.approval.preparedBy} />,
            },
            {
              key: 'at',
              header: 'Submitted',
              kind: 'datetime',
              render: (i) => formatDateTime(i.approval.submittedAt),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (i) => <IaafActions iaaf={i} onDone={refresh} />,
            },
          ]}
        />
      </Card>
    </div>
  );
}
