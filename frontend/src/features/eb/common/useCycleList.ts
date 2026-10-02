import { useQuery } from '@tanstack/react-query';
import type { ProgrammeView } from '@/api/eb';
import { useCompanyId } from '@/context/workspaceContext';

/** The current cycle of a programme, if any. */
export function currentCycleOf(programme: ProgrammeView) {
  return programme.cycles.find((c) => c.id === programme.currentCycleId);
}

/**
 * A list of records of a cycle (TOR, requests, proposals…), fetched once the cycle is known;
 * `rows` is empty until it has loaded.
 */
export function useCycleList<T>(
  name: string,
  cycleId: number | undefined,
  fetch: (companyId: number, cycleId: number) => Promise<T[]>,
) {
  const companyId = useCompanyId();
  const query = useQuery({
    queryKey: ['eb', name, cycleId],
    queryFn: () => fetch(companyId, cycleId ?? 0),
    enabled: cycleId !== undefined,
  });
  return { ...query, rows: query.data ?? [] };
}
