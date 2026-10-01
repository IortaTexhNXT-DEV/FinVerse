import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '@/api/catalog';
import type { Insurer, InsurerDetail } from '@/api/catalog';
import type { Company } from '@/api/types';
import { WorkspaceContext } from '@/context/workspaceContext';
import { InsurerWithBranch } from './LovLabel';

function show(insurer: string, branch?: string) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const workspace = {
    companies: [],
    company: { id: 1 } as Company,
    branches: [],
    branchId: undefined,
    setCompanyId: () => undefined,
    setBranchId: () => undefined,
  };
  render(
    <QueryClientProvider client={queries}>
      <WorkspaceContext.Provider value={workspace}>
        <p>
          <InsurerWithBranch insurer={insurer} branch={branch} />
        </p>
      </WorkspaceContext.Provider>
    </QueryClientProvider>,
  );
}

describe('InsurerWithBranch', () => {
  afterEach(() => vi.restoreAllMocks());

  it('names the insurer and its branch, never the branch code', async () => {
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue([
      { id: 4, partyCode: 'INS-MGIC', name: 'Mabuhay General Insurance Corp.' } as Insurer,
    ]);
    vi.spyOn(catalogApi, 'insurer').mockResolvedValue({
      insurer: { id: 4 },
      branches: [{ code: 'MKT', name: 'Makati' }],
      commissions: [],
    } as unknown as InsurerDetail);
    show('INS-MGIC', 'MKT');
    expect(await screen.findByText('Mabuhay General Insurance Corp. / Makati')).toBeTruthy();
  });

  it('shows the insurer alone when there is no branch', async () => {
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue([
      { id: 4, partyCode: 'INS-MGIC', name: 'Mabuhay General Insurance Corp.' } as Insurer,
    ]);
    vi.spyOn(catalogApi, 'insurer').mockResolvedValue({
      insurer: { id: 4 },
      branches: [],
      commissions: [],
    } as unknown as InsurerDetail);
    show('INS-MGIC');
    expect(await screen.findByText('Mabuhay General Insurance Corp.')).toBeTruthy();
  });
});
