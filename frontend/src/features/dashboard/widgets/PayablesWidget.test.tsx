import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { dashboardApi } from '@/api/dashboard';
import type { Company } from '@/api/types';
import { WorkspaceContext } from '@/context/workspaceContext';
import { PayablesWidget } from './PayablesWidget';

describe('payables widget', () => {
  it('counts the open items in words', async () => {
    vi.spyOn(dashboardApi, 'payables').mockResolvedValue({
      overdue: 10,
      dueIn7Days: 0,
      dueIn30Days: 0,
      total: 10,
      openItems: 1,
    } as Awaited<ReturnType<typeof dashboardApi.payables>>);
    const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queries}>
        <WorkspaceContext.Provider
          value={{
            companies: [],
            company: { id: 1 } as Company,
            branches: [],
            branchId: undefined,
            setCompanyId: () => undefined,
            setBranchId: () => undefined,
          }}
        >
          <PayablesWidget enabled />
        </WorkspaceContext.Provider>
      </QueryClientProvider>,
    );
    expect(await screen.findByText('1 open item')).toBeInTheDocument();
  });
});
