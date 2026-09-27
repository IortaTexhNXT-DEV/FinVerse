import { useQuery } from '@tanstack/react-query';
import { useContext } from 'react';
import { catalogApi } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import { WorkspaceContext } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';

const STALE = 5 * 60_000;

/** Label lookup of a list of values: returns a function from code to label (never the code). */
export function useLovLabel(type: string): (code: string | null | undefined) => string {
  const options = useQuery({
    queryKey: ['lov', type],
    queryFn: () => lovApi.options(type),
    staleTime: STALE,
  });
  return (code) => {
    if (!code) {
      return '';
    }
    return options.data?.find((o) => o.code === code)?.label ?? humanize(code);
  };
}

/** Name lookup of product lines: returns a function from line code to its name. */
export function useLineName(): (code: string | null | undefined) => string {
  const lines = useQuery({
    queryKey: ['catalog', 'lines'],
    queryFn: catalogApi.lines,
    staleTime: STALE,
  });
  return (code) => {
    if (!code) {
      return '';
    }
    return lines.data?.find((l) => l.code === code)?.name ?? humanize(code);
  };
}

/** Name lookup of insurers: returns a function from insurer party code to its name. */
export function useInsurerName(): (code: string | null | undefined) => string {
  const companyId = useContext(WorkspaceContext)?.company?.id ?? 0;
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    staleTime: STALE,
    enabled: companyId > 0,
  });
  return (code) => {
    if (!code) {
      return '';
    }
    return insurers.data?.find((i) => i.partyCode === code)?.name ?? code;
  };
}
