import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { roleHomesApi } from '@/api/roleHomes';
import type { RoleHome } from '@/api/roleHomes';
import { AuthContext } from '@/auth/authContext';
import type { AuthState } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import RoleHomeSetupPage from './RoleHomeSetupPage';
import {
  moveWidget,
  roleHomeActions,
  roleHomeStatus,
  toggleWidget,
  widgetNames,
} from './roleHomeRows';

const toast = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() };
const CATALOGUE = [
  { code: 'KPIS', name: 'Headline figures' },
  { code: 'WORKLOAD', name: 'My workload' },
  { code: 'CASH', name: 'Cash position' },
];
const CASHIER: RoleHome = {
  roleCode: 'CASHIER',
  roleName: 'Cashier',
  widgets: ['KPIS', 'WORKLOAD', 'CASH'],
  delivered: true,
};
const PENDING: RoleHome = {
  ...CASHIER,
  pendingWidgets: ['CASH'],
  pendingReason: 'Counter first',
  pendingBy: 'admin',
  pendingAt: '2026-10-15T01:00:00Z',
};

function renderPage(permissions: string[], username = 'admin') {
  render(
    <MemoryRouter>
      <QueryClientProvider client={new QueryClient()}>
        <ToastContext.Provider value={toast}>
          <AuthContext.Provider
            value={
              {
                user: { username },
                can: (p: string) => permissions.includes(p),
              } as unknown as AuthState
            }
          >
            <RoleHomeSetupPage />
          </AuthContext.Provider>
        </ToastContext.Provider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('roleHomeRows', () => {
  it('orders, ticks and names the widgets', () => {
    expect(moveWidget(['A', 'B', 'C'], 2, -1)).toEqual(['A', 'C', 'B']);
    expect(moveWidget(['A', 'B'], 0, -1)).toEqual(['A', 'B']);
    expect(toggleWidget(['A'], 'B')).toEqual(['A', 'B']);
    expect(toggleWidget(['A', 'B'], 'A')).toEqual(['B']);
    expect(widgetNames(['CASH', 'X'], CATALOGUE)).toBe('Cash position, X');
  });

  it('gives the maker and the checker their actions', () => {
    const choose = vi.fn();
    const maker = { username: 'admin', mayManage: true, mayApprove: false };
    const checker = { username: 'infosec', mayManage: false, mayApprove: true };
    expect(roleHomeStatus(CASHIER).label).toBe('As Delivered');
    expect(roleHomeStatus({ ...CASHIER, delivered: false }).label).toBe('Set');
    expect(roleHomeStatus(PENDING).label).toBe('Change Pending');
    expect(roleHomeActions(CASHIER, maker, choose).map((a) => a.label)).toEqual(['Change Widgets']);
    expect(roleHomeActions(CASHIER, checker, choose)).toEqual([]);
    expect(roleHomeActions(PENDING, maker, choose).map((a) => a.label)).toEqual([
      'Withdraw Request',
    ]);
    const decide = roleHomeActions(PENDING, checker, choose);
    expect(decide.map((a) => a.label)).toEqual(['Approve', 'Reject']);
    decide[0]?.onSelect();
    expect(choose).toHaveBeenCalledWith({ kind: 'approve', home: PENDING });
  });
});

describe('RoleHomeSetupPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('sends the chosen widgets in their order for approval', async () => {
    vi.spyOn(roleHomesApi, 'list').mockResolvedValue([CASHIER]);
    vi.spyOn(roleHomesApi, 'widgets').mockResolvedValue(CATALOGUE);
    const request = vi.spyOn(roleHomesApi, 'request').mockResolvedValue(PENDING);
    renderPage(['ROLE_HOME_MANAGE']);
    const user = userEvent.setup();

    expect(await screen.findByText('Headline figures, My workload, Cash position')).toBeVisible();
    await user.click(screen.getByRole('button', { name: /actions.*cashier|cashier/i }));
    await user.click(await screen.findByRole('menuitem', { name: 'Change Widgets' }));
    await user.click(screen.getByRole('checkbox', { name: 'Headline figures' }));
    await user.click(screen.getByRole('button', { name: 'Move Cash position up' }));
    await user.click(screen.getByRole('button', { name: 'Send for Approval' }));
    expect(screen.getByText('Enter the reason for the change')).toBeVisible();
    expect(request).not.toHaveBeenCalled();

    await user.type(screen.getByLabelText(/Reason/), 'Counter first');
    await user.click(screen.getByRole('button', { name: 'Send for Approval' }));
    expect(request).toHaveBeenCalledWith('CASHIER', ['CASH', 'WORKLOAD'], 'Counter first');
  });

  it('shows the change that waits for approval', async () => {
    vi.spyOn(roleHomesApi, 'list').mockResolvedValue([PENDING]);
    vi.spyOn(roleHomesApi, 'widgets').mockResolvedValue(CATALOGUE);
    renderPage(['ROLE_HOME_APPROVE'], 'infosec');
    expect(await screen.findByText('Change Pending')).toBeVisible();
    expect(screen.getByText(/1 home page change wait/)).toBeVisible();
  });
});
