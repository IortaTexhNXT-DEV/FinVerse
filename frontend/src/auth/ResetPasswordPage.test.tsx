import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '@/api/auth';
import { BRAND } from '@/branding';
import ResetPasswordPage from './ResetPasswordPage';

describe('ResetPasswordPage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('names the system as every sign-in page does, never an internal product name', async () => {
    vi.spyOn(authApi, 'checkReset').mockResolvedValue({ expiresAt: '2026-10-02T12:28:00Z' });
    vi.spyOn(authApi, 'signInOptions').mockResolvedValue({
      mode: 'LOCAL',
      singleSignOn: false,
      passwordSignIn: true,
      passwordReset: true,
      environment: 'uat',
    });
    render(
      <MemoryRouter initialEntries={['/reset-password?token=t']}>
        <QueryClientProvider client={new QueryClient()}>
          <ResetPasswordPage />
        </QueryClientProvider>
      </MemoryRouter>,
    );
    expect((await screen.findAllByText(BRAND.productName)).length).toBeGreaterThan(0);
    expect(screen.queryByText(/BrokerVerse/)).toBeNull();
    expect(await screen.findByText('UAT environment')).toBeInTheDocument();
  });
});
