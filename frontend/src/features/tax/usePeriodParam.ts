import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { today } from '@/utils/format';
import type { Granularity, PeriodChoice } from './taxPeriods';
import { choiceLabel, defaultChoice, parseChoice } from './taxPeriods';

/** Search parameter holding the worksheet period, e.g. `?period=2026-Q3`. */
export const PERIOD_PARAM = 'period';

/**
 * Period of a tax worksheet kept in the URL (`?period=2026-Q3`), so that returning from a
 * drill-down (browser Back) or sharing the link reopens the same period. The URL entry is replaced,
 * not pushed, so Back leaves the worksheet instead of stepping through earlier periods.
 */
export function usePeriodParam(granularity: Granularity) {
  const [params, setParams] = useSearchParams();
  const [choice, setChoiceState] = useState<PeriodChoice>(
    () => parseChoice(params.get(PERIOD_PARAM)) ?? defaultChoice(today(), granularity),
  );
  const setChoice = (next: PeriodChoice) => {
    setChoiceState(next);
    setParams(
      (current) => {
        const updated = new URLSearchParams(current);
        updated.set(PERIOD_PARAM, choiceLabel(next));
        return updated;
      },
      { replace: true },
    );
  };
  return [choice, setChoice] as const;
}
