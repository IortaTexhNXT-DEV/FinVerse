import { useQuery } from '@tanstack/react-query';
import { lovApi } from '@/api/lov';
import { humanize } from '@/utils/format';

interface LovLabelProps {
  /** List of values the code belongs to (e.g. SOURCE_CHANNEL). */
  type: string;
  code: string;
}

/** The label of a list-of-values code (never the code itself); the humanized code while loading. */
export function LovLabel({ type, code }: Readonly<LovLabelProps>) {
  const options = useQuery({
    queryKey: ['lov', type],
    queryFn: () => lovApi.options(type),
    staleTime: 5 * 60_000,
  });
  return <>{options.data?.find((o) => o.code === code)?.label ?? humanize(code)}</>;
}
