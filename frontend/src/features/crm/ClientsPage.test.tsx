import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { clientsApi } from '@/api/clients';
import type { Company } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import { WorkspaceContext } from '@/context/workspaceContext';
import ClientsPage from './ClientsPage';

describe('ClientsPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('keeps the prospect or client code on one line (code column)', async () => {
    vi.spyOn(clientsApi, 'search').mockResolvedValue({
      content: [
        {
          id: 9,
          code: 'PR-2026-000009',
          prospectCode: 'PR-2026-000009',
          displayName: 'Aquino, Lorna Faye',
          clientType: 'INDIVIDUAL',
          status: 'PROSPECT',
          kycStatus: 'NOT_STARTED',
          bankClient: false,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
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
              <ClientsPage />
            </WorkspaceContext.Provider>
          </AuthContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>,
    );
    const code = await screen.findByText('PR-2026-000009');
    expect(code.closest('td')?.classList.contains('col-code')).toBe(true);
  });
});
