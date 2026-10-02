import type { QueryClient, QueryKey } from '@tanstack/react-query';
import { workflowKey } from './workflowKey';

/**
 * After an action on a record with a workflow: shows the record the action returned at once and
 * reloads both the record and its workflow panel, so the header status and the stepper always
 * come from the same state (a cached draft is never shown next to a submitted stage).
 */
export async function refreshRecord(
  queryClient: QueryClient,
  recordKey: QueryKey,
  entityType: string,
  entityId: string | number,
  record?: unknown,
) {
  if (record !== undefined) {
    queryClient.setQueryData(recordKey, record);
  }
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: recordKey }),
    queryClient.invalidateQueries({ queryKey: workflowKey(entityType, entityId) }),
    queryClient.invalidateQueries({ queryKey: ['workflow', 'queue'] }),
  ]);
}
