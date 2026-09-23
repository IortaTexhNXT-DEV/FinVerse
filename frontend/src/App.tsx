import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthProvider';
import { RequireAuth, RequirePermission } from '@/auth/RequireAuth';
import { AppShell } from '@/components/layout/AppShell';
import { ToastProvider } from '@/components/ui/ToastProvider';
import { WorkspaceProvider } from '@/context/WorkspaceProvider';
import { MODULES } from '@/navigation/modules';

const LoginPage = lazy(() => import('@/auth/LoginPage'));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 15_000 },
  },
});

const screens = MODULES.flatMap((m) => m.screens);

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
                  {screens.map((s) => {
                    const Screen = s.component;
                    return (
                      <Route
                        key={s.path}
                        path={s.path}
                        element={
                          <RequirePermission permission={s.permission}>
                            <Screen />
                          </RequirePermission>
                        }
                      />
                    );
                  })}
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
