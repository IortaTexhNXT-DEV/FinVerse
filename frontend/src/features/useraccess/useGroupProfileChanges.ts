import { useQueries } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest } from '@/api/nbadmin';
import { OPEN_STATUSES } from './groupProfileChange';

/**
 * The open group-profile changes the viewer may see (drafts only their maker's), newest first, and
 * the one of each group profile.
 */
export function useOpenChanges(): {
  changes: AccessRequest[];
  byProfile: Map<string, AccessRequest>;
  loading: boolean;
  error: unknown;
} {
  const results = useQueries({
    queries: OPEN_STATUSES.map((status) => ({
      queryKey: ['nbadmin', 'requests', 'open', status],
      queryFn: () => nbadminApi.requests({ scope: 'ALL', status, groupProfiles: true }),
    })),
  });
  const changes = results.flatMap((r) => r.data?.content ?? []).sort((a, b) => b.id - a.id);
  const byProfile = new Map<string, AccessRequest>();
  for (const c of changes) {
    if (c.roleCode !== undefined && !byProfile.has(c.roleCode)) {
      byProfile.set(c.roleCode, c);
    }
  }
  return {
    changes,
    byProfile,
    loading: results.some((r) => r.isLoading),
    error: results.find((r) => r.error !== null)?.error ?? null,
  };
}
