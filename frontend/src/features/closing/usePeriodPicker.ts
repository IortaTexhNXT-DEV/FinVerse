import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { periodApi } from '@/api/periods';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { defaultPeriod, defaultYear } from './periodDefaults';

/**
 * Fiscal year and period selection shared by the closing screens. Defaults: the year and period
 * containing today (see periodDefaults), not the newest year of the list.
 */
export function usePeriodPicker() {
  const companyId = useCompanyId();
  const [yearId, setYearId] = useState<number | undefined>();
  const [periodId, setPeriodId] = useState<number | undefined>();
  const years = useQuery({
    queryKey: ['years', companyId],
    queryFn: () => periodApi.years(companyId),
    enabled: companyId > 0,
  });
  const year = years.data?.find((y) => y.id === yearId) ?? defaultYear(years.data ?? [], today());
  const periods = useQuery({
    queryKey: ['periods', year?.id],
    queryFn: () => periodApi.periods(year?.id ?? 0),
    enabled: year !== undefined,
  });
  const period =
    periods.data?.find((p) => p.id === periodId) ?? defaultPeriod(periods.data ?? [], today());
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
