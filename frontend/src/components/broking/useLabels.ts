import { useQuery } from '@tanstack/react-query';
import { useContext } from 'react';
import { catalogApi } from '@/api/catalog';
import { productCatalogApi } from '@/api/productCatalog';
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

/**
 * Name lookup of cover types: returns a function from a line and cover type code to the cover type
 * name ("Comprehensive"); the humanized code while the list loads.
 */
export function useCoverTypeName(): (
  line: string | null | undefined,
  code: string | null | undefined,
) => string {
  const coverTypes = useQuery({
    queryKey: ['catalog', 'cover-types'],
    queryFn: catalogApi.coverTypes,
    staleTime: STALE,
  });
  return (line, code) => {
    if (!code) {
      return '';
    }
    const all = coverTypes.data ?? [];
    const found =
      all.find((c) => c.code === code && (!line || c.lineCode === line)) ??
      all.find((c) => c.code === code);
    return found?.name ?? humanize(code);
  };
}

/**
 * Name lookup of the coverages and perils of a line: returns a function from coverage code to its
 * name ("Fire and Lightning"); the code itself when the line has no such coverage.
 */
export function useCoverageName(
  line: string | null | undefined,
): (code: string | null | undefined) => string {
  const coverages = useQuery({
    queryKey: ['catalog', 'coverages', line ?? ''],
    queryFn: () => productCatalogApi.coverages(line ?? undefined),
    staleTime: STALE,
  });
  return (code) => {
    if (!code) {
      return '';
    }
    return coverages.data?.find((c) => c.code === code)?.name ?? code;
  };
}

/** Name lookup of products: returns a function from product code to its name (the code while loading). */
export function useProductName(): (code: string | null | undefined) => string {
  const products = useQuery({
    queryKey: ['catalog', 'products', 'names'],
    queryFn: () => catalogApi.products(),
    staleTime: STALE,
  });
  return (code) => {
    if (!code) {
      return '';
    }
    return products.data?.find((p) => p.code === code)?.name ?? code;
  };
}

/**
 * Name lookup of sales units (regions, departments, teams): returns a function from unit code to
 * its name ("CBG Metro Team 1"); the code while the organisation loads.
 */
export function useSalesUnitName(): (code: string | null | undefined) => string {
  const companyId = useContext(WorkspaceContext)?.company?.id ?? 0;
  const org = useQuery({
    queryKey: ['catalog', 'sales-organisation', companyId],
    queryFn: () => catalogApi.salesOrganisation(companyId),
    staleTime: STALE,
    enabled: companyId > 0,
  });
  return (code) => {
    if (!code) {
      return '';
    }
    return org.data?.units.find((u) => u.code === code)?.name ?? code;
  };
}
