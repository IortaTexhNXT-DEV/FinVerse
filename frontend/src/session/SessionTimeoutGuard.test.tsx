import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '@/api/auth';
import { systemApi } from '@/api/system';
import type { SessionPolicy } from '@/api/system';
import { AuthContext } from '@/auth/authContext';
import type { AuthState } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { SessionTimeoutGuard } from './SessionTimeoutGuard';
import { clearTimedOut, hasTimedOut } from './timedOut';

const toast = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() };

function renderGuard(policy: SessionPolicy, logout: AuthState['logout']) {
  vi.spyOn(systemApi, 'sessionPolicy').mockResolvedValue(policy);
  const auth = { logout } as unknown as AuthState;
  render(
    <QueryClientProvider client={new QueryClient()}>
      <ToastContext.Provider value={toast}>
        <AuthContext.Provider value={auth}>
          <SessionTimeoutGuard />
        </AuthContext.Provider>
      </ToastContext.Provider>
    </QueryClientProvider>,
  );
}

async function advance(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}

describe('SessionTimeoutGuard', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: false });
    clearTimedOut();
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    clearTimedOut();
  });

  it("shows BDOI's inactivity warning, records it once and opens the timed-out page", async () => {
    const report = vi.spyOn(authApi, 'reportInactivity').mockResolvedValue(undefined);
    const logout = vi.fn();
    renderGuard(
      {
        timeoutMinutes: 2,
        warningSeconds: 60,
        idleWarningMinutes: 1,
        bdoiDialog: true,
        timeoutPage: true,
      },
      logout,
    );
    await advance(10);
    await advance(61_000);
    expect(
      screen.getByText(
        'You have been inactive for 1 minute. For your security, your session is about to expire. Would you like to stay logged in or log out?',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Stay Logged In' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Log Out' })).toBeInTheDocument();
    await advance(5_000);
    expect(report).toHaveBeenCalledTimes(1);
    await advance(60_000);
    expect(logout).toHaveBeenCalledWith('IDLE_TIMEOUT');
    expect(hasTimedOut()).toBe(true);
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('keeps the earlier wording and the sign-in message when the settings are off', async () => {
    vi.spyOn(authApi, 'reportInactivity').mockResolvedValue(undefined);
    const logout = vi.fn();
    renderGuard(
      {
        timeoutMinutes: 2,
        warningSeconds: 60,
        idleWarningMinutes: 1,
        bdoiDialog: false,
        timeoutPage: false,
      },
      logout,
    );
    await advance(10);
    await advance(61_000);
    expect(screen.getByRole('button', { name: 'Stay Signed In' })).toBeInTheDocument();
    await advance(61_000);
    expect(logout).toHaveBeenCalledWith('IDLE_TIMEOUT');
    expect(hasTimedOut()).toBe(false);
    expect(toast.error).toHaveBeenCalledWith('You were signed out after 2 minutes of inactivity.');
  });
});
