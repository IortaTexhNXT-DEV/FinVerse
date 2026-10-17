import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ScanSearch } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { submittedApi } from '@/api/submitted';
import type { RenewalRow } from '@/api/submitted';
import { useAuth } from '@/auth/authContext';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useTabParam } from '@/components/ui/useTabParam';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { SBM_LOV, SUBMITTED_SECTION, policyLink } from '../common/submittedCodes';
import { ReassignDialog } from './ReassignDialog';

const TABS = [
  { id: 'ALL', label: 'All', statuses: [] as string[] },
  { id: 'HANDED_OFF', label: 'With Renewal', statuses: ['HANDED_OFF'] },
  { id: 'PENDING', label: 'Pending', statuses: ['PENDING'] },
  { id: 'REFUSED', label: 'Refused', statuses: ['REFUSED'] },
] as const;

type TabId = (typeof TABS)[number]['id'];

/**
 * Renewal Work List (FR-SP-060 to 064): the records handed to Renewal with the insurer assigned by
 * the rules, the hold cover and the outcome; the expiry scan started by hand and the re-assignment
 * of an insurer that has not accepted.
 */
export default function RenewalWorkListPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useTabParam<TabId>(
    TABS.map((t) => t.id),
    'ALL',
  );
  const [page, setPage] = useState(0);
  const [reassign, setReassign] = useState<RenewalRow>();
  const statuses = TABS.find((t) => t.id === tab)?.statuses ?? [];
  const list = useQuery({
    queryKey: ['submitted', 'renewals', companyId, tab, page],
    queryFn: () => submittedApi.renewals(companyId, statuses, page),
    enabled: companyId > 0,
  });
  const scan = useMutation({
    mutationFn: () => submittedApi.scan(companyId),
    onSuccess: (s) => {
      toast.success(
        `${String(s.handedOff)} handed to Renewal, ${String(s.noInsurer)} without an insurer rule, ${String(s.replayed)} pending taken`,
      );
      void queryClient.invalidateQueries({ queryKey: ['submitted'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="Renewal Work List"
        description="Submitted policies handed to Renewal with their insurer, hold cover and outcome."
        actions={
          can('SBM_PROCESS') && (
            <ConfirmButton
              icon={<ScanSearch size={16} />}
              confirm={{
                title: 'Scan Now',
                effect:
                  'The policies For Renewal near their expiry are handed to Renewal now, as the nightly scan does.',
              }}
              onConfirm={() => scan.mutateAsync()}
            >
              Scan Now
            </ConfirmButton>
          )
        }
      />
      <Tabs
        tabs={TABS}
        active={tab}
        onChange={(t) => {
          setTab(t);
          setPage(0);
        }}
      />
      <ErrorAlert error={list.error ?? scan.error} onRetry={() => void list.refetch()} />
      <Card flush>
        <DataTable<RenewalRow>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No record handed to Renewal in this tab"
          columns={[
            {
              key: 'sbm',
              header: 'Masterlist No.',
              kind: 'code',
              render: (r) => <Link to={`${policyLink(r.policyId)}?tab=renewal`}>{r.sbmNo}</Link>,
            },
            { key: 'assured', header: 'Assured', render: (r) => r.assuredName },
            {
              key: 'segment',
              header: 'Segment',
              render: (r) => <LovLabel type={SBM_LOV.segment} code={r.segment} />,
            },
            {
              key: 'expiry',
              header: 'Expiry',
              kind: 'date',
              render: (r) => formatDate(r.expiryDate),
            },
            {
              key: 'insurer',
              header: 'Insurer Assigned',
              render: (r) => <InsurerName code={r.insurerAssigned} />,
            },
            {
              key: 'handoff',
              header: 'Hand-off',
              kind: 'status',
              render: (r) => <StatusBadge status={r.handoffStatus} />,
            },
            { key: 'ref', header: 'Renewal', kind: 'code', render: (r) => r.renewalRef ?? '—' },
            {
              key: 'hold',
              header: 'Hold Cover',
              defaultHidden: true,
              kind: 'date',
              render: (r) => formatDate(r.holdCoverOn),
            },
            {
              key: 'accepted',
              header: 'Accepted',
              defaultHidden: true,
              kind: 'date',
              render: (r) => formatDate(r.insurerAcceptedOn),
            },
            {
              key: 'outcome',
              header: 'Outcome',
              render: (r) => (r.outcome ? <StatusBadge status={r.outcome} /> : '—'),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (r) =>
                can('SBM_PROCESS') && r.insurerAcceptedOn === null && r.outcome === null ? (
                  <Button variant="ghost" onClick={() => setReassign(r)}>
                    Re-assign
                  </Button>
                ) : null,
            },
          ]}
          footer={<PageFooter data={list.data} noun="records" onPage={setPage} />}
        />
      </Card>
      {reassign && <ReassignDialog row={reassign} onClose={() => setReassign(undefined)} />}
    </div>
  );
}
