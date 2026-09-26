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

const WORKFLOW = 'PM_PACKAGE_REQUEST';

type Filters = Omit<QueueFilters, 'companyId'>;

/**
 * TSU Workbench for packages (BRPM.009-015): the package request queues of the TSU Officer, Team
 * Lead and Head, one tile per stage (review, approval, negotiation, terms review, requirements),
 * oldest due first. Claim a request to work it; team leads assign requests.
 */
export default function PackageTsuWorkbenchPage() {
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
        section="Product Maintenance"
        title="TSU Workbench"
        description="Package requests waiting for the Technical Services Unit: review and recommendation, approval, negotiation with the insurers, terms review and requirements."
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
