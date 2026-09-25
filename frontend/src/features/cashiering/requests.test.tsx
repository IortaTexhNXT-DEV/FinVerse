import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Company } from '@/api/types';
import { AuthContext } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { WorkspaceContext } from '@/context/workspaceContext';
import { cashieringApi } from './cashieringApi';
import CashieringRequestsPage from './CashieringRequestsPage';
import type { CollectorRequest, PaymentReversal, RefundValidation } from './requestsApi';
import { acceptBody, acceptErrors, requestsApi } from './requestsApi';

const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
});

const request: CollectorRequest = {
  id: 7,
  requestNo: 'CRQ-2026-000007',
  unappliedId: 3,
  unappliedRef: 'UNP-2026-000003',
  currency: 'PHP',
  balance: 500,
  payorName: 'Mega Traders Inc.',
  action: 'RECLASS',
  requestedBy: 'clxhandler',
  requestedAt: '2026-09-25T01:00:00Z',
  source: 'COLLECTIONS',
  sourceRef: 'CLX-UPP-1',
  status: 'QUEUED',
};

const validation: RefundValidation = {
  id: 4,
  taskNo: 'RVL-2026-000004',
  sourceModule: 'PAYREQUEST',
  sourceRef: 'RRF-1#1:1',
  invoiceNo: 'BI-HO-2026-000004',
  arNo: 'AR-HO-1',
  currency: 'PHP',
  amount: 300,
  requestedBy: 'mktao',
  requestedAt: '2026-09-25T01:00:00Z',
  status: 'OPEN',
};

const reversal: PaymentReversal = {
  id: 9,
  requestNo: 'PRV-2026-000009',
  sourceModule: 'ACSL',
  sourceRef: 'ACS-2026-000001',
  invoiceNo: 'BI-HO-2026-000009',
  receiptNo: 'AR-HO-9',
  valueDate: '2026-09-25',
  requestedBy: 'acsl',
  requestedAt: '2026-09-25T01:00:00Z',
  status: 'SUBMITTED',
};

function wrap(children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve(),
    logout: () => undefined,
    can: () => true,
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

function mockQueues() {
  vi.spyOn(requestsApi, 'counts').mockResolvedValue({
    collectorRequests: 1,
    refundValidations: 1,
    paymentReversals: 1,
  });
  vi.spyOn(requestsApi, 'collectorRequests').mockResolvedValue(page([request]));
  vi.spyOn(requestsApi, 'validations').mockResolvedValue(page([validation]));
  vi.spyOn(requestsApi, 'reversals').mockResolvedValue(page([reversal]));
  vi.spyOn(cashieringApi, 'dispositionTypes').mockResolvedValue([
    { code: 'RECLASS', action: 'RECLASS', requiresApproval: true, description: 'Reclass' },
  ]);
}

afterEach(() => vi.restoreAllMocks());

describe('incoming requests', () => {
  it('checks the acceptance form', () => {
    const form = {
      dispositionType: 'RECLASS',
      amount: '',
      targetClientCode: '',
      targetUnit: '',
      payeeName: '',
      remarks: ' ok ',
      submit: false,
    };
    expect(acceptErrors(request, form).targetClientCode).toBeDefined();
    expect(acceptErrors({ ...request, action: 'TRANSFER' }, form).targetUnit).toBeDefined();
    expect(
      acceptErrors(request, { ...form, targetClientCode: 'CL-1', amount: '900' }).amount,
    ).toBeDefined();
    expect(acceptErrors({ ...request, action: 'REFUND' }, form)).toEqual({});
    expect(acceptBody({ ...form, amount: '10' })).toEqual({
      dispositionType: 'RECLASS',
      amount: 10,
      targetClientCode: undefined,
      targetUnit: undefined,
      payeeName: undefined,
      remarks: 'ok',
      submit: false,
    });
  });

  it('accepts a collector request with the client to reclass to', async () => {
    mockQueues();
    const accept = vi.spyOn(requestsApi, 'accept').mockResolvedValue({
      id: 1,
      dispositionType: 'RECLASS',
      action: 'RECLASS',
      amount: 500,
      status: 'MONITORING',
      requestedBy: 'cashier',
      requestedAt: '2026-09-25T01:00:00Z',
    });
    render(wrap(<CashieringRequestsPage />));
    expect(await screen.findByText('CRQ-2026-000007')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Select CRQ-2026-000007'));
    await userEvent.click(screen.getByRole('button', { name: 'Accept Request' }));
    await screen.findByRole('dialog', { name: 'Accept CRQ-2026-000007' });
    const inDialog = screen.getAllByRole('button', { name: 'Accept Request' });
    await userEvent.click(inDialog[inDialog.length - 1]!);
    expect(screen.getByText('Enter the client to reclass to')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/Client to Reclass To/), 'CL-2026-000001');
    const buttons = screen.getAllByRole('button', { name: 'Accept Request' });
    await userEvent.click(buttons[buttons.length - 1]!);
    await waitFor(() =>
      expect(accept).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ targetClientCode: 'CL-2026-000001' }),
      ),
    );
  });

  it('rejects a refund validation and approves a payment reversal', async () => {
    mockQueues();
    const reject = vi.spyOn(requestsApi, 'rejectValidation').mockResolvedValue({
      ...validation,
      status: 'REJECTED',
    });
    const approve = vi
      .spyOn(requestsApi, 'approveReversal')
      .mockResolvedValue({ ...reversal, status: 'APPROVED' });
    render(wrap(<CashieringRequestsPage />));
    await userEvent.click(await screen.findByRole('tab', { name: /Refund Validations/ }));
    await userEvent.click(await screen.findByLabelText('Select RVL-2026-000004'));
    await userEvent.click(screen.getByRole('button', { name: 'Reject Validation' }));
    await userEvent.type(await screen.findByLabelText(/Reason/), 'Not reinstated');
    await userEvent.click(screen.getByRole('button', { name: 'Reject' }));
    await waitFor(() => expect(reject).toHaveBeenCalledWith(4, 'Not reinstated'));

    await userEvent.click(screen.getByRole('tab', { name: /Payment Reversals/ }));
    await userEvent.click(await screen.findByLabelText('Select PRV-2026-000009'));
    await userEvent.click(screen.getByRole('button', { name: 'Approve Reversal' }));
    await waitFor(() => expect(approve).toHaveBeenCalledWith(9));
  });
});
