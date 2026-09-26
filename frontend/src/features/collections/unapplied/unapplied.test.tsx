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
import type { DispositionRule, UnappliedRow } from './api';
import { unappliedApi } from './api';
import { DispositionDialog } from './DispositionDialog';
import { UNAPPLIED_HELP } from './helpEntries';
import {
  cashieringStatus,
  dispositionErrors,
  listFilters,
  paymentFacts,
  requestStatuses,
  toDraft,
} from './labels';
import { UNAPPLIED_SCREENS } from './screens';
import UnappliedPaymentsPage from './UnappliedPaymentsPage';

const APPLY: DispositionRule = {
  code: 'FOR_APPLICATION_TO_INVOICE',
  label: 'For application to invoice',
  requiresInvoice: true,
  cashieringAction: 'APPLY_TO_INVOICE',
};
const NOTE: DispositionRule = {
  code: 'COORDINATE_FURTHER',
  label: 'Coordinate further',
  requiresInvoice: false,
  cashieringAction: 'NONE',
};
const PATTERN = '^(I\\d{8}|BI-.+)$';

const row: UnappliedRow = {
  unappliedRef: 'UNP-2026-000001',
  paymentDate: '2026-09-20',
  ageDays: 5,
  transactionNo: 'PAY-1',
  currency: 'PHP',
  amount: 1000,
  balance: 1000,
  paymentType: 'CASH',
  payor: 'Grace Villanueva',
  invoiceNo: 'BI-HO-2026-000001',
  cashieringTab: 'UNAPPLIED',
  cashieringStatus: 'REQUEST_QUEUED',
  account: { assuredName: 'Grace Villanueva', prBalance: 2500, segment: 'CBG' },
  dispositionCode: 'COORDINATE_FURTHER',
  dispositionBy: 'upphandler',
  dispositionAt: '2026-09-21T01:00:00Z',
};

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

afterEach(() => vi.restoreAllMocks());

describe('unapplied payments labels', () => {
  it('checks the invoice rules and the amount of a disposition', () => {
    const empty = { dispositionCode: '', invoiceNo: '', amount: '', remarks: '' };
    expect(dispositionErrors(empty, undefined, PATTERN, 100).dispositionCode).toBeDefined();
    const apply = { ...empty, dispositionCode: APPLY.code };
    expect(dispositionErrors(apply, APPLY, PATTERN, 100).invoiceNo).toMatch(/Enter/);
    expect(
      dispositionErrors({ ...apply, invoiceNo: 'X-1' }, APPLY, PATTERN, 100).invoiceNo,
    ).toMatch(/format/);
    expect(dispositionErrors({ ...apply, invoiceNo: 'I12345678' }, APPLY, PATTERN, 100)).toEqual(
      {},
    );
    expect(
      dispositionErrors({ ...apply, invoiceNo: 'BI-HO-1', amount: '150' }, APPLY, PATTERN, 100)
        .amount,
    ).toMatch(/at most 100.00/);
    expect(dispositionErrors({ ...empty, dispositionCode: NOTE.code }, NOTE, '(', 100)).toEqual({});
  });

  it('builds the request body, the list filters and the status pills', () => {
    expect(
      toDraft({ dispositionCode: 'X', invoiceNo: ' BI-1 ', amount: '12.5', remarks: ' ' }),
    ).toEqual({ dispositionCode: 'X', invoiceNo: 'BI-1', amount: 12.5, remarks: undefined });
    expect(listFilters('ALL', '', { segment: '', disposition: 'NONE', age: '8-30' })).toEqual({
      q: undefined,
      tab: undefined,
      segment: undefined,
      disposition: 'NONE',
      ageMin: 8,
      ageMax: 30,
    });
    expect(listFilters('UNAPPLIED', 'UNP', { segment: 'CBG', disposition: '', age: '' }).tab).toBe(
      'UNAPPLIED',
    );
    expect(requestStatuses('OPEN')).toEqual(['SENT', 'DEFERRED', 'ACCEPTED']);
    expect(requestStatuses('ALL')).toEqual([]);
    expect(cashieringStatus('REQUEST_QUEUED')).toBe('QUEUED');
    expect(cashieringStatus('MONITORING')).toBe('MONITORING');
    expect(cashieringStatus(undefined)).toBeUndefined();
    const facts = Object.fromEntries(paymentFacts(row));
    expect(facts['PR Balance']).toBe('2,500.00');
    expect(facts['Unit Head']).toBeUndefined();
  });

  it('has one help entry per menu screen, in menu order', () => {
    const menu = UNAPPLIED_SCREENS.filter((s) => s.hidden !== true).map((s) => s.path);
    expect(UNAPPLIED_HELP.map((h) => h.path)).toEqual(menu);
    expect(UNAPPLIED_SCREENS.every((s) => s.path.startsWith('/collections/unapplied'))).toBe(true);
  });
});

describe('unapplied payments screens', () => {
  it('asks for the invoice number before requesting an application', async () => {
    vi.spyOn(unappliedApi, 'rules').mockResolvedValue({
      rules: [APPLY, NOTE],
      invoicePattern: PATTERN,
    });
    const onSave = vi.fn();
    render(
      wrap(
        <DispositionDialog
          item={{ ...row, invoiceNo: undefined }}
          initialCode={APPLY.code}
          busy={false}
          error={undefined}
          onClose={() => undefined}
          onSave={onSave}
        />,
      ),
    );
    const confirm = await screen.findByRole('button', { name: 'Request Application' });
    await userEvent.click(confirm);
    expect(onSave).not.toHaveBeenCalled();
    expect(
      screen.getByText('Enter the invoice number to apply the payment to'),
    ).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/Invoice No/), 'BI-HO-2026-000009');
    await userEvent.click(confirm);
    expect(onSave).toHaveBeenCalledWith({
      dispositionCode: APPLY.code,
      invoiceNo: 'BI-HO-2026-000009',
      amount: undefined,
      remarks: undefined,
    });
  });

  it('lists the unapplied payments and opens the disposition of the selected one', async () => {
    vi.spyOn(unappliedApi, 'list').mockResolvedValue({
      content: [row],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    vi.spyOn(unappliedApi, 'rules').mockResolvedValue({
      rules: [APPLY, NOTE],
      invoicePattern: PATTERN,
    });
    const dispose = vi.spyOn(unappliedApi, 'dispose').mockResolvedValue({
      id: 1,
      unappliedRef: row.unappliedRef,
      dispositionCode: NOTE.code,
      cashieringAction: 'NONE',
      createdBy: 'upphandler',
      createdAt: '2026-09-25T01:00:00Z',
    });
    render(wrap(<UnappliedPaymentsPage />));
    expect(await screen.findByText('UNP-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('Queued')).toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Select UNP-2026-000001'));
    await userEvent.click(screen.getByRole('button', { name: 'Record Disposition' }));
    await userEvent.selectOptions(
      await screen.findByRole('combobox', { name: 'Disposition' }),
      NOTE.code,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Save Disposition' }));
    await waitFor(() => expect(dispose).toHaveBeenCalled());
    await userEvent.click(screen.getByRole('button', { name: 'Filters' }));
    expect(screen.getByLabelText('Age')).toBeInTheDocument();
  });
});
