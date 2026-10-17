import { useQuery } from '@tanstack/react-query';
import { nbadminApi } from '@/api/nbadmin';
import type { SodRule } from '@/api/nbadmin';

const NONE: SodRule[] = [];

/**
 * The separation-of-duties rules for the profile pickers' warnings. A user who may not read them
 * gets none: the rules are still checked when the request is submitted.
 */
export function useSodRules(): readonly SodRule[] {
  const rules = useQuery({
    queryKey: ['nbadmin', 'sod-rules'],
    queryFn: nbadminApi.sodRules,
    retry: false,
  });
  return rules.data ?? NONE;
}
