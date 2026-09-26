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
import { AssignDialog } from './AssignDialog';
import { QueueTable } from './QueueTable';
import { QueueToolbar } from './QueueToolbar';
import { StageTiles, WorkSummary } from './WorkTiles';

type Filters = Omit<QueueFilters, 'companyId'>;

/**
 * My Work (BRNB.096/115/080): the queues of the stages your team works, with open, overdue and
 * assigned-to-me counts. Claim items from the team queue, open them to act, and — as a team
 * leader — assign or re-assign them.
 */
export default function MyWorkPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<Filters>({ scope: 'ALL' });
  const [assigning, setAssigning] = useState<WorkItem | null>(null);
  const query: QueueFilters = { ...filters, companyId };

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
  const setFilter = (patch: Partial<Filters>) => setFilters({ ...filters, page: 0, ...patch });
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
        description="Items waiting for your team, oldest due first. Claim an item to work it; overdue items are past their stage service level."
      />
      <ErrorAlert error={counts.error ?? queue.error ?? claim.error} />
      <WorkSummary counts={stageCounts} onFilter={setFilter} />
      <StageTiles counts={stageCounts} filters={filters} onFilter={setFilter} />
      <Card>
        <div className="stack">
          <QueueToolbar filters={filters} onFilter={setFilter} />
          <QueueTable
            items={queue.data?.content ?? []}
            loading={queue.isLoading}
            canAssign={can('WORK_ASSIGN')}
            claiming={claim.isPending}
            onOpen={openItem}
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
