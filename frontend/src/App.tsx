import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthProvider';
import { useAuth } from '@/auth/authContext';
import { RequireAuth, RequirePermission } from '@/auth/RequireAuth';
import { AppShell } from '@/components/layout/AppShell';
import { ToastProvider } from '@/components/ui/ToastProvider';
import { WorkspaceProvider } from '@/context/WorkspaceProvider';
import { landingPath } from '@/navigation/access';
import { MODULES } from '@/navigation/modules';
import type { ScreenDef } from '@/navigation/types';

const LoginPage = lazy(() => import('@/auth/LoginPage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 15_000 },
  },
});

const screens = MODULES.flatMap((m) => m.screens);

/** A routed screen behind its permission guard; the home page falls back to the landing screen. */
function GuardedScreen({ screen }: Readonly<{ screen: ScreenDef }>) {
  const { can } = useAuth();
  const Screen = screen.component;
  const fallback = screen.path === '/' ? landingPath(screens, can) : undefined;
  return (
    <RequirePermission
      permission={screen.permission}
      alsoPermissions={screen.alsoPermissions}
      fallbackTo={fallback}
    >
      <Screen />
    </RequirePermission>
  );
}

/** Root component: providers, authentication gate and routes generated from the module registry. */
export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <BrowserRouter>
            <Suspense fallback={<span className="spinner" aria-label="Loading" />}>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route
                  element={
                    <RequireAuth>
                      <WorkspaceProvider>
                        <AppShell />
                      </WorkspaceProvider>
                    </RequireAuth>
                  }
                >
                  {screens.map((s) => (
                    <Route key={s.path} path={s.path} element={<GuardedScreen screen={s} />} />
                  ))}
                  <Route path="*" element={<Navigate to="/" replace />} />
                </Route>
              </Routes>
            </Suspense>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}
