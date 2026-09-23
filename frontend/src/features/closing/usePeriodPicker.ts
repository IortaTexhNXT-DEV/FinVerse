import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { periodApi } from '@/api/periods';
import { useCompanyId } from '@/context/workspaceContext';

/** Fiscal year and period selection shared by the closing screens (defaults: latest year). */
export function usePeriodPicker() {
  const companyId = useCompanyId();
  const [yearId, setYearId] = useState<number | undefined>();
  const [periodId, setPeriodId] = useState<number | undefined>();
  const years = useQuery({
    queryKey: ['years', companyId],
    queryFn: () => periodApi.years(companyId),
    enabled: companyId > 0,
  });
  const year = years.data?.find((y) => y.id === yearId) ?? years.data?.[0];
  const periods = useQuery({
    queryKey: ['periods', year?.id],
    queryFn: () => periodApi.periods(year?.id ?? 0),
    enabled: year !== undefined,
  });
  const openPeriods = (periods.data ?? []).filter((p) => p.status !== 'FUTURE');
  const period =
    periods.data?.find((p) => p.id === periodId) ?? openPeriods[openPeriods.length - 1];
  return {
    companyId,
    years: years.data ?? [],
    year,
    periods: periods.data ?? [],
    period,
    selectYear: (id: number) => {
      setYearId(id);
      setPeriodId(undefined);
    },
    selectPeriod: setPeriodId,
  };
}
