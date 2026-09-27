import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/client';
import type { Company } from '@/api/types';
import { workflowApi } from '@/api/workflow';
import { AuthContext } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { WorkspaceContext } from '@/context/workspaceContext';
import { coverApi } from '../cover/api';
import CoverLookupPage from '../cover/CoverLookupPage';
import LocationRefsPage from '../location/LocationRefsPage';
import { locationApi } from '../location/api';
import type { Claim } from './api';
import { claimApi } from './api';
import ClaimPage from './ClaimPage';
import RecordClaimPage from './RecordClaimPage';

const OFFICER = new Set(['BCL_VIEW', 'BCL_COVER_VIEW', 'BCL_RECORD', 'BCL_AUTHORIZE']);

function wrap(children: ReactNode, path = '/', permissions: ReadonlySet<string> = OFFICER) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve(),
    logout: () => undefined,
    can: (p: string) => permissions.has(p),
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
    <MemoryRouter initialEntries={[path]}>
      <QueryClientProvider client={queries}>
        <AuthContext.Provider value={auth}>
          <WorkspaceContext.Provider value={workspace}>
            <ToastContext.Provider value={{ success: vi.fn(), error: vi.fn() }}>
              <Routes>
                <Route path="/claims-handling/:id" element={children} />
                <Route path="*" element={children} />
              </Routes>
            </ToastContext.Provider>
          </WorkspaceContext.Provider>
        </AuthContext.Provider>
      </QueryClientProvider>
    </MemoryRouter>
  );
}

const CLAIM: Claim = {
  id: 7,
  claimNo: 'BCL-2026-000007',
  source: 'BDOI_NOTICE',
  handler: 'clmofficer',
  unitLabel: 'Motor HO',
  cover: {
    arn: 'ARN-2026-000602',
    policyYear: 1,
    policyNo: 'FI-2026-1',
    versionNo: 1,
    versionLabel: 'Cover v1 (END-1)',
    latestVersionNo: 2,
    assuredName: 'Acme Corp',
    salesTeam: 'T-CBG1',
    accountOfficer: 'ao',
    currency: 'USD',
  },
  premium: {
    live: {
      status: 'UNPAID',
      blocking: true,
      unpaid: [
        {
          invoiceNo: 'I-1',
          kind: 'ENDORSEMENT_PLUS',
          paymentStatus: 'UNPAID',
          balance: 18400,
          currency: 'PHP',
        },
      ],
    },
    dpPolicy: 'CONFIRM',
    canAuthorize: false,
    unremittedInvoices: ['I-1'],
  },
  loss: {
    lossDate: '2026-09-20',
    reportedDate: '2026-09-21',
    lossNatureLabel: 'Fire',
    claimTypeLabel: 'Property',
    lossDescription: 'Roof damage',
    catastropheLabel: 'Typhoon',
    catastropheEvent: 'Kristine',
    claimantName: 'Acme Corp',
    claimantOverridden: false,
  },
  progress: { phase: 'NEW', followUpOverridden: false, statusLabel: 'Newly filed' },
  flags: {
    unpaidPremium: true,
    awaitingPremiumRemittance: true,
    newerCoverVersion: true,
    multiLocation: true,
    multiInsurer: true,
    catastrophe: true,
    claimantOverridden: false,
  },
  locationCount: 2,
  insurerCount: 2,
  totalReserve: 150000,
  createdBy: 'clmofficer',
  createdAt: '2026-09-21T01:00:00Z',
};

afterEach(() => vi.restoreAllMocks());

describe('Claims Handling screens of wave CL1-A', () => {
  it('Cover Lookup asks for three characters and says when nothing matches', async () => {
    const search = vi.spyOn(coverApi, 'search').mockResolvedValue([]);
    render(wrap(<CoverLookupPage />));
    const input = screen.getByLabelText(/Search Text/);
    await userEvent.type(input, 'MC');
    await userEvent.click(screen.getByRole('button', { name: /Search/ }));
    expect(screen.getByText('Enter at least 3 characters')).toBeInTheDocument();
    expect(search).not.toHaveBeenCalled();
    await userEvent.clear(input);
    await userEvent.type(input, 'ZZZ-NO-MATCH');
    await userEvent.click(screen.getByRole('button', { name: /Search/ }));
    expect(await screen.findByText('No cover found for ZZZ-NO-MATCH')).toBeInTheDocument();
  });

  it('Record Claim starts with the cover search', () => {
    render(wrap(<RecordClaimPage />));
    expect(screen.getByRole('heading', { name: 'Record Claim' })).toBeInTheDocument();
    expect(screen.getByText('Find the Cover')).toBeInTheDocument();
  });

  it('shows a claim with its flags, premium block and special remittance link', async () => {
    vi.spyOn(claimApi, 'get').mockResolvedValue(CLAIM);
    vi.spyOn(workflowApi, 'byRecord').mockRejectedValue(new ApiError(404, { code: 'NOT_FOUND' }));
    render(wrap(<ClaimPage />, '/claims-handling/7'));
    expect(await screen.findByRole('heading', { name: 'BCL-2026-000007' })).toBeInTheDocument();
    for (const flag of ['Unpaid premium', 'Newer cover version', 'Multi-location', 'CAT']) {
      expect(screen.getAllByText(flag).length).toBeGreaterThan(0);
    }
    expect(screen.getByRole('button', { name: /Generate Authorization Code/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /Use Latest Version/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'I-1' })).toHaveAttribute(
      'href',
      '/remittance/special?invoiceNo=I-1&condition=CLAIMS',
    );
    expect(screen.getByText('Roof damage')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Override Claimant/ })).not.toBeInTheDocument();
  });

  it('lists the insurer location references', async () => {
    vi.spyOn(locationApi, 'refs').mockResolvedValue({
      content: [
        {
          id: 1,
          arn: 'ARN-2026-940002',
          itemNo: 1,
          insurerCode: 'INS-MGIC',
          reference: 'MGIC-LOC-0091',
          effectiveFrom: '2026-09-15',
          createdBy: 'clmofficer',
          createdAt: '2026-09-15T01:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    render(wrap(<LocationRefsPage />));
    await waitFor(() => expect(screen.getByText('MGIC-LOC-0091')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /New Reference/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Save Reference' }));
    expect(screen.getByText('Enter the insurer location reference')).toBeInTheDocument();
  });
});
