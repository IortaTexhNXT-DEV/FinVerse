import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '@/api/catalog';
import { nbReportsApi } from '@/api/nbReports';
import type { Company } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { WorkspaceContext } from '@/context/workspaceContext';
import SalesTargetsPage from './SalesTargetsPage';

describe('SalesTargetsPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('names the sales unit of each target, never its code', async () => {
    vi.spyOn(nbReportsApi, 'targets').mockResolvedValue([
      {
        id: 1,
        unitLevel: 'TEAM',
        unitCode: 'T-CBG1',
        periodFrom: '2026-10-01',
        periodTo: '2026-10-31',
        targetCount: 10,
        targetPremium: 180000,
        targetCommission: 36000,
      },
    ]);
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({
      units: [{ code: 'T-CBG1', name: 'CBG Metro Team 1' }],
    } as unknown as Awaited<ReturnType<typeof catalogApi.salesOrganisation>>);
    const auth = {
      user: null,
      loading: false,
      login: () => Promise.resolve({ expiresAt: '' }),
      completeSignIn: () => undefined,
      logout: () => undefined,
      can: () => true,
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
    render(
      <MemoryRouter>
        <QueryClientProvider client={new QueryClient()}>
          <AuthContext.Provider value={auth}>
            <WorkspaceContext.Provider value={workspace}>
              <ToastContext.Provider
                value={{ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }}
              >
                <SalesTargetsPage />
              </ToastContext.Provider>
            </WorkspaceContext.Provider>
          </AuthContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>,
    );
    expect(await screen.findByText('CBG Metro Team 1')).toBeTruthy();
    expect(screen.queryByText('T-CBG1')).toBeNull();
  });
});
