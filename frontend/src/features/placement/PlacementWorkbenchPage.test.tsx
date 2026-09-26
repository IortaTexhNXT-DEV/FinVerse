import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { issuanceApi } from '@/api/issuance';
import { placementApi } from '@/api/placement';
import type { WorkbenchRow } from '@/api/placement';
import type { Company } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { WorkspaceContext } from '@/context/workspaceContext';
import IssuanceWorkbenchPage from '@/features/issuance/IssuanceWorkbenchPage';
import PlacementWorkbenchPage from './PlacementWorkbenchPage';

const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
});

const row = (arn: string, status: string): WorkbenchRow => ({
  accountId: 1,
  arn,
  clientId: 1,
  clientCode: 'CL-2026-900001',
  clientName: `Client ${arn}`,
  status,
  productCode: 'PAR01',
  lineCode: 'PROPERTY',
  department: 'CBG-NCR',
  directPayment: false,
  paymentStatus: 'PAID',
});

function wrap(children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve(),
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
  return (
    <MemoryRouter>
      <QueryClientProvider client={queries}>
        <AuthContext.Provider value={auth}>
          <WorkspaceContext.Provider value={workspace}>
            <ToastContext.Provider value={{ success: vi.fn(), error: vi.fn() }}>
              {children}
            </ToastContext.Provider>
          </WorkspaceContext.Provider>
        </AuthContext.Provider>
      </QueryClientProvider>
    </MemoryRouter>
  );
}

describe('placement and issuance workbenches', () => {
  afterEach(() => vi.restoreAllMocks());

  it('generates slips for the selected accounts once every prerequisite is met', async () => {
    const user = userEvent.setup();
    vi.spyOn(placementApi, 'counts').mockResolvedValue({
      awaitingPayment: 1,
      readyForPlacement: 2,
      placed: 0,
      returnedByInsurer: 1,
      holdCoverExpiring: 0,
      placementCancelled: 0,
      policyIssued: 0,
      booked: 0,
    });
    vi.spyOn(placementApi, 'workbench').mockResolvedValue(
      page([
        row('ARN-2026-000001', 'READY_FOR_PLACEMENT'),
        row('ARN-2026-000002', 'POLICY_ISSUED'),
      ]),
    );
    vi.spyOn(placementApi, 'readiness').mockResolvedValue([
      {
        arn: 'ARN-2026-000001',
        clientName: 'Client',
        status: 'READY_FOR_PLACEMENT',
        ready: true,
        unmet: [],
      },
    ]);
    const generate = vi.spyOn(placementApi, 'generateSlips').mockResolvedValue([]);
    render(wrap(<PlacementWorkbenchPage />));
    expect(await screen.findByText('Client ARN-2026-000001')).toBeInTheDocument();
    const place = screen.getByRole('button', { name: /For Placement$/ });
    expect(place).toBeDisabled();
    await user.click(screen.getByLabelText('Select ARN-2026-000001'));
    expect(place).toBeEnabled();
    await user.click(place);
    const confirm = await screen.findByRole('button', { name: /Generate Slips/ });
    await waitFor(() => expect(confirm).toBeEnabled());
    await user.click(confirm);
    await waitFor(() => expect(generate).toHaveBeenCalledWith(1, ['ARN-2026-000001']));
  }, 20_000);

  it('lists the issuance work by tab', async () => {
    const user = userEvent.setup();
    vi.spyOn(issuanceApi, 'counts').mockResolvedValue({
      awaitingPolicy: 1,
      toReview: 0,
      readyToDispatch: 0,
      adviceToGenerate: 0,
    });
    const workbench = vi.spyOn(issuanceApi, 'workbench').mockResolvedValue(
      page([
        {
          accountId: 1,
          arn: 'ARN-2026-000003',
          clientCode: 'CL-1',
          clientName: 'Issued Client',
          status: 'PLACED',
          productCode: 'MTR10',
          lineCode: 'MOTOR',
          termYears: 1,
          policyNumbers: [],
        },
      ]),
    );
    render(wrap(<IssuanceWorkbenchPage />));
    expect(await screen.findByText('Issued Client')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Upload E-policy' })).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'IA to Generate' }));
    await waitFor(() => expect(workbench).toHaveBeenLastCalledWith(1, 'IA_TO_GENERATE', '', 0));
  });
});
