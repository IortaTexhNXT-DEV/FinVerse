import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import type { Company } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import type { AuthState } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { WorkspaceContext } from '@/context/workspaceContext';

/** Toast spies of the tests. */
const toast = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() };

/**
 * The providers of a Product Maintenance screen in a test: router, queries, a signed-in user with
 * every permission (or the given ones), company 1 and toasts.
 */
export function Providers({
  children,
  username = 'tsuhead',
  permissions,
  path = '/',
}: Readonly<{ children: ReactNode; username?: string; permissions?: string[]; path?: string }>) {
  const auth = {
    user: { username, fullName: username },
    can: (p: string) => permissions === undefined || permissions.includes(p),
  } as unknown as AuthState;
  const workspace = {
    companies: [],
    company: { id: 1 } as Company,
    branches: [],
    branchId: undefined,
    setCompanyId: () => undefined,
    setBranchId: () => undefined,
  };
  return (
    <MemoryRouter initialEntries={[path]}>
      <QueryClientProvider client={new QueryClient()}>
        <AuthContext.Provider value={auth}>
          <WorkspaceContext.Provider value={workspace}>
            <ToastContext.Provider value={toast}>{children}</ToastContext.Provider>
          </WorkspaceContext.Provider>
        </AuthContext.Provider>
      </QueryClientProvider>
    </MemoryRouter>
  );
}
