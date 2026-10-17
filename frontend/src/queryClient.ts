import { MutationCache, QueryClient } from '@tanstack/react-query';

/**
 * The query client of the application. Every successful business action (a mutation anywhere)
 * marks the workflow headers stale, so the stepper, the current stage and the stage actions of a
 * record are reloaded after Approve, Post, Book or any other action, on the page that ran it and on
 * the next page that shows the same record (the header and the stepper never disagree).
 */
export function createQueryClient(): QueryClient {
  const client: QueryClient = new QueryClient({
    mutationCache: new MutationCache({
      onSuccess: () => client.invalidateQueries({ queryKey: ['workflow'] }),
    }),
    defaultOptions: {
      queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 15_000 },
    },
  });
  return client;
}
