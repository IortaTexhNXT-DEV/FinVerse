import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import { WordCopyOffer } from '@/components/broking/WordCopyOffer';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

/**
 * Authenticated layout: header, permission-filtered sidebar and routed content, with the Word copy
 * offer of downloaded documents.
 */
export function AppShell() {
  return (
    <div className="app-shell">
      <Header />
      <Sidebar />
      <main className="app-main" id="main-content">
        <Suspense fallback={<span className="spinner" aria-label="Loading screen" />}>
          <Outlet />
        </Suspense>
      </main>
      <WordCopyOffer />
    </div>
  );
}
