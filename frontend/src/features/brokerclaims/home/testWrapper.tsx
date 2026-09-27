import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import type { Company, UserProfile } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import type { AuthState } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { WorkspaceContext } from '@/context/workspaceContext';

/** Providers of a Claims Handling screen in tests: queries, user rights, company and toasts. */
export function claimsWrapper(
  permissions: ReadonlySet<string>,
  username = 'clmofficer',
  route = '/',
): (children: ReactNode) => ReactNode {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const auth: AuthState = {
    user: {
      id: 1,
      username,
      fullName: username,
      roles: [],
      permissions: [],
    } as unknown as UserProfile,
    loading: false,
    login: () => Promise.resolve(),
    logout: () => undefined,
    can: (p) => permissions.has(p),
    passwordChange: null,
    passwordChanged: () => undefined,
  };
  const workspace = {
    companies: [],
    company: { id: 1 } as Company,
    branches: [],
    branchId: undefined,
    setCompanyId: () => undefined,
    setBranchId: () => undefined,
  };
  const toast = { success: () => undefined, error: () => undefined };
  return function Wrapper(children: ReactNode) {
    return (
      <MemoryRouter initialEntries={[route]}>
        <QueryClientProvider client={queries}>
          <AuthContext.Provider value={auth}>
            <WorkspaceContext.Provider value={workspace}>
              <ToastContext.Provider value={toast}>{children}</ToastContext.Provider>
            </WorkspaceContext.Provider>
          </AuthContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>
    );
  };
}
