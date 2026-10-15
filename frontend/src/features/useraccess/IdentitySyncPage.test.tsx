import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { identityApi } from '@/api/identity';
import { userAccessApi } from '@/api/userAccess';
import { ToastContext } from '@/components/ui/toastContext';
import IdentitySyncPage from './IdentitySyncPage';

const toast = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() };

function renderPage(tab = 'events') {
  render(
    <MemoryRouter initialEntries={[`/access/identity-sync?tab=${tab}`]}>
      <QueryClientProvider client={new QueryClient()}>
        <ToastContext.Provider value={toast}>
          <IdentitySyncPage />
        </ToastContext.Provider>
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('IdentitySyncPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('lists the events with their outcome and processes a refused one again', async () => {
    vi.spyOn(identityApi, 'settings').mockResolvedValue({
      provisioning: true,
      directoryName: 'Enterprise SSO simulator',
      simulator: true,
    });
    vi.spyOn(identityApi, 'events').mockResolvedValue({
      content: [
        {
          id: 41,
          receivedAt: '2026-10-09T01:00:00Z',
          source: 'UIDM_ISC',
          sourceLabel: 'UIDM-ISC',
          eventType: 'JOINER',
          windowsId: 'DOMAIN\\sit.transferee',
          userId: 'a013000901',
          status: 'REFUSED',
          message: 'Windows ID DOMAIN\\sit.transferee belongs to another user',
          attempts: 1,
          reprocessable: true,
        },
      ],
      page: 0,
      size: 25,
      totalElements: 1,
      totalPages: 1,
    });
    const reprocess = vi.spyOn(identityApi, 'reprocess').mockResolvedValue({
      id: 41,
      receivedAt: '2026-10-09T01:00:00Z',
      source: 'UIDM_ISC',
      sourceLabel: 'UIDM-ISC',
      eventType: 'JOINER',
      status: 'APPLIED',
      attempts: 2,
      reprocessable: false,
    });
    renderPage();
    expect(
      await screen.findByText('Windows ID DOMAIN\\sit.transferee belongs to another user'),
    ).toBeInTheDocument();
    expect(screen.getAllByText('Refused').length).toBeGreaterThan(1);
    expect(await screen.findByRole('tab', { name: 'Simulator' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Event 41/ }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Process again' }));
    expect(reprocess).toHaveBeenCalledWith(41);
  });

  it('shows the connection status and synchronises now', async () => {
    vi.spyOn(identityApi, 'settings').mockResolvedValue({
      provisioning: true,
      directoryName: 'UIDM-ISC',
      simulator: false,
    });
    vi.spyOn(userAccessApi, 'identityStatus').mockResolvedValue({
      lastCall: '2026-10-09T01:00:00Z',
      errorsLast24Hours: 2,
      refusedOpen: 1,
      mismatchesOpen: 3,
    });
    const sync = vi.spyOn(userAccessApi, 'synchroniseNow').mockResolvedValue({
      id: 1,
      jobName: 'UAM_IDENTITY_RECONCILIATION',
      trigger: 'MANUAL',
      triggeredBy: 'uamadmin',
      startedAt: '2026-10-09T02:00:00Z',
      status: 'SUCCEEDED',
      itemsProcessed: 10,
      message: '10 user(s) compared, 0 mismatch(es)',
    });
    renderPage('status');
    expect(await screen.findByText('Errors in the last 24 hours')).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: 'Simulator' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Synchronise Now' }));
    expect(sync).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith('10 user(s) compared, 0 mismatch(es)');
  });

  it('lists the open mismatches and resolves one with a remark', async () => {
    vi.spyOn(identityApi, 'settings').mockResolvedValue({
      provisioning: true,
      directoryName: 'UIDM-ISC',
      simulator: false,
    });
    vi.spyOn(userAccessApi, 'mismatches').mockResolvedValue([
      {
        id: 7,
        runDate: '2026-10-09',
        username: 'a100000001',
        kind: 'STATUS',
        detail: 'Active here, inactive in the directory',
        foundAt: '2026-10-09T01:40:00Z',
      },
    ]);
    const resolve = vi.spyOn(userAccessApi, 'resolveMismatch').mockResolvedValue(undefined);
    renderPage('reconciliation');
    expect(await screen.findByText('Status differs')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Mismatch of a100000001/ }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Resolve' }));
    await userEvent.type(screen.getByRole('textbox'), 'Corrected by UIDM-ISC');
    await userEvent.click(screen.getByRole('button', { name: 'Resolve' }));
    expect(resolve).toHaveBeenCalledWith(7, 'Corrected by UIDM-ISC');
  });
});
