import { act, renderHook } from '@testing-library/react';
import { useAutosave } from './useAutosave';

describe('draft autosave', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('saves changed drafts on the interval only', async () => {
    const save = vi.fn(() => Promise.resolve());
    const { rerender, result } = renderHook(
      ({ value, enabled }) => useAutosave(value, enabled, save, 1000),
      { initialProps: { value: { a: 1 }, enabled: false } },
    );
    await act(() => vi.advanceTimersByTimeAsync(3000));
    expect(save).not.toHaveBeenCalled();

    rerender({ value: { a: 1 }, enabled: true });
    await act(() => vi.advanceTimersByTimeAsync(1000));
    expect(save).toHaveBeenCalledTimes(1);
    await act(() => vi.advanceTimersByTimeAsync(2000));
    expect(save).toHaveBeenCalledTimes(1);

    rerender({ value: { a: 2 }, enabled: true });
    await act(() => vi.advanceTimersByTimeAsync(1000));
    expect(save).toHaveBeenLastCalledWith({ a: 2 });

    act(() => result.current({ a: 3 }));
    rerender({ value: { a: 3 }, enabled: true });
    await act(() => vi.advanceTimersByTimeAsync(1000));
    expect(save).toHaveBeenCalledTimes(2);
  });

  it('retries after a failed save', async () => {
    const save = vi.fn(() => Promise.reject(new Error('offline')));
    renderHook(() => useAutosave({ a: 1 }, true, save, 1000));
    await act(() => vi.advanceTimersByTimeAsync(2000));
    expect(save).toHaveBeenCalledTimes(2);
  });
});
