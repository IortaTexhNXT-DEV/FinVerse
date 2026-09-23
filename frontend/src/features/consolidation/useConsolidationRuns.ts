import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { consolidationApi } from '@/api/consolidation';
import type { ConsolidationRun } from '@/api/consolidation';
import { useToast } from '@/components/ui/toastContext';

/**
 * Runs of a consolidation group: list, selected run (defaults to the latest live run), run and
 * finalize actions.
 */
export function useConsolidationRuns(groupId: number, asOf: string) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [runId, setRunId] = useState<number | undefined>();
  const runs = useQuery({
    queryKey: ['con-runs', groupId],
    queryFn: () => consolidationApi.runs(groupId),
    enabled: groupId > 0,
  });
  const live = (runs.data ?? []).find((r) => r.status !== 'CANCELLED');
  const selectedRunId = runId ?? live?.id ?? 0;
  const run = useQuery({
    queryKey: ['con-run', selectedRunId],
    queryFn: () => consolidationApi.get(selectedRunId),
    enabled: selectedRunId > 0,
  });
  const execute = useMutation({
    mutationFn: () => consolidationApi.run(groupId, asOf),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['con-runs'] });
      setRunId(r.id);
      toast.success(`${r.runNo} calculated`);
    },
  });
  const finalize = useMutation({
    mutationFn: (r: ConsolidationRun) => consolidationApi.finalize(r.id),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['con-runs'] });
      await queryClient.invalidateQueries({ queryKey: ['con-run'] });
      toast.success(`${r.runNo} is final`);
    },
  });
  return {
    runs: runs.data ?? [],
    loading: runs.isLoading,
    run: run.data,
    error: execute.error ?? finalize.error ?? run.error,
    running: execute.isPending,
    select: setRunId,
    execute: () => execute.mutate(),
    finalize: (r: ConsolidationRun) => finalize.mutate(r),
  };
}
