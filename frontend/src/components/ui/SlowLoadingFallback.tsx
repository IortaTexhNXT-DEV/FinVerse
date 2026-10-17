import { useEffect, useState, type ReactNode } from 'react';
import { LoadErrorPage } from './LoadErrorBoundary';

export const SLOW_LOAD_MS = 15_000;

/**
 * Loading placeholder of a lazily loaded page. When loading takes longer than {@link SLOW_LOAD_MS},
 * it offers Reload instead of a never-ending placeholder.
 */
export function SlowLoadingFallback({
  children,
  afterMs = SLOW_LOAD_MS,
  onReload,
}: Readonly<{ children: ReactNode; afterMs?: number; onReload?: () => void }>) {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), afterMs);
    return () => clearTimeout(timer);
  }, [afterMs]);
  if (!slow) {
    return <>{children}</>;
  }
  return (
    <LoadErrorPage
      title="This page is taking longer than expected to load."
      text="Check your connection and reload the page to continue."
      onReload={onReload}
    />
  );
}
