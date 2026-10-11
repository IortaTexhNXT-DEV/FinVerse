import { Suspense } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { LoadErrorBoundary } from '@/components/ui/LoadErrorBoundary';
import { SlowLoadingFallback } from '@/components/ui/SlowLoadingFallback';
import { WordCopyOffer } from '@/components/broking/WordCopyOffer';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

/**
 * Authenticated layout: header, permission-filtered sidebar and routed content, with the Word copy
 * offer of downloaded documents.
 */
export function AppShell() {
  const { pathname } = useLocation();
  return (
    <div className="app-shell">
      <Header />
      <Sidebar />
      <main className="app-main" id="main-content">
        <LoadErrorBoundary resetKey={pathname}>
          <Suspense
            fallback={
              <SlowLoadingFallback>
                <span className="spinner" aria-label="Loading screen" />
              </SlowLoadingFallback>
            }
          >
            <Outlet />
          </Suspense>
        </LoadErrorBoundary>
      </main>
      <WordCopyOffer />
    </div>
  );
}
