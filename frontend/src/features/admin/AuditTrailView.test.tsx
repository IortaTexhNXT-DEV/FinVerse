import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { adminApi } from '@/api/admin';
import { api } from '@/api/client';
import { AuthContext } from '@/auth/authContext';
import type { AuthState } from '@/auth/authContext';
import { ToastContext } from '@/components/ui/toastContext';
import { AuditTrailView } from './AuditTrailView';

// A documentation address (RFC 5737 range), never a real host.
const ADDRESS = ['192', '0', '2', '15'].join('.');
const toast = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() };

describe('AuditTrailView', () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows BDOI's audit columns with the role and the source address", async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    const audit = vi.spyOn(adminApi, 'audit').mockResolvedValue({
      content: [
        {
          id: 1,
          occurredAt: '2026-10-09T01:00:00Z',
          username: 'auditor',
          entityType: 'AppUser',
          entityId: 'auditor',
          action: 'TIMEOUT',
          actionLabel: 'Timeout',
          summary: 'Session timed out after inactivity',
          module: 'User Access Maintenance',
          windowsId: 'BDO\\aud01',
          roleNames: 'Auditor',
          ipAddress: ADDRESS,
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    render(
      <MemoryRouter>
        <QueryClientProvider client={new QueryClient()}>
          <ToastContext.Provider value={toast}>
            <AuthContext.Provider value={{ can: () => true } as unknown as AuthState}>
              <AuditTrailView section="Administration" title="Audit Trail" description="d" />
            </AuthContext.Provider>
          </ToastContext.Provider>
        </QueryClientProvider>
      </MemoryRouter>,
    );
    expect(await screen.findByText('Session timed out after inactivity')).toBeInTheDocument();
    ['Timestamp', 'Module', 'User Id', 'Performed By', 'Role', 'Action', 'IP Address'].forEach(
      (h) => expect(screen.getAllByText(h).length).toBeGreaterThan(0),
    );
    expect(screen.getByText(ADDRESS)).toBeInTheDocument();
    expect(screen.getByText('BDO\\aud01')).toBeInTheDocument();
    await waitFor(() =>
      expect(audit).toHaveBeenCalledWith(
        expect.objectContaining({ sort: 'occurredAt', direction: 'desc' }),
      ),
    );
  });
});
