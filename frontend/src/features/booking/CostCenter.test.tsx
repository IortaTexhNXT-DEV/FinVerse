import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '@/api/catalog';
import type { Company } from '@/api/types';
import { WorkspaceContext } from '@/context/workspaceContext';
import { CostCenter, JournalLines } from './BookingParts';

describe('CostCenter', () => {
  afterEach(() => vi.restoreAllMocks());

  it('names the department of the cost center, never its code', async () => {
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({
      units: [{ code: 'CBG-NCR', name: 'Consumer Banking - NCR' }],
    } as unknown as Awaited<ReturnType<typeof catalogApi.salesOrganisation>>);
    const workspace = {
      companies: [],
      company: { id: 1 } as Company,
      branches: [],
      branchId: undefined,
      setCompanyId: () => undefined,
      setBranchId: () => undefined,
    };
    render(
      <QueryClientProvider client={new QueryClient()}>
        <WorkspaceContext.Provider value={workspace}>
          <CostCenter costCenter="NB-CBG-M" department="CBG-NCR" />
        </WorkspaceContext.Provider>
      </QueryClientProvider>,
    );
    expect(await screen.findByText('Consumer Banking - NCR')).toBeTruthy();
    expect(screen.getByText('NB-CBG-M')).toBeTruthy();
    expect(screen.queryByText('CBG-NCR')).toBeNull();
  });

  it('names the insurer of the journal preview lines', async () => {
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue([
      { id: 4, partyCode: 'INS-MGIC', name: 'Mabuhay General Insurance Corp.' },
    ] as unknown as Awaited<ReturnType<typeof catalogApi.insurers>>);
    render(
      <QueryClientProvider client={new QueryClient()}>
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
          <JournalLines
            lines={[
              {
                insurerCode: 'INS-MGIC',
                accountCode: '2210',
                accountName: 'Payable to Insurance Companies',
                side: 'CREDIT',
                amount: 14315.88,
              } as unknown as Parameters<typeof JournalLines>[0]['lines'][number],
            ]}
          />
        </WorkspaceContext.Provider>
      </QueryClientProvider>,
    );
    expect(await screen.findByText('Mabuhay General Insurance Corp.')).toBeTruthy();
  });
});
