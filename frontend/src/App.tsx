import { QueryClientProvider } from '@tanstack/react-query';
import { lazy, Suspense } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthProvider';
import { useAuth } from '@/auth/authContext';
import { RequireAuth, RequirePermission } from '@/auth/RequireAuth';
import { AppShell } from '@/components/layout/AppShell';
import { StatusPage } from '@/components/ui/StatusPage';
import { ToastProvider } from '@/components/ui/ToastProvider';
import { WorkspaceProvider } from '@/context/WorkspaceProvider';
import { landingPath } from '@/navigation/access';
import { MODULES } from '@/navigation/modules';
import type { ScreenDef } from '@/navigation/types';
import { createQueryClient } from '@/queryClient';

const LoginPage = lazy(() => import('@/auth/LoginPage'));
const ResetPasswordPage = lazy(() => import('@/auth/ResetPasswordPage'));
const SsoCallbackPage = lazy(() => import('@/auth/SsoCallbackPage'));

const queryClient = createQueryClient();

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
      requiresAll={screen.requiresAll}
      productModule={screen.productModule}
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
                <Route path="/reset-password" element={<ResetPasswordPage />} />
                <Route path="/sso/callback" element={<SsoCallbackPage />} />
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
                  <Route path="*" element={<StatusPage kind="notFound" />} />
                </Route>
              </Routes>
            </Suspense>
          </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
}
