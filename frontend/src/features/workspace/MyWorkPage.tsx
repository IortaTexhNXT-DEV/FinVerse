import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { workflowApi } from '@/api/workflow';
import type { QueueFilters, WorkItem } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { AssignDialog } from './AssignDialog';
import { queueParamsOf, queueViewOf } from './queueParams';
import type { QueueView } from './queueParams';
import { QueueTable } from './QueueTable';
import { QueueToolbar } from './QueueToolbar';
import { StageTiles, WorkSummary } from './WorkTiles';

/**
 * My Work (BRNB.096/115/080): the queues of the stages your team works, with open, overdue and
 * assigned-to-me counts. Claim items from the team queue, open them to act, and — as a team
 * leader — assign or re-assign them. The filters are kept in the page URL, so a dashboard figure
 * opens the filtered queue and Back returns to it.
 */
export default function MyWorkPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [assigning, setAssigning] = useState<WorkItem | null>(null);
  const view = queueViewOf(params);
  const query: QueueFilters = { ...view, page, companyId };

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
  const setFilter = (patch: Partial<QueueView>) => {
    setPage(0);
    setParams((current) => queueParamsOf({ ...queueViewOf(current), ...patch }));
  };
  const stageCounts = counts.data ?? [];
  const openItem = (item: WorkItem) => {
    if (item.link) {
      void navigate(item.link);
    }
  };

  return (
    <div className="stack">
      <PageHeader
        section="My Work"
        title="My Work"
        description="Items waiting for your team, oldest due first."
      />
      <ErrorAlert error={counts.error ?? queue.error ?? claim.error} />
      <WorkSummary counts={stageCounts} />
      <StageTiles counts={stageCounts} filters={view} onFilter={setFilter} />
      <Card flush callout="work-queue-card">
        <QueueToolbar
          filters={view}
          stageCounts={stageCounts}
          onFilter={setFilter}
          onSearch={() => setPage(0)}
        />
        <QueueTable
          items={queue.data?.content ?? []}
          loading={queue.isLoading}
          canAssign={can('WORK_ASSIGN')}
          claiming={claim.isPending}
          onOpen={openItem}
          onClaim={(item) => claim.mutate(item.id)}
          onAssign={setAssigning}
        />
        <PageFooter data={queue.data} noun="items" onPage={setPage} />
      </Card>
      {assigning && (
        <AssignDialog item={assigning} onClose={() => setAssigning(null)} onDone={refresh} />
      )}
    </div>
  );
}
