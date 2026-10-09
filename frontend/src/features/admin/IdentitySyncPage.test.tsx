import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { identityApi } from '@/api/identity';
import { ToastContext } from '@/components/ui/toastContext';
import IdentitySyncPage from './IdentitySyncPage';

const toast = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() };

function renderPage() {
  render(
    <MemoryRouter>
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
    expect(
      await screen.findByRole('tab', { name: 'Enterprise SSO Simulator' }),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /Event 41/ }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Process again' }));
    expect(reprocess).toHaveBeenCalledWith(41);
  });
});
