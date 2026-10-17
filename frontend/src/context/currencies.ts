import { useQuery } from '@tanstack/react-query';
import { mastersApi } from '@/api/masters';
import { useBaseCurrency } from './workspaceContext';

/**
 * Currency codes a user can pick: the base currency of the selected company first, then the other
 * active currencies of the currency master. No currency list is written into the screens.
 */
export function useCurrencyCodes(): string[] {
  const base = useBaseCurrency();
  const currencies = useQuery({
    queryKey: ['currencies'],
    queryFn: mastersApi.currencies,
    staleTime: 300_000,
  });
  const others = (currencies.data ?? [])
    .filter((c) => c.active && c.code !== base)
    .map((c) => c.code);
  return base === '' ? others : [base, ...others];
}
