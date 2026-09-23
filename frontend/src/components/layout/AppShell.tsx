import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

/** Authenticated layout: header, permission-filtered sidebar and routed content. */
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
    </div>
  );
}
