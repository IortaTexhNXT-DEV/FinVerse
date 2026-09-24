import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowApi } from '@/api/workflow';
import type { QueueFilters, WorkItem } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { AssignDialog } from '@/features/workspace/AssignDialog';
import { QueueTable } from '@/features/workspace/QueueTable';
import { StageTiles } from '@/features/workspace/WorkTiles';

const WORKFLOW = 'NB_PROPOSAL';

type Filters = Omit<QueueFilters, 'companyId'>;

/**
 * TSU workbench (BRNB.006/008/009): the proposal-request queues of the Technical Services Unit,
 * one tile per stage (with TSU, quotation slip, terms, proposal slip). Claim a PRF, open it to
 * work the slips and the insurer responses, and — as a team leader — assign PRFs.
 */
export default function TsuWorkbenchPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<Filters>({ workflow: WORKFLOW, scope: 'ALL' });
  const [assigning, setAssigning] = useState<WorkItem | null>(null);
  const query: QueueFilters = { ...filters, workflow: WORKFLOW, companyId };
  const counts = useQuery({
    queryKey: ['workflow', 'counts', companyId],
    queryFn: () => workflowApi.counts(companyId),
    enabled: companyId > 0,
  });
  const queue = useQuery({
    queryKey: ['workflow', 'queue', query],
    queryFn: () => workflowApi.queue(query),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['workflow'] });
  const claim = useMutation({
    mutationFn: workflowApi.claim,
    onSuccess: async (item) => {
      await refresh();
      toast.success(`${item.reference} is now yours`);
    },
  });
  const stageCounts = (counts.data ?? []).filter((c) => c.workflowCode === WORKFLOW);
  const setFilter = (patch: Partial<Filters>) => setFilters({ ...filters, page: 0, ...patch });
  return (
    <div className="stack">
      <PageHeader
        section="Non-Package Management"
        title="TSU Workbench"
        description="Proposal requests waiting for the Technical Services Unit, oldest due first. Claim a PRF to prepare its quotation slip, key in the insurer terms and prepare the proposal slip."
      />
      <ErrorAlert error={counts.error ?? queue.error ?? claim.error} />
      <StageTiles counts={stageCounts} filters={filters} onFilter={setFilter} />
      <Card>
        <div className="stack">
          <QueueTable
            items={queue.data?.content ?? []}
            loading={queue.isLoading}
            canAssign={can('WORK_ASSIGN')}
            claiming={claim.isPending}
            onOpen={(item) => item.link && void navigate(item.link)}
            onClaim={(item) => claim.mutate(item.id)}
            onAssign={setAssigning}
          />
          <PageFooter
            data={queue.data}
            noun="items"
            onPage={(page) => setFilters({ ...filters, page })}
          />
        </div>
      </Card>
      {assigning && (
        <AssignDialog item={assigning} onClose={() => setAssigning(null)} onDone={refresh} />
      )}
    </div>
  );
}
