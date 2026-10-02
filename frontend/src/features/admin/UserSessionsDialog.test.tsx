import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '@/api/auth';
import { resetUserDirectory, setUserDirectory } from '@/api/users';
import { ToastContext } from '@/components/ui/toastContext';
import { UserSessionsDialog } from './UserSessionsDialog';

describe('UserSessionsDialog', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    resetUserDirectory();
  });

  it('names the user in the title with the user ID, never the user ID alone', async () => {
    setUserDirectory([{ username: 'requestor', displayName: 'Rhea Access Requestor' }] as never);
    vi.spyOn(authApi, 'sessions').mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    });
    const toast = { success: vi.fn(), error: vi.fn(), info: vi.fn(), warning: vi.fn() };
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ToastContext.Provider value={toast as never}>
          <UserSessionsDialog username="requestor" onClose={vi.fn()} />
        </ToastContext.Provider>
      </QueryClientProvider>,
    );
    expect(
      await screen.findByText('Sessions of Rhea Access Requestor (requestor)'),
    ).toBeInTheDocument();
  });
});
