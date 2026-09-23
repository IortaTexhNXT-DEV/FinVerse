import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { choiceLabel, parseChoice } from './taxPeriods';
import { usePeriodParam } from './usePeriodParam';

function routerAt(url: string) {
  return function Wrapper({ children }: Readonly<{ children: ReactNode }>) {
    return <MemoryRouter initialEntries={[url]}>{children}</MemoryRouter>;
  };
}

describe('worksheet period in the URL', () => {
  it('parses what choiceLabel writes and rejects anything else', () => {
    const q3 = { year: 2026, granularity: 'QUARTER', index: 3 } as const;
    const sep = { year: 2026, granularity: 'MONTH', index: 9 } as const;
    expect(parseChoice(choiceLabel(q3))).toEqual(q3);
    expect(parseChoice(choiceLabel(sep))).toEqual(sep);
    expect(parseChoice('2026-Q5')).toBeUndefined();
    expect(parseChoice('2026-13')).toBeUndefined();
    expect(parseChoice(null)).toBeUndefined();
  });

  it('reopens the period of the URL, e.g. after Back from a drill-down', () => {
    const { result } = renderHook(() => usePeriodParam('QUARTER'), {
      wrapper: routerAt('/tax/vat?period=2026-Q3'),
    });
    expect(result.current[0]).toEqual({ year: 2026, granularity: 'QUARTER', index: 3 });
  });

  it('writes a new selection to the URL', () => {
    const { result } = renderHook(
      () => ({ period: usePeriodParam('QUARTER'), location: useLocation() }),
      { wrapper: routerAt('/tax/vat?other=1') },
    );
    act(() => result.current.period[1]({ year: 2025, granularity: 'MONTH', index: 12 }));
    expect(result.current.period[0]).toEqual({ year: 2025, granularity: 'MONTH', index: 12 });
    expect(result.current.location.search).toBe('?other=1&period=2025-12');
  });
});
