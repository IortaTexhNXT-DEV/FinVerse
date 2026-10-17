import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { accountsApi } from '@/api/accounts';
import type { Company } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import { WorkspaceContext } from '@/context/workspaceContext';
import AccountsPage from './AccountsPage';

describe('AccountsPage filters', () => {
  afterEach(() => vi.restoreAllMocks());

  it('holds the filter form in the filter band, as the other work lists do', async () => {
    vi.spyOn(accountsApi, 'search').mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
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
      <MemoryRouter initialEntries={['/accounts?status=DRAFT']}>
        <QueryClientProvider client={new QueryClient()}>
          <AuthContext.Provider value={auth}>
            <WorkspaceContext.Provider value={workspace}>
              <AccountsPage />
            </WorkspaceContext.Provider>
          </AuthContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>,
    );
    const form = await screen.findByRole('form', { name: 'Account filters' });
    expect(form.classList.contains('worklist-filters')).toBe(false);
    expect(form.parentElement?.classList.contains('worklist-filters')).toBe(true);
  });
});
