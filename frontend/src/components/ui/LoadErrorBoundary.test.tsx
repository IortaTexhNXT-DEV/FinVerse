import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LoadErrorBoundary } from './LoadErrorBoundary';
import { SlowLoadingFallback } from './SlowLoadingFallback';

function Throws({ error }: Readonly<{ error: Error }>): never {
  throw error;
}

describe('LoadErrorBoundary', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the error message with Reload instead of a blank page', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const onReload = vi.fn();
    render(
      <LoadErrorBoundary onReload={onReload}>
        <Throws error={new Error('broken')} />
      </LoadErrorBoundary>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong loading this page.');
    fireEvent.click(screen.getByRole('button', { name: 'Reload' }));
    expect(onReload).toHaveBeenCalledTimes(1);
  });

  it('says that a new version is available when a script file is missing', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(
      <LoadErrorBoundary>
        <Throws
          error={new TypeError('Failed to fetch dynamically imported module: /assets/x.js')}
        />
      </LoadErrorBoundary>,
    );
    expect(
      screen.getByText('A new version of BIBS is available. Reload to continue.'),
    ).toBeInTheDocument();
  });

  it('clears the error when the route changes', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { rerender } = render(
      <LoadErrorBoundary resetKey="/a">
        <Throws error={new Error('broken')} />
      </LoadErrorBoundary>,
    );
    rerender(
      <LoadErrorBoundary resetKey="/b">
        <p>Next page</p>
      </LoadErrorBoundary>,
    );
    expect(screen.getByText('Next page')).toBeInTheDocument();
  });

  it('renders its content when nothing fails', () => {
    render(
      <LoadErrorBoundary>
        <p>Page</p>
      </LoadErrorBoundary>,
    );
    expect(screen.getByText('Page')).toBeInTheDocument();
  });
});

describe('SlowLoadingFallback', () => {
  afterEach(() => vi.useRealTimers());

  it('offers Reload when loading takes longer than 15 seconds', () => {
    vi.useFakeTimers();
    const onReload = vi.fn();
    render(
      <SlowLoadingFallback onReload={onReload}>
        <span>Loading</span>
      </SlowLoadingFallback>,
    );
    expect(screen.getByText('Loading')).toBeInTheDocument();
    act(() => {
      vi.advanceTimersByTime(14_999);
    });
    expect(screen.queryByRole('button', { name: 'Reload' })).toBeNull();
    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(
      screen.getByText('This page is taking longer than expected to load.'),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Reload' }));
    expect(onReload).toHaveBeenCalledTimes(1);
  });
});
