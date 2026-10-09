import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '@/api/auth';
import { hasTimedOut, markTimedOut } from '@/session/timedOut';
import SessionTimedOutPage from './SessionTimedOutPage';

describe('SessionTimedOutPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('tells the user the session timed out and the Log In button opens the Login page', async () => {
    vi.spyOn(authApi, 'signInOptions').mockResolvedValue({
      mode: 'LOCAL',
      singleSignOn: false,
      passwordSignIn: true,
      passwordReset: false,
      environment: 'sit',
    });
    vi.spyOn(authApi, 'ssoSignOut').mockResolvedValue({ redirectUrl: null });
    markTimedOut();
    render(
      <MemoryRouter initialEntries={['/session-timed-out']}>
        <QueryClientProvider client={new QueryClient()}>
          <Routes>
            <Route path="/session-timed-out" element={<SessionTimedOutPage />} />
            <Route path="/login" element={<p>Login page</p>} />
          </Routes>
        </QueryClientProvider>
      </MemoryRouter>,
    );
    expect(
      screen.getByText('Your session timed out. Please log in again to continue'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Log In' }));
    expect(await screen.findByText('Login page')).toBeInTheDocument();
    expect(hasTimedOut()).toBe(false);
  });
});
